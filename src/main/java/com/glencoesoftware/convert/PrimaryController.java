package com.glencoesoftware.convert;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import loci.formats.ImageReader;
import org.apache.commons.io.FilenameUtils;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;


public class PrimaryController {

    @FXML
    public HBox mainPanel;
    public VBox logVBox;
    public StackPane stackPanel;
    public TextField statusBox;
    public TextArea logBox;
    public TextField outputDirectory;
    public TextArea systemLog;
    public CheckBox wantDebug;
    public CheckBox wantVersion;
    public CheckBox wantHelp;
    public ListView<IOPackage> inputFileList;
    public Button addFileButton;
    public Button removeFileButton;
    public Button clearFileButton;
    public Button clearFinishedButton;
    public Label versionDisplay;

    public Set<String> supportedExtensions = new HashSet<>(Arrays.asList(new ImageReader().getSuffixes()));

    @FXML
    public void initialize(){
        supportedExtensions.add("zarr");
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
        addFileButton.setTooltip(new Tooltip("Add files"));
        removeFileButton.setTooltip(new Tooltip("Remove selected file"));
        clearFileButton.setTooltip(new Tooltip("Remove all files"));
        clearFinishedButton.setTooltip(new Tooltip("Clear finished"));
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) { version = "DEV"; }
        versionDisplay.setText(versionDisplay.getText() + version);
        ConsoleStream console = new ConsoleStream(logBox);
        PrintStream printStream = new PrintStream(console, true);
        System.setOut(printStream);
        System.setErr(printStream);
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
            fileList.add(new IOPackage(file, outFile));
        }
    }

    @FXML
    private void listClickHandler(MouseEvent event) {
        if (event.getButton().equals(MouseButton.PRIMARY)) {
            if (event.getClickCount() == 2) {
                final IOPackage target = inputFileList.getSelectionModel().getSelectedItem();
                if (target != null) {
                    // Todo: UI for editing file path
                    System.out.println(target.fileIn);
                    target.setFileIn(target.fileOut);
                    inputFileList.refresh();
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
    private void testRun() {
        // I am a testing function to forcibly change file list item state
        inputFileList.getItems().forEach((item) -> {
            item.status = "working";
        });
        inputFileList.refresh();
    }

    @FXML
    private void displayLog() {
        logVBox.setVisible(!logVBox.isVisible());
    }

    @FXML
    private void runConvert() throws Exception {
        logBox.setText("Beginning file conversion...\n");
        List<String> extraArgs =  new ArrayList<String>();
        if (wantDebug.isSelected()) {
            extraArgs.add("--debug");
        }
        if (wantHelp.isSelected()) {
            extraArgs.add("--help");
        }
        if (wantVersion.isSelected()) {
            extraArgs.add("--version");
        }
        ConverterTask job = new ConverterTask(extraArgs, inputFileList, statusBox, logBox);


        Thread th = new Thread(job);
        th.setDaemon(true);
        th.start();
        // Todo: Freeze/unfreeze UI settings.
    }


}
