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
import com.glencoesoftware.pyramid.PyramidFromDirectoryWriter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;


public class PrimaryController {

    private static final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(App.class);

    @FXML
    public Label statusBox;
    public TextArea extraParams;
    public TextField outputDirectory;
    public CheckBox wantOverwrite;
    public ListView<IOPackage> inputFileList;
    public Label fileListHelpText;
    public Button addFileButton;
    public Button removeFileButton;
    public Button clearFileButton;
    public Button clearFinishedButton;
    public Button chooseDirButton;
    public Button clearDirButton;
    public Button showLogButton;
    public Label versionDisplay;
    public Button runButton;
    public ChoiceBox<String> outputFormat;
    public ChoiceBox<String> logLevel;
    public Separator listSeparator;
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
    public TextArea logBox;
    public Button logFileButton;
    private boolean isRunning = false;
    private Thread runnerThread;
    private ConverterTask currentJob;
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
        inputFileList.setCellFactory(list -> new FileCell());
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
        addFileButton.setGraphic(addIcon);
        removeFileButton.setGraphic(removeIcon);
        clearFileButton.setGraphic(clearIcon);
        clearFinishedButton.setGraphic(finishedIcon);
        showLogButton.setGraphic(logIcon);
        extraParams.setText(userPreferences.get(prefName.ARGS.name(), ""));
        extraParams.setTooltip(new Tooltip("Extra arguments (one per line)"));
        addFileButton.setTooltip(new Tooltip("Add files"));
        removeFileButton.setTooltip(new Tooltip("Remove selected file"));
        clearFileButton.setTooltip(new Tooltip("Remove all files"));
        clearFinishedButton.setTooltip(new Tooltip("Clear finished"));
        ObservableList<String> outputModes = FXCollections.observableArrayList(
                OutputMode.NGFF.getDisplayName(),
                OutputMode.TIFF.getDisplayName());
        outputFormat.getItems().setAll(outputModes);
        outputFormat.setValue(userPreferences.get(prefName.FORMAT.name(), OutputMode.NGFF.getDisplayName()));
        outputFormatGroup = new ToggleGroup();
        outputModes.forEach(mode -> {
            RadioMenuItem item = new RadioMenuItem(mode);
            item.setToggleGroup(outputFormatGroup);
            item.setOnAction(event -> outputFormat.setValue(mode));
            if (Objects.equals(mode, OutputMode.NGFF.getDisplayName())) {
                item.setSelected(true);
            }
            menuOutputFormat.getItems().add(item);
        });
        ObservableList<String> logModes = FXCollections.observableArrayList("Debug", "Info", "Warn", "Error",
                "Trace", "All", "Off");
        logLevel.setItems(logModes);
        logLevel.setValue(userPreferences.get(prefName.LOG_LEVEL.name(), "Warn"));
        logLevelGroup = new ToggleGroup();
        logModes.forEach(mode -> {
            RadioMenuItem item = new RadioMenuItem(mode);
            item.setToggleGroup(logLevelGroup);
            item.setOnAction(event -> logLevel.setValue(mode));
            if (Objects.equals(mode, "Warn")) {
                item.setSelected(true);
            }
            menuLogLevel.getItems().add(item);
        });
        outputFormat.setTooltip(new Tooltip("File format to convert to"));
        logLevel.setTooltip(new Tooltip("Level of detail to show in the logs"));
        wantOverwrite.setSelected(userPreferences.getBoolean(prefName.OVERWRITE.name(), false));
        wantOverwrite.setTooltip(new Tooltip("Overwrite existing output files"));
        outputDirectory.setText(userPreferences.get(prefName.OUTPUT_FOLDER.name(), defaultOutputText));
        outputDirectory.setTooltip(new Tooltip("Directory to save converted files to.\n" +
                "Applies to new files added to the list."));
        version = getClass().getPackage().getImplementationVersion();
        if (version == null) { version = "DEV"; }
        versionDisplay.setText(versionDisplay.getText() + version);
        createLogControl();

