/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert.tasks;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.glencoesoftware.convert.App;
import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import com.glencoesoftware.convert.workflows.ConvertToTiff;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.controlsfx.control.ToggleSwitch;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

// Virtual task to allow configuration of the output file destination and other misc options
public class Output extends BaseTask {

    public static final String name = "Output";
    public static final Preferences taskPreferences = Preferences.userRoot().node(name);
    public enum prefKeys {OVERWRITE, DIRECT_WRITE, LOG_CHOICE, LOG_LOCATION,
        WORKING_CHOICE, WORKING_DIR, OUTPUT_CHOICE, OUTPUT_DIR}

    public enum outputLocationType {
        INPUT_FOLDER("Same folder as input"),
        CUSTOM_FOLDER("Custom directory");
        private final String label;
        outputLocationType(String label) { this.label = label; }
        public String toString() { return label; }
    }

    public enum logFileType {
        DISABLED("Disabled"),
        OUTPUT_FOLDER("Same folder as output"),
        CUSTOM_FOLDER("Custom directory");
        private final String label;
        logFileType(String label) { this.label = label; }
        public String toString() { return label; }
    }

    public enum workingDirectoryType {
        SYSTEM_TEMP("System temporary directory"),
        OUTPUT_FOLDER("Same folder as output"),
        CUSTOM_FOLDER("Custom directory");
        private final String label;
        workingDirectoryType(String label) { this.label = label; }
        public String toString() { return label; }
    }

    // Variables storing true settings for this instance. n.b. this.input and this.output arise from BaseTask.
    private boolean overwrite = false;
    private boolean directWrite = true;

    private outputLocationType outputLocation = outputLocationType.INPUT_FOLDER;
    private File outputFolder = null;
    private logFileType logToFile = logFileType.DISABLED;
    private File logFileLocation = null;

    private workingDirectoryType workingDirectoryLocation = workingDirectoryType.SYSTEM_TEMP;
    private File trueWorkingDirectory = null;

    // Static widget settings for display
    private static final ArrayList<Node> standardSettings = new ArrayList<>();
    private static final ArrayList<Node> addFilesSettings = new ArrayList<>();
    static final ToggleSwitch overwriteBox = new ToggleSwitch();
    private static final ToggleSwitch directWriteBox = new ToggleSwitch();
    private static final ChoiceBox<outputLocationType> outputChoice = new ChoiceBox<>();
    private static final TextField outputDirectory = new TextField();
    private static final TextField outputFileName = new TextField();
    private static final ChoiceBox<workingDirectoryType> workingDirectoryChoice = new ChoiceBox<>();
    private static final TextField workingDirectoryField = new TextField();
    private static final ChoiceBox<logFileType> logChoice = new ChoiceBox<>();
    private static final TextField logDirectory = new TextField();
    private static final VBox outputLocationBox = getSettingGroupContainer();
    public static final VBox outputFileNameContainer;
    private static final VBox outputDirectoryContainer;
    private static final VBox outputChoiceContainer;
    private static final VBox overwriteContainer;
    private static final VBox directWriteContainer;
    private static final Label outputTitle;
    private static final FontIcon fileBrowseButton;
    private static final FontIcon fileResetButton;
    private static final HBox logDirWidget;
    private static final HBox workingDirWidget;
    private static final VBox logSettingsBox = getSettingGroupContainer();
    private static final VBox workingSettingsBox = getSettingGroupContainer();
    private static File sysTemp = null;

    public String getName() { return name; }

    public Output(BaseWorkflow parent) {
        super(parent);
        // When adding jobs we reset the widgets for the addFilesDialog,
        // so if the dialog wasn't shown we're just applying defaults here
        applySettings();
    }


    public void applyOutputToWidgets() {
        outputDirectory.setText(this.output.getParent());
        outputFileName.setText(this.output.getName());
        if (parent.firstInput.getParent().equals(this.output.getParent())) outputChoice.setValue(outputLocationType.INPUT_FOLDER);
        else outputChoice.setValue(outputLocationType.CUSTOM_FOLDER);
    }

