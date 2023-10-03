package com.glencoesoftware.convert.dialogs;

import com.glencoesoftware.convert.tasks.BaseTask;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;

public class ConfigureJobDialog {

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

    private void initialize() {}

    private void resetDialog() {
        this.jobs = null;
        standardSettings.getChildren().clear();
        advancedSettings.getChildren().clear();
        this.tasks = null;
        this.taskIndex = 0;
        this.currentTask = null;
        prevButton.setDisable(true);
        nextButton.setDisable(false);
}

    public void initData(ObservableList<BaseWorkflow> jobs) {
        resetDialog();
        this.jobs = jobs;
        if (jobs.isEmpty()) {
            System.out.println("No jobs given?");
            return;
        }
        this.multiMode = jobs.size() > 1;


        BaseWorkflow jobSample = this.jobs.get(0);
        if (this.multiMode) mainLabel.setText("Configuring %d jobs".formatted(this.jobs.size()));
        else mainLabel.setText("Configuring %s".formatted(jobSample.getInput()));

        this.tasks = jobSample.tasks;
        displayTaskSettings();
    }

    private void displayTaskSettings() {
        standardSettings.getChildren().clear();
        advancedSettings.getChildren().clear();
        this.currentTask = tasks.get(this.taskIndex);
        this.taskLabel.setText("Settings for %s".formatted(this.currentTask.getName()));
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
        System.out.println("Applying settings");
        // Different handling if configuring multiple jobs
        if (this.multiMode) {
            // Iterate through tasks
            for (int i = 0; i < this.tasks.size(); i++) {
                // Fetch settings from the template task used in the dialog
                BaseTask task = this.tasks.get(i);
                Object[] newValues = task.getValues();
                // Remember to apply those
                task.applySettings();
                // Iterate through the other selected jobs and apply the same settings
                for (int j = 1; j < this.jobs.size(); j++) {
                    BaseWorkflow otherJob = this.jobs.get(j);
                    BaseTask otherTask = otherJob.tasks.get(i);
                    otherTask.setValues(newValues);
                    otherTask.applySettings();
                }
            }
            System.out.println("Done");
            return;
        }
        // If there's just one task we do it this easy way.
        for (BaseTask task : this.tasks) {
            task.applySettings();
        }
        System.out.println("Done");
    }

    @FXML
    private void closeDialog() {
        Stage stage = (Stage) configureJob.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void nextTask() {
        this.taskIndex += 1;
        if (this.taskIndex == tasks.size() - 1) {
            nextButton.setDisable(true);
        }
        prevButton.setDisable(false);
        displayTaskSettings();
    }
    @FXML
    private void prevTask() {
        this.taskIndex -= 1;
        if (this.taskIndex == 0) {
            prevButton.setDisable(true);
        }
        nextButton.setDisable(false);
        displayTaskSettings();
    }

}
