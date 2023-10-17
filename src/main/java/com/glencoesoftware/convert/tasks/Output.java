package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.convert.App;
import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import com.glencoesoftware.convert.workflows.ConvertToTiff;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.controlsfx.control.ToggleSwitch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

// Virtual task to allow configuration of the output file destination and other misc options
public class Output extends BaseTask {

    public static String name = "Output";
    public static final Preferences taskPreferences = Preferences.userRoot().node(name);
    public enum prefKeys {OVERWRITE, LOG_CHOICE, LOG_LOCATION, WORKING_CHOICE, WORKING_DIR, OUTPUT_CHOICE, OUTPUT_DIR}

    public enum outputLocationType {
        INPUT_FOLDER("Same folder as input"),
        CUSTOM_FOLDER("Custom directory");
        private String label;
        outputLocationType(String label) { this.label = label; }
        public String toString() { return label; }
    }

    public enum logFileType {
        DISABLED("Disabled"),
        OUTPUT_FOLDER("Same folder as output"),
        CUSTOM_FOLDER("Custom directory");
        private String label;
        logFileType(String label) { this.label = label; }
        public String toString() { return label; }
    }

    public enum workingDirectoryType {
        SYSTEM_TEMP("System temporary directory"),
        OUTPUT_FOLDER("Same folder as output"),
        CUSTOM_FOLDER("Custom directory");
        private String label;
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
    private static final ToggleSwitch overwriteBox = new ToggleSwitch();
    private static final ChoiceBox<outputLocationType> outputChoice = new ChoiceBox<>();
    private static final TextField outputDirectory = new TextField();
    private static final TextField outputFileName = new TextField();
    private static final ChoiceBox<workingDirectoryType> workingDirectoryChoice = new ChoiceBox<>();
    private static final TextField workingDirectoryField = new TextField();
    private static final ChoiceBox<logFileType> logChoice = new ChoiceBox<>();
    private static final TextField logDirectory = new TextField();

    public String getName() { return name; }


    public Output(BaseWorkflow parent) {
        super(parent);
        // Load the preferences stored as defaults
        applyDefaults();
    }

    private void updateOutput() {
        if (outputLocation == outputLocationType.INPUT_FOLDER)
            this.output = new File(parent.firstInput.getParent(), this.output.getName());
        else this.output = new File(outputFolder, this.output.getName());
    };

    public void applyOutputToWidgets() {
        outputDirectory.setText(this.output.getParent());
        outputFileName.setText(this.output.getName());
        if (parent.firstInput.getParent().equals(this.output.getParent())) outputChoice.setValue(outputLocationType.INPUT_FOLDER);
        else outputChoice.setValue(outputLocationType.CUSTOM_FOLDER);
    }

    public void setOutputFromWidgets() {
        if (outputChoice.getValue() == outputLocationType.INPUT_FOLDER)
            this.output = new File(parent.firstInput.getParent(), outputFileName.getText());
        else this.output = new File(outputDirectory.getText(), outputFileName.getText());
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
        // This File choosers in particular is instance-specific
        outputFileName.onMouseClickedProperty().set(e -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setInitialDirectory(this.output);
                    fileChooser.setTitle("Set output file");
                    if (parent instanceof ConvertToTiff) {
                        fileChooser.getExtensionFilters().addAll(
                                new FileChooser.ExtensionFilter("OME-TIFF Files", "*.ome.tiff", "*.ome.tif"),
                                new FileChooser.ExtensionFilter("TIFF Files", "*.tif", "*.tiff"));
                    } else fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("NGFF Files", "*.zarr", "*.ngff"));

                    File selectedFile = fileChooser.showOpenDialog(App.getScene().getWindow());
                    if (selectedFile != null) {
                        if (selectedFile.getParent().equals(input.getParent()))
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
        overwrite = overwriteBox.selectedProperty().get();

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

        updateOutput();
    }


    public File getLogFile() {
        if (logToFile == logFileType.DISABLED) return null;
        return new File(logFileLocation, "%s.log".formatted(output.getName()));
        }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean getOverwrite() {
        return this.overwrite;
    }

    public File getWorkingDirectory() {
        switch (workingDirectoryLocation) {
            case SYSTEM_TEMP -> { try {
                return Files.createTempDirectory("ngff-converter").toFile();
            } catch (IOException e) {
                System.out.println("Failed to get temp dir");
                throw new RuntimeException(e);
            }}
            case OUTPUT_FOLDER -> {
                return output.getParentFile();
            }
            case CUSTOM_FOLDER -> {
                if (trueWorkingDirectory != null) return trueWorkingDirectory;
                else return output.getParentFile();
            }
            default -> {
                System.out.println("Oops" + workingDirectoryLocation);
                return null;
            }
        }
    }

    public void setOutput(String basePath) {
        this.outputFolder = new File(basePath);
        if (basePath.equals(parent.firstInput.getAbsolutePath()))
            outputLocation = outputLocationType.INPUT_FOLDER;
        else outputLocation = outputLocationType.CUSTOM_FOLDER;
        updateOutput();
        this.parent.finalOutput = this.output;
        // Get the previous task and update it's output
        // Disabled for now, we copy over
            // this.parent.tasks.get(this.parent.tasks.size() - 2).output = this.output;
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
        updateOutput();
        this.status = JobState.status.RUNNING;
        LOGGER.info("Saving final output file");
        try {
            if (!Objects.equals(this.input.getAbsolutePath(), this.output.getAbsolutePath())) {
                // Todo: Check this works with NGFF
                Files.copy(this.input.toPath(), this.output.toPath(), ATOMIC_MOVE);
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
        // Todo: Group items in containers
        outputChoice.getItems().setAll(outputLocationType.values());
        outputChoice.getSelectionModel().selectedIndexProperty().addListener((e, oldVal, newVal) -> {
            System.out.println("Got output trigger" + newVal);
            outputDirectory.getParent().setVisible(newVal.intValue() != 0);
            outputDirectory.getParent().setManaged(newVal.intValue() != 0);
        }
        );
        standardSettings.add(
                getSettingContainer(outputChoice, "Output location",
                        """
                Where to save the resulting files. Choose whether to export
                resulting files into the same directory as the source file,
                or to a specified folder.
                """
                ));
        // Todo: Improve directory browser widgets

        outputDirectory.onMouseClickedProperty().set(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose output directory");
            File newDir = directoryChooser.showDialog(App.getScene().getWindow());
            if (newDir != null) {
                outputDirectory.setText(newDir.getAbsolutePath());
            }
        });
        standardSettings.add(
                getSettingContainer(outputDirectory, "Output directory",
                        """
                Folder where resulting files will be saved.
                Note that this is not the file name.
                """
                ));
        standardSettings.add(
        getSettingContainer(outputFileName, "Output file name",
                """
        Name of the file to be exported.
        """
        ));

        standardSettings.add(getSettingContainer(
                overwriteBox,
                "Overwrite Existing Files",
                "Choose whether existing files at the path above will be overwritten"
                ));

        logChoice.getItems().setAll(logFileType.values());
        logChoice.getSelectionModel().selectedIndexProperty().addListener((e, oldVal, newVal) -> {
                logDirectory.getParent().setVisible(newVal.intValue() == 2);
                logDirectory.getParent().setManaged(newVal.intValue() == 2);
                }
        );
        standardSettings.add(getSettingContainer(
                logChoice,
                "Log to File",
                """
                        If enabled, execution logs will also be recorded into a text file.
                        The file will be named <output_file_name>.log
                        
                        Regardless of this setting, logs are captured in the viewer within NGFF-Converter
                        """
        ));

        logDirectory.onMouseClickedProperty().set(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose log directory");
            File newDir = directoryChooser.showDialog(App.getScene().getWindow());
            if (newDir != null) {
                logDirectory.setText(newDir.getAbsolutePath());
            }
        });
        standardSettings.add(
                getSettingContainer(logDirectory, "Log file location",
                        """
                Folder in which to save log files.
                """
                ));

        workingDirectoryChoice.getItems().setAll(workingDirectoryType.values());
        workingDirectoryChoice.getSelectionModel().selectedIndexProperty().addListener((i, o, v) -> {
                workingDirectoryField.getParent().setVisible(v.intValue() == 2);
                workingDirectoryField.getParent().setManaged(v.intValue() == 2);
                });
        standardSettings.add(
                getSettingContainer(workingDirectoryChoice, "Working directory location",
                        """
                Some conversion stages will produce temporary intermediate files.
                
                This setting controls where those files are written.
                
                Temporary files are cleaned up after conversion finishes.
                """
                ));

        workingDirectoryField.onMouseClickedProperty().set(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose working directory");
            // Todo: Fix
//            directoryChooser.setInitialDirectory(getWorkingDirectory());
            File newDir = directoryChooser.showDialog(App.getScene().getWindow());
            if (newDir != null) {
                workingDirectoryField.setText(newDir.getAbsolutePath());
            }
        });
        standardSettings.add(
                getSettingContainer(workingDirectoryField, "Working directory",
                        """
                Folder in which to write temporary files.
                """
                ));
    }


    public ArrayList<Node> getStandardSettings() {
        return standardSettings;
    }

    public ArrayList<Node> getAdvancedSettings() {
        return null;
    }

    public void setDefaults() throws BackingStoreException {
        taskPreferences.clear();

        taskPreferences.put(prefKeys.OUTPUT_CHOICE.name(), outputLocation.label);
        if (outputLocation == outputLocationType.CUSTOM_FOLDER)
            taskPreferences.put(prefKeys.OUTPUT_DIR.name(), outputFolder.getAbsolutePath());

        taskPreferences.putBoolean(prefKeys.OVERWRITE.name(), overwrite);

        // Log settings
        taskPreferences.put(prefKeys.LOG_CHOICE.name(), logToFile.label);
        if (logToFile == logFileType.CUSTOM_FOLDER)
            taskPreferences.put(prefKeys.LOG_LOCATION.name(), logFileLocation.getAbsolutePath());


        // Working dir
        taskPreferences.put(prefKeys.WORKING_CHOICE.name(), workingDirectoryLocation.label);
        if (workingDirectoryLocation == workingDirectoryType.CUSTOM_FOLDER)
            taskPreferences.put(prefKeys.WORKING_DIR.name(), trueWorkingDirectory.getAbsolutePath());

        taskPreferences.flush();
    }

    public void applyDefaults() {
        // Output settings
        outputLocation = outputLocationType.valueOf(
                taskPreferences.get(prefKeys.OUTPUT_CHOICE.name(), outputLocationType.INPUT_FOLDER.name()));
        String outputFolderDefault = taskPreferences.get(prefKeys.OUTPUT_DIR.name(), null);
        if (outputFolderDefault == null) outputFolder = null;
        else outputFolder = new File(outputFolderDefault);

        if (input != null) updateOutput();

        overwrite = taskPreferences.getBoolean(prefKeys.OVERWRITE.name(), false);

        // Log settings
        logToFile = logFileType.valueOf(taskPreferences.get(prefKeys.LOG_CHOICE.name(), logFileType.DISABLED.name()));
        String logLocation = taskPreferences.get(prefKeys.LOG_LOCATION.name(), null);
        if (logLocation != null) logFileLocation = new File(logLocation);
        else logFileLocation = null;

        // Working dir
        workingDirectoryLocation = workingDirectoryType.valueOf(taskPreferences.get(prefKeys.WORKING_CHOICE.name(),
                workingDirectoryType.SYSTEM_TEMP.name()));
        String workingDir = taskPreferences.get(prefKeys.WORKING_DIR.name(), null);
        if (workingDir == null) trueWorkingDirectory = null;
        else trueWorkingDirectory = new File(workingDir);

        // Now update to respect the initial mode choice
        trueWorkingDirectory = getWorkingDirectory();

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
}


