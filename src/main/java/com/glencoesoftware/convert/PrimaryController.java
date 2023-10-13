/**
 * Copyright (c) 2022 Glencoe Software, Inc. All rights reserved.
 *
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.glencoesoftware.bioformats2raw.Converter;
import com.glencoesoftware.convert.dialogs.AddFilesDialog;
import com.glencoesoftware.convert.dialogs.ConfigureJobDialog;
import com.glencoesoftware.convert.tables.*;
import com.glencoesoftware.convert.tasks.BaseTask;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import com.glencoesoftware.convert.workflows.ConvertToNGFF;
import com.glencoesoftware.convert.workflows.ConvertToTiff;
import com.glencoesoftware.pyramid.PyramidFromDirectoryWriter;
import javafx.application.Platform;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.*;
import loci.formats.ImageReader;
import org.apache.commons.io.FilenameUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


public class PrimaryController {

    private static final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(App.class);

    @FXML
    public Label jobsText;
    public Label tasksText;
    public TextField outputDirectory;
    public TableView<BaseWorkflow> jobList;
    public TableColumn<BaseWorkflow, Boolean> workflowSelectionColumn;
    public TableColumn<BaseWorkflow, String> workflowNameColumn;
    public TableColumn<BaseWorkflow, String> workflowFormatColumn;
    public TableColumn<BaseWorkflow, Void> workflowStatusColumn;
    public TableColumn<BaseWorkflow, Void> workflowActionColumn;
    public TableView<BaseTask> taskList;
    public TableColumn<BaseTask, String> taskNameColumn;
    public TableColumn<BaseTask, Void> taskStatusColumn;
    public TableColumn<BaseTask, Void> taskActionColumn;
    public Label fileListHelpText;
    public SplitPane jobsPane;
    public Button addJobButton;
    public Button clearButton;
    public Button chooseDirButton;
    public Button clearDirButton;
    public Button runJobsButton;
    public Button configureSelectedButton;
    public Button removeSelectedButton;
    public VBox jobsBox;
    public MenuItem menuOutputFormat;
    public CheckMenuItem menuOverwrite;
    public CheckMenuItem wantLogToFile;
    public MenuItem menuRun;
    public MenuItem menuChooseDirectory;
    public MenuItem menuResetDirectory;
    public MenuItem menuTempDirectory;
    public MenuItem menuAddFiles;
    public MenuItem menuRemoveFile;
    public MenuItem menuClearFinished;
    public MenuItem menuClearAll;
    public MenuItem menuSavePrefs;
    public MenuItem menuResetPrefs;
    public MenuBar menuBar;

    private Stage consoleWindow;
    private Stage b2rHelpWindow;
    private Stage r2oHelpWindow;
    public boolean isRunning = false;
    private Thread runnerThread;
    private WorkflowRunner worker;
    private ArrayList<Control> fileControlButtons;
    private ArrayList<MenuItem> menuControlButtons;
    public TextAreaStream textAreaStream;
    public ToggleGroup logLevelGroup;
    public ToggleGroup outputFormatGroup;
    public FileAppender<ILoggingEvent> fileAppender;
    public File tempDirectory;

    // We keep a record of whether the console has been displayed to the user before.
    // If it hasn't we open it if a conversion fails.
    public boolean logShown = false;

    public final Set<String> supportedExtensions = new HashSet<>(Arrays.asList(new ImageReader().getSuffixes()));
    public String version;

    public final String defaultOutputText = "<Same as input>";

    public enum prefName {FORMAT, LOG_LEVEL, OVERWRITE, OUTPUT_FOLDER, ARGS, DEFAULT_FORMAT, SHOW_FORMAT_DLG};

    public static final Preferences userPreferences = Preferences.userRoot();

    private AddFilesDialog addFilesDialog;

    private Stage jobSettingsStage;
    private ConfigureJobDialog jobSettingsController;

    public ChangeListener<Number> taskWatcher = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            taskList.refresh();
        }
    };


    public enum OutputMode {
        TIFF("OME-TIFF", ".ome.tiff"),
        NGFF("OME-NGFF", ".zarr");

        OutputMode(String displayName, String extension) {
            this.displayName = displayName;
            this.extension = extension;
        }
        public String getDisplayName() { return this.displayName; }
        public String getExtension() { return this.extension; }
        private final String displayName;
        private final String extension;
    }

    @FXML
    public void initialize() throws IOException {
        LOGGER.setLevel(Level.DEBUG);
        supportedExtensions.add("zarr");
        supportedExtensions.add("mrxs");
        menuBar.setUseSystemMenuBar(true);

        // Hide jobs pane until we need it.
        jobsPane.setVisible(false);
        jobsPane.getDividers().get(0).positionProperty().addListener(
                (o, oldPos, newPos) -> dividerResized());

        // Configure jobs table
        CheckBox selectAllBox = new CheckBox();
        selectAllBox.setOnAction(e -> {
            for (BaseWorkflow job: jobList.getItems()) {
                job.setSelected(selectAllBox.isSelected());
            }
        });
        workflowSelectionColumn.setGraphic(selectAllBox);
        workflowSelectionColumn.setCellFactory(CheckBoxTableCell.forTableColumn(idx -> {
            handleSelectionChange();
            return jobList.getItems().get(idx).getSelected();
        }));
        workflowSelectionColumn.setCellValueFactory(cd -> cd.getValue().getSelected());
        workflowNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        workflowNameColumn.setCellValueFactory(new PropertyValueFactory<>("input"));
        workflowFormatColumn.setCellFactory(col -> new ChoiceTableCell());
        workflowFormatColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        workflowStatusColumn.setCellFactory(col -> new WorkflowStatusTableCell());
        workflowActionColumn.setCellFactory(col -> new MultiButtonTableCell());

        // Configure tasks table
        taskNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        taskNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        taskActionColumn.setCellFactory(col -> new ButtonTableCell());
        taskStatusColumn.setCellFactory(col -> new TaskStatusTableCell());

        jobList.getItems().addListener((ListChangeListener<BaseWorkflow>)(c -> {
            if (jobList.getItems().isEmpty()) {
                fileListHelpText.setVisible(true);
                jobsPane.setVisible(false);
            } else {
                // Compute column sizing
                dividerResized();
                fileListHelpText.setVisible(false);
                jobsPane.setVisible(true);
            }
        }));

        // Monitor the job list and display tasks when a job is clicked.
        jobList.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (oldSelection != null) {
                oldSelection.currentStage.removeListener(taskWatcher);
            }
            if (newSelection != null) {
                newSelection.currentStage.addListener(taskWatcher);
                taskList.setItems(newSelection.tasks);
                if (newSelection.status.get() == JobState.status.RUNNING) {
                    tasksText.setText(
                            String.format("Tasks: %d of %d",
                                    newSelection.currentStage.get(), newSelection.tasks.size()));
                }
                tasksText.setText("Tasks: " + newSelection.getInput());
                taskList.refresh();
            } else {
                taskList.setItems(null);
                tasksText.setText("Tasks");
            }
        });

        taskList.setPlaceholder(new Label("Select a job for details"));
        // Disable entry selection on the task list
        taskList.setSelectionModel(null);

        // Create the file addition dialog
        addFilesDialog = new AddFilesDialog();

        FontIcon addIcon = new FontIcon("bi-plus");
        FontIcon removeIcon = new FontIcon("bi-dash");
        FontIcon clearIcon = new FontIcon("bi-x");
        FontIcon finishedIcon = new FontIcon("bi-check");

        addIcon.setIconSize(20);
        removeIcon.setIconSize(20);
        clearIcon.setIconSize(20);
        finishedIcon.setIconSize(20);
        addJobButton.setGraphic(addIcon);
        ObservableList<String> logModes = FXCollections.observableArrayList("Debug", "Info", "Warn", "Error",
                "Trace", "All", "Off");
        outputDirectory.setText(userPreferences.get(prefName.OUTPUT_FOLDER.name(), defaultOutputText));
        outputDirectory.setTooltip(new Tooltip("Directory to save converted files to.\n" +
                "Applies to new files added to the list."));
        version = getClass().getPackage().getImplementationVersion();
        if (version == null) { version = "DEV"; }

        // Arrays of controls we want to lock during a run. Menu items have different class inheritance to controls.
        // Todo: update
        fileControlButtons = new ArrayList<>(Arrays.asList(outputDirectory, chooseDirButton, clearDirButton));
        menuControlButtons = new ArrayList<>(Arrays.asList(menuOutputFormat, menuAddFiles, menuRemoveFile,
                menuClearFinished, menuClearAll, menuSavePrefs, menuResetPrefs, menuOverwrite, menuChooseDirectory,
                menuResetDirectory, menuTempDirectory));
        createLogControl();

    }

    private void dividerResized() {
        // Adjust Name column sizes as the divider is moved
        workflowNameColumn.setMinWidth(Math.max(jobList.getWidth() - 390, 120));
        taskNameColumn.setMinWidth(Math.max(taskList.getWidth() - 150, 100));
    }

    private void createLogControl() throws IOException {
        FXMLLoader logLoader = new FXMLLoader();
        logLoader.setLocation(getClass().getResource("LogWindow.fxml"));
        Scene scene = new Scene(logLoader.load());
        LogController logControl = logLoader.getController();
        consoleWindow = new Stage();
        consoleWindow.setScene(scene);
        textAreaStream = logControl.stream;
        logControl.setParent(this);
    }

    @FXML
    private void configureDefaultFormat() {
        addFilesDialog.show(this);
    }


    @FXML
    private void b2rHelp() throws IOException {
        if (b2rHelpWindow != null) {
            b2rHelpWindow.show();
        } else {
            createHelpWindow("bioformats2raw");
        }
    }
    @FXML
    private void r2oHelp() throws IOException {
        if (r2oHelpWindow != null) {
            r2oHelpWindow.show();
        } else {
            createHelpWindow("raw2ometiff");
        }
    }

    private void createHelpWindow(String command) throws IOException {
        FXMLLoader helpLoader = new FXMLLoader();
        helpLoader.setLocation(getClass().getResource("HelpWindow.fxml"));
        Scene helpScene = new Scene(helpLoader.load());
        Stage helpWindow = new Stage();
        helpWindow.setScene(helpScene);
        Label helpHeader = (Label) helpScene.lookup("#helpHeader");
        TextArea helpContents = (TextArea) helpScene.lookup("#helpBox");
        helpHeader.setText(helpHeader.getText() + command);
        CommandLine cmd;
        if (command.equals("bioformats2raw")) {
            cmd = new CommandLine(new Converter());
            b2rHelpWindow = helpWindow;
        } else {
            cmd = new CommandLine(new PyramidFromDirectoryWriter());
            r2oHelpWindow = helpWindow;
        }
        helpContents.appendText(cmd.getUsageMessage());
        helpWindow.show();
        helpContents.setScrollTop(0);
    }

    @FXML
    private void addFiles() {
        Stage stage = (Stage) addJobButton.getScene().getWindow();
        FileChooser addFileChooser = new FileChooser();
        addFileChooser.setTitle("Select files to load...");
        List<File> newFiles = addFileChooser.showOpenMultipleDialog(stage);
        if (newFiles != null && !newFiles.isEmpty()) {
            addFilesToList(newFiles);
        }
    }

    @FXML
    private void removeFile() {
        final int selectedIdx = jobList.getSelectionModel().getSelectedIndex();
        if (selectedIdx != -1) {
            jobList.getItems().remove(selectedIdx);
        }
    }

    @FXML
    private void clearFiles() {
        jobList.getItems().clear();
    }

    @FXML
    private void removeSelected() {
        List<BaseWorkflow> new_list = jobList.getItems()
                .stream()
                .filter((item) -> (!item.getSelected().getValue()))
                .toList();
        jobList.getItems().clear();
        jobList.getItems().addAll(new_list);
    }

    @FXML
    private void configureSelected() {
        // Todo: Write this
        System.out.println("Status is:");
        for (BaseWorkflow job : jobList.getItems()) {
            System.out.println("Job " + job.getName() + job.getSelected().getValue());
        }
        displaySettingsDialog(FXCollections.observableArrayList(
                jobList.getItems().stream().filter(job -> job.getSelected().getValue()).toList()), 0);

    }

    @FXML
    private void chooseOutputDirectory() {
        Stage stage = (Stage) outputDirectory.getScene().getWindow();
        DirectoryChooser outputDirectoryChooser = new DirectoryChooser();
        outputDirectoryChooser.setTitle("Choose output directory");
        File newDir = outputDirectoryChooser.showDialog(stage);
        if (newDir != null) {
            outputDirectory.setText(newDir.getAbsolutePath());
        }
        updateJobIO();
    }

    @FXML
    private void chooseTemporaryDirectory(){
        if (tempDirectory != null) {
            tempDirectory = null;
            menuTempDirectory.setText("Choose Temporary Directory");
            LOGGER.info("Temporary intermediate files will be stored in the same folder as the final output.");
            return;
        }
        Stage stage = (Stage) outputDirectory.getScene().getWindow();
        DirectoryChooser outputDirectoryChooser = new DirectoryChooser();
        outputDirectoryChooser.setTitle("Choose temporary directory for zarr intermediates");
        File tempDir = outputDirectoryChooser.showDialog(stage);
        if (tempDir != null) {
            tempDirectory = tempDir;
            menuTempDirectory.setText("Reset Temporary Directory");
            LOGGER.info("Zarr intermediates will be temporarily written to " + tempDir);
        }
        updateJobIO();
    }

    private void updateJobIO() {
        for (BaseWorkflow job : jobList.getItems()) {
            String outBase = getOutputBaseDirectory(job.firstInput);
            String temporaryStorage = getWorkingDirectory(job.firstInput);
            job.calculateIO(job.firstInput.getAbsolutePath(), outBase, temporaryStorage);
        }
    }

    @FXML
    private void resetOutputDirectory() {
        outputDirectory.setText(defaultOutputText);
        updateJobIO();
    }

    @FXML
    private void handleSelectionChange() {
        HashSet<String> selectedTypes = new HashSet<>();
        boolean allowConfig = true;
        for (BaseWorkflow job: jobList.getItems()) {
            if (job.getSelected().getValue()) {
                selectedTypes.add(job.getName());
                if (job.status.getValue() != JobState.status.READY &&
                        job.status.getValue() != JobState.status.WARNING) {
                    // A selected job is already running, completed or failed = should not change config.
                    allowConfig = false;
                    break;
                }
            }
        }
        removeSelectedButton.setVisible(false);
        configureSelectedButton.setVisible(false);
        if (!selectedTypes.isEmpty()) {
            removeSelectedButton.setVisible(true);
            if (allowConfig && selectedTypes.size() < 2) {
                configureSelectedButton.setVisible(true);
            }
        };
    }
    @FXML
    private void handleFileDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        } else {
            event.consume();
        }
    }

    @FXML
    private void handleFileDrop(DragEvent event) {
        if (isRunning) {return;}
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            success = true;
            addFilesToList(db.getFiles());
        }
        event.setDropCompleted(success);
        event.consume();
    }

    public String getOutputBaseDirectory(File sourceFile) {
        if (outputDirectory.getText().equals(defaultOutputText)) {
            return sourceFile.getParent();
        } else {
            return outputDirectory.getText();
        }
    }

    public String getWorkingDirectory(File sourceFile) {
        if (tempDirectory != null) {
            return tempDirectory.getAbsolutePath();
        } else {
            return getOutputBaseDirectory(sourceFile);
        }
    }

    @FXML
    private void addFilesToList(List<File> files) {
        OutputMode desiredFormat = addFilesDialog.show(this);
        // Handle user cancel
        if (desiredFormat == null) { return; };

        Queue<File> fileQueue = new LinkedList<>(files);
        List<BaseWorkflow> jobs = jobList.getItems();
        HashSet<String> existing = new HashSet<>();
        for (BaseWorkflow job : jobs) {
            existing.add(job.firstInput.getAbsolutePath());
        }
        while (!fileQueue.isEmpty()) {
            File file = fileQueue.remove();
            String extension = FilenameUtils.getExtension(file.getName());
            if (file.isDirectory() && !extension.equals("zarr")) {
                // Traverse subdirectory unless it's a zarr
                fileQueue.addAll(Arrays.asList(Objects.requireNonNull(file.listFiles())));
                continue;
            }
            if (!supportedExtensions.contains(extension.toLowerCase())) {
                // Not a supported image file
                continue;
            }
            String filePath = file.getAbsolutePath();
            if (existing.contains(filePath)){
                LOGGER.debug("File already in queue: " + file.getName());
                continue;
            }
            String outBase = getOutputBaseDirectory(file);
            String temporaryStorage = getWorkingDirectory(file);
            BaseWorkflow job;
            switch (desiredFormat) {
                case TIFF -> job = new ConvertToTiff(this);
                case NGFF -> job = new ConvertToNGFF(this);
                default -> {
                    System.out.println("Invalid format " + desiredFormat);
                    return;
                }
            }
            job.calculateIO(filePath, outBase, temporaryStorage);
            jobs.add(job);
        }
    }

    @FXML
    private void listKeyHandler(KeyEvent event) {
        if (!isRunning && event.getCode().equals(KeyCode.DELETE)) {
            removeFile();
        }
    }

    @FXML
    private void clearFinished() {
        List<BaseWorkflow> new_list = jobList.getItems()
                .stream()
                .filter((item) -> (item.status.get() != JobState.status.COMPLETED))
                .toList();
        jobList.getItems().clear();
        jobList.getItems().addAll(new_list);
    }

    @FXML
    public void toggleFileLogging() {
        if (fileAppender == null) {
            Stage stage = (Stage) jobList.getScene().getWindow();
            FileChooser outputFileChooser = new FileChooser();
            outputFileChooser.setInitialFileName("ngff-converter.log");
            outputFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Log file",".log"));
            outputFileChooser.setTitle("Choose where to save logs");
            File newOutput = outputFileChooser.showSaveDialog(stage);
            if (newOutput != null) {
                String logFile = newOutput.getAbsolutePath();
                LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
                PatternLayoutEncoder ple = new PatternLayoutEncoder();
                ple.setPattern("%date [%thread] %-5level %logger{36} - %msg%n");
                ple.setContext(lc);
                ple.start();
                fileAppender = new FileAppender<>();
                fileAppender.setFile(logFile);
                fileAppender.setEncoder(ple);
                fileAppender.setContext(lc);
                fileAppender.start();
                ch.qos.logback.classic.Logger rootLogger =
                        (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
                rootLogger.addAppender(fileAppender);
//                logFileButton.setText("Stop logging to file");
                wantLogToFile.setSelected(true);
            } else {
                wantLogToFile.setSelected(false);
            }
        } else if (wantLogToFile.isSelected()) {
            fileAppender.start();
//            logFileButton.setText("Stop logging to file");
        } else {
            fileAppender.stop();
//            logFileButton.setText("Resume logging to file");
        }
    }

    @FXML
    private void toggleOverwrite() {
        boolean overwrite = menuOverwrite.isSelected();
        List<JobState.status> doNotChange = Arrays.asList(JobState.status.COMPLETED,
                JobState.status.FAILED, JobState.status.RUNNING);
        jobList.getItems().forEach((item) -> {
            if (doNotChange.contains(item.status.get())) { return; }
            if ((!overwrite) && item.finalOutput.exists()) {
                item.status.set(JobState.status.WARNING);
            } else {
                item.status.set(JobState.status.QUEUED);
            }
        });
        jobList.refresh();
    }

    @FXML
    private void overwriteMenu() {
        toggleOverwrite();
    }

//    @FXML
//    private void updateLogLevel() {
//        if (logLevelGroup == null) return;
//        String val = logLevel.getValue();
//        int idx = logLevel.getItems().indexOf(val);
//        logLevelGroup.selectToggle(logLevelGroup.getToggles().get(idx));
//    }


    @FXML
    private void resetPrefs() throws BackingStoreException {
        userPreferences.clear();
        userPreferences.flush();
        // Todo: Apply to task prefs too
    }

    @FXML
    private void savePrefs() throws BackingStoreException {
//        userPreferences.put(prefName.FORMAT.name(), outputFormat.getValue());
//        userPreferences.put(prefName.LOG_LEVEL.name(), logLevel.getValue());
//        userPreferences.putBoolean(prefName.OVERWRITE.name(), wantOverwrite.isSelected());
        userPreferences.put(prefName.OUTPUT_FOLDER.name(), outputDirectory.getText());
//        userPreferences.put(prefName.ARGS.name(), extraParams.getText());
        userPreferences.flush();
    };

    @FXML
    public Runnable displayLog() {
        consoleWindow.show();
        consoleWindow.toFront();
        logShown = true;
        return null;
    }

    @FXML
    private void displayAbout() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader();
        String bfVer = ImageReader.class.getPackage().getImplementationVersion();
        String b2rVer = Converter.class.getPackage().getImplementationVersion();
        String r2oVer = PyramidFromDirectoryWriter.class.getPackage().getImplementationVersion();
        fxmlLoader.getNamespace().put("guiVer", version);
        fxmlLoader.getNamespace().put("b2rVer", b2rVer);
        fxmlLoader.getNamespace().put("bfVer", bfVer);
        fxmlLoader.getNamespace().put("r2oVer", r2oVer);
        fxmlLoader.setLocation(App.class.getResource("AboutDialog.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(jobList.getScene().getWindow());
        stage.setResizable(false);
        stage.show();
    }

    public void displaySettingsDialog(ObservableList<BaseWorkflow> jobs, int taskIndex){
        if (jobSettingsStage == null) {
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(App.class.getResource("ConfigureJob.fxml"));
            Scene scene;
            try {
                scene = new Scene(fxmlLoader.load());
            } catch (IOException e) {
                System.out.println("Error encountered" + e);
                return;
            }
            scene.setFill(Color.TRANSPARENT);
            jobSettingsStage = new Stage();
            jobSettingsStage.setScene(scene);
            jobSettingsStage.initModality(Modality.APPLICATION_MODAL);
            jobSettingsStage.initStyle(StageStyle.TRANSPARENT);
            jobSettingsStage.initOwner(jobList.getScene().getWindow());
            jobSettingsStage.setResizable(false);
            jobSettingsController = fxmlLoader.getController();
        }
//        ObservableList<BaseWorkflow> jobs = jobList.getSelectionModel().getSelectedItems();
        jobSettingsController.initData(jobs, taskIndex);
        jobSettingsStage.show();
    }

    public void runCompleted() {
        runJobsButton.setText("Run Conversions");
        menuRun.setText("Run Conversions");
        isRunning = false;
        fileControlButtons.forEach((control -> control.setDisable(false)));
        menuControlButtons.forEach((control -> control.setDisable(false)));
        // Print anything left in the console buffer.
        textAreaStream.forceFlush();
    }

    @FXML
    public void onExit() {
        Platform.exit();
    }

    public void runCancel() throws InterruptedException {
        worker.interrupted = true;
        runnerThread.interrupt();
        runnerThread.join();
        jobList.getItems().forEach((job) -> {
            if (job.status.get() == JobState.status.RUNNING) {
                job.status.set(JobState.status.FAILED);
                for (BaseTask task: job.tasks) {
                    switch (task.status) {
                        case RUNNING, QUEUED -> task.status = JobState.status.FAILED;
                    }
                }
            }});
        jobList.refresh();
        runCompleted();
    }

    @FXML
    private void runConvert() throws Exception {
        if (isRunning) {
            // Jobs are already running, need to stop.
            runCancel();
            return;
        }

        // Validate there is enough space to perform conversions.
        HashMap<Path, Double> spaceMap = new HashMap<>();
        jobList.getItems().stream().filter(job ->
                job.status.get() != JobState.status.COMPLETED).forEach(job -> {
                    double estimatedSize = job.firstInput.length() * 1.3;
                    Path targetDrive = Paths.get(job.finalOutput.getAbsolutePath()).getRoot();
                    double neededSpace = spaceMap.getOrDefault(targetDrive, 0.0);
                    spaceMap.put(targetDrive, neededSpace + estimatedSize);
                });
        for (Map.Entry<Path, Double> entry : spaceMap.entrySet()) {
            Path drive = entry.getKey();
            double neededSpace = entry.getValue();
            double freeSpace = new File(drive.toString()).getFreeSpace();
            if (freeSpace < neededSpace) {
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        String.format("""
                                Output files from conversion may be larger than available disk space on drive %s
                                
                                Required: ~%,dMB | Free: %,dMB\s
                                
                                Do you want to continue?""",
                                drive,
                                (long) neededSpace / 1048576,
                                (long) freeSpace / 1048576),
                        ButtonType.YES,
                        ButtonType.NO);
                alert.setTitle("NGFF-Converter");
                alert.setHeaderText("Possible storage space issue");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.YES){
                    LOGGER.info("User opted to continue despite disk space warning");
                } else {
                    LOGGER.error("User aborted conversions in response to disk space warning");
                    return;
                }}
            }

        fileControlButtons.forEach((control -> control.setDisable(true)));
        menuControlButtons.forEach((control -> control.setDisable(true)));
        runJobsButton.setText("Stop Conversions");
        menuRun.setText("Stop Conversions");
        isRunning = true;
        LOGGER.info("Beginning file conversion...\n");

        worker = new WorkflowRunner(this);
        worker.interrupted = false;
        runnerThread = new Thread(worker, "Converter");
        runnerThread.setDaemon(true);
        runnerThread.start();
    }


}
