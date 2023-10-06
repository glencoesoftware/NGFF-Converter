package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import com.glencoesoftware.pyramid.CompressionType;
import com.glencoesoftware.pyramid.PyramidFromDirectoryWriter;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import loci.formats.codec.CodecOptions;
import loci.formats.codec.JPEG2000CodecOptions;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.UnaryOperator;

public class CreateTiff extends BaseTask {

    private final PyramidFromDirectoryWriter converter = new PyramidFromDirectoryWriter();

    public CreateTiff(BaseWorkflow parent) {
        super(parent);
    }

    public String getName() {
        return "Convert to TIFF";
    }

    private ArrayList<Node> standardSettings;

    private ArrayList<Node> advancedSettings;


    private ChoiceBox<CompressionType> compression;

    private ChoiceBox<String> logLevel;
    private CheckBox legacy;
    private TextField maxWorkers;
    private TextField compressionQuality;

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
        LOGGER.info("Running raw2ometiff");
        this.status = JobState.status.RUNNING;
        try {
            converter.call();
            this.status = JobState.status.COMPLETED;
            LOGGER.info("TIFF creation successful");
        } catch (Exception e) {
            this.status = JobState.status.FAILED;
            LOGGER.error("TIFF creation failed - " + e);
        }
    }

    public void updateStatus() {
        if (this.status == JobState.status.COMPLETED) { return; }
        if (this.output == null | this.input == null) {
            this.status = JobState.status.WARNING;
            this.warningMessage = "I/O not configured";
        } else {
            this.status = JobState.status.READY;
        }
    }

    public void generateNodes() {
        if (standardSettings != null) return;
        // Generate standard controls
        standardSettings = new ArrayList<>();
        advancedSettings = new ArrayList<>();

        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("([1-9][0-9]*)?")) {
                return change;
            }
            return null;
        };

        UnaryOperator<TextFormatter.Change> floatFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("([0-9]*\\.?[0-9]*)?")) {
                return change;
            }
            return null;
        };


        logLevel = new ChoiceBox<>(FXCollections.observableArrayList("OFF", "ERROR", "WARN",
                "INFO", "DEBUG", "TRACE","ALL"));
        HBox logSettings = new HBox(5, new Label("Log Level"), logLevel);
        logSettings.setAlignment(Pos.CENTER_LEFT);
        standardSettings.add(logSettings);

        maxWorkers = new TextField();
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

        compressionQuality = new TextField();
        compressionQuality.setTextFormatter(new TextFormatter<>(floatFilter));
        compressionQuality.setTooltip(new Tooltip("""
                Only applied if compression type is set to JPEG-2000 Lossy.
                This option controls the encoded bitrate in bits per pixel.
                The quality is a floating point number and must be greater than 0.
                A larger number implies less data loss but also larger file size.
                By default, the quality is set to the largest positive finite value of type double
                (64 bit floating point). This is equivalent to lossless compression,
                 i.e. setting --compression to JPEG-2000.
                 To see truly lossy compression, the quality should be set to less than the
                 bit depth of the input image (e.g. less than 8 for uint8 data).
                 
                 We recommend experimenting with different quality values between 0.25 and the
                 bit depth of the input image to find an acceptable tradeoff between file size
                 and visual appeal of the converted images.
                """));
        HBox compressionQualityBox = new HBox(5, new Label("Compression Quality"), compressionQuality);
        compressionQualityBox.setAlignment(Pos.CENTER_LEFT);
        advancedSettings.add(compressionQualityBox);

    }

    public void updateNodes() {
        logLevel.getSelectionModel().select("WARN");
        maxWorkers.setText(String.valueOf(converter.getMaxWorkers()));
        compression.setValue(converter.getCompression());
        rgb.setSelected(converter.getRGB());
        split.setSelected(converter.getSplitTIFFs());
        legacy.setSelected(converter.getLegacyTIFF());
        CodecOptions opts = converter.getCompressionOptions();
        if (opts != null) {
            compressionQuality.setText(String.valueOf(opts.quality));
        } else {
            compressionQuality.setText(null);
        }
        converter.setCompressionOptions(CodecOptions.getDefaultOptions());
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

        if (compression.getValue() == CompressionType.JPEG_2000) {
            CodecOptions codec = JPEG2000CodecOptions.getDefaultOptions();
            if (!compressionQuality.getText().isEmpty()) {
                codec.quality = Double.parseDouble(compressionQuality.getText());
            }
            converter.setCompressionOptions(codec);
        } else {
            converter.setCompressionOptions(null);
        }
    }


    public void setDefaults() {
        return;
    }

    public void applyDefaults() {
        return;
    }

    public void cloneValues(BaseTask sourceInstance) {
        if (!(sourceInstance instanceof CreateTiff source)) {
            System.out.println("Incorrect input type");
            return;
        }
        if (this.standardSettings == null) {
            generateNodes();
        }
        logLevel.setValue(source.logLevel.getValue());
        maxWorkers.setText(source.maxWorkers.getText());
        compression.setValue(source.compression.getValue());
        legacy.setSelected(source.legacy.isSelected());
        rgb.setSelected(source.rgb.isSelected());
        split.setSelected(source.split.isSelected());
        compressionQuality.setText(source.compressionQuality.getText());
    }

}
