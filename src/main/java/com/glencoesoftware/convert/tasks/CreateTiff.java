/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert.tasks;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.tasks.progress.ConverterProgressListener;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import com.glencoesoftware.pyramid.CompressionType;
import com.glencoesoftware.pyramid.PyramidFromDirectoryWriter;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import loci.formats.codec.CodecOptions;
import loci.formats.codec.JPEG2000CodecOptions;
import org.controlsfx.control.ToggleSwitch;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.UnaryOperator;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

// Run raw2ometiff on a file
public class CreateTiff extends BaseTask {

    private final PyramidFromDirectoryWriter converter = new PyramidFromDirectoryWriter();
    private final CommandLine cli = new CommandLine(converter);

    public static final String name = "Convert to TIFF";

    public static final Preferences taskPreferences = Preferences.userRoot().node(name);
    public enum prefKeys {LOG_LEVEL, MAX_WORKERS, COMPRESSION, LEGACY, RGB, SPLIT, COMPRESSION_OPTS}

    private static final ArrayList<Node> standardSettings = new ArrayList<>();

    private static final ArrayList<Node> advancedSettings = new ArrayList<>();

    private static final ChoiceBox<CompressionType> compression;

    private static final ChoiceBox<String> logLevel;
    private static final ToggleSwitch legacy;
    private static final TextField maxWorkers;
    private static final TextField compressionQuality;

    private static final ToggleSwitch rgb;

    private static final ToggleSwitch split;

    private boolean overwrite = Output.overwriteBox.selectedProperty().get();

    public String getName() { return name; }

    public CreateTiff(BaseWorkflow parent) {
        super(parent);
        // Apply default args from cli
        cli.parseArgs();
        // Load the preferences stored as defaults
        applyDefaults();
    }

    // Load settings from this instance's converter into the static widgets
    public void prepareForDisplay() {
        // Populate setting values
        logLevel.getSelectionModel().select(converter.getLogLevel());
        maxWorkers.setText(String.valueOf(converter.getMaxWorkers()));
        compression.setValue(converter.getCompression());
        rgb.setSelected(converter.getRGB());
        split.setSelected(converter.getSplitTIFFs());
        legacy.setSelected(converter.getLegacyTIFF());
        CodecOptions opts = converter.getCompressionOptions();
        if (opts != null) {
            compressionQuality.setText(String.valueOf(opts.quality));
        } else {
            compressionQuality.clear();
        }
        converter.setCompressionOptions(CodecOptions.getDefaultOptions());
    }


