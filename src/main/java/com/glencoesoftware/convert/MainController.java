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
import com.glencoesoftware.convert.tasks.BaseTask;
import com.glencoesoftware.convert.tasks.CreateNGFF;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import com.glencoesoftware.convert.workflows.ConvertToNGFF;
import com.glencoesoftware.pyramid.PyramidFromDirectoryWriter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import loci.formats.ImageReader;
import org.apache.commons.io.FilenameUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;


public class MainController {

    private static final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(App.class);

    @FXML
    public Label jobsText;
    public Label tasksText;
    public TextField outputDirectory;
    public ListView<BaseWorkflow> jobList;
    public ListView<BaseTask> taskList;
    public Label fileListHelpText;
    public SplitPane jobsPane;
    public Button addJobButton;
    public Button clearButton;
    public Button chooseDirButton;
    public Button clearDirButton;
    public Button runJobsButton;
    public VBox jobsBox;
    public VBox tasksBox;
    public Menu menuLogLevel;
    public Menu menuOutputFormat;
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
    public ConsoleStream consoleStream;
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

    public enum jobStatus {READY, ERROR, COMPLETED, FAILED, RUNNING, NO_OUTPUT}

    public enum prefName {FORMAT, LOG_LEVEL, OVERWRITE, OUTPUT_FOLDER, ARGS};

    private final Preferences userPreferences = Preferences.userRoot();

    public HashMap<TextField, Method> setters = new HashMap<>();

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
        jobList.setCellFactory(list -> new JobCell());
//        taskList.setCellFactory(list -> new TaskCell());
        FontIcon addIcon = new FontIcon("bi-plus");
        FontIcon removeIcon = new FontIcon("bi-dash");
        FontIcon clearIcon = new FontIcon("bi-x");
        FontIcon finishedIcon = new FontIcon("bi-check");
        FontIcon logIcon = new FontIcon("bi-text-left");
        addIcon.setIconSize(20);
        removeIcon.setIconSize(20);
        clearIcon.setIconSize(20);
        finishedIcon.setIconSize(20);
        logIcon.setIconSize(20);
        addJobButton.setGraphic(addIcon);
//        clearButton.setGraphic(removeIcon);
//        clearDirButton.setGraphic(clearIcon);
        ObservableList<String> logModes = FXCollections.observableArrayList("Debug", "Info", "Warn", "Error",
                "Trace", "All", "Off");
        logLevelGroup = new ToggleGroup();
        logModes.forEach(mode -> {
            RadioMenuItem item = new RadioMenuItem(mode);
            item.setToggleGroup(logLevelGroup);
            if (Objects.equals(mode, "Warn")) {
                item.setSelected(true);
            }
            menuLogLevel.getItems().add(item);
        });
        outputDirectory.setText(userPreferences.get(prefName.OUTPUT_FOLDER.name(), defaultOutputText));
        outputDirectory.setTooltip(new Tooltip("Directory to save converted files to.\n" +
                "Applies to new files added to the list."));
        version = getClass().getPackage().getImplementationVersion();
        if (version == null) { version = "DEV"; }

        // Arrays of controls we want to lock during a run. Menu items have different class inheritance to controls.
        fileControlButtons = new ArrayList<>(Arrays.asList(outputDirectory, chooseDirButton, clearDirButton));
        menuControlButtons = new ArrayList<>(Arrays.asList(menuLogLevel, menuOutputFormat, menuAddFiles, menuRemoveFile,
                menuClearFinished, menuClearAll, menuSavePrefs, menuResetPrefs, menuOverwrite, menuChooseDirectory,
                menuResetDirectory, menuTempDirectory));
        createLogControl();
        System.out.println("Complete");

    }

    private void createLogControl() throws IOException {
        FXMLLoader logLoader = new FXMLLoader();
        logLoader.setLocation(getClass().getResource("LogWindow.fxml"));
        Scene scene = new Scene(logLoader.load());
        LogController logControl = logLoader.getController();
        consoleWindow = new Stage();
        consoleWindow.setScene(scene);
        consoleStream = logControl.stream;
        logControl.setParent(this);
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
        final int selectedIdx = taskList.getSelectionModel().getSelectedIndex();
        if (selectedIdx != -1) {
            taskList.getItems().remove(selectedIdx);
        }
        if (taskList.getItems().isEmpty()) {
            fileListHelpText.setVisible(true);
            jobsPane.setVisible(false);
        }
    }

    @FXML
    private void clearFiles() {
        taskList.getItems().clear();
        fileListHelpText.setVisible(true);
        jobsPane.setVisible(false);
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
    }

    @FXML
    private void resetOutputDirectory() {
        outputDirectory.setText(defaultOutputText);
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

    @FXML
    private void addFilesToList(List<File> files) {
        int count = 0;
        Queue<File> fileQueue = new LinkedList<>(files);
        List<BaseWorkflow> jobs = jobList.getItems();
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
            String outPath = FilenameUtils.getBaseName(filePath);
            String outBase;
            if (outputDirectory.getText().equals(defaultOutputText)) {
                outBase = file.getParent();
            } else {
                outBase = outputDirectory.getText();
            }
            String temporaryStorage;
            if (tempDirectory != null) {
                temporaryStorage = tempDirectory.getAbsolutePath();
            } else {
                temporaryStorage = outBase;
            }
            BaseWorkflow job = new ConvertToNGFF();
            job.calculateIO(filePath, outBase, temporaryStorage);
            jobs.add(job);
            count++;
        }
        if (!jobs.isEmpty()) {
            fileListHelpText.setVisible(false);
            jobsPane.setVisible(true);
        }
    }

    @FXML
    private void listClickHandler(MouseEvent event) throws IOException {
        return;
    }
