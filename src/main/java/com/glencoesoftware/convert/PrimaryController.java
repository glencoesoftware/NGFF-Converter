package com.glencoesoftware.convert;

import com.glencoesoftware.bioformats2raw.Converter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import loci.formats.ImageReader;
import org.apache.commons.io.FilenameUtils;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.stream.Collectors;


public class PrimaryController {

    @FXML
    public HBox mainPanel;
    public VBox logVBox;
    public StackPane stackPanel;
    public TextField statusBox;
    public TextArea extraParams;
    public TextArea logBox;
    public TextField outputDirectory;
    public CheckBox wantOverwrite;
    public ListView<IOPackage> inputFileList;
    public Button addFileButton;
    public Button removeFileButton;
    public Button clearFileButton;
    public Button clearFinishedButton;
    public Button chooseDirButton;
    public Button clearDirButton;
    public Label versionDisplay;
    public Button runButton;
    public ChoiceBox<String> logLevel;

    private boolean isRunning = false;
    private Thread runnerThread;
    private ConverterTask currentJob;
    private ArrayList<Control> fileControlButtons;

    public final Set<String> supportedExtensions = new HashSet<>(Arrays.asList(new ImageReader().getSuffixes()));
    public String version;

    @FXML
    public void initialize(){
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
        logLevel.setItems(FXCollections.observableArrayList("Debug", "Info", "Warn", "Error", "Trace", "All"));
        logLevel.setValue("Warn");
        logLevel.setTooltip(new Tooltip("Level of detail to show on the log tab"));
        wantOverwrite.setTooltip(new Tooltip("Overwrite existing output files"));
        outputDirectory.setTooltip(new Tooltip("Directory to save converted files to.\n" +
                "Applies to new files added to the list."));
        version = getClass().getPackage().getImplementationVersion();
        if (version == null) { version = "DEV"; }
        versionDisplay.setText(versionDisplay.getText() + version);
        ConsoleStream console = new ConsoleStream(logBox);
        PrintStream printStream = new PrintStream(console, true);
        System.setOut(printStream);
        System.setErr(printStream);
        // Array of controls we want to lock during a run.
        fileControlButtons = new ArrayList<>(Arrays.asList(addFileButton, removeFileButton, clearFileButton,
                clearFinishedButton, outputDirectory, chooseDirButton, clearDirButton ));
    }

    @FXML
    private void addFiles() {
        Stage stage = (Stage) addFileButton.getScene().getWindow();
        FileChooser addFileChooser = new FileChooser();
        addFileChooser.setTitle("Select files to load...");
        List<File> newFiles = addFileChooser.showOpenMultipleDialog(stage);
        if (newFiles.size() > 0) {
            addFilesToList(newFiles);
        }
    }

    @FXML
    private void removeFile() {
        final int selectedIdx = inputFileList.getSelectionModel().getSelectedIndex();
        if (selectedIdx != -1) {
            inputFileList.getItems().remove(selectedIdx);
        }
    }

    @FXML
    private void clearFiles() {
        inputFileList.getItems().clear();
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
            if (!supportedExtensions.contains(extension)) {
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
        statusBox.setText("Found and added " + count + " supported files");
    }

    @FXML
    private void listClickHandler(MouseEvent event) throws IOException {
        if (event.getButton().equals(MouseButton.PRIMARY)) {
            if (event.getClickCount() == 2) {
                if (isRunning) { return; }
                final IOPackage target = inputFileList.getSelectionModel().getSelectedItem();
                if (target != null) {
                    if (target.status.equals("success")) {
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
                        target.status = "ready";
                        inputFileList.refresh();
                    }
                    }

                }
            }
        }


    @FXML
    private void clearFinished() {
        inputFileList.setItems(inputFileList.getItems()
                .stream()
                .filter((item) -> (!item.status.equals("success")))
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));
    }

    @FXML
    private void toggleOverwrite() {
        boolean overwrite = wantOverwrite.isSelected();
        List<String> doNotChange = Arrays.asList("success", "fail", "running");
        inputFileList.getItems().forEach((item) -> {
            if (doNotChange.contains(item.status)) { return; }
            if ((!overwrite) && item.fileOut.exists()) {
                item.status = "error";
            } else {
                item.status = "ready";
            }
        });
        inputFileList.refresh();
    }

    @FXML
    private void displayLog() {
        logVBox.setVisible(!logVBox.isVisible());
    }

    @FXML
    private void displayAbout() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.getNamespace().put("guiVer", version);
        fxmlLoader.getNamespace().put("b2rVer", Converter.class.getPackage().getImplementationVersion());
        fxmlLoader.getNamespace().put("bfVer", ImageReader.class.getPackage().getImplementationVersion());
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
        runButton.setText("Run conversions");
        isRunning = false;
        fileControlButtons.forEach((control -> control.setDisable(false)));
    }

    public void runCancel() throws InterruptedException {
        currentJob.interrupted = true;
        runnerThread.interrupt();
        runnerThread.join();
        inputFileList.getItems().forEach((job) -> {
            if (job.status.equals("running")) {
                job.status = "fail";
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
        runButton.setText("Stop conversions");
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
            if (userArg.chars().filter(num -> num == ' ').count() == 1) {
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
