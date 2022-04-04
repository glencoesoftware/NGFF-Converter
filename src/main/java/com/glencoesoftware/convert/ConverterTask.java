package com.glencoesoftware.convert;

import ch.qos.logback.classic.Level;
import com.glencoesoftware.bioformats2raw.Converter;
import com.glencoesoftware.pyramid.PyramidFromDirectoryWriter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.glencoesoftware.convert.PrimaryController.jobStatus.*;

class ConverterTask extends Task<Integer> {
    private final List<String> args;
    private final ListView<IOPackage> inputFileList;
    private final Label statusBox;
    private final PrimaryController parent;
    public boolean interrupted;
    private static final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(ConverterTask.class);

    public ConverterTask(List<String> args, PrimaryController parent) {
        LOGGER.setLevel(Level.DEBUG);
        this.parent = parent;
        this.args = args;
        this.inputFileList = parent.inputFileList;
        this.statusBox = parent.statusBox;
        this.interrupted = false;
    }

    @Override
    protected Integer call() throws IOException {
        RunnerParameterExceptionHandler paramHandler = new RunnerParameterExceptionHandler();
        RunnerExecutionExceptionHandler runHandler = new RunnerExecutionExceptionHandler();
        int count = 0;
        for (IOPackage job : inputFileList.getItems()) {
            CommandLine runner = new CommandLine(new Converter());
            runner.setParameterExceptionHandler(paramHandler);
            runner.setExecutionExceptionHandler(runHandler);
            File in = job.fileIn;
            File out;
            if (this.interrupted || job.status == COMPLETED || job.status == ERROR) {
                continue;
            }

            job.status = RUNNING;
            Platform.runLater(() -> {
                statusBox.setText("Working on " + in.getName());
                LOGGER.info("Working on " + in.getName());
                inputFileList.refresh();
            });

            ArrayList<String> params;
            if (job.outputMode == PrimaryController.OutputMode.TIFF) {
                LOGGER.info("Will convert to NGFF first");
                out = new File(System.getProperty("java.io.tmpdir") + UUID.randomUUID() + ".zarr");
                Set<String> validArgs = runner.getCommandSpec().optionsMap().keySet();
                List<String> argsToUse = args.stream().filter(
                        (arg) -> validArgs.contains(arg.split("=")[0])).toList();
                params = new ArrayList<>(argsToUse);
            } else {
                out = job.fileOut;
                params = new ArrayList<>(args);
            }

            // Construct args list
            params.add(0, out.getAbsolutePath());
            params.add(0, in.getAbsolutePath());
            String[] fullParams = params.toArray(new String[args.size()]);


            int result;
            if (in.getAbsolutePath().endsWith(".zarr")) {
                // Input file is already a zarr, no need to run Phase 1.
                result = 0;
                out = job.fileIn;
            } else {
                LOGGER.info("Executing bioformats2raw with args " + Arrays.toString(fullParams));
                result = runner.execute(fullParams);
            }

            if (result == 0 && job.outputMode == PrimaryController.OutputMode.TIFF)  {
                LOGGER.info("NGFF intermediate generated, converting to TIFF");
                CommandLine converter = new CommandLine(new PyramidFromDirectoryWriter());
                converter.setParameterExceptionHandler(paramHandler);
                converter.setExecutionExceptionHandler(runHandler);
                Set<String> validConverterArgs = converter.getCommandSpec().optionsMap().keySet();
                List<String> convertArgs = args.stream().filter(
                        (arg) -> validConverterArgs.contains(arg.split("=")[0])).toList();
                ArrayList<String> convertParams = new ArrayList<>(convertArgs);
                convertParams.add(0, job.fileOut.getAbsolutePath());
                convertParams.add(0, out.getAbsolutePath());
                String[] phase2Params = convertParams.toArray(new String[0]);
                LOGGER.info("Executing raw2ometiff with args " + Arrays.toString(phase2Params));
                result = converter.execute(phase2Params);
                LOGGER.info("Cleaning up intermediate files");
                if (out != job.fileIn) {
                    FileUtils.deleteDirectory(out);
                }
            }

            if (this.interrupted && result != 0) {
                job.status = FAILED;
                LOGGER.info("User aborted job: " + job.fileOut.getName() + "\n");
            } else if (result == 0 && job.fileOut.exists()) {
                job.status = COMPLETED;
                LOGGER.info("Successfully created: " + job.fileOut.getName() + "\n");
            } else if (result == 0) {
                job.status = NOOUTPUT;
                LOGGER.warn("Job completed but no output detected: " + job.fileOut.getName() + "\n");
            } else if (result == 9) {
                job.status = FAILED;
                LOGGER.error("Input arguments were invalid for: " + job.fileOut.getName() + "\n");
            } else {
                job.status = FAILED;
                LOGGER.info("Failed with Exit Code " + result +  " : " + job.fileOut.getName() + "\n");
            }
            Platform.runLater(inputFileList::refresh);
            if (result == 0) { count++; } else if (!this.parent.logShown) {
                Platform.runLater(this.parent::displayLog);
            }
        }
        String finalStatus = String.format("Completed conversion of %s files.", count);
        Platform.runLater(() -> {
            statusBox.setText(finalStatus);
            LOGGER.info(finalStatus);
            parent.runCompleted();
        });
        return 0;
    }

    private static class RunnerExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {
        @Override
        public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult parsed)  {
            LOGGER.error(ex.toString());
            if (commandLine.getExecutionResult() != null) {
                return commandLine.getExecutionResult();
            }
            return 1;
        }
    }

    private static class RunnerParameterExceptionHandler implements CommandLine.IParameterExceptionHandler {
        @Override
        public int handleParseException(CommandLine.ParameterException ex, String[] args) {
            LOGGER.error(ex.toString());
            return 9;
        }
    }
}