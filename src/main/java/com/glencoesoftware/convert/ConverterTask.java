package com.glencoesoftware.convert;

import ch.qos.logback.classic.Level;
import com.glencoesoftware.bioformats2raw.Converter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    protected Integer call() {
        RunnerParameterExceptionHandler paramHandler = new RunnerParameterExceptionHandler();
        RunnerExecutionExceptionHandler runHandler = new RunnerExecutionExceptionHandler();
        int count = 0;
        for (IOPackage job : inputFileList.getItems()) {
            CommandLine runner = new CommandLine(new Converter());
            runner.setParameterExceptionHandler(paramHandler);
            runner.setExecutionExceptionHandler(runHandler);
            File in = job.fileIn;
            File out = job.fileOut;
            if (this.interrupted || job.status == COMPLETED || job.status == ERROR) {
                continue;
            }

            job.status = RUNNING;
            Platform.runLater(() -> {
                statusBox.setText("Working on " + in.getName());
                LOGGER.info("Working on " + in.getName());
                inputFileList.refresh();
            });

            // Construct args list
            ArrayList<String> params = new ArrayList<>(args);
            params.add(0, out.getAbsolutePath());
            params.add(0, in.getAbsolutePath());
            String[] fullParams = params.toArray(new String[args.size()]);
            LOGGER.info("Executing with args " + Arrays.toString(fullParams));

            int result = runner.execute(fullParams);

            if (this.interrupted && result != 0) {
                job.status = FAILED;
                LOGGER.info("User aborted job: " + out.getName() + "\n");
            } else if (result == 0 && out.exists()) {
                job.status = COMPLETED;
                LOGGER.info("Successfully created: " + out.getName() + "\n");
            } else if (result == 0) {
                job.status = NOOUTPUT;
                LOGGER.warn("Job completed but no output detected: " + out.getName() + "\n");
            } else if (result == 9) {
                job.status = FAILED;
                LOGGER.error("Input arguments were invalid for: " + out.getName() + "\n");
            } else {
                job.status = FAILED;
                LOGGER.info("Failed with Exit Code " + result +  " : " + out.getName() + "\n");
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
            return commandLine.getExecutionResult();
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