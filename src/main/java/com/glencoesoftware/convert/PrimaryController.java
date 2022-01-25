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


public class PrimaryController {

    @FXML
    public TextField logBox;
    public TextField outputDirectory;
    public TextArea systemLog;
    public CheckBox wantDebug;
    public CheckBox wantVersion;
    public CheckBox wantHelp;
    public ListView inputFileList;
    public ListView outputFileList;

    public Set<String> SupportedExtensions = new HashSet<String>(Arrays.asList("txt", "b"));

    @FXML
    public void initialize(){
        inputFileList.setCellFactory(new Callback<ListView<File>,
                                             ListCell<File>>() {
                                         public FileCell call(ListView<File> list) {
                                             return new FileCell();
                                         }
                                     }
        );
        outputFileList.setCellFactory(new Callback<ListView<File>,
                                              ListCell<File>>() {
                                          public OutputCell call(ListView<File> list) {
                                              return new OutputCell();
                                          }
                                      }
        );

    }

    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
        System.out.println("Sec done");
    }

    @FXML
    private void addFiles() throws IOException {
        System.out.println("Drop detected");
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
        // Link listbox scrollbars together
        // This is a suboptimal place to have this functionality, but it needs to run after startup.
        Node n1 = inputFileList.lookup(".scroll-bar");
        Node n2 = outputFileList.lookup(".scroll-bar");
        if (n1 instanceof ScrollBar && n2 instanceof ScrollBar) {
            final ScrollBar barIn = (ScrollBar) n1;
            final ScrollBar barOut = (ScrollBar) n2;
            barIn.valueProperty().bindBidirectional(barOut.valueProperty());
            }

        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            success = true;
//            String filePath = null;
            List fileList = inputFileList.getItems();
            List outputList = outputFileList.getItems();
//            List<File> fl = db.getFiles().stream().filter(f -> SupportedExtensions.contains(FilenameUtils.getExtension(f.getName())));
            for (File file:db.getFiles()) {
                String filePath = file.getAbsolutePath();
                String outPath = FilenameUtils.getBaseName(filePath);
                String outBase;
                System.out.println(outputDirectory.getText());
                if (outputDirectory.getText().equals("Same as input")) {
                    outBase = file.getParent();
                } else {
                    outBase = outputDirectory.getText();
                }
                File outfile = new File(outBase, outPath + ".zarr");
                fileList.add(file);
                outputList.add(outfile);
//                inputFileList.addAll(filePath);
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
