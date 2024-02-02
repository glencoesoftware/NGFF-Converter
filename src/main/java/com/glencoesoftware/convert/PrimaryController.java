/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert;

import ch.qos.logback.classic.Level;
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
import javafx.application.Platform;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.*;
import javafx.scene.input.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.stage.Window;
import loci.formats.ImageReader;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.controlsfx.control.HyperlinkLabel;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.StatusBar;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
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

    public IntegerProperty completedJobs = new SimpleIntegerProperty(0);
    public IntegerProperty queuedJobs = new SimpleIntegerProperty(0);

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
    public Button runSelectedButton;
    public Button removeSelectedButton;
    public VBox jobsBox;
    public MenuItem menuOutputFormat;
    public MenuItem menuRun;
    public MenuItem menuAddFiles;
    public MenuItem menuAddZarr;
    public MenuItem menuRemoveFile;
    public MenuItem menuClearFinished;
    public MenuItem menuClearAll;
    public MenuItem menuResetPrefs;
    public MenuBar menuBar;

    private List<MenuItem> menuControlButtons;

    public final Set<String> supportedExtensions = new HashSet<>(Arrays.asList(new ImageReader().getSuffixes()));

    public enum prefName {DEFAULT_FORMAT, SHOW_FORMAT_DLG, LAST_UPDATE}

    public static final Preferences userPreferences = Preferences.userRoot();

    private Stage jobSettingsStage;
    private ConfigureJobDialog jobSettingsController;

    private Stage addFilesStage;
    private AddFilesDialog addFilesController;

    private UpdateDialog updaterController;
    @FXML
    private StatusBar statusBar;

    public final ChangeListener<Number> taskWatcher = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            taskList.refresh();
        }
    };

    public static final Map<String, Class<? extends BaseWorkflow>> installedWorkflows  =  Map.ofEntries(
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

        // Bind progress meters
        queuedJobs.addListener((o, oldPos, newPos) -> Platform.runLater(this::updateProgress));
        completedJobs.addListener((o, oldPos, newPos) -> Platform.runLater(this::updateProgress));

        // Array of menu controls we want to lock during a run.
        menuControlButtons = Arrays.asList(menuOutputFormat, menuAddFiles, menuAddZarr, menuRemoveFile,
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
        addFilesStage.setResizable(false);
        addFilesStage.getIcons().add(App.appIcon);
        addFilesController = fileLoader.getController();

        // Job settings window
        FXMLLoader settingsLoader = new FXMLLoader();
        settingsLoader.setLocation(App.class.getResource("ConfigureJob.fxml"));
        Scene jobScene = new Scene(settingsLoader.load());
        jobScene.setFill(Color.TRANSPARENT);
        jobSettingsStage = new Stage();
        jobSettingsStage.setScene(jobScene);
        jobSettingsStage.initModality(Modality.APPLICATION_MODAL);
        jobSettingsStage.getIcons().add(App.appIcon);
        jobSettingsController = settingsLoader.getController();

        // Updater Window
        FXMLLoader updaterLoader = new FXMLLoader();
        updaterLoader.setLocation(App.class.getResource("UpdateDialog.fxml"));
        Scene updaterScene = new Scene(updaterLoader.load());
        updaterScene.setFill(Color.TRANSPARENT);
        Stage updaterStage = new Stage();
        updaterStage.setScene(updaterScene);
        updaterStage.initModality(Modality.APPLICATION_MODAL);
        updaterStage.initStyle(StageStyle.UTILITY);
        updaterStage.getIcons().add(App.appIcon);
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
    private void b2rHelp() {
        try {
            Desktop.getDesktop().browse(new URI("https://ngff.openmicroscopy.org/"));
        } catch (IOException | URISyntaxException e) {
            LOGGER.error("Failed to open URL");
            throw new RuntimeException(e);
        }
    }
    @FXML
    private void r2oHelp() {
        try {
            Desktop.getDesktop().browse(new URI("https://ome-model.readthedocs.io/en/stable/ome-tiff/"));
        } catch (IOException | URISyntaxException e) {
            LOGGER.error("Failed to open URL");
            throw new RuntimeException(e);
        }
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
    private void addFolder() {
        Stage stage = (Stage) addJobButton.getScene().getWindow();
        DirectoryChooser addDirChooser = new DirectoryChooser();
        addDirChooser.setTitle("Select Zarr root folder to load...");
        File directory = addDirChooser.showDialog(stage);
        if (directory != null) {
            addFilesToList(List.of(directory));
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
    private void runSelected() {
        for (BaseWorkflow job: jobList.getItems())
            if (job.getSelected().getValue() && job.canRun()) job.queueJob();
    }

    @FXML
    private void handleSelectionChange() {
        HashSet<String> selectedTypes = new HashSet<>();
        boolean allowConfig = !jobList.getItems().isEmpty();
        for (BaseWorkflow job: jobList.getItems()) {
            if (job.getSelected().getValue()) {
                if (job.status.get() == JobState.status.RUNNING) {
                    // Absolutely can't remove/configure a running job
                    selectedTypes.clear();
                    break;
                }
                selectedTypes.add(job.getTechnicalName());
                if (!job.canRun()) {
                    // A selected job is already running, completed or failed = should not change config.
                    allowConfig = false;
                    break;
                }
            }
        }
        removeSelectedButton.setDisable(true);
        configureSelectedButton.setDisable(true);
        runSelectedButton.setDisable(true);
        if (!selectedTypes.isEmpty()) {
            removeSelectedButton.setDisable(false);
            runSelectedButton.setDisable(!allowConfig);
            if (allowConfig && selectedTypes.size() < 2) {
                configureSelectedButton.setDisable(false);
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
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            event.setDropCompleted(true);
            List<File> files = db.getFiles();
            Platform.runLater(() -> addFilesToList(files));
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
            Window primary = App.getScene().getWindow();
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "Selected file(s) already in the job list or not supported",
                    ButtonType.OK);
            alert.initOwner(primary);
            alert.setTitle("Add Jobs");
            alert.setHeaderText("No jobs added");
            alert.getDialogPane().getStylesheets().add(
                    Objects.requireNonNull(App.class.getResource("Alert.css")).toExternalForm());
            alert.showAndWait();
        }
    }

    @FXML
    private void listKeyHandler(KeyEvent event) {
        if (!jobsRunning() && event.getCode().equals(KeyCode.DELETE)) {
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
        Window primary = App.getScene().getWindow();
        confirm.initOwner(primary);
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
        stage.initStyle(StageStyle.UTILITY);
        stage.initOwner(App.getScene().getWindow());
        stage.setResizable(false);
        stage.getIcons().add(App.appIcon);
        stage.show();
    }

    public void displaySettingsDialog(ObservableList<BaseWorkflow> jobs, int taskIndex){
        jobSettingsController.initData(jobs, taskIndex);
        showStage(jobSettingsStage);
    }

    public void updateRunButton() {
        // Update "Run/Stop Jobs" button state
        if (jobsRunning()) {
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
    }

    public boolean jobsRunning() {
        for (BaseWorkflow job: jobList.getItems()) {
            if (job.status.get() == JobState.status.QUEUED || job.status.get() == JobState.status.RUNNING) return true;
        }
        return false;
    }

    public void jobFinished() {
        if (jobsRunning()) {
            completedJobs.setValue(completedJobs.getValue() + 1);
            return;
        }
        Pos position = Pos.BOTTOM_RIGHT;
        if (SystemUtils.IS_OS_MAC) position = Pos.TOP_RIGHT;
        menuControlButtons.forEach((control -> control.setDisable(false)));
        addJobButton.setDisable(false);
        updateStatus("Run finished");
        Notifications.create()
                .title("NGFF-Converter")
                .position(position)
                .text("%d conversions have finished".formatted(completedJobs.getValue() + 1))
                .showInformation();
        completedJobs.setValue(0);
        queuedJobs.setValue(0);
    }

    @FXML
    public void onExit() {
        Stage stage = (Stage) addJobButton.getScene().getWindow();
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    public void runCancel() {
        updateStatus("Stopping all tasks");
        jobList.getItems().forEach((job) -> {
            if (job.status.get() == JobState.status.QUEUED ||
                    job.status.get() == JobState.status.RUNNING) job.cancelJob();
        });
        jobList.refresh();
    }

    @FXML
    private void runConvert() {
        if (jobsRunning()) {
            // Jobs are already running, need to stop.
            runCancel();
            return;
        }

        // Validate there is enough space to perform conversions.
        updateStatus("Validating drive space");
        HashMap<Path, Double> spaceMap = new HashMap<>();
        jobList.getItems().stream().filter(BaseWorkflow::canRun).forEach(job -> {
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
        LOGGER.info("Beginning file conversion...\n");
        updateStatus("Beginning file conversion");
        for (BaseWorkflow job: jobList.getItems()) {
            if (job.canRun()) {
                job.queueJob();
            }
        }
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
        Window primary = App.getScene().getWindow();
        alert.initOwner(primary);
        alert.setTitle("NGFF-Converter");
        alert.setHeaderText("Possible storage space issue");

        return alert.showAndWait();
    }

    public void updateStatus(String newStatus) {
        Platform.runLater(() -> statusBar.setText(newStatus));
    }

    public void updateProgress() {
        double newProgress = jobsRunning() ? 0.01 : 0;
        int completed = completedJobs.getValue();
        if (completed > 0) newProgress = (double) completed / queuedJobs.getValue();
        statusBar.setProgress(newProgress);
        // Javafx doesn't support native taskbar interaction, but on some platforms we can do this with basic awt.
        Taskbar taskbar = Taskbar.getTaskbar();
        if (taskbar.isSupported(Taskbar.Feature.PROGRESS_VALUE)) {
            if (newProgress == 0) Taskbar.getTaskbar().setProgressValue(-1);
            else Taskbar.getTaskbar().setProgressValue((int) (newProgress * 100));
        }
    }

    public void showExceptionDialog(Throwable ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(App.getScene().getWindow());
        alert.setTitle("NGFF-Converter Error");
        alert.setHeaderText("Unhandled Exception!");
        alert.setContentText("NGFF-Converter encountered an error:\n\n%s\n\n".formatted(ex));

        // Fetch exception text.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        HyperlinkLabel link = new HyperlinkLabel("Please consider reporting this on [GitHub]");
        link.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/glencoesoftware/NGFF-Converter/issues"));
            } catch (IOException | URISyntaxException e) {
                LOGGER.error("Failed to open URL");
            }
        });

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        Button copyButton = new Button("Copy to Clipboard");
        copyButton.setOnAction(e -> Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(exceptionText), null));

        VBox expContent = new VBox(5, new Label("The exception traceback was:"), textArea, copyButton, link);
        expContent.setMaxWidth(Double.MAX_VALUE);

        // Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);
        alert.show();
    }

}
