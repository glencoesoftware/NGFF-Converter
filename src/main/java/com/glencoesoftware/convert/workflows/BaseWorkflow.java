/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert.workflows;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glencoesoftware.convert.*;
import com.glencoesoftware.convert.dialogs.LogDisplayDialog;
import com.glencoesoftware.convert.tasks.BaseTask;
import com.glencoesoftware.convert.tasks.Output;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static com.glencoesoftware.convert.JobState.status.*;

public abstract class BaseWorkflow extends Service<Void> {

    // Keep a reference to the parent controller
    public PrimaryController controller;

    public BaseWorkflow(PrimaryController parentController, File input) {
        controller = parentController;
        // Use the single thread executor assigned to NGFF-Converter
        setExecutor(App.executor);
        status.addListener((i, o, n) -> {
            // Update job/task display when jobs update
            controller.jobList.refresh();
            controller.taskList.refresh();
        });
        firstInput = input;
    }

    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public void setSelected(boolean val) {
        selected.set(val);
    }

    public BooleanProperty getSelected() {
        return selected;
    }

    abstract public String getShortName();

    abstract public String getFullName();

    abstract public String getTechnicalName();


    public String getStatusString() {
        return StringUtils.capitalize(status.get().toString().toLowerCase());
    }

    private final ch.qos.logback.classic.Logger rootLogger =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    private final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(getClass());

    {
        // We want some basic log messages to always show
            LOGGER.setLevel(Level.INFO);
    }


    public final ObjectProperty<JobState.status> status = new SimpleObjectProperty<>(READY);

    public String statusText = "";

    public File firstInput;

    public File finalOutput = null;

    // Used by the joblist table to fetch file name. NOT unused!
    public String getInput() {
        return firstInput.getName();
    }

    // Index of the current running task
    public final IntegerProperty currentStage = new SimpleIntegerProperty(-1);

    public ObservableList<BaseTask> tasks;
    protected void setTasks(ObservableList<BaseTask> newTasks) {
        tasks = newTasks;
    }

    public File getLogFile() {
        Output outputTask = (Output) tasks.get(tasks.size() - 1);
        return outputTask.getLogFile();
    }

    private Stage consoleWindow;
    private TextAreaStream textAreaStream;

    private FileAppender<ILoggingEvent> fileAppender;
    private TextAreaAppender<ILoggingEvent> logBoxAppender;
    private LogDisplayDialog logControl;

    private void createLogControl() throws IOException {
        FXMLLoader logLoader = new FXMLLoader();
        logLoader.setLocation(App.class.getResource("LogDisplay.fxml"));
        Scene scene = new Scene(logLoader.load());
        scene.setFill(Color.TRANSPARENT);
        logControl = logLoader.getController();
        consoleWindow = new Stage();
        consoleWindow.setScene(scene);
        consoleWindow.getIcons().add(App.appIcon);
        textAreaStream = logControl.stream;
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
        for (BaseTask task : tasks) {
            task.setOverwrite(shouldOverwrite);
        }
    }

    public boolean canOverwrite() {
        Output outputTask = (Output) tasks.get(tasks.size() - 1);
        return outputTask.getOverwrite();
    }