        // Arrays of controls we want to lock during a run. Menu items have different class inheritance to controls.
        fileControlButtons = new ArrayList<>(Arrays.asList(addFileButton, removeFileButton, clearFileButton,
                clearFinishedButton, outputDirectory, chooseDirButton, clearDirButton, wantOverwrite, logLevel,
                outputFormat));
        menuControlButtons = new ArrayList<>(Arrays.asList(menuLogLevel, menuOutputFormat, menuAddFiles, menuRemoveFile,
                menuClearFinished, menuClearAll, menuSavePrefs, menuResetPrefs, menuOverwrite, menuChooseDirectory,
                menuResetDirectory, menuTempDirectory));
    }

    private void createLogControl() throws IOException {
        FXMLLoader logLoader = new FXMLLoader();
        logLoader.setLocation(getClass().getResource("LogWindow.fxml"));
        Scene scene = new Scene(logLoader.load());
        LogController logControl = logLoader.getController();
        consoleWindow = new Stage();
        consoleWindow.setScene(scene);
        logBox = logControl.logBox;
        logFileButton = logControl.logFileButton;
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
        Stage stage = (Stage) addFileButton.getScene().getWindow();
        FileChooser addFileChooser = new FileChooser();
        addFileChooser.setTitle("Select files to load...");
        List<File> newFiles = addFileChooser.showOpenMultipleDialog(stage);
        if (newFiles != null && newFiles.size() > 0) {
            addFilesToList(newFiles);
        }
    }

    @FXML
    private void removeFile() {
        final int selectedIdx = inputFileList.getSelectionModel().getSelectedIndex();
        if (selectedIdx != -1) {
            inputFileList.getItems().remove(selectedIdx);
        }
        if (inputFileList.getItems().size() == 0) {
            fileListHelpText.setVisible(true);
            listSeparator.setVisible(false);
        }
    }

    @FXML
    private void clearFiles() {
        inputFileList.getItems().clear();
        fileListHelpText.setVisible(true);
        listSeparator.setVisible(false);
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
            statusBox.setText("Will use output folder as temporary directory for intermediates.");
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
            statusBox.setText("User specified a temporary directory for intermediates.");
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
        List<IOPackage> fileList = inputFileList.getItems();
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
            OutputMode outputMode;
            if (outputFormat.getValue().equals(OutputMode.NGFF.getDisplayName()) && !extension.equals("zarr")) {
                outputMode = OutputMode.NGFF;
            } else {
                outputMode = OutputMode.TIFF;
            }
            File outFile = new File(outBase, outPath + outputMode.getExtension());
            fileList.add(new IOPackage(file, outFile, wantOverwrite.isSelected(), outputMode));
            count++;
        }
        statusBox.setText("Found and added " + count + " supported file(s)");
        if (fileList.size() > 0) {
            fileListHelpText.setVisible(false);
            listSeparator.setVisible(true);
        }
    }

    @FXML
    private void listClickHandler(MouseEvent event) throws IOException {
        if (event.getButton().equals(MouseButton.PRIMARY)) {
            if (event.getClickCount() == 2) {
                if (isRunning) { return; }
                final IOPackage target = inputFileList.getSelectionModel().getSelectedItem();
                if (target != null) {
                    if (target.status == jobStatus.COMPLETED) {
                        Desktop.getDesktop().open(target.fileOut.getParentFile());
                        return;
                    }
                    // Todo: Full UI for editing file path
                    Stage stage = (Stage) inputFileList.getScene().getWindow();
                    FileChooser outputFileChooser = new FileChooser();
                    outputFileChooser.setInitialDirectory(target.fileOut.getParentFile());
                    outputFileChooser.setInitialFileName(target.fileOut.getName());
                    FileChooser.ExtensionFilter zarrFilter = new FileChooser.ExtensionFilter(
                            "Zarr file",OutputMode.NGFF.getExtension());
                    FileChooser.ExtensionFilter tiffFilter = new FileChooser.ExtensionFilter(
                            "OME TIFF file",OutputMode.TIFF.getExtension());
                    outputFileChooser.getExtensionFilters().add(zarrFilter);
                    outputFileChooser.getExtensionFilters().add(tiffFilter);
                    if (target.outputMode == OutputMode.NGFF) {
                        outputFileChooser.setSelectedExtensionFilter(zarrFilter);
                    } else {
                        outputFileChooser.setSelectedExtensionFilter(tiffFilter);
                    }
                    outputFileChooser.setTitle("Choose output file for " + target.fileIn.getName());
                    File newOutput = outputFileChooser.showSaveDialog(stage);
                    if (newOutput != null) {
                        String desiredExtension = outputFileChooser.getSelectedExtensionFilter().getExtensions().get(0);
                        if (!newOutput.getName().toLowerCase().endsWith(desiredExtension)) {
                            newOutput = new File(newOutput.getAbsolutePath() + desiredExtension);
                        }
                        if (desiredExtension.equals(OutputMode.NGFF.getExtension())){
                            target.outputMode = OutputMode.NGFF;
                        } else {
                            target.outputMode = OutputMode.TIFF;
                        }
                        target.fileOut = newOutput;
                        // Reset status
                        target.status = jobStatus.READY;
                        inputFileList.refresh();
                    }
                    }

                }
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
        inputFileList.setItems(inputFileList.getItems()
                .stream()
                .filter((item) -> (item.status != jobStatus.COMPLETED))
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));
    }

    @FXML
    public void toggleFileLogging() {
        if (fileAppender == null) {
            Stage stage = (Stage) inputFileList.getScene().getWindow();
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
                logFileButton.setText("Stop logging to file");
                wantLogToFile.setSelected(true);
            } else {
                wantLogToFile.setSelected(false);
            }
        } else if (wantLogToFile.isSelected()) {
            fileAppender.start();
            logFileButton.setText("Stop logging to file");
        } else {
            fileAppender.stop();
            logFileButton.setText("Resume logging to file");
        }
    }

    @FXML
    private void toggleOverwrite() {
        boolean overwrite = wantOverwrite.isSelected();
        menuOverwrite.setSelected(overwrite);
        List<jobStatus> doNotChange = Arrays.asList(jobStatus.COMPLETED, jobStatus.FAILED, jobStatus.RUNNING);
        inputFileList.getItems().forEach((item) -> {
            if (doNotChange.contains(item.status)) { return; }
            if ((!overwrite) && item.fileOut.exists()) {
                item.status = jobStatus.ERROR;
            } else {
                item.status = jobStatus.READY;
            }
        });
        inputFileList.refresh();
    }

    @FXML
    private void overwriteMenu() {
        wantOverwrite.setSelected(!wantOverwrite.isSelected());
        toggleOverwrite();
    }

    @FXML
    private void updateFormat() {
        if (outputFormatGroup == null) return;
        String val = outputFormat.getValue();
        int idx = outputFormat.getItems().indexOf(val);
        outputFormatGroup.selectToggle(outputFormatGroup.getToggles().get(idx));
    }

    @FXML
    private void updateLogLevel() {
        if (logLevelGroup == null) return;
        String val = logLevel.getValue();
        int idx = logLevel.getItems().indexOf(val);
        logLevelGroup.selectToggle(logLevelGroup.getToggles().get(idx));
    }


    @FXML
    private void resetPrefs() throws BackingStoreException {
        userPreferences.clear();
        userPreferences.flush();
        statusBox.setText("Cleared saved default settings.");
    }

    @FXML
    private void savePrefs() throws BackingStoreException {
        userPreferences.put(prefName.FORMAT.name(), outputFormat.getValue());
        userPreferences.put(prefName.LOG_LEVEL.name(), logLevel.getValue());
        userPreferences.putBoolean(prefName.OVERWRITE.name(), wantOverwrite.isSelected());
        userPreferences.put(prefName.OUTPUT_FOLDER.name(), outputDirectory.getText());
        userPreferences.put(prefName.ARGS.name(), extraParams.getText());
        userPreferences.flush();
        statusBox.setText("Saved current settings as defaults.");
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
        File props;
        String bfVer;
        String b2rVer;
        String r2oVer;
        // Fat jar packaging overwrites our class version names.
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null) {
            // Figure out if we're running from a build
            String sysName = System.getProperty("os.name");
            if (sysName.startsWith("Mac")) {
                props = new File(appPath + "/../../lib/versions.properties");
            } else {
                props = new File(appPath + "/../app/versions.properties");
            }
        } else {
            props = null;
        }
        if (props != null) {
            // Extract package versions from .properties file stored in build
            Properties properties = new Properties();
            properties.load(new FileInputStream(props.getCanonicalPath()));
            bfVer = properties.getProperty("BioformatsVersion");
            b2rVer = properties.getProperty("Bioformats2RawVersion");
            r2oVer = properties.getProperty("Raw2OMETiffVersion");
        } else {
            bfVer = ImageReader.class.getPackage().getImplementationVersion();
            b2rVer = Converter.class.getPackage().getImplementationVersion();
            r2oVer = PyramidFromDirectoryWriter.class.getPackage().getImplementationVersion();
        }
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
        stage.initOwner(inputFileList.getScene().getWindow());
        stage.setResizable(false);
        stage.show();
    }

    public void runCompleted() {
        runButton.setText("Run Conversions");
        menuRun.setText("Run Conversions");
        isRunning = false;
        fileControlButtons.forEach((control -> control.setDisable(false)));
        menuControlButtons.forEach((control -> control.setDisable(false)));
        // Print anything left in the console buffer.
        consoleStream.forceFlush();
    }

    @FXML
    public void onExit() throws InterruptedException {
        if (isRunning) {
            runCancel();
        }
        Platform.exit();
    }

    public void runCancel() throws InterruptedException {
        currentJob.interrupted = true;
        runnerThread.interrupt();
        runnerThread.join();
        inputFileList.getItems().forEach((job) -> {
            if (job.status == jobStatus.RUNNING) {
                job.status = jobStatus.FAILED;
            }});
        inputFileList.refresh();
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
        inputFileList.getItems().stream().filter(job -> job.status != jobStatus.COMPLETED).forEach(job -> {
            double estimatedSize = job.fileIn.length() * 1.3;
            Path targetDrive = Paths.get(job.fileOut.getAbsolutePath()).getRoot();
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
                alert.setTitle("NGFF Converter");
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
        runButton.setText("Stop Conversions");
        menuRun.setText("Stop Conversions");
        isRunning = true;
        LOGGER.info("Beginning file conversion...\n");
        List<String> extraArgs =  new ArrayList<>();
        extraArgs.add("--log-level=" + logLevel.getValue().toUpperCase());
        if (wantOverwrite.isSelected()) {
            extraArgs.add("--overwrite");
        }
        String[] userArgs = extraParams.getText().split("\n");

        Arrays.asList(userArgs).forEach((String userArg) -> {
            if (userArg.equals("")) return;
            // Fix missing '--'
            if (!userArg.startsWith("-")) {
                userArg = "--" + userArg;
            }
            // Fix common typo using space instead of equals
            if (!userArg.contains("=") && userArg.chars().filter(num -> num == ' ').count() == 1) {
                    userArg = userArg.replace(' ', '=');
            }
            extraArgs.add(userArg);
        });

        currentJob = new ConverterTask(extraArgs, this);
        currentJob.interrupted = false;
        runnerThread = new Thread(currentJob);
        runnerThread.setDaemon(true);
        runnerThread.start();
    }


}
