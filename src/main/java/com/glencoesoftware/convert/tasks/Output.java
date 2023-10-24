package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.convert.App;
import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
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
import org.controlsfx.control.ToggleSwitch;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

// Virtual task to allow configuration of the output file destination and other misc options
public class Output extends BaseTask {

    public static String name = "Output";
    public static final Preferences taskPreferences = Preferences.userRoot().node(name);
    public enum prefKeys {OVERWRITE, LOG_CHOICE, LOG_LOCATION, WORKING_CHOICE, WORKING_DIR, OUTPUT_CHOICE, OUTPUT_DIR}

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

    private outputLocationType outputLocation = outputLocationType.INPUT_FOLDER;
    private File outputFolder = null;
    private logFileType logToFile = logFileType.DISABLED;
    private File logFileLocation = null;

    private workingDirectoryType workingDirectoryLocation = workingDirectoryType.SYSTEM_TEMP;
    private File trueWorkingDirectory = null;

    // Static widget settings for display
    private static final ArrayList<Node> standardSettings = new ArrayList<>();
    private static final ArrayList<Node> addFilesSettings = new ArrayList<>();
    private static final ToggleSwitch overwriteBox = new ToggleSwitch();
    private static final ChoiceBox<outputLocationType> outputChoice = new ChoiceBox<>();
    private static final TextField outputDirectory = new TextField();
    private static final TextField outputFileName = new TextField();
    private static final ChoiceBox<workingDirectoryType> workingDirectoryChoice = new ChoiceBox<>();
    private static final TextField workingDirectoryField = new TextField();
    private static final ChoiceBox<logFileType> logChoice = new ChoiceBox<>();
    private static final TextField logDirectory = new TextField();
    private static final VBox outputLocationBox = getSettingGroupContainer();
    private static final VBox outputFileNameContainer;
    private static final VBox outputDirectoryContainer;
    private static final VBox outputChoiceContainer;
    private static final VBox overwriteContainer;
    private static final Label outputTitle;
    private static final FontIcon fileBrowseButton;
    private static final FontIcon fileResetButton;
    private static HBox logDirWidget;
    private static HBox workingDirWidget;
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
        // User cleared a custom name
        if (fileName.isEmpty()) fileName = input.getName();
        if (outputChoice.getValue() == outputLocationType.INPUT_FOLDER)
            this.output = new File(parent.firstInput.getParent(), fileName);
        else this.output = new File(outputDirectory.getText(), fileName);
    }

    public void prepareForDisplay() {
        bindWidgets();
        applyOutputToWidgets();
        overwriteBox.setSelected(overwrite);
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

    public void applySettings() {
        outputLocation = outputChoice.getValue();
        String outputText = outputDirectory.getText();
        if (outputText == null || outputLocation == outputLocationType.INPUT_FOLDER) outputFolder = null;
        else outputFolder = new File(outputText);
        setOutputFromWidgets();
        if (parent.tasks != null) {
            // Overwrite applies to all tasks, not just this one
            parent.setOverwrite(overwriteBox.selectedProperty().get());
        } else {
            // Todo: solve this during initial init
            overwrite = overwriteBox.selectedProperty().get();
        }

        logToFile = logChoice.getValue();
        switch (logToFile) {
            case DISABLED -> logFileLocation = null;
            case OUTPUT_FOLDER -> logFileLocation = output.getParentFile();
            case CUSTOM_FOLDER -> {
                String txt = logDirectory.getText();
                if (txt == null || txt.isEmpty()) logFileLocation = output.getParentFile();
                else logFileLocation = new File(txt);
            }
        }

        workingDirectoryLocation = workingDirectoryChoice.getValue();
        String txt = workingDirectoryField.getText();
        if (txt == null || txt.isEmpty()) trueWorkingDirectory = null;
        else trueWorkingDirectory = new File(txt);
        // Recalculate job IO in case the user switched temp directory
        if (input != null) parent.calculateIO();
    }


    public File getLogFile() {
        if (logToFile == logFileType.DISABLED) return null;
        return new File(logFileLocation, "%s.log".formatted(output.getName()));
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
                System.out.println("Failed to get temp dir");
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
                System.out.println("Oops" + workingDirectoryLocation);
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

    public void run() {
        // Apply GUI configurations first
        calculateOutput("");
        this.status = JobState.status.RUNNING;
        LOGGER.info("Saving final output file");
        try {
            if (!Objects.equals(input.getAbsolutePath(), output.getAbsolutePath())) {
                if (this.input.isDirectory()) FileUtils.moveDirectory(input, output);
                else if (overwrite) FileUtils.moveFile(input, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                else FileUtils.moveFile(input, output);
            }
            this.status = JobState.status.COMPLETED;
        } catch (IOException e) {
            this.status = JobState.status.FAILED;
            LOGGER.error("Failed to copy");
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

        // Todo: Improve directory browser widgets

        overwriteContainer = getSettingContainer(
                overwriteBox,
                "Overwrite Existing Files",
                "Choose whether existing files at the path above will be overwritten"
        );
        standardSettings.add(overwriteContainer);
        addFilesSettings.add(overwriteContainer);

        standardSettings.add(logSettingsBox);

        logChoice.getItems().setAll(logFileType.values());
        logChoice.getSelectionModel().selectedIndexProperty().addListener((e, oldVal, newVal) -> {
                logDirWidget.getParent().setVisible(newVal.intValue() == 2);
                logDirWidget.getParent().setManaged(newVal.intValue() == 2);
                }
        );
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
        logSettingsBox.getChildren().add(
                getSettingContainer(logDirWidget, "Log file location",
                        """
                Folder in which to save log files.
                """
                ));

        standardSettings.add(workingSettingsBox);
        addFilesSettings.add(workingSettingsBox);

        workingDirectoryChoice.getItems().setAll(workingDirectoryType.values());
        workingDirectoryChoice.getSelectionModel().selectedIndexProperty().addListener((i, o, v) -> {
                workingDirWidget.getParent().setVisible(v.intValue() == 2);
                workingDirWidget.getParent().setManaged(v.intValue() == 2);
                });
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

        workingSettingsBox.getChildren().add(
                getSettingContainer(workingDirWidget, "Working directory",
                        """
                Folder in which to write temporary files.
                """
                ));
    }


    public ArrayList<Node> getStandardSettings() {
        outputFileName.getParent().setVisible(true);
        outputFileName.getParent().setManaged(true);
        return standardSettings;
    }

    public ArrayList<Node> getAdvancedSettings() {
        return null;
    }

    public static ArrayList<Node> getAddFilesSettings() {
        // We hide the file name from this reduced settings display
        outputFileName.getParent().setVisible(false);
        outputFileName.getParent().setManaged(false);
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
            System.out.println("Incorrect input type");
            return;
        }
        overwrite = source.overwrite;
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

        taskPreferences.put(prefKeys.OUTPUT_CHOICE.name(), outputChoice.getValue().label);
        if (outputChoice.getValue() == outputLocationType.CUSTOM_FOLDER)
            taskPreferences.put(prefKeys.OUTPUT_DIR.name(), outputDirectory.getText());

        taskPreferences.putBoolean(prefKeys.OVERWRITE.name(), overwriteBox.isSelected());

        // Log settings
        taskPreferences.put(prefKeys.LOG_CHOICE.name(), logChoice.getValue().label);
        if (logChoice.getValue() == logFileType.CUSTOM_FOLDER)
            taskPreferences.put(prefKeys.LOG_LOCATION.name(), logDirectory.getText());

        // Working dir
        taskPreferences.put(prefKeys.WORKING_CHOICE.name(), workingDirectoryChoice.getValue().label);
        if (workingDirectoryChoice.getValue() == workingDirectoryType.CUSTOM_FOLDER)
            taskPreferences.put(prefKeys.WORKING_DIR.name(), workingDirectoryField.getText());

        taskPreferences.flush();
    }
}


