/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert.dialogs;

import com.fasterxml.jackson.core.*;
import com.glencoesoftware.convert.App;
import com.glencoesoftware.convert.tasks.BaseTask;
import com.glencoesoftware.convert.tasks.Output;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.prefs.BackingStoreException;

public class ConfigureJobDialog {

    private final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(this.getClass());

    private boolean multiMode = false;

    private ObservableList<BaseTask> tasks = null;
    private int taskIndex = 0;
    private BaseTask currentTask = null;
    private ObservableList<BaseWorkflow> jobs;
    @FXML
    private BorderPane configureJob;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private VBox standardSettings;
    @FXML
    private VBox advancedSettings;
    @FXML
    private Accordion advancedPane;
    @FXML
    private Label mainLabel;
    @FXML
    private Label taskLabel;
    @FXML
    private TitledPane advancedExpando;

    public void initialize() {
        HBox expandoHeader = new HBox(5);
        expandoHeader.setMaxWidth(Double.MAX_VALUE);
        expandoHeader.setAlignment(Pos.CENTER_LEFT);
        Label expandoText = new Label("Show Advanced Settings...");
        expandoText.getStyleClass().add("expando-title");
        Region expandoSpacer = new Region();
        FontIcon collapseButton = new FontIcon("bi-caret-down-fill");
        expandoHeader.getChildren().addAll(expandoText, expandoSpacer, collapseButton);
        HBox.setHgrow(expandoSpacer, Priority.ALWAYS);
        expandoHeader.setPrefWidth(350);
        advancedExpando.expandedProperty().addListener((e, ov, nv) -> {
            if (nv) {
                expandoText.setText("Hide advanced settings...");
                collapseButton.setIconLiteral("bi-caret-up-fill");
            } else {
                expandoText.setText("Show advanced settings...");
                collapseButton.setIconLiteral("bi-caret-down-fill");
            }
        });
        advancedExpando.setGraphic(expandoHeader);
        advancedExpando.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

    }

    private void resetDialog() {
        this.jobs = null;
        standardSettings.getChildren().clear();
        advancedSettings.getChildren().clear();
        standardSettings.setFillWidth(true);
        standardSettings.setMaxWidth(Double.MAX_VALUE);
        this.tasks = null;
        this.taskIndex = 0;
        this.currentTask = null;
        prevButton.setDisable(true);
        nextButton.setDisable(false);
}

    public void initData(ObservableList<BaseWorkflow> jobs, int taskIndex) {
        resetDialog();
        this.jobs = jobs;
        if (jobs.isEmpty()) {
            LOGGER.error("No jobs given to config dialog? This shouldn't happen");
            return;
        }
        multiMode = jobs.size() > 1;


        BaseWorkflow jobSample = this.jobs.get(0);
        if (multiMode) mainLabel.setText("Configuring %d jobs".formatted(this.jobs.size()));
        else mainLabel.setText("Configuring %s".formatted(jobSample.firstInput.getName()));

        this.tasks = jobSample.tasks;
        jobSample.prepareGUI();
        this.taskIndex = taskIndex;
        displayTaskSettings();
    }

