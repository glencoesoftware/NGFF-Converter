package com.glencoesoftware.convert;

import javafx.scene.paint.*;
import javafx.geometry.*;
import java.io.File;
import javafx.scene.control.ListCell;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import org.kordamp.ikonli.javafx.FontIcon;

public class FileCell extends ListCell<IOPackage> {
    final HBox content;
    final Text nameIn;
    final Text pathIn;
    final Text nameOut;
    final Text pathOut;
    final Label monitor;
    final FontIcon ok;
    final FontIcon notOk;

    public FileCell() {
        super();
        Font nameFont = new Font(14);
        Font pathFont = new Font(10);
        nameIn = new Text();
        pathIn = new Text();
        nameOut = new Text();
        pathOut = new Text();
        pathIn.setFont(pathFont);
        nameIn.setFont(nameFont);
        pathOut.setFont(pathFont);
        nameOut.setFont(nameFont);
        pathIn.setFill(Color.GRAY);
        pathOut.setFill(Color.GRAY);
        ok = new FontIcon("bi-check-circle");
        notOk = new FontIcon("bi-exclamation-circle");
        ok.setIconSize(20);
        notOk.setIconSize(20);
        monitor = new Label();
        VBox vBoxIn = new VBox(nameIn, pathIn);
        vBoxIn.setMinWidth(200);
        vBoxIn.setMaxWidth(200);
        Separator sep = new Separator();
        sep.setOrientation(Orientation.VERTICAL);
        VBox vBoxOut = new VBox(nameOut, pathOut);
        content = new HBox(monitor, vBoxIn, sep, vBoxOut);
        content.setAlignment(Pos.CENTER_LEFT);

        content.setSpacing(10);
        content.setBackground(null);
    }

    @Override
    public void updateItem(IOPackage pack, boolean empty) {
        super.updateItem(pack, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else if (pack != null) {
            File fileIn = pack.fileIn;
            File fileOut = pack.fileOut;
            nameIn.setText(fileIn.getName());
            String inPath = fileIn.getParent();
            if (inPath.length() > 50) {
                inPath = "..." + inPath.substring(inPath.length() - 45);
            }
            String outPath = fileOut.getParent();
            if (outPath.length() > 50) {
                outPath = "..." + outPath.substring(outPath.length() - 45);
            }
            pathIn.setText(inPath);
            nameOut.setText(fileOut.getName());
            pathOut.setText(outPath);
            if (pack.status.equals("error")) {
                monitor.setGraphic(notOk);
            } else {
                monitor.setGraphic(ok);
            }
            setGraphic(content);
        } else {
            setText(null);
            setGraphic(null);
        }
    }
}
