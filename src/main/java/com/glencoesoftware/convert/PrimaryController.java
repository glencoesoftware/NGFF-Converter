package com.glencoesoftware.convert;

import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javafx.util.Callback;
import javafx.fxml.FXML;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import com.glencoesoftware.bioformats2raw.Converter;
import picocli.CommandLine;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.event.EventHandler;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.Node;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;


public class PrimaryController {

    @FXML
    public TextField logBox;
    public TextField outputDirectory;
    public TextArea systemLog;
    public CheckBox wantDebug;
    public CheckBox wantVersion;
    public CheckBox wantHelp;
    public ListView<IOPackage> inputFileList;

    public Set<String> SupportedExtensions = new HashSet<String>(Arrays.asList("txt", "b"));

    @FXML
    public void initialize(){
        inputFileList.setCellFactory(new Callback<ListView<IOPackage>,
                                             ListCell<IOPackage>>() {
                                         public FileCell call(ListView<IOPackage> list) {
                                             return new FileCell();
                                         }
                                     }
        );
    }

    @FXML
    private void addFiles() throws IOException {
        System.out.println("Drop detected");
    }

    @FXML
    private void chooseOutputDirectory() throws IOException {
        System.out.println("Kliked");
        Stage stage = (Stage) outputDirectory.getScene().getWindow();
        DirectoryChooser outputDirectoryChooser = new DirectoryChooser();
        outputDirectoryChooser.setTitle("Choose output directory");
        File newDir = outputDirectoryChooser.showDialog(stage);
        if (newDir != null) {
            outputDirectory.setText(newDir.getAbsolutePath());
        }

    }

    @FXML
    private void resetOutputDirectory() throws IOException {
        outputDirectory.setText("<Same as input>");
    }

    @FXML
    private void handleFileDragOver(DragEvent event) throws IOException {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        } else {
            event.consume();
        }
    }

    @FXML
    private void handleFileDrop(DragEvent event) throws IOException {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            success = true;
//            String filePath = null;
            List<IOPackage> fileList = inputFileList.getItems();

            Queue<File> fileQueue = new LinkedList<>(db.getFiles());
            while (!fileQueue.isEmpty()) {
                File file = fileQueue.remove();
                if (file.isDirectory()) {
                    fileQueue.addAll(Arrays.asList(Objects.requireNonNull(file.listFiles())));
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
        event.setDropCompleted(success);
        event.consume();
    }


    @FXML
    private void runConvert() throws Exception {
//        ConsoleLogger console = new ConsoleLogger(systemLog);
//        PrintWriter pw = new PrintWriter(console);
//        System.setOut(ps);

//        PrintStream printStream = new PrintStream(new ConsoleLogger(systemLog));
//        System.setOut(printStream);

        System.out.println(logBox.getText());

        // These are temporary testing parameters
        System.load("C:\\Program Files (x86)\\blosc\\bin\\blosc.dll");
        String outputFilename = "D:\\David\\Data\\convert\\BF007.zarr";
        FileUtils.deleteDirectory(new File(outputFilename));

        List<String> args =  new ArrayList(Arrays.asList("D:\\David\\Data\\BF007.nd2", outputFilename));
        if (wantDebug.isSelected()) {
            args.add("--debug");
        }
        if (wantHelp.isSelected()) {
            args.add("--help");
        }
        if (wantVersion.isSelected()) {
            args.add("--version");
        }
        System.out.println("Will execute with");
        System.out.println(args);
        CommandLine runner = new CommandLine(new Converter());
        ConsoleWriter sw = new ConsoleWriter(systemLog);
         runner.setOut(new PrintWriter(sw));
        int exitCode = runner.execute(args.toArray(new String[args.size()]));


        System.out.println("FFF2");
        logBox.setText("ANALYSIS COMPLETE");
        System.out.println(exitCode);

    }


}
