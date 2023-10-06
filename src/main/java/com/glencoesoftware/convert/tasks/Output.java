package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

public class Output extends BaseTask {
    // Virtual task to enable display of global config and cleanup of intermediates
    private boolean overwrite = false;
    private ArrayList<Node> standardSettings;
    private CheckBox overwriteBox;
    private CheckBox logBox;

    private boolean logToFile = false;
    private TextField logFile;
    private TextField outputPath;


    public Output(BaseWorkflow parent) {
        super(parent);
    }
    public String getName() {
        return "Output";
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean getOverwrite() {
        return this.overwrite;
    }

    public File getLogFile() {
        if (!logToFile) return null;
        if (Objects.equals(logFile.getText(), defaultLogFileName)) {
            // Stick .log on the end of the target file
            return new File(output.getAbsolutePath() + ".log");
        }
        // Use whatever path the user specified instead
        return new File(logFile.getText());
    }

    public void setOutput(String basePath) {
        // Todo: Conform to desired workflow output type
        this.output = Paths.get(basePath, this.input.getName()).toFile();
        this.parent.finalOutput = this.output;
        // Get the previous task and update it's output
        this.parent.tasks.get(this.parent.tasks.size() - 2).output = this.output;
    }

    public void updateStatus() {
        System.out.println("Doing status update");
        if (this.status == JobState.status.COMPLETED) {
            return;
        }
        if (this.output == null | this.input == null) {
            this.status = JobState.status.WARNING;
            this.warningMessage = "I/O not configured";
        } else if (this.output.exists() && !this.overwrite) {
            this.status = JobState.status.WARNING;
            this.warningMessage = "Output file already exists";
        } else {
            this.status = JobState.status.READY;
        }
    }

    private void setupIO() {
        this.output = this.input;
    }

    public void run() {
        // Apply GUI configurations first
        setupIO();
        this.status = JobState.status.RUNNING;
        LOGGER.info("Saving final output file");
        try {
            if (!Objects.equals(this.input.getAbsolutePath(), this.output.getAbsolutePath())) {
                Files.copy(this.input.toPath(), this.output.toPath(), ATOMIC_MOVE);
            }
            this.status = JobState.status.COMPLETED;
        } catch (IOException e) {
            this.status = JobState.status.FAILED;
            LOGGER.error("Failed to copy");
        }
    }

    private final String defaultLogFileName = "<Output File Name>.log";

    public void generateNodes() {
        if (standardSettings != null) return;
        // Generate standard controls
        standardSettings = new ArrayList<>();
        outputPath = new TextField();
        HBox outputControl = new HBox(5, new Label("Output file: "), outputPath);
        standardSettings.add(outputControl);
        overwriteBox = new CheckBox("Overwrite existing file");
        standardSettings.add(overwriteBox);
        logBox = new CheckBox("Log to file");
        logBox.selectedProperty().addListener((e, oldValue, newValue) -> logToFile = newValue);
        standardSettings.add(logBox);
        logFile = new TextField(defaultLogFileName);
        logFile.setEditable(false);
        logFile.onMouseClickedProperty().set(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Set log file");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Log Files", "*.log"),
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            File selectedFile = fileChooser.showOpenDialog(parent.controller.jobList.getScene().getWindow());
            if (selectedFile != null) {
                logFile.setText(selectedFile.getAbsolutePath());
            }
        });
        HBox logFileBox = new HBox(5, new Label("Log file"), logFile);
        logFileBox.setAlignment(Pos.CENTER_LEFT);
        standardSettings.add(logFileBox);


    }

    public void updateNodes() {
        outputPath.setText(this.output.getAbsolutePath());
        overwriteBox.setSelected(this.parent.controller.menuOverwrite.isSelected());
    }


    public ArrayList<Node> getStandardSettings() {
        if (this.standardSettings == null) {
            generateNodes();
        }
        updateNodes();
        return this.standardSettings;
    }

    public ArrayList<Node> getAdvancedSettings() {
        return null;
    }

    public void applySettings() {
        System.out.println("Applying settings" + overwriteBox.selectedProperty().get());
        parent.setOverwrite(overwriteBox.selectedProperty().get());
        // Todo: set output properly

        setOutput(new File(outputPath.getText()).getParent());
    }

    public void setDefaults() {

    }

    public void applyDefaults() {

    }

    public void cloneValues(BaseTask sourceInstance) {
        if (!(sourceInstance instanceof Output source)) {
            System.out.println("Incorrect input type");
            return;
        }
        if (this.standardSettings == null) {
            generateNodes();
        }
        overwriteBox.setSelected(source.overwriteBox.isSelected());
    }


}


