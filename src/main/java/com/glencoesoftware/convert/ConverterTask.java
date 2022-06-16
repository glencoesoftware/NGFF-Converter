package com.glencoesoftware.convert;

import ch.qos.logback.classic.Level;
import com.glencoesoftware.bioformats2raw.Converter;
import com.glencoesoftware.pyramid.PyramidFromDirectoryWriter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.glencoesoftware.convert.PrimaryController.jobStatus.*;

class ConverterTask extends Task<Integer> {
    private final List<String> args;
    private final ListView<IOPackage> inputFileList;
    private final Label statusBox;
    private final PrimaryController parent;
    private final boolean splitSeries;
    public boolean interrupted;
    private static final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(ConverterTask.class);

    public ConverterTask(List<String> args, PrimaryController parent) {
        LOGGER.setLevel(Level.DEBUG);
        this.parent = parent;
        this.args = args;
        this.inputFileList = parent.inputFileList;
        this.statusBox = parent.statusBox;
        this.splitSeries = parent.wantSplit.isSelected();
        this.interrupted = false;
    }

    @Override
    protected Integer call() throws IOException, ServiceException, DependencyException, FormatException {
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

            List<Integer> subJobs = List.of(-1);
            int numSeries = 0;
            if (job.outputMode == PrimaryController.OutputMode.TIFF &&
                    this.splitSeries && !in.getAbsolutePath().endsWith(".zarr")) {
                numSeries = getSeriesCount(in.getAbsolutePath());
                subJobs = IntStream.range(0, numSeries).boxed().collect(Collectors.toList());
            }
            File destFile = job.fileOut;
            int result = -1;
            for (Integer seriesIdx : subJobs) {
                if (this.interrupted) { continue; }
                if (numSeries > 1) {
                    // Show progress by series, but don't move to 'Done' status.
                    job.progress = Math.min((seriesIdx + 1.0 / numSeries), 0.99);
                }
                Platform.runLater(inputFileList::refresh);
                ArrayList<String> params;
                if (job.outputMode == PrimaryController.OutputMode.TIFF) {
                    LOGGER.info("Will convert to NGFF first");
                    String temporaryStorage;
                    if (parent.tempDirectory != null) {
                        temporaryStorage = parent.tempDirectory.getAbsolutePath();
                    } else {
                        temporaryStorage = job.fileOut.getParent();
                    }
                    out = new File(Paths.get(temporaryStorage, UUID.randomUUID() + ".zarr").toString());
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

                if (seriesIdx != -1) {
                    LOGGER.info("Working on series " + seriesIdx);
                    params.add("--series=" + seriesIdx);
                    String outName = job.fileOut.getName();
                    String outDir = job.fileOut.getParent();
                    int insertionIndex = outName.toLowerCase().indexOf(".ome.tif");
                    outName = new StringBuilder(outName).insert(insertionIndex, "_" + seriesIdx).toString();
                    destFile = new File(outDir, outName);
                    LOGGER.info("Output file will be " + destFile.getName());
                }

                String[] fullParams = params.toArray(new String[args.size()]);

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
                    convertParams.add(0, destFile.getAbsolutePath());
                    convertParams.add(0, out.getAbsolutePath());
                    String[] phase2Params = convertParams.toArray(new String[0]);
                    LOGGER.info("Executing raw2ometiff with args " + Arrays.toString(phase2Params));
                    result = converter.execute(phase2Params);
                    LOGGER.info("Cleaning up intermediate files");
                    if (out != job.fileIn) {
                        FileUtils.deleteDirectory(out);
                    }
                }
                if (result != 0) {
                    break;
                }

            }
            if (this.interrupted && result != 0) {
                job.status = FAILED;
                LOGGER.info("User aborted job: " + job.fileOut.getName() + "\n");
            } else if (result == 0 && destFile.exists()) {
                job.status = COMPLETED;
                LOGGER.info("Successfully created: " + job.fileOut.getName() + "\n");
            } else if (result == 0) {
                job.status = NO_OUTPUT;
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

    private Integer getSeriesCount(String filename) throws IOException, FormatException, DependencyException,
            ServiceException {
        ServiceFactory factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        IMetadata meta = service.createOMEXMLMetadata();
        // Create reader object
        IFormatReader reader = new ImageReader();
        reader.setFlattenedResolutions(false);
        reader.setMetadataStore(meta);
        reader.setId(filename);
        return reader.getSeriesCount();
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