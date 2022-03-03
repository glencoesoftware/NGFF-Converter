package com.glencoesoftware.convert;

import com.glencoesoftware.bioformats2raw.Converter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
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

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.stream.Collectors;


public class PrimaryController {

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
    public Label versionDisplay;
    public Button runButton;
    public ChoiceBox<String> logLevel;
    public Separator listSeparator;
    public Menu menuLogLevel;
    public CheckMenuItem menuOverwrite;
    public MenuItem menuRun;
    public MenuItem menuChooseDirectory;
    public MenuItem menuResetDirectory;
    public MenuItem menuAddFiles;
    public MenuItem menuRemoveFile;
    public MenuItem menuClearFinished;
    public MenuItem menuClearAll;

    private Stage consoleWindow;
    public TextArea logBox;
    private boolean isRunning = false;
    private Thread runnerThread;
    private ConverterTask currentJob;
    private ArrayList<Control> fileControlButtons;
    private ArrayList<MenuItem> menuControlButtons;
    public ConsoleStream consoleStream;
    public ToggleGroup logLevelGroup;

    public final Set<String> supportedExtensions = new HashSet<>(Arrays.asList(new ImageReader().getSuffixes()));
    public String version;

    public enum jobStatus {
        READY, ERROR, COMPLETED, FAILED, RUNNING, NOOUTPUT
    }

    @FXML
    public void initialize() throws IOException {
        // Todo: Support zarr to OME.TIFF
        // supportedExtensions.add("zarr");
        inputFileList.setCellFactory(list -> new FileCell());
        FontIcon addIcon = new FontIcon("bi-plus");
        FontIcon removeIcon = new FontIcon("bi-dash");
        FontIcon clearIcon = new FontIcon("bi-x");
        FontIcon finishedIcon = new FontIcon("bi-check");
        addIcon.setIconSize(20);
        removeIcon.setIconSize(20);
        clearIcon.setIconSize(20);
        finishedIcon.setIconSize(20);
        addFileButton.setGraphic(addIcon);
        removeFileButton.setGraphic(removeIcon);
        clearFileButton.setGraphic(clearIcon);
        clearFinishedButton.setGraphic(finishedIcon);
        extraParams.setTooltip(new Tooltip("Extra arguments (one per line)"));
        addFileButton.setTooltip(new Tooltip("Add files"));
        removeFileButton.setTooltip(new Tooltip("Remove selected file"));
        clearFileButton.setTooltip(new Tooltip("Remove all files"));
        clearFinishedButton.setTooltip(new Tooltip("Clear finished"));
        ObservableList<String> logModes = FXCollections.observableArrayList("Debug", "Info", "Warn", "Error",
                "Trace", "All", "Off");
        logLevel.setItems(logModes);
        logLevel.setValue("Warn");
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
        logLevel.setTooltip(new Tooltip("Level of detail to show on the log tab"));
        wantOverwrite.setTooltip(new Tooltip("Overwrite existing output files"));
        outputDirectory.setTooltip(new Tooltip("Directory to save converted files to.\n" +
                "Applies to new files added to the list."));
        version = getClass().getPackage().getImplementationVersion();
        if (version == null) { version = "DEV"; }
        versionDisplay.setText(versionDisplay.getText() + version);
        createLogControl();

        // Arrays of controls we want to lock during a run. Menu items have different class inheritance to controls.
        fileControlButtons = new ArrayList<>(Arrays.asList(addFileButton, removeFileButton, clearFileButton,
                clearFinishedButton, outputDirectory, chooseDirButton, clearDirButton, wantOverwrite, logLevel));
        menuControlButtons = new ArrayList<>(Arrays.asList(menuLogLevel, menuAddFiles, menuRemoveFile,
                menuClearFinished, menuClearAll, menuOverwrite, menuChooseDirectory, menuResetDirectory));
    }

    private void createLogControl() throws IOException {
        FXMLLoader logLoader = new FXMLLoader();
        logLoader.setLocation(getClass().getResource("LogWindow.fxml"));
        Scene scene = new Scene(logLoader.load());
        LogController logControl = logLoader.getController();
        consoleWindow = new Stage();
        consoleWindow.setScene(scene);
        logBox = logControl.logBox;
        consoleStream = new ConsoleStream(logBox);
        PrintStream printStream = new PrintStream(consoleStream, true);
        System.setOut(printStream);
        System.setErr(printStream);
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
    private void resetOutputDirectory() {
        outputDirectory.setText("<Same as input>");
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
            if (outputDirectory.getText().equals("<Same as input>")) {
                outBase = file.getParent();
            } else {
                outBase = outputDirectory.getText();
            }
            File outFile = new File(outBase, outPath + ".zarr");
            fileList.add(new IOPackage(file, outFile, wantOverwrite.isSelected()));
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
                    outputFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Zarr file",".zarr"));
                    outputFileChooser.setTitle("Choose output file for " + target.fileIn.getName());
                    File newOutput = outputFileChooser.showSaveDialog(stage);
                    if (newOutput != null) {
                        if (!newOutput.getName().endsWith(".zarr")) {
                            newOutput = new File(newOutput.getAbsolutePath() + ".zarr");
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
    private void updateLogLevel() {
        if (logLevelGroup == null) return;
        String val = logLevel.getValue();
        int idx = logLevel.getItems().indexOf(val);
        logLevelGroup.selectToggle(logLevelGroup.getToggles().get(idx));
    }

    @FXML
    private void displayLog() {
        if (consoleWindow.getOwner() == null) {
            consoleWindow.initOwner(addFileButton.getScene().getWindow());
        }
        consoleWindow.show();
    }

    @FXML
    private void displayAbout() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader();
        File props;
        String bfVer;
        String b2rVer;
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
        } else {
            bfVer = ImageReader.class.getPackage().getImplementationVersion();
            b2rVer = Converter.class.getPackage().getImplementationVersion();
        }
        fxmlLoader.getNamespace().put("guiVer", version);
        fxmlLoader.getNamespace().put("b2rVer", b2rVer);
        fxmlLoader.getNamespace().put("bfVer", bfVer);
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
        fileControlButtons.forEach((control -> control.setDisable(true)));
        menuControlButtons.forEach((control -> control.setDisable(true)));
        runButton.setText("Stop Conversions");
        menuRun.setText("Stop Conversions");
        isRunning = true;

        logBox.appendText("\n\nBeginning file conversion...\n");
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
