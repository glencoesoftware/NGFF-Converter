/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert;

import ch.qos.logback.classic.Level;
import com.glencoesoftware.bioformats2raw.Converter;
import com.glencoesoftware.convert.dialogs.AddFilesDialog;
import com.glencoesoftware.convert.dialogs.ConfigureJobDialog;
import com.glencoesoftware.convert.dialogs.UpdateDialog;
import com.glencoesoftware.convert.tables.*;
import com.glencoesoftware.convert.tasks.BaseTask;
import com.glencoesoftware.convert.tasks.CreateNGFF;
import com.glencoesoftware.convert.tasks.CreateTiff;
import com.glencoesoftware.convert.tasks.Output;
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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.stage.Window;
import loci.formats.ImageReader;
import org.apache.commons.io.FilenameUtils;
import org.controlsfx.control.StatusBar;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static java.util.Map.entry;


public class PrimaryController {

    private static final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(App.class);

    @FXML
    public Label jobsText;
    public Label taskNameText;
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
    public Button runJobsButton;
    public FontIcon runJobsButtonIcon;
    public Button configureSelectedButton;
    public Button removeSelectedButton;
    public VBox jobsBox;
    public MenuItem menuOutputFormat;
    public MenuItem menuRun;
    public MenuItem menuAddFiles;
    public MenuItem menuRemoveFile;
    public MenuItem menuClearFinished;
    public MenuItem menuClearAll;
    public MenuItem menuResetPrefs;
    public MenuBar menuBar;

    private Stage b2rHelpWindow;
    private Stage r2oHelpWindow;
    public boolean isRunning = false;
    public Thread runnerThread;
    private WorkflowRunner worker;
    private List<MenuItem> menuControlButtons;

    public final Set<String> supportedExtensions = new HashSet<>(Arrays.asList(new ImageReader().getSuffixes()));

    public enum prefName {DEFAULT_FORMAT, SHOW_FORMAT_DLG, LAST_UPDATE}

    public static final Preferences userPreferences = Preferences.userRoot();

    private Stage jobSettingsStage;
    private ConfigureJobDialog jobSettingsController;

    private Stage addFilesStage;
    private AddFilesDialog addFilesController;

    private Stage updaterStage;
    private UpdateDialog updaterController;
    @FXML
    private StatusBar statusBar;

