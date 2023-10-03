package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

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

    public void setOutput(String basePath) {
        // Todo: Conform to desired workflow output type
        this.output = Paths.get(basePath, this.input.getName()).toFile();
        this.parent.finalOutput = this.output;
        // Get the previous task and update it's output
        this.parent.tasks.get(this.parent.tasks.size() - 2).output = this.output;
    }

    public void updateStatus() {
        System.out.println("Doing status update");
        if (this.status == taskStatus.COMPLETED) {
            return;
        }
        if (this.output == null | this.input == null) {
            this.status = taskStatus.ERROR;
            this.warningMessage = "I/O not configured";
        } else if (this.output.exists() && !this.overwrite) {
            this.status = taskStatus.ERROR;
            this.warningMessage = "Output file already exists";
        } else {
            this.status = taskStatus.PENDING;
        }
    }

    private void setupIO() {
        this.output = this.input;
    }

    public void run() {
        // Apply GUI configurations first
        setupIO();
        this.status = taskStatus.RUNNING;
        try {
            if (!Objects.equals(this.input.getAbsolutePath(), this.output.getAbsolutePath())) {
                Files.copy(this.input.toPath(), this.output.toPath(), ATOMIC_MOVE);
            }
            this.status = taskStatus.COMPLETED;
        } catch (IOException e) {
            this.status = taskStatus.FAILED;
            System.out.println("Failed to copy");
        }
    }

    private void generateNodes() {
        // Generate standard controls
        standardSettings = new ArrayList<>();
        outputPath = new TextField();
        HBox outputControl = new HBox(5, new Label("Output file: "), outputPath);
        standardSettings.add(outputControl);
        overwriteBox = new CheckBox("Overwrite existing file");
        standardSettings.add(overwriteBox);
    }

    private void updateNodes() {
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
        setOverwrite(overwriteBox.selectedProperty().get());
        // Todo: set output properly
        setOutput(outputPath.getText());
    }

    public void setValues(Object[] values) {
        overwriteBox.setSelected((boolean) values[0]);
    }

    public void setDefaults() {

    }

    public void applyDefaults() {

    }

    public Object[] getValues() {
        return new Object[]{
                overwriteBox.isSelected(),
        };
    }
}