    // Save settings from widgets into the converter's values
    public void applySettings() {
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

    public void calculateOutput(String basePath) {
        this.output = Paths.get(basePath, this.outputName + ".ome.tiff").toFile();
    }

    private void setupIO() {
        converter.setInputPath(this.input.getAbsolutePath());
        converter.setOutputPath(this.output.getAbsolutePath());
    }

    public void setOverwrite(boolean shouldOverwrite) {
        overwrite = shouldOverwrite;
    }

    public void run() {
        // Apply GUI configurations first
        setupIO();
        ConverterProgressListener listener = new ConverterProgressListener(progressBar, progressLabel, timerLabel);
        converter.setProgressListener(listener);
        LOGGER.info("Running raw2ometiff");
        this.status = JobState.status.RUNNING;
        try {
            if (!overwrite && output.exists()) throw new IOException("Output path already exists");
            converter.call();
            this.status = JobState.status.COMPLETED;
            LOGGER.info("TIFF creation successful");
        } catch (Exception e) {
            this.status = JobState.status.FAILED;
            LOGGER.error("TIFF creation failed - " + e);
            parent.statusText = "Job Failed: " + e;
        } finally {
            listener.stop();
        }
    }

    public void updateStatus() {
        if (this.status == JobState.status.COMPLETED) { return; }
        if (this.output == null | this.input == null) {
            this.status = JobState.status.WARNING;
            this.warningMessage = "I/O not configured";
        } else if (!overwrite && output.exists()) {
            this.status = JobState.status.WARNING;
            this.warningMessage = "Output file already exists";
        } else {
            this.status = JobState.status.READY;
        }
    }


    // Generate standard controls
    static {
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
        standardSettings.add(getSettingContainer(
                logLevel,
                "Log Level",
                "Detail level of logs to record"
        ));

        maxWorkers = new TextField();
        maxWorkers.setTextFormatter(new TextFormatter<>(integerFilter));
        standardSettings.add(getSettingContainer(
                maxWorkers,
                "Max Workers",
                "Maximum number of worker processes to use for this task"
        ));

        compression = new ChoiceBox<>();
        compression.setItems(FXCollections.observableArrayList( CompressionType.values()));
        standardSettings.add(getSettingContainer(
                compression,
                "Compression Type",
                "Compression type for output OME-TIFF file"
        ));

        rgb = new ToggleSwitch();
        advancedSettings.add(getSettingContainer(
                rgb,
                "RGB",
                "Attempt to write channels as RGB; channel count must be a multiple of 3"
        ));

        split = new ToggleSwitch();
        advancedSettings.add(getSettingContainer(
                split,
                "Split Series",
                "Split output into one OME-TIFF file per OME Image/Zarr group"
        ));

        legacy = new ToggleSwitch();
        advancedSettings.add(getSettingContainer(
                legacy,
                "Legacy Mode",
                "Write a Bio-Formats 5.9.x pyramid instead of OME-TIFF"
        ));

        compressionQuality = new TextField();
        compressionQuality.setTextFormatter(new TextFormatter<>(floatFilter));
        advancedSettings.add(getSettingContainer(
                compressionQuality,
                "Compression Quality",
                """
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
                 """
        ));

    }


    public ArrayList<Node> getStandardSettings() {
        return standardSettings;
    }

    public ArrayList<Node> getAdvancedSettings() {
        return advancedSettings;
    }


    public void setDefaults() throws BackingStoreException {
        taskPreferences.clear();
        taskPreferences.put(prefKeys.LOG_LEVEL.name(), converter.getLogLevel());
        taskPreferences.putInt(prefKeys.MAX_WORKERS.name(), converter.getMaxWorkers());
        taskPreferences.put(prefKeys.COMPRESSION.name(), converter.getCompression().name());
        taskPreferences.putBoolean(prefKeys.LEGACY.name(), converter.getLegacyTIFF());
        taskPreferences.putBoolean(prefKeys.RGB.name(), converter.getRGB());
        taskPreferences.putBoolean(prefKeys.SPLIT.name(), converter.getSplitTIFFs());
        taskPreferences.putDouble(prefKeys.COMPRESSION_OPTS.name(), converter.getCompressionOptions().quality);
        taskPreferences.flush();
    }

    public void applyDefaults() {
        converter.setLogLevel(taskPreferences.get(prefKeys.LOG_LEVEL.name(), converter.getLogLevel()));
        converter.setMaxWorkers(taskPreferences.getInt(prefKeys.MAX_WORKERS.name(), converter.getMaxWorkers()));
        converter.setCompression(CompressionType.lookup(
                taskPreferences.get(prefKeys.COMPRESSION.name(), converter.getCompression().name())));
        converter.setLegacyTIFF(taskPreferences.getBoolean(prefKeys.LEGACY.name(), converter.getLegacyTIFF()));
        converter.setRGB(taskPreferences.getBoolean(prefKeys.RGB.name(), converter.getRGB()));
        converter.setSplitTIFFs(taskPreferences.getBoolean(prefKeys.SPLIT.name(), converter.getSplitTIFFs()));
        CodecOptions codec = null;
        if (converter.getCompression() == CompressionType.JPEG_2000) {
            codec = JPEG2000CodecOptions.getDefaultOptions();
            if (!compressionQuality.getText().isEmpty()) {
                codec.quality = taskPreferences.getDouble(
                        prefKeys.COMPRESSION_OPTS.name(), converter.getCompressionOptions().quality);
            }}
        converter.setCompressionOptions(codec);
    }

    public void cloneValues(BaseTask sourceInstance) {
        if (!(sourceInstance instanceof CreateTiff source)) {
            LOGGER.error("Incorrect input type for value cloning");
            return;
        }
        converter.setLogLevel(source.converter.getLogLevel());
        converter.setMaxWorkers(source.converter.getMaxWorkers());
        converter.setCompression(source.converter.getCompression());
        converter.setLegacyTIFF(source.converter.getLegacyTIFF());
        converter.setRGB(source.converter.getRGB());
        converter.setSplitTIFFs(source.converter.getSplitTIFFs());
        converter.setCompressionOptions(source.converter.getCompressionOptions());
    }

    public void resetToDefaults() {
        resetConverter();
        applyDefaults();
    }

    public void resetConverter() {
        // Apply default args from cli
        cli.parseArgs();
    }

    public void exportSettings(JsonGenerator generator) throws IOException {
        generator.writeFieldName(getName());
        generator.writeStartObject();
        generator.writeFieldName(prefKeys.LOG_LEVEL.name());
        generator.writeString(converter.getLogLevel());
        generator.writeFieldName(prefKeys.MAX_WORKERS.name());
        generator.writeString(String.valueOf(converter.getMaxWorkers()));
        generator.writeFieldName(prefKeys.COMPRESSION.name());
        generator.writeString(converter.getCompression().name());
        generator.writeFieldName(prefKeys.LEGACY.name());
        generator.writeBoolean(converter.getLegacyTIFF());
        generator.writeFieldName(prefKeys.RGB.name());
        generator.writeBoolean(converter.getRGB());
        generator.writeFieldName(prefKeys.SPLIT.name());
        generator.writeBoolean(converter.getSplitTIFFs());
        generator.writeFieldName(prefKeys.COMPRESSION_OPTS.name());
        generator.writeString(String.valueOf(converter.getCompressionOptions().quality));
        generator.writeEndObject();
    }

    public void importSettings(JsonNode mainNode) {
        JsonNode settings = mainNode.get(getName());
        if (settings == null) {
            LOGGER.warn("No settings node for Task %s".formatted(getName()));
            return;
        }
        JsonNode subject;
        subject = settings.get(prefKeys.LOG_LEVEL.name());
        if (subject != null) logLevel.setValue(subject.textValue());

        subject = settings.get(prefKeys.MAX_WORKERS.name());
        if (subject != null) maxWorkers.setText(String.valueOf(subject.intValue()));

        subject = settings.get(prefKeys.COMPRESSION.name());
        if (subject != null) compression.setValue(CompressionType.valueOf(subject.textValue()));

        subject = settings.get(prefKeys.LEGACY.name());
        if (subject != null) legacy.setSelected(subject.booleanValue());

        subject = settings.get(prefKeys.RGB.name());
        if (subject != null) rgb.setSelected(subject.booleanValue());

        subject = settings.get(prefKeys.SPLIT.name());
        if (subject != null) split.setSelected(subject.booleanValue());

        subject = settings.get(prefKeys.COMPRESSION_OPTS.name());
        if (subject != null) compressionQuality.setText(subject.textValue());

        LOGGER.info("Loaded settings for Task %s".formatted(getName()));
    }
}