//        if (event.getButton().equals(MouseButton.PRIMARY)) {
//            if (event.getClickCount() == 2) {
//                if (isRunning) { return; }
//                final BaseWorkflow target = jobList.getSelectionModel().getSelectedItem();
//                if (target != null) {
//                    if (target.status == jobStatus.COMPLETED) {
//                        Desktop.getDesktop().open(target.finalOutput.getParentFile());
//                        return;
//                    }
//                    // Todo: Full UI for editing file path
//                    Stage stage = (Stage) jobList.getScene().getWindow();
//                    FileChooser outputFileChooser = new FileChooser();
//                    outputFileChooser.setInitialDirectory(target.finalOutput.getParentFile());
//                    outputFileChooser.setInitialFileName(target.finalOutput.getName());
//                    FileChooser.ExtensionFilter zarrFilter = new FileChooser.ExtensionFilter(
//                            "Zarr file", OutputMode.NGFF.getExtension());
//                    FileChooser.ExtensionFilter tiffFilter = new FileChooser.ExtensionFilter(
//                            "OME TIFF file", OutputMode.TIFF.getExtension());
//                    outputFileChooser.getExtensionFilters().add(zarrFilter);
//                    outputFileChooser.getExtensionFilters().add(tiffFilter);
//                    if (target.outputMode == OutputMode.NGFF) {
//                        outputFileChooser.setSelectedExtensionFilter(zarrFilter);
//                    } else {
//                        outputFileChooser.setSelectedExtensionFilter(tiffFilter);
//                    }
//                    outputFileChooser.setTitle("Choose output file for " + target.fileIn.getName());
//                    File newOutput = outputFileChooser.showSaveDialog(stage);
//                    if (newOutput != null) {
//                        String desiredExtension = outputFileChooser.getSelectedExtensionFilter().getExtensions().get(0);
//                        if (!newOutput.getName().toLowerCase().endsWith(desiredExtension)) {
//                            newOutput = new File(newOutput.getAbsolutePath() + desiredExtension);
//                        }
//                        if (desiredExtension.equals(OutputMode.NGFF.getExtension())){
//                            target.outputMode = OutputMode.NGFF;
//                        } else {
//                            target.outputMode = OutputMode.TIFF;
//                        }
//                        target.fileOut = newOutput;
//                        // Reset status
//                        target.status = jobStatus.READY;
//                        jobList.refresh();
//                    }
//                    }
//
//                }
//            }
//        }

    @FXML
    private void listKeyHandler(KeyEvent event) {
        if (!isRunning && event.getCode().equals(KeyCode.DELETE)) {
            removeFile();
        }
    }

    @FXML
    private void clearFinished() {
        jobList.setItems(jobList.getItems()
                .stream()
                .filter((item) -> (item.status != BaseWorkflow.workflowStatus.COMPLETED))
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));
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
        List<BaseWorkflow.workflowStatus> doNotChange = Arrays.asList(BaseWorkflow.workflowStatus.COMPLETED,
                BaseWorkflow.workflowStatus.FAILED, BaseWorkflow.workflowStatus.RUNNING);
        jobList.getItems().forEach((item) -> {
            if (doNotChange.contains(item.status)) { return; }
            if ((!overwrite) && item.finalOutput.exists()) {
                item.status = BaseWorkflow.workflowStatus.WARNING;
            } else {
                item.status = BaseWorkflow.workflowStatus.PENDING;
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

    public void runCompleted() {
        runJobsButton.setText("Run Conversions");
        menuRun.setText("Run Conversions");
        isRunning = false;
        fileControlButtons.forEach((control -> control.setDisable(false)));
        menuControlButtons.forEach((control -> control.setDisable(false)));
        // Print anything left in the console buffer.
        consoleStream.forceFlush();
    }

    @FXML
    public void onExit() {
        Platform.exit();
    }

    public void runCancel() throws InterruptedException {
        worker.interrupted = true;
        runnerThread.interrupt();
        runnerThread.join();
//        jobList.getItems().forEach((job) -> {
//            if (job.status == workflowStatus.RUNNING) {
//                job.status = workflowStatus.FAILED;
//            }});
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
        jobList.getItems().stream().filter(job -> job.status != BaseWorkflow.workflowStatus.COMPLETED).forEach(job -> {
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
        List<String> extraArgs =  new ArrayList<>();
//        extraArgs.add("--log-level=" + logLevel.getValue().toUpperCase());
//        if (menuOverwrite.isSelected()) {
//            extraArgs.add("--overwrite");
//        }
//        String[] userArgs = extraParams.getText().split("\n");
//
//        Arrays.asList(userArgs).forEach((String userArg) -> {
//            if (userArg.equals("")) return;
//            // Fix missing '--'
//            if (!userArg.startsWith("-")) {
//                userArg = "--" + userArg;
//            }
//            // Fix common typo using space instead of equals
//            if (!userArg.contains("=") && userArg.chars().filter(num -> num == ' ').count() == 1) {
//                    userArg = userArg.replace(' ', '=');
//            }
//            extraArgs.add(userArg);
//        });

        worker = new WorkflowRunner(this);
        worker.interrupted = false;
        runnerThread = new Thread(worker);
        runnerThread.setDaemon(true);
        runnerThread.start();
    }


}