    private void displayTaskSettings() {
        standardSettings.getChildren().clear();
        advancedSettings.getChildren().clear();
        prevButton.setDisable(this.taskIndex == 0);
        nextButton.setDisable(this.taskIndex == tasks.size() - 1);
        this.currentTask = tasks.get(this.taskIndex);
        this.taskLabel.setText("Settings for: %s".formatted(this.currentTask.getName()));
        ArrayList<Node> baseSettings = this.currentTask.getStandardSettings();
        ArrayList<Node> advSettings = this.currentTask.getAdvancedSettings();
        if (baseSettings != null) standardSettings.getChildren().addAll(baseSettings);
        if (advSettings != null) {
            advancedSettings.getChildren().addAll(advSettings);
            advancedPane.setVisible(true);
        } else {
            advancedPane.setVisible(false);
        }

    }
    @FXML
    private void applySettings() {
        // Different handling if configuring multiple jobs
        if (multiMode) {
            // Iterate through tasks
            for (int i = 0; i < this.tasks.size(); i++) {
                // Fetch settings from the template task used in the dialog
                BaseTask task = this.tasks.get(i);
                // Remember to apply settings to the first task
                task.applySettings();
                task.updateStatus();
                // Iterate through the other selected jobs and apply the same settings
                for (int j = 1; j < this.jobs.size(); j++) {
                    BaseWorkflow otherJob = this.jobs.get(j);
                    BaseTask otherTask = otherJob.tasks.get(i);
                    otherTask.cloneValues(task);
                    otherTask.applySettings();
                    otherTask.updateStatus();
                }
            }
            currentTask.parent.controller.updateStatus("Applied settings to %d jobs".formatted(jobs.size()));
            onClose();
            return;
        }
        // If there's just one task we do it this easy way.
        for (BaseTask task : this.tasks) {
            task.applySettings();
            task.updateStatus();
        }
        currentTask.parent.controller.updateStatus("Applied job settings");
        onClose();
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) configureJob.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void nextTask() {
        this.taskIndex += 1;
        displayTaskSettings();
        // Don't allow user to configure File Name when editing multiple entries
        Output.outputFileNameContainer.setVisible(!multiMode);
        Output.outputFileNameContainer.setManaged(!multiMode);
    }
    @FXML
    private void prevTask() {
        this.taskIndex -= 1;
        displayTaskSettings();
    }
    @FXML
    private void restoreDefaults() {
        ButtonType thisTask = new ButtonType("This Task");
        ButtonType allTasks = new ButtonType("All Tasks");
        Alert choice = new Alert(Alert.AlertType.INFORMATION,
                "Reset settings for this task (%s) or all tasks?".formatted(currentTask.getName()),
                ButtonType.CANCEL, thisTask, allTasks
        );
        choice.initOwner(configureJob.getScene().getWindow());
        choice.setTitle("Restore defaults");
        choice.setHeaderText("Choose which settings to reset");
        choice.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(App.class.getResource("Alert.css")).toExternalForm());
        Button thisTaskButton = (Button) choice.getDialogPane().lookupButton(thisTask);
        thisTaskButton.setDefaultButton(true);
        choice.showAndWait().ifPresent(response -> {
            if (response == thisTask) {
                for (BaseWorkflow job: jobs) {
                    BaseTask task = job.tasks.get(taskIndex);
                    task.resetToDefaults();
                    task.updateStatus();
                }
                currentTask.parent.controller.updateStatus("Reset task to defaults");
            } if (response == allTasks) {
                for (BaseWorkflow job: jobs) {
                    for (BaseTask task : job.tasks) {
                        task.resetToDefaults();
                        task.updateStatus();
                    }
                }
                currentTask.parent.controller.updateStatus("Reset all tasks to defaults");
            }
        });
        this.jobs.get(0).prepareGUI();
    }
    @FXML
    private void setDefaults() {
        ButtonType thisTask = new ButtonType("This Task");
        ButtonType allTasks = new ButtonType("All Tasks");
        Alert choice = new Alert(Alert.AlertType.INFORMATION,
                "Set defaults for this task (%s) or all tasks?".formatted(currentTask.getName()),
                ButtonType.CANCEL, thisTask, allTasks
        );
        choice.initOwner(configureJob.getScene().getWindow());
        choice.setTitle("Set defaults");
        choice.setHeaderText("Choose which settings to reset");
        choice.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(App.class.getResource("Alert.css")).toExternalForm());
        Button thisTaskButton = (Button) choice.getDialogPane().lookupButton(thisTask);
        thisTaskButton.setDefaultButton(true);
        choice.showAndWait().ifPresent(response -> {
            if (response == thisTask) {
                try {
                    currentTask.setDefaults();
                    currentTask.parent.controller.updateStatus("Saved defaults for " + currentTask.getName());
                } catch (BackingStoreException e) {
                    LOGGER.error("Failed to set defaults: " + e);
                    currentTask.parent.controller.updateStatus("Failed to save defaults");
                }
            } if (response == allTasks) {
                for (BaseTask task : tasks) {
                    try {
                        task.setDefaults();
                        currentTask.parent.controller.updateStatus("Saved defaults for all tasks");
                    } catch (BackingStoreException e) {
                        LOGGER.error("Failed to set defaults: " + e);
                        currentTask.parent.controller.updateStatus("Failed to save defaults");
                    }
                }
            }
        });
    }
    @FXML
    private void applyToAll() {
        BaseWorkflow thisJob = currentTask.parent;
        Alert choice = new Alert(Alert.AlertType.CONFIRMATION);
        choice.initOwner(configureJob.getScene().getWindow());
        choice.setTitle("Apply to all");
        choice.setHeaderText("Are you sure?");
        choice.setContentText(
                "Apply these settings to all '%s' jobs in the job list?".formatted(thisJob.getFullName())
        );
        choice.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(App.class.getResource("Alert.css")).toExternalForm());
        choice.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Apply settings from GUI before cloning
                for (BaseTask task : thisJob.tasks) {task.applySettings();}
                List<BaseWorkflow> allJobs = currentTask.parent.controller.jobList.getItems();
                int count = 0;
                for (BaseWorkflow job: allJobs) {
                    if (job == thisJob) {
                        continue;
                    }
                    if (job.getClass().equals( thisJob.getClass())) {
                        LOGGER.info("Cloning values from " + thisJob.getFullName());
                        job.cloneSettings(thisJob);
                        count++;
                    }
                }
                LOGGER.info("Copied values to " + count + " instances");
                currentTask.parent.controller.updateStatus("Applied settings to %d jobs".formatted(count));

            }
        });
    }

    @FXML
    private void exportSettings() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName("exported-settings.json");
        fileChooser.setTitle("Choose file to save settings to");
        File selectedFile = fileChooser.showSaveDialog(App.getScene().getWindow());
        if (selectedFile == null) return;
        ButtonType thisTask = new ButtonType("This Task");
        ButtonType allTasks = new ButtonType("All Tasks");
        Alert choice = new Alert(Alert.AlertType.INFORMATION,
                "Export settings for this task (%s) or all tasks?".formatted(currentTask.getName()),
                ButtonType.CANCEL, thisTask, allTasks
        );
        choice.initOwner(configureJob.getScene().getWindow());
        choice.setTitle("Export settings");
        choice.setHeaderText("Choose settings to export");
        choice.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(App.class.getResource("Alert.css")).toExternalForm());
        Button thisTaskButton = (Button) choice.getDialogPane().lookupButton(thisTask);
        thisTaskButton.setDefaultButton(true);
        choice.showAndWait().ifPresent(response -> {
            if (response == thisTask) {
                try {
                    JsonFactory factory = new JsonFactory();
                    JsonGenerator generator = factory.createGenerator(selectedFile, JsonEncoding.UTF8);
                    generator.useDefaultPrettyPrinter();
                    generator.writeStartObject();
                    currentTask.exportSettings(generator);
                    generator.writeEndObject();
                    generator.close();
                    currentTask.parent.controller.updateStatus("Exported settings for " + currentTask.getName());
                } catch (IOException e) {
                    currentTask.parent.controller.updateStatus("Failed to export settings");
                    throw new RuntimeException(e);
                }
            }
            if (response == allTasks) {
                try {
                    currentTask.parent.writeSettings(selectedFile);
                    currentTask.parent.controller.updateStatus("Exported settings for all tasks");
                } catch (IOException e) {
                    currentTask.parent.controller.updateStatus("Failed to export settings");
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @FXML
    private void importSettings() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName("exported-settings.json");
        fileChooser.setTitle("Choose file to load settings from");
        File selectedFile = fileChooser.showOpenDialog(App.getScene().getWindow());
        if (selectedFile == null) return;
        try {
            currentTask.parent.loadSettings(selectedFile);
            currentTask.parent.controller.updateStatus("Imported settings successfully");
        } catch (IOException e) {
            currentTask.parent.controller.updateStatus("Failed to import settings");
            throw new RuntimeException(e);
        }


    }
}
