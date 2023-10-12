package com.glencoesoftware.convert.workflows;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.glencoesoftware.convert.*;
import com.glencoesoftware.convert.tasks.BaseTask;
import com.glencoesoftware.convert.tasks.Output;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static com.glencoesoftware.convert.JobState.status.READY;

public abstract class BaseWorkflow {

    // Keep a reference to the parent controller
    public PrimaryController controller = null;

    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public void setSelected(boolean val) {
        this.selected.set(val);
    }

    public BooleanProperty getSelected() {
        return this.selected;
    }

    static String getDisplayName() { return null; }

    public String getName() {
        return getDisplayName();
    }

    public String getStatusString() {
        return StringUtils.capitalize(this.status.get().toString().toLowerCase());
    }

    private final ch.qos.logback.classic.Logger rootLogger =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    private final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(this.getClass());

    {
        // We want some basic log messages to always show
            LOGGER.setLevel(Level.INFO);
    }


    public ObjectProperty<JobState.status> status = new SimpleObjectProperty<>(READY);

    public String statusText = "";

    public File firstInput = null;

    public File finalOutput = null;

    public String workingDirectory = null;

    public String getInput() {
        if (firstInput == null) {
            return null;
        }
        return firstInput.getName();
    }

    public IntegerProperty currentStage = new SimpleIntegerProperty(-1);


    public ObservableList<BaseTask> tasks;
    protected void setTasks(ObservableList<BaseTask> tasks) {
        this.tasks = tasks;
    }

    public File getLogFile() {
        Output outputTask = (Output) this.tasks.get(this.tasks.size() - 1);
        return outputTask.getLogFile();
    }

    private Stage consoleWindow;
    private TextAreaStream textAreaStream;
    private LogController logControl;

    private void createLogControl() throws IOException {
        FXMLLoader logLoader = new FXMLLoader();
        logLoader.setLocation(App.class.getResource("LogWindow.fxml"));
        Scene scene = new Scene(logLoader.load());
        logControl = logLoader.getController();
        consoleWindow = new Stage();
        consoleWindow.setScene(scene);
        textAreaStream = logControl.stream;
        logControl.setParent(this.controller);
    }