    public ChangeListener<Number> taskWatcher = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            taskList.refresh();
        }
    };

    public static Map<String, Class<? extends BaseWorkflow>> installedWorkflows  =  Map.ofEntries(
            entry(ConvertToNGFF.shortName, ConvertToNGFF.class),
            entry(ConvertToTiff.shortName, ConvertToTiff.class)
    );


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
        selectAllBox.getStyleClass().add("select-all-box");
        selectAllBox.setOnAction(e -> {
            for (BaseWorkflow job: jobList.getItems()) job.setSelected(selectAllBox.isSelected());
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
        workflowFormatColumn.setCellValueFactory(new PropertyValueFactory<>("shortName"));
        workflowStatusColumn.setCellFactory(col -> new WorkflowStatusTableCell());
        workflowActionColumn.setCellFactory(col -> new MultiButtonTableCell());
        workflowSelectionColumn.setReorderable(false);
        workflowNameColumn.setReorderable(false);
        workflowFormatColumn.setReorderable(false);
        workflowStatusColumn.setReorderable(false);
        workflowActionColumn.setReorderable(false);

        // Configure tasks table
        taskNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        taskNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        taskActionColumn.setCellFactory(col -> new ButtonTableCell());
        taskStatusColumn.setCellFactory(col -> new TaskStatusTableCell());

        taskNameColumn.setReorderable(false);
        taskStatusColumn.setReorderable(false);
        taskActionColumn.setReorderable(false);

        jobList.getItems().addListener((ListChangeListener<BaseWorkflow>)(c -> {
            updateRunButton();
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
                taskNameText.setText(newSelection.firstInput.getName());
                taskList.refresh();
            } else {
                taskList.setItems(null);
                taskNameText.setText(null);
            }
        });

        taskList.setPlaceholder(new Label("Select a job for details"));
        // Disable entry selection on the task list
        taskList.setSelectionModel(null);

        // Array of menu controls we want to lock during a run.
        menuControlButtons = Arrays.asList(menuOutputFormat, menuAddFiles, menuRemoveFile,
                menuClearFinished, menuClearAll, menuResetPrefs);
        initSecondaryDialogs();
        autoCheckForUpdate();
        updateStatus("Startup complete");
    }

    private void dividerResized() {
        // Adjust Name column sizes as the divider is moved
        workflowNameColumn.setMinWidth(Math.max(jobList.getWidth() - 375, 120));
        taskNameColumn.setMinWidth(Math.max(taskList.getWidth() - 150, 100));
    }

    private boolean displayAddFilesDialog() {
        addFilesController.prepareForDisplay();
        if (!addFilesController.shouldNotShow.isSelected()) {
            showStage(addFilesStage);
            addFilesStage.hide();
            addFilesStage.showAndWait();
            if (addFilesController.shouldProceed) {
                addFilesController.handlePostDisplay();
                return true;
            }
            return false;
        }
        return true;

    }

    private void initSecondaryDialogs() throws IOException {
        // Add Files Window
        FXMLLoader fileLoader = new FXMLLoader();
        fileLoader.setLocation(App.class.getResource("AddFiles.fxml"));
        Scene fileScene = new Scene(fileLoader.load());
        fileScene.setFill(Color.TRANSPARENT);
        addFilesStage = new Stage();
        addFilesStage.setScene(fileScene);
        addFilesStage.initModality(Modality.APPLICATION_MODAL);
        addFilesStage.initStyle(StageStyle.UNIFIED);
        addFilesStage.setResizable(false);
        addFilesController = fileLoader.getController();

        // Job settings window
        FXMLLoader settingsLoader = new FXMLLoader();
        settingsLoader.setLocation(App.class.getResource("ConfigureJob.fxml"));
        Scene jobScene = new Scene(settingsLoader.load());
        jobScene.setFill(Color.TRANSPARENT);
        jobSettingsStage = new Stage();
        jobSettingsStage.setScene(jobScene);
        jobSettingsStage.initModality(Modality.APPLICATION_MODAL);
        jobSettingsStage.initStyle(StageStyle.UNIFIED);
        jobSettingsController = settingsLoader.getController();

        // Updater Window
        FXMLLoader updaterLoader = new FXMLLoader();
        updaterLoader.setLocation(App.class.getResource("UpdateDialog.fxml"));
        Scene updaterScene = new Scene(updaterLoader.load());
        updaterScene.setFill(Color.TRANSPARENT);
        updaterStage = new Stage();
        updaterStage.setScene(updaterScene);
        updaterStage.initModality(Modality.APPLICATION_MODAL);
        updaterStage.initStyle(StageStyle.UNIFIED);
        updaterController = updaterLoader.getController();
    }

    // Moves a stage to the middle of the main program window and displays it.
    public void showStage(Stage subject) {
        Window primary = App.getScene().getWindow();
        if (subject.getOwner() == null) {
            subject.initOwner(primary);
        }
        subject.show();
        subject.setX(primary.getX() + (primary.getWidth() / 2) - (subject.getWidth() / 2));
        subject.setY(primary.getY() + (primary.getHeight() / 2) - (subject.getHeight() / 2));
    }

    @FXML
    private void configureDefaultFormat() {
        addFilesController.prepareForDisplay();
        showStage(addFilesStage);
        addFilesStage.hide();
        addFilesStage.showAndWait();
        if (addFilesController.shouldProceed) {
            addFilesController.handlePostDisplay();
        }
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
        displaySettingsDialog(FXCollections.observableArrayList(
                jobList.getItems().stream().filter(job -> job.getSelected().getValue()).toList()), 0);
    }

    @FXML
    private void handleSelectionChange() {
        HashSet<String> selectedTypes = new HashSet<>();
        boolean allowConfig = true;
        for (BaseWorkflow job: jobList.getItems()) {
            if (job.getSelected().getValue()) {
                selectedTypes.add(job.getShortName());
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
        }
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
        if (db.hasFiles()) {
            event.setDropCompleted(true);
            addFilesToList(db.getFiles());
        } else event.setDropCompleted(false);
        event.consume();
    }

    @FXML
    private void addFilesToList(List<File> files) {
        boolean wantProceed = displayAddFilesDialog();
        // Handle if user cancelled
        if (!wantProceed) return;
        String desiredFormat = addFilesController.getOutputFormat();

        Queue<File> fileQueue = new LinkedList<>(files);
        List<BaseWorkflow> jobs = jobList.getItems();
        HashSet<String> existing = new HashSet<>();
        for (BaseWorkflow job : jobs)
            if (job.status.get() != JobState.status.COMPLETED) existing.add(job.firstInput.getAbsolutePath());
        int count = 0;
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
            BaseWorkflow job;
            try {
                // Lookup the job name in the workflow list
                Constructor<? extends BaseWorkflow> jobClass = installedWorkflows.get(
                        desiredFormat).getConstructor(PrimaryController.class, File.class);
                job = jobClass.newInstance(this, file);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                // We catch these but they ---should--- never happen
                throw new RuntimeException(e);
            }

            job.calculateIO();
            jobs.add(job);
            count++;
        }
        LOGGER.info("Added %d jobs".formatted(count));
        updateStatus("Added %d jobs".formatted(count));
        if (count == 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "Selected file(s) were either not supported or already in the job list",
                    ButtonType.OK);
            alert.setTitle("Add Jobs");
            alert.setHeaderText("No jobs added");
            alert.getDialogPane().getStylesheets().add(
                    Objects.requireNonNull(App.class.getResource("Alert.css")).toExternalForm());
            alert.showAndWait();
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
        updateStatus("Cleared completed job(s)");
    }


    @FXML
    private void resetPrefs() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "This will reset all Workflow defaults.", ButtonType.YES, ButtonType.NO
                );
        confirm.setTitle("Reset all settings");
        confirm.setHeaderText("Clear all saved settings?");
        confirm.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(App.class.getResource("Alert.css")).toExternalForm());
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    userPreferences.clear();
                    CreateNGFF.taskPreferences.clear();
                    CreateTiff.taskPreferences.clear();
                    Output.taskPreferences.clear();
                    userPreferences.flush();
                    CreateNGFF.taskPreferences.flush();
                    CreateTiff.taskPreferences.flush();
                    Output.taskPreferences.flush();
                    updateStatus("Preferences were reset");

                } catch (BackingStoreException e) {
                    LOGGER.error("Unable to save preferences" + e);
                    updateStatus("Failed to reset preferences");
                }
            }});
    }


    @FXML
    private void displayAbout() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(App.class.getResource("AboutDialog.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = new Stage();
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.UNIFIED);
        stage.initOwner(App.getScene().getWindow());
        stage.setResizable(false);
        stage.show();
    }

    public void displaySettingsDialog(ObservableList<BaseWorkflow> jobs, int taskIndex){
        jobSettingsController.initData(jobs, taskIndex);
        showStage(jobSettingsStage);
    }

    public void updateRunButton() {
        if (isRunning) {
            menuRun.setText("Stop job(s)");
            runJobsButton.setText("Stop job(s)");
            runJobsButtonIcon.setIconLiteral("bi-stop-fill");
            runJobsButton.setDisable(false);
        } else {
            menuRun.setText("Run job(s)");
            runJobsButton.setText("Run job(s)");
            runJobsButtonIcon.setIconLiteral("bi-play-fill");
            runJobsButton.setDisable(true);
            menuRun.setDisable(true);

            for (BaseWorkflow job : jobList.getItems()) {
                // Only enable the runJobsButton if a job is runnable
                if (job.status.get() == JobState.status.READY || job.status.get() == JobState.status.WARNING) {
                    runJobsButton.setDisable(false);
                    menuRun.setDisable(false);
                    break;
                }
            }
        }
    }

    @FXML
    private void manualCheckForUpdate() {
        try {
            updaterController.doUpdateCheck();
        } catch (BackingStoreException | IOException e) {
            LOGGER.error("Failed to check for updates");
            updateStatus("Failed to check for updates");
            return;
        }
        updaterController.show();
        updateStatus("Update check complete");
    }

    private void autoCheckForUpdate(){
        if (updaterController.shouldCheckForUpdates.isSelected()) {
            try {
                updaterController.doUpdateCheck();
            } catch (BackingStoreException | IOException e) {
                LOGGER.error("Failed to check for updates");
                return;
            }
            if (updaterController.updateAvailable) {
                Platform.runLater(() -> updaterController.show());

                updateStatus("A new version of NGFF-Converter is available");
            } else {
                updateStatus("No newer versions of NGFF-Converter are available");
            }
        }
        System.out.println("Check completed");

    }

    public void runCompleted() {
        isRunning = false;
        updateRunButton();
        menuControlButtons.forEach((control -> control.setDisable(false)));
        addJobButton.setDisable(false);
        updateStatus("Run finished");
        updateProgress(0.0);
    }

    @FXML
    public void onExit() {
        Platform.exit();
    }

    public void runCancel() throws InterruptedException {
        worker.interrupted = true;
        updateStatus("Stopping run");
        runnerThread.interrupt();
        runnerThread.join();
        jobList.getItems().forEach((job) -> {
            // If we're stopping a running job we need to shut it down
            if (job.status.get() == JobState.status.RUNNING) {
                job.shutdown();
            }
            else if (job.status.get() == JobState.status.QUEUED) {
                job.status.set(JobState.status.READY);
                for (BaseTask task: job.tasks) task.status = JobState.status.READY;
                // Recalculate current status now that we're not queued
                job.respondToUpdate();
            }
        });

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
        updateStatus("Validating drive space");
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
                Optional<ButtonType> result = warnLowDriveSpace(drive, (long) neededSpace, (long) freeSpace);
                if (result.isPresent() && result.get() == ButtonType.YES){
                    LOGGER.info("User opted to continue despite disk space warning");
                } else {
                    LOGGER.error("User aborted conversions in response to disk space warning");
                    updateStatus("Run cancelled");
                    return;
                }}
            }

        menuControlButtons.forEach((control -> control.setDisable(true)));
        addJobButton.setDisable(true);
        isRunning = true;
        updateRunButton();
        LOGGER.info("Beginning file conversion...\n");
        updateStatus("Beginning file conversion");

        worker = new WorkflowRunner(this);
        worker.interrupted = false;
        runnerThread = new Thread(worker, "Converter");
        runnerThread.setDaemon(true);
        runnerThread.start();
    }

    private static Optional<ButtonType> warnLowDriveSpace(Path drive, long neededSpace, long freeSpace) {
        Alert alert = new Alert(Alert.AlertType.WARNING,
                String.format("""
                        Output files from conversion may be larger than available disk space on drive %s
                        
                        Required: ~%,dMB | Free: %,dMB\s
                        
                        Do you want to continue?
                        """,
                        drive,
                        neededSpace / 1048576,
                        freeSpace / 1048576),
                ButtonType.YES,
                ButtonType.NO);
        alert.setTitle("NGFF-Converter");
        alert.setHeaderText("Possible storage space issue");

        return alert.showAndWait();
    }

    public void updateStatus(String newStatus) {
        Platform.runLater(() -> statusBar.setText(newStatus));
    }

    public void updateProgress(double newProgress) {
        Platform.runLater(() -> statusBar.setProgress(newProgress));
        // Javafx doesn't support native taskbar interaction, but on some platforms we can do this with basic awt.
        Taskbar taskbar = Taskbar.getTaskbar();
        if (taskbar.isSupported(Taskbar.Feature.PROGRESS_VALUE)) {
            if (newProgress == 0) Taskbar.getTaskbar().setProgressValue(-1);
            else Taskbar.getTaskbar().setProgressValue((int) (newProgress * 100));
        }
    }

}

// Todo: Expand about dialog