    public void setOutputFromWidgets() {
        // Can only do this if input was already set
        if (input == null) return;
        String fileName = outputFileName.getText();
        String extension = parent.getOutputExtension();
        // User cleared a custom name
        if (fileName.isEmpty()) fileName = input.getName();
        else if (!fileName.toLowerCase().endsWith(extension)) {
            fileName += extension;
            outputFileName.setText(fileName);
        }
        if (outputChoice.getValue() == outputLocationType.INPUT_FOLDER)
            this.output = new File(parent.firstInput.getParent(), fileName);
        else this.output = new File(outputDirectory.getText(), fileName);
    }

    public void prepareForDisplay() {
        bindWidgets();
        applyOutputToWidgets();
        overwriteBox.setSelected(overwrite);
        directWriteBox.setSelected(directWrite);
        logChoice.setValue(logToFile);
        if (logFileLocation != null) logDirectory.setText(logFileLocation.getAbsolutePath());
        workingDirectoryChoice.setValue(workingDirectoryLocation);
        if (trueWorkingDirectory != null) workingDirectoryField.setText(trueWorkingDirectory.getAbsolutePath());
    }

    // Attach this instance to the static widgets
    private void bindWidgets() {
        // This File chooser in particular is instance-specific
        fileResetButton.onMouseClickedProperty().set(e -> outputFileName.setText(input.getName()));

        fileBrowseButton.onMouseClickedProperty().set(e -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setInitialDirectory(this.output.getParentFile());
                    fileChooser.setInitialFileName(this.output.getName());
                    fileChooser.setTitle("Set output file");
                    fileChooser.getExtensionFilters().addAll(parent.getExtensionFilters());
                    File selectedFile = fileChooser.showSaveDialog(App.getScene().getWindow());
                    if (selectedFile != null) {
                        if (selectedFile.getParent().equals(parent.firstInput.getParent()))
                            outputChoice.getSelectionModel().select(0);
                        else outputChoice.getSelectionModel().select(1);
                        outputDirectory.setText(selectedFile.getParent());
                        outputFileName.setText(selectedFile.getName());
                    }
                });
    }

    public int applySettings() {
        int errors = 0;
        outputLocation = outputChoice.getValue();
        String outputText = outputDirectory.getText();
        if (outputText == null || outputLocation == outputLocationType.INPUT_FOLDER) outputFolder = null;
        else outputFolder = new File(outputText);
        setOutputFromWidgets();
        overwrite = overwriteBox.selectedProperty().get();
        if (parent.tasks != null) {
            // Overwrite applies to all tasks, not just this one
            // Won't work on init, but this is handled by the task defaults
            parent.setOverwrite(overwrite);
        }
        directWrite = directWriteBox.selectedProperty().get();
        logToFile = logChoice.getValue();
        logDirectory.getStyleClass().remove("setting-warn");
        switch (logToFile) {
            case DISABLED, OUTPUT_FOLDER -> logFileLocation = null;
            case CUSTOM_FOLDER -> {
                String txt = logDirectory.getText();
                if (txt == null || txt.isEmpty()) logFileLocation = output.getParentFile();
                else {
                    logFileLocation = new File(txt);
                    if (!logFileLocation.canWrite()) {
                        errors++;
                        if (!logDirectory.getStyleClass().contains("setting-warn"))
                            logDirectory.getStyleClass().add("setting-warn");
                    }
                }
            }
        }

        workingDirectoryLocation = workingDirectoryChoice.getValue();
        String txt = workingDirectoryField.getText();
        workingDirectoryField.getStyleClass().remove("setting-warn");
        if (txt == null || txt.isEmpty()) trueWorkingDirectory = null;
        else {
            trueWorkingDirectory = new File(txt);
            if (!trueWorkingDirectory.canWrite()) {
                errors++;
                if (!workingDirectoryField.getStyleClass().contains("setting-warn"))
                    workingDirectoryField.getStyleClass().add("setting-warn");
            }
        }
        // Recalculate job IO in case the user switched temp directory
        if (input != null) parent.calculateIO();
        outputFileName.getStyleClass().remove("setting-warn");
        if (output != null && !output.getParentFile().canWrite()) {
            errors++;
            if (!outputFileName.getStyleClass().contains("setting-warn"))
                outputFileName.getStyleClass().add("setting-warn");
        }
        // No settings
        return errors;
    }


    public File getLogFile() {
        switch (logToFile) {
            case DISABLED -> {
                return null;
            }
            case OUTPUT_FOLDER -> {
                return new File(output.getParentFile(), "%s.log".formatted(output.getName()));
            }
            default -> {
                return new File(logFileLocation, "%s.log".formatted(output.getName()));
            }
        }
    }

    public void setOverwrite(boolean shouldOverwrite) {
        overwrite = shouldOverwrite;
    }

    public boolean getOverwrite() {
        return this.overwrite;
    }

    private static File getSysTemp() {
        if (sysTemp == null) {
            try {
                sysTemp = Files.createTempDirectory("ngff-converter").toFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
        }}
        return sysTemp;
    }

    public File getWorkingDirectory() {
        switch (workingDirectoryLocation) {
            case SYSTEM_TEMP -> {
                return getSysTemp();
            }
            case OUTPUT_FOLDER -> {
                return getOutputFolder();
            }
            case CUSTOM_FOLDER -> {
                return trueWorkingDirectory;
            }
            default -> {
                LOGGER.error("Invalid working dir: " + workingDirectoryLocation);
                return null;
            }
        }
    }

    public void calculateOutput(String workingDir) {
        // This is the virtual output task, so we actually don't care about the working directory our parent proposes

        // If we're not calling this for the first time (output exists), make sure we copy over any custom filename
        String sourceName;
        if (output == null) sourceName = input.getName();
        else sourceName = output.getName();
        this.output = new File(getOutputFolder(), sourceName);
    }

    public File getOutputFolder() {
        switch (outputLocation) {
            case INPUT_FOLDER -> {
                return parent.firstInput.getParentFile();
            }
            case CUSTOM_FOLDER -> {
                return outputFolder;
            }
            default -> {
                LOGGER.error("Unknown output folder type: " + outputLocation);
                return null;
            }
        }
    }

    public void updateStatus() {
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

    @Override
    public void prepareToRun() {
        calculateOutput("");
        if (output.exists() && !overwrite) {
            parent.statusText = "Output file already exists (file overwrite setting is disabled)";
            LOGGER.error("Output file already exists (overwriting is disabled). Aborting run.");
            throw new IllegalArgumentException("Output file already exists");
        }
        if (workingDirectoryLocation == workingDirectoryType.OUTPUT_FOLDER) directWrite = true;
        if (directWrite) {
            // Replace the final runner task's output with the target directory
            BaseTask secondLastTask = parent.tasks.get(parent.tasks.size() - 2);
            secondLastTask.output = output;
        }
    }

    public void run() {
        // Apply GUI configurations first
        this.status = JobState.status.RUNNING;
        if (directWrite){
            // No work to do in this mode
            this.status = JobState.status.COMPLETED;
            return;
        }

        LOGGER.info("Saving final output file");
        try {
            File[] toMove;
            String baseName = input.getName().replace(".ome.tiff", "");
            String finalName = output.getName().replace(".ome.tiff", "");
            if (directWrite || !parent.getShortName().equals(ConvertToTiff.shortName)) toMove = new File[]{input};
            else {
                File[] matching = input.getParentFile().listFiles(
                        (FilenameFilter) new WildcardFileFilter(
                                input.getName().replace(".ome.tiff", "*.ome.tiff")));
                // Look for companion file from split mode
                File companion = new File(input.getParent(), baseName + ".companion.ome");
                if (companion.exists()) {
                    toMove = Arrays.copyOf(Objects.requireNonNull(matching), matching.length + 1);
                    toMove[matching.length] = companion;
                } else toMove = matching;
            }
            for (File fileToMove : Objects.requireNonNull(toMove)) {
                if (!Objects.equals(fileToMove.getAbsolutePath(), output.getAbsolutePath())) {
                    if (fileToMove.isDirectory()) FileUtils.moveDirectory(fileToMove, output);
                    else {
                        String moveName = fileToMove.getName().replace(baseName, finalName);
                        File targetFile = new File(output.getParentFile().getAbsolutePath(), moveName);
                        if (overwrite)
                            FileUtils.moveFile(fileToMove, targetFile,
                                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                        else FileUtils.moveFile(fileToMove, targetFile);
                    }
                }
            }
            LOGGER.error("Output file was written sucessfully");
            this.status = JobState.status.COMPLETED;
        } catch (IOException e) {
            this.status = JobState.status.FAILED;
            LOGGER.error("Failed to copy");
            LOGGER.error(String.valueOf(e));
        }
    }

    // Generate widgets
    static {
        // Generate standard controls
        outputTitle = getSettingHeader("Output Location",
                "Choose where to save the resulting files");
        standardSettings.add(outputLocationBox);
        addFilesSettings.add(outputLocationBox);

        outputChoice.getItems().setAll(outputLocationType.values());

        // We do the file name widget manually as it needs special handling
        fileBrowseButton = new FontIcon("bi-folder-fill");
        fileBrowseButton.setIconSize(16);
        fileBrowseButton.getStyleClass().add("file-browser-button");

        fileResetButton = new FontIcon("bi-x-circle-fill");
        fileResetButton.setIconSize(16);
        fileResetButton.getStyleClass().add("file-browser-button");
        HBox fileNameWidget = new HBox(5, outputFileName, fileBrowseButton, fileResetButton);
        HBox.setHgrow(outputFileName, Priority.ALWAYS);
        fileNameWidget.setAlignment(Pos.CENTER_LEFT);

        outputFileNameContainer = getSettingContainer(fileNameWidget, "File name", "");
        outputChoiceContainer = getSettingContainer(outputChoice, "Location", "");

        HBox outputDirWidget = getDirectorySelectWidget(outputDirectory, "Choose output directory", null);

        outputDirectoryContainer = getSettingContainer(outputDirWidget, "Directory", "");

        outputChoice.getSelectionModel().selectedItemProperty().addListener(
                (observableValue, oldVal, newVal) -> {
                    outputLocationBox.getChildren().clear();
                    switch (newVal) {
                        case INPUT_FOLDER -> outputLocationBox.getChildren().addAll(
                                outputTitle, outputFileNameContainer, outputChoiceContainer);
                        case CUSTOM_FOLDER -> outputLocationBox.getChildren().addAll(
                                outputTitle, outputFileNameContainer, outputChoiceContainer, outputDirectoryContainer);
                    }

                });


        overwriteContainer = getSettingContainer(
                overwriteBox,
                "Overwrite Existing Files",
                "Choose whether existing files at the path above will be overwritten"
        );
        standardSettings.add(overwriteContainer);
        addFilesSettings.add(overwriteContainer);

        directWriteContainer = getSettingContainer(
                directWriteBox,
                "Write output directly to destination folder",
                """
                        If enabled, the final output file will be written directly to the destination folder.
                        If disabled, data is first written to the working directory, and is only moved to the
                        destination folder if all tasks complete successfully.
                        """
        );
        standardSettings.add(directWriteContainer);
        addFilesSettings.add(directWriteContainer);

        standardSettings.add(logSettingsBox);

        logSettingsBox.getChildren().add(getSettingContainer(
                logChoice,
                "Log to File",
                """
                        If enabled, execution logs will also be recorded into a text file.
                        The file will be named <output_file_name>.log
                        
                        Regardless of this setting, logs are captured in the viewer within NGFF-Converter
                        """
        ));

        logDirWidget = getDirectorySelectWidget(logDirectory, "Choose log directory", null);

        logChoice.getItems().setAll(logFileType.values());
        logChoice.getSelectionModel().selectedIndexProperty().addListener((e, oldVal, newVal) -> {
                    logDirWidget.getParent().setVisible(newVal.intValue() == 2);
                    logDirWidget.getParent().setManaged(newVal.intValue() == 2);
                }
        );

        logSettingsBox.getChildren().add(
                getSettingContainer(logDirWidget, "Log file location",
                        """
                Folder in which to save log files.
                """
                ));

        standardSettings.add(workingSettingsBox);
        addFilesSettings.add(workingSettingsBox);

        workingSettingsBox.getChildren().add(
                getSettingContainer(workingDirectoryChoice, "Working directory location",
                        """
                Some conversion stages will produce temporary intermediate files.
                
                This setting controls where those files are written.
                
                Temporary files are cleaned up after conversion finishes.
                """
                ));

        workingDirWidget = getDirectorySelectWidget(workingDirectoryField, "Choose working directory",
                getSysTemp());

        workingDirectoryChoice.getItems().setAll(workingDirectoryType.values());
        workingDirectoryChoice.getSelectionModel().selectedItemProperty().addListener(
                (observableValue, oldVal, newVal) -> {
                    // Only show custom folder entry if selected
                    workingDirWidget.getParent().setVisible(newVal == workingDirectoryType.CUSTOM_FOLDER);
                    workingDirWidget.getParent().setManaged(newVal == workingDirectoryType.CUSTOM_FOLDER);
                    if (newVal == workingDirectoryType.OUTPUT_FOLDER) {
                        // DirectWrite mode is irrelevant if writing to output anyway
                        directWriteBox.setSelected(true);
                        directWriteBox.setDisable(true);
                    } else directWriteBox.setDisable(false);
                });

        workingSettingsBox.getChildren().add(
                getSettingContainer(workingDirWidget, "Working directory",
                        """
                Folder in which to write temporary files.
                """
                ));
    }


    public ArrayList<Node> getStandardSettings() {
        outputFileNameContainer.setVisible(true);
        outputFileNameContainer.setManaged(true);
        return standardSettings;
    }

    public ArrayList<Node> getAdvancedSettings() {
        return null;
    }

    public static ArrayList<Node> getAddFilesSettings() {
        // We hide the file name from this reduced settings display
        outputFileNameContainer.setVisible(false);
        outputFileNameContainer.setManaged(false);
        return addFilesSettings;
    }

    public void setDefaults() throws BackingStoreException {
        // Just use the static widgets
        setDefaultsFromWidgets();
    }

    public void applyDefaults() {
        // Output settings
        resetWidgets();
        applySettings();
    }

    public void cloneValues(BaseTask sourceInstance) {
        if (!(sourceInstance instanceof Output source)) {
            LOGGER.error("Incorrect input type for value cloning");
            return;
        }
        overwrite = source.overwrite;
        directWrite = source.directWrite;
        logToFile = source.logToFile;
        logFileLocation = source.logFileLocation;
        workingDirectoryLocation = source.workingDirectoryLocation;
    }

    public void resetToDefaults() {
        applyDefaults();
    }

    // Reset the static widgets back to defaults
    public static void resetWidgets() {
        outputChoice.setValue(outputLocationType.valueOf(taskPreferences.get(prefKeys.OUTPUT_CHOICE.name(),
                outputLocationType.INPUT_FOLDER.name())));

        String outputFolderDefault = taskPreferences.get(prefKeys.OUTPUT_DIR.name(), null);
        outputDirectory.setText(Objects.requireNonNullElse(outputFolderDefault, ""));

        overwriteBox.setSelected(taskPreferences.getBoolean(prefKeys.OVERWRITE.name(), false));
        directWriteBox.setSelected(taskPreferences.getBoolean(prefKeys.DIRECT_WRITE.name(), true));

        // Log settings
        logChoice.setValue(logFileType.valueOf(
                taskPreferences.get(prefKeys.LOG_CHOICE.name(), logFileType.DISABLED.name())));

        String logLocation = taskPreferences.get(prefKeys.LOG_LOCATION.name(), null);
        logDirectory.setText(Objects.requireNonNullElse(logLocation, ""));

        // Working dir
        workingDirectoryChoice.setValue(workingDirectoryType.valueOf(taskPreferences.get(prefKeys.WORKING_CHOICE.name(),
                workingDirectoryType.SYSTEM_TEMP.name())));
        String workingDir = taskPreferences.get(prefKeys.WORKING_DIR.name(), null);
        workingDirectoryField.setText(Objects.requireNonNullElse(workingDir, ""));
    }

    public static void setDefaultsFromWidgets() throws BackingStoreException {
        taskPreferences.clear();

        taskPreferences.put(prefKeys.OUTPUT_CHOICE.name(), outputChoice.getValue().name());
        if (outputChoice.getValue() == outputLocationType.CUSTOM_FOLDER)
            taskPreferences.put(prefKeys.OUTPUT_DIR.name(), outputDirectory.getText());

        taskPreferences.putBoolean(prefKeys.OVERWRITE.name(), overwriteBox.isSelected());
        taskPreferences.putBoolean(prefKeys.DIRECT_WRITE.name(), directWriteBox.isSelected());

        // Log settings
        taskPreferences.put(prefKeys.LOG_CHOICE.name(), logChoice.getValue().name());
        if (logChoice.getValue() == logFileType.CUSTOM_FOLDER)
            taskPreferences.put(prefKeys.LOG_LOCATION.name(), logDirectory.getText());

        // Working dir
        taskPreferences.put(prefKeys.WORKING_CHOICE.name(), workingDirectoryChoice.getValue().name());
        if (workingDirectoryChoice.getValue() == workingDirectoryType.CUSTOM_FOLDER)
            taskPreferences.put(prefKeys.WORKING_DIR.name(), workingDirectoryField.getText());

        taskPreferences.flush();
    }

    public void exportSettings(JsonGenerator generator) throws IOException {
        // Ensure displayed settings are what gets saved
        applySettings();
        generator.writeFieldName(getName());
        generator.writeStartObject();
        generator.writeFieldName(prefKeys.OUTPUT_CHOICE.name());
        generator.writeString(outputLocation.name());
        if (outputLocation == outputLocationType.CUSTOM_FOLDER) {
            generator.writeFieldName(prefKeys.OUTPUT_DIR.name());
            generator.writeString(outputFolder.getAbsolutePath());
        }
        generator.writeFieldName(prefKeys.OVERWRITE.name());
        generator.writeBoolean(overwrite);
        generator.writeFieldName(prefKeys.DIRECT_WRITE.name());
        generator.writeBoolean(directWrite);
        generator.writeFieldName(prefKeys.LOG_CHOICE.name());
        generator.writeString(logToFile.name());
        if (logToFile == logFileType.CUSTOM_FOLDER) {
            generator.writeFieldName(prefKeys.LOG_LOCATION.name());
            generator.writeString(logFileLocation.getAbsolutePath());
        }

        generator.writeFieldName(prefKeys.WORKING_CHOICE.name());
        generator.writeString(workingDirectoryLocation.name());
        if (workingDirectoryLocation == workingDirectoryType.CUSTOM_FOLDER) {
            generator.writeFieldName(prefKeys.WORKING_DIR.name());
            generator.writeString(trueWorkingDirectory.getAbsolutePath());
        }
        generator.writeEndObject();

    }

    public void importSettings(JsonNode mainNode) {
        JsonNode settings = mainNode.get(getName());
        if (settings == null) {
            LOGGER.warn("No settings node for Task %s".formatted(getName()));
            return;
        }
        JsonNode subject;
        subject = settings.get(prefKeys.OUTPUT_CHOICE.name());
        if (subject != null) outputChoice.setValue(outputLocationType.valueOf(subject.textValue()));

        subject = settings.get(prefKeys.OUTPUT_DIR.name());
        if (subject != null) outputDirectory.setText(subject.textValue());

        subject = settings.get(prefKeys.OVERWRITE.name());
        if (subject != null) overwriteBox.setSelected(subject.booleanValue());

        subject = settings.get(prefKeys.DIRECT_WRITE.name());
        if (subject != null) directWriteBox.setSelected(subject.booleanValue());

        subject = settings.get(prefKeys.LOG_CHOICE.name());
        if (subject != null) logChoice.setValue(logFileType.valueOf(subject.textValue()));

        subject = settings.get(prefKeys.LOG_LOCATION.name());
        if (subject != null) logDirectory.setText(subject.textValue());

        subject = settings.get(prefKeys.WORKING_CHOICE.name());
        if (subject != null) workingDirectoryChoice.setValue(workingDirectoryType.valueOf(subject.textValue()));

        subject = settings.get(prefKeys.WORKING_DIR.name());
        if (subject != null) workingDirectoryField.setText(subject.textValue());

        LOGGER.info("Loaded settings for Task %s".formatted(getName()));
    }
}