    public File getWorkingDirectory() {
        Output outputTask = (Output) tasks.get(tasks.size() - 1);
        return outputTask.getWorkingDirectory();
    }
    public void respondToUpdate() {
        JobState.status finalStatus = READY;
        statusText = "";
        loop: for (BaseTask task : tasks) {
            task.updateStatus();
            switch (task.status) {
                case RUNNING, FAILED, STOPPING -> {
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

    public void calculateIO() {
        // Run through each task and determine the input/output file paths to feed into each-other.
        File workingInput = firstInput;
        File workingDir = getWorkingDirectory();
        for (BaseTask task: tasks) {
            task.setInput(workingInput);
            task.calculateOutput(workingDir.getAbsolutePath());
            workingInput = task.getOutput();
        }

        finalOutput = workingInput;
        if (finalOutput.exists() && !canOverwrite()) {
            status.set(JobState.status.WARNING);
            statusText = "Output path already exists: " + finalOutput.getAbsolutePath();
        } else {
            status.set(READY);
            statusText = "";
        }
        LOGGER.info("Path calculation complete. Final output will be:");
        LOGGER.info(workingInput.getAbsolutePath());
        logControl.setTitle("%s (to %s)".formatted(firstInput.getName(), getShortName()));
        respondToUpdate();
    }

    public void resetJob() {
        reset();
        currentStage.set(-1);
        calculateIO();
        for (BaseTask task : tasks) {
            task.status = READY;
        }
        respondToUpdate();
    }

    public void prepareGUI() {
        for (BaseTask task : tasks) task.prepareForDisplay();
    }

    public void queueJob() {
        if (canRun()) {
            status.set(JobState.status.QUEUED);
            for (BaseTask task : tasks) task.status = JobState.status.QUEUED;
            start();
            controller.queuedJobs.setValue(controller.queuedJobs.getValue() + 1);
        } else {
            LOGGER.error("Workflow %s is not in a state to be queued (%s)".formatted(
                    firstInput.getName(), status.getValue()));
        }
    }

    public void cancelJob() {
        cancel();
        if (status.get() == JobState.status.QUEUED) {
            // Task wasn't started, so we can reset immediately.
            resetJob();
            controller.queuedJobs.setValue(controller.queuedJobs.getValue() - 1);
        } else {
            status.set(STOPPING);
            statusText = "Waiting for task to stop safely";
            for (BaseTask task: tasks) {
                if (task.status == RUNNING) task.status = STOPPING;
            }
        }
    }

    private void cleanupIntermediates() {
        for (int i = 0; i < tasks.size() - 1; ++i) {
            BaseTask task = tasks.get(i);
            File output = task.getOutput();
            if (!Objects.equals(output, finalOutput) && !Objects.equals(output, firstInput)) {
                if (!output.exists()){
                    continue;
                }
                try {
                    if (output.isDirectory()) FileUtils.deleteDirectory(output);
                    else FileUtils.delete(output);
                } catch (IOException ioe) {
                    LOGGER.error("Failed to clean up intermediates - %s - Error was: %s".formatted(
                            output.getAbsolutePath(), ioe));
                }
            }
        }
    }

    public void shutdown() {
        // Cleanup intermediates
        LOGGER.info("Cleaning up intermediates");
        cleanupIntermediates();
        LOGGER.info("Execution finished\n");

        // Print anything left in the console buffer.
        textAreaStream.forceFlush();
        if (logBoxAppender != null) rootLogger.detachAppender(logBoxAppender);
        if (fileAppender != null) rootLogger.detachAppender(fileAppender);

        // Evaluate whether we completed successfully
        if (currentStage.get() == tasks.size()) {
            // We completed all tasks
            status.set(JobState.status.COMPLETED);
            // After a success we won't be running again, shut down the loggers.
            if (logBoxAppender != null) logBoxAppender.stop();
            if (fileAppender != null) fileAppender.stop();
        } else {
            // Tasks were interrupted or errored out
            status.set(JobState.status.FAILED);
            // Mark any unfinished tasks as failed
            for (BaseTask task: tasks)
                switch (task.status) {
                    case RUNNING, QUEUED -> task.status = JobState.status.FAILED;
                }
        }
        currentStage.set(-1);
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

    // Copy setting values from supplied instance to this one
    public void cloneSettings(BaseWorkflow sourceInstance) {
        if (!(sourceInstance.getClass() == getClass())) {
            LOGGER.error("Cannot clone a different workflow");
            return;
        }
        for (int i = 0; i < tasks.size(); i++) {
            BaseTask localTask = tasks.get(i);
            BaseTask sourceTask = sourceInstance.tasks.get(i);
            localTask.cloneValues(sourceTask);
        }

    }

    // Return a list of extension filters for use with dialogs setting this workflow's final output
    abstract public FileChooser.ExtensionFilter[] getExtensionFilters();

    public void writeSettings(File targetFile) throws IOException {
        JsonFactory factory = new JsonFactory();
        JsonGenerator generator = factory.createGenerator(targetFile, JsonEncoding.UTF8);
        generator.useDefaultPrettyPrinter();
        generator.writeStartObject();
        for (BaseTask task: tasks) task.exportSettings(generator);
        generator.writeEndObject();
        generator.close();
    }

    public void loadSettings(File targetFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(targetFile);
        for (BaseTask task: tasks) task.importSettings(node);
    }

    public boolean canRun() {
        return !isRunning() &&
                (status.getValue() == JobState.status.READY || status.getValue() == JobState.status.WARNING);
    }


    // Execute the conversion as a task on the worker thread
    protected Task<Void> createTask() {
        return new Task<>() {
            protected Void call() {
                // Don't run if cancelled or already completed
                if (isCancelled() || (status.get() == JobState.status.COMPLETED) ||
                        (status.get() == JobState.status.FAILED)) {
                    return null;
                }

                status.set(JobState.status.RUNNING);
                currentStage.set(0);
                Platform.runLater(() -> {
                    // Display this job on the task tree
                    controller.jobList.getSelectionModel().select(BaseWorkflow.this);
                    controller.jobList.refresh();
                });
                controller.updateStatus("Working on %s".formatted(firstInput.getName()));
                // Setup logging
                fileAppender = getFileAppender(getLogFile());
                logBoxAppender = logControl.getAppender();
                rootLogger.addAppender(logBoxAppender);
                if (fileAppender != null) rootLogger.addAppender(fileAppender);
                LOGGER.info("Beginning conversion of " + firstInput.getName());
                try {
                    LOGGER.info("Preparing to run tasks");
                    for (BaseTask task : tasks) {
                        task.prepareToRun();
                    }
                    LOGGER.info("Executing tasks");
                    for (BaseTask task : tasks) {
                        LOGGER.info("Running task " + task.getClass().getSimpleName());
                        task.run();
                        if (task.status != JobState.status.COMPLETED) break;
                        currentStage.set(currentStage.get() + 1);
                    }
                    LOGGER.info("Tasks finished");
                } finally {
                    shutdown();
                }

                switch (status.get()) {
                    case COMPLETED -> {
                        LOGGER.info("Successfully created: " + finalOutput.getName() + "\n");
                        controller.updateStatus(firstInput.getName() + " completed");
                    }
                    case FAILED -> {
                        controller.updateStatus(firstInput.getName() + " failed");
                        if (isCancelled()) {
                            LOGGER.info("User aborted job: " + finalOutput.getName() + "\n");
                        } else {
                            LOGGER.info("Job failed, see logs \n");
                        }
                    }
                    default -> LOGGER.info("Job status is invalid????: " + status);
                }

                String finalStatus = String.format("Completed conversion of %s files.", controller.completedJobs);
                Platform.runLater(() -> {
                    LOGGER.info(finalStatus);
                    controller.jobFinished();
                    controller.jobList.refresh();
                });
                return null;
            }
        };
    }

}
