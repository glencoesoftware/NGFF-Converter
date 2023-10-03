package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.convert.workflows.BaseWorkflow;
import com.glencoesoftware.pyramid.CompressionType;
import com.glencoesoftware.pyramid.PyramidFromDirectoryWriter;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.UnaryOperator;

public class CreateTiff extends BaseTask {

    private final PyramidFromDirectoryWriter converter = new PyramidFromDirectoryWriter();

    public CreateTiff(BaseWorkflow parent) {
        super(parent);
    }

    private String name = "Convert to TIFF";

    public String getName() {
        return this.name;
    }

    private ArrayList<Node> standardSettings;

    private ArrayList<Node> advancedSettings;


    private ChoiceBox<CompressionType> compression;

    private ChoiceBox<String> logLevel;
    private CheckBox legacy;
    private TextField maxWorkers;

    private VBox quality;

    private CheckBox rgb;

    private CheckBox split;


    public void setOutput(String basePath) {
        this.output = Paths.get(basePath, this.outputName + ".ome.tiff").toFile();
    }


    private void setupIO() {
        converter.setInputPath(this.input.getAbsolutePath());
        converter.setOutputPath(this.output.getAbsolutePath());
    }

    public void run() {
        // Apply GUI configurations first
        setupIO();
        this.status = taskStatus.RUNNING;
        try {
            converter.call();
            this.status = taskStatus.COMPLETED;
        } catch (Exception e) {
            this.status = taskStatus.FAILED;
            System.out.println("Failed");
        }
    }

    public void updateStatus() {
        if (this.status == taskStatus.COMPLETED) { return; }
        if (this.output == null | this.input == null) {
            this.status = taskStatus.ERROR;
            this.warningMessage = "I/O not configured";
        } else {
            this.status = taskStatus.PENDING;
        }
    }


    private void generateNodes() {
        // Generate standard controls
        standardSettings = new ArrayList<>();
        advancedSettings = new ArrayList<>();
        logLevel = new ChoiceBox<>(FXCollections.observableArrayList("OFF", "ERROR", "WARN",
                "INFO", "DEBUG", "TRACE","ALL"));
        HBox logSettings = new HBox(5, new Label("Log Level"), logLevel);
        logSettings.setAlignment(Pos.CENTER_LEFT);
        standardSettings.add(logSettings);

        maxWorkers = new TextField();
        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("([1-9][0-9]*)?")) {
                return change;
            }
            return null;
        };
        maxWorkers.setTextFormatter(new TextFormatter<>(integerFilter));
        HBox workerBox = new HBox(5, new Label("Max Workers"), maxWorkers);
        workerBox.setAlignment(Pos.CENTER_LEFT);
        standardSettings.add(workerBox);

        compression = new ChoiceBox<>();
        compression.setItems(FXCollections.observableArrayList( CompressionType.values()));
        HBox compressionBox = new HBox(5, new Label("Compression Type"), compression);
        compressionBox.setAlignment(Pos.CENTER_LEFT);
        standardSettings.add(compressionBox);

        rgb = new CheckBox("RGB");
        advancedSettings.add(rgb);

        split = new CheckBox("Split series");
        advancedSettings.add(split);

        legacy = new CheckBox("Legacy mode");
        advancedSettings.add(legacy);

        // Todo: Properly implement compressionoptions
        quality = new VBox(5, new Label("Placeholder []"), new Label("Option 1"));
        advancedSettings.add(quality);
    }

    private void updateNodes() {
        logLevel.getSelectionModel().select("WARN");
        maxWorkers.setText(String.valueOf(converter.getMaxWorkers()));
        compression.setValue(converter.getCompression());
        rgb.setSelected(converter.getRGB());
        split.setSelected(converter.getSplitTIFFs());
        legacy.setSelected(converter.getLegacyTIFF());
        // Todo: Quality
    }


    public ArrayList<Node> getStandardSettings() {
        if (this.standardSettings == null) {
            generateNodes();
        }
        updateNodes();
        return this.standardSettings;
    }

    public ArrayList<Node> getAdvancedSettings() {
        if (this.advancedSettings == null) {
            generateNodes();
        }
        return this.advancedSettings;
    }

    public void applySettings() {
        System.out.println("Applying settings for " + getName());
        converter.setLogLevel(logLevel.getValue());
        converter.setMaxWorkers(Integer.parseInt(maxWorkers.getText()));
        converter.setCompression(compression.getValue());

        converter.setLegacyTIFF(legacy.isSelected());
        converter.setRGB(rgb.isSelected());
        converter.setSplitTIFFs(split.isSelected());
        //Todo: Quality configurator

    }


    public void setDefaults() {
        return;
    }

    public void applyDefaults() {
        return;
    }

    public Object[] getValues() {
        return new Object[]{
                logLevel.getValue(),
                maxWorkers.getText(),
                compression.getValue(),
                legacy.isSelected(),
                rgb.isSelected(),
                split.isSelected(),
        };
    }

    public void setValues(Object[] values) {
        logLevel.setValue((String) values[0]);
        maxWorkers.setText((String) values[1]);
        compression.setValue((CompressionType) values[2]);
        legacy.setSelected((Boolean) values[3]);
        rgb.setSelected((Boolean) values[3]);
        split.setSelected((Boolean) values[3]);
    }


}