    {
        try {
            createLogControl();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void showLogBox() {
        consoleWindow.show();
        consoleWindow.toFront();
    }

    public void setOverwrite(boolean shouldOverwrite) {
        for (BaseTask task : this.tasks) {
            task.setOverwrite(shouldOverwrite);
        }
    }

    public boolean canOverwrite() {
        Output outputTask = (Output) this.tasks.get(this.tasks.size() - 1);
        return outputTask.getOverwrite();
    }

    public File getWorkingDirectory() {
        Output outputTask = (Output) this.tasks.get(this.tasks.size() - 1);
        return outputTask.getWorkingDirectory();
    }
    public void respondToUpdate() {
        JobState.status finalStatus = READY;
        statusText = "";
        loop: for (BaseTask task : this.tasks) {
            task.updateStatus();
            System.out.println("New status for "+ task.getName()  + " is " + task.getStatusString());
            switch (task.status) {
                case READY -> System.out.println("Job queued");
                case RUNNING, FAILED -> {
                    finalStatus = task.status;
                    break loop;
                }
                case COMPLETED, QUEUED -> finalStatus = task.status;
                case WARNING -> {
                    finalStatus = task.status;
                    statusText = "Issue with %s:\n%s".formatted(task.getName(), task.warningMessage);
                    break loop;
                }
            }
        }
        status.set(finalStatus);
    }

    public void calculateIO(String inputPath, String outputBasePath, String workingDir) {
        // Run through each task and determine the input/output file paths to feed into each-other.
        this.workingDirectory = workingDir;
        String targetDir = workingDir;
        this.firstInput = new File(inputPath);
        File workingInput = this.firstInput;
        for (int i = 0; i < this.tasks.size(); i++) {
            if (i == this.tasks.size() - 2) {
                // This is the last "real" task, use the final output destination rather than the temp dir
                targetDir = outputBasePath;
            }
            BaseTask task = this.tasks.get(i);
            if (task instanceof Output) {
                // This is the virtual output task that lets the user configure output names.
                // Carry forward i/o from last task
                task.input = workingInput;
                task.output = workingInput;
            } else {
                task.setInput(workingInput);
                task.setOutput(targetDir);
            }
            workingInput = task.getOutput();
        }
        this.finalOutput = workingInput;
        if (this.finalOutput.exists() && !canOverwrite()) {
            this.status.set(JobState.status.WARNING);
            this.statusText = "Output path already exists: " + this.finalOutput.getAbsolutePath();
        } else {
            this.status.set(READY);
            this.statusText = "";
        }
        LOGGER.info("Path calculation complete. Final output will be:");
        LOGGER.info(workingInput.getAbsolutePath());
        logControl.setTitle("Execution Logs: %s (-> %s)".formatted(firstInput.getName(), getName()));
        this.respondToUpdate();
    }

    public void reset() {
        this.currentStage.set(-1);
        this.status.set(READY);
        for (BaseTask task : this.tasks) {
            task.status = READY;
            task.updateStatus();
        }
    }

    public void prepareGUI() {
        for (BaseTask task : this.tasks) task.prepareWidgets();
    }

    public void execute(){
        // Presumes input/outputs are pre-calculated
        this.status.set(JobState.status.RUNNING);
        this.currentStage.set(0);
        // Setup logging
        FileAppender<ILoggingEvent> fileAppender = getFileAppender(getLogFile());
        TextAreaAppender<ILoggingEvent> logBoxAppender = logControl.getAppender();
        rootLogger.addAppender(logBoxAppender);
        if (fileAppender != null) rootLogger.addAppender(fileAppender);
        LOGGER.info("Beginning conversion of " + this.firstInput.getName());

//        this.calculateIO();
        for (BaseTask task : this.tasks) {
            LOGGER.info("Running task " + task.getClass().getSimpleName());
            task.run();
            this.currentStage.set(this.currentStage.get() + 1);
            if (task.status != JobState.status.COMPLETED) {
                this.status.set(JobState.status.FAILED);
                this.currentStage.set(-1);
                break;
            }
        }
        LOGGER.info("Tasks finished, cleaning up intermediates");

        // Cleanup intermediates
        for (int i = 0; i < this.tasks.size() - 1; ++i) {
            BaseTask task = this.tasks.get(i);
            File output = task.getOutput();
            if (!Objects.equals(output, this.finalOutput)) {
                if (!output.exists()){
                    continue;
                }
                try {
                    if (output.isDirectory()) {
                        FileUtils.deleteDirectory(output);
                    } else {
                        FileUtils.delete(output);
                    }
                } catch (IOException ioe) {
                    LOGGER.error("Failed to clean up intermediates - %s - Error was: %s".formatted(
                            output.getAbsolutePath(), ioe));

                }
            }
        }
        LOGGER.info("Conversion finished");

        // Print anything left in the console buffer.
        textAreaStream.forceFlush();
        rootLogger.detachAppender(logBoxAppender);
        if (fileAppender != null) rootLogger.detachAppender(fileAppender);
        this.status.set(JobState.status.COMPLETED);
        this.currentStage.set(-1);
    }

    public void prepareToActivate() {
        if (status.get() == JobState.status.READY | status.get() == JobState.status.WARNING) {
            status.set(JobState.status.QUEUED);
            for (BaseTask task : tasks) task.status = JobState.status.QUEUED;
        }
    }

    private static FileAppender<ILoggingEvent> getFileAppender(File logFile) {
        if (logFile == null) return null;
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setPattern("%date [%thread] %-5level %logger{36} - %msg%n");
        ple.setContext(lc);
        ple.start();
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setFile(logFile.getAbsolutePath());
        fileAppender.setEncoder(ple);
        fileAppender.setContext(lc);
        fileAppender.start();
        return fileAppender;
    }
}