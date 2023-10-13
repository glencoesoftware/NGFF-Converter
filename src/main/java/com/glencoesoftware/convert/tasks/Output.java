package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.convert.App;
import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.controlsfx.control.ToggleSwitch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

// Virtual task to allow configuration of the output file destination and other misc options
public class Output extends BaseTask {

    public static String name = "Output";
    public static final Preferences taskPreferences = Preferences.userRoot().node(name);
    public enum prefKeys {OVERWRITE, LOG_TO_FILE, LOG_LOCATION, WORKING_DIR}

    // Variables storing true settings for this instance
    private boolean overwrite = false;
    private boolean logToFile = false;
    private File logFileLocation = null;
    private File workingDirectoryLocation = null;

    // Static widget settings for display
    private static final ArrayList<Node> standardSettings = new ArrayList<>();
    private static final ToggleSwitch overwriteBox;
    private static final ToggleSwitch logBox;
    private static final TextField logFile;
    private static final TextField outputPath;
    private static final TextField workingDirectory;

    private static ChangeListener<Boolean> logWatcher; // Current listener bound to the widget



    public String getName() { return name; }


    public Output(BaseWorkflow parent) {
        super(parent);
        // Load the preferences stored as defaults
        applyDefaults();
    }

    public void prepareForDisplay() {
        bindWidgets();
        // Todo: set output properly

//        setOutput(new File(outputPath.getText()).getParent());

        outputPath.setText(this.output.getAbsolutePath());
        overwriteBox.setSelected(overwrite);
        logBox.setSelected(logToFile);
        if (logFileLocation == null) {
            logFile.setText(defaultLogFileName);
        } else {
            logFile.setText(logFileLocation.getAbsolutePath());
        }
        if (workingDirectoryLocation == null) {
            workingDirectory.setText("");
        } else {
            workingDirectory.setText(workingDirectoryLocation.getAbsolutePath());
        }

    }

    // Attach this instance to the static widgets
    private void bindWidgets() {
        // Remove any old listener and set a new one
        if (logWatcher != null) logBox.selectedProperty().removeListener(logWatcher);
        logWatcher = (e, oldValue, newValue) -> logToFile = newValue;
        logBox.selectedProperty().addListener(logWatcher);
    }

    public void applySettings() {
        System.out.println("Applying settings for " + name);

        parent.setOverwrite(overwriteBox.selectedProperty().get());
        overwrite = overwriteBox.selectedProperty().get();

        // Todo: set output properly

        setOutput(new File(outputPath.getText()).getParent());

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



    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean getOverwrite() {
        return this.overwrite;
    }

    public File getWorkingDirectory() {
        return new File(workingDirectory.getText());
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

    private static final String defaultLogFileName = "<Output File Name>.log";

    // Generate widgets
    static {
        // Generate standard controls
        outputPath = new TextField();
        standardSettings.add(
            getSettingContainer(outputPath, "Output File",
                    """
            Path to the final output file on disk.
            Use this option to change file name and/or location.
            """
            ));

        overwriteBox = new ToggleSwitch();
        standardSettings.add(getSettingContainer(
                overwriteBox,
                "Overwrite Existing Files",
                "Choose whether existing files at the path above will be overwritten"
                ));

        logBox = new ToggleSwitch();
        standardSettings.add(getSettingContainer(
                logBox,
                "Log to File",
                "If enabled, execution logs will also be recorded into the specified text file."
        ));

        logFile = new TextField(defaultLogFileName);
        logFile.setEditable(false);
        logFile.onMouseClickedProperty().set(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Set log file");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Log Files", "*.log"),
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            File selectedFile = fileChooser.showOpenDialog(App.getScene().getWindow());
            if (selectedFile != null) {
                logFile.setText(selectedFile.getAbsolutePath());
            }
        });
        standardSettings.add(getSettingContainer(
                logFile,
                "Log File Location",
                "Choose the path to the file where execution logs will be saved to."
        ));

        workingDirectory = new TextField();
        workingDirectory.setEditable(false);
        workingDirectory.onMouseClickedProperty().set(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose working directory");
            File newDir = directoryChooser.showDialog(App.getScene().getWindow());
            if (newDir != null) {
                workingDirectory.setText(newDir.getAbsolutePath());
            }
        });
        standardSettings.add(getSettingContainer(
                workingDirectory,
                "Working Directory",
                """
            The converter may generate intermediate files during a conversion.
            This setting specifies where those should be stored (default = your OS's temp folder).
            These files are automatically cleaned up after tasks finish, but you may want to alter this setting
            if you have limited drive space available.
            """
        ));
        // TODO: Make workingdirectory the one true working directory

    }


    public ArrayList<Node> getStandardSettings() {
        return standardSettings;
    }

    public ArrayList<Node> getAdvancedSettings() {
        return null;
    }

    public void setDefaults() throws BackingStoreException {
        taskPreferences.clear();
        taskPreferences.putBoolean(prefKeys.OVERWRITE.name(), overwrite);
        taskPreferences.putBoolean(prefKeys.LOG_TO_FILE.name(), logToFile);
        if (logFileLocation != null) {
            taskPreferences.put(prefKeys.LOG_LOCATION.name(), logFileLocation.getParent());
        }
        if (workingDirectoryLocation != null) {
            taskPreferences.put(prefKeys.WORKING_DIR.name(), workingDirectoryLocation.getAbsolutePath());
        }
        taskPreferences.flush();
    }

    public void applyDefaults() {
        overwrite = taskPreferences.getBoolean(prefKeys.OVERWRITE.name(), false);
        logToFile = taskPreferences.getBoolean(prefKeys.LOG_TO_FILE.name(), false);
        // Todo: Call proper workingdir path
        String logPath = taskPreferences.get(prefKeys.LOG_LOCATION.name(), null);
        if (logPath != null) logFileLocation = new File(logPath);
        String workingPath = taskPreferences.get(prefKeys.WORKING_DIR.name(), null);
        if (workingPath != null) workingDirectoryLocation = new File(workingPath);
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


