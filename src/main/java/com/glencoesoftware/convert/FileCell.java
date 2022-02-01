package com.glencoesoftware.convert;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;

public class FileCell extends ListCell<IOPackage> {
    final HBox content;
    final Label nameIn;
    final Text pathIn;
    final Label nameOut;
    final Text pathOut;
    final Label monitor;
    final FontIcon ok;
    final FontIcon notOk;
    final FontIcon success;
    final FontIcon fail;
    final ProgressIndicator progress;

    public FileCell() {
        super();
        Font nameFont = new Font(14);
        Font pathFont = new Font(10);
        nameIn = new Label();
        pathIn = new Text();
        nameOut = new Label();
        pathOut = new Text();
        pathIn.setFont(pathFont);
        nameIn.setFont(nameFont);
        pathOut.setFont(pathFont);
        nameOut.setFont(nameFont);
        pathIn.setFill(Color.GRAY);
        pathOut.setFill(Color.GRAY);
        // Todo: Find a way to move these resources to a better location. Only need 1 of each.
        ok = new FontIcon("bi-play-circle");
        notOk = new FontIcon("bi-exclamation-circle");
        success = new FontIcon("bi-check-circle");
        fail = new FontIcon("bi-x-circle");
        ok.setIconSize(20);
        notOk.setIconSize(20);
        success.setIconSize(20);
        fail.setIconSize(20);
        monitor = new Label();
        progress = new ProgressIndicator();
        progress.setPrefSize(20, 20);
        VBox vBoxIn = new VBox(nameIn, pathIn);
        vBoxIn.setMinWidth(200);
        vBoxIn.setMaxWidth(200);
        Separator sep = new Separator();
        sep.setOrientation(Orientation.VERTICAL);
        VBox vBoxOut = new VBox(nameOut, pathOut);
        content = new HBox(monitor, vBoxIn, sep, vBoxOut);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setSpacing(10);
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
            switch (pack.status) {
                case "ready" -> {
                    monitor.setGraphic(ok);
                    monitor.setTooltip(new Tooltip("Ready to run"));
                }
                case "success" -> {
                    monitor.setGraphic(success);
                    monitor.setTooltip(new Tooltip("Conversion successful"));
                }
                case "fail" -> {
                    monitor.setGraphic(fail);
                    monitor.setTooltip(new Tooltip("Conversion failed"));
                }
                case "error" -> {
                    monitor.setGraphic(notOk);
                    monitor.setTooltip(new Tooltip("Output file already exists"));
                }
                case "running" -> {
                    monitor.setGraphic(progress);
                    progress.setTooltip(new Tooltip("Running"));
                }
                case "noOutput" -> {
                    monitor.setGraphic(notOk);
                    monitor.setTooltip(new Tooltip("Run completed with no output"));
                }
            }
            nameIn.setTooltip(new Tooltip(fileIn.getAbsolutePath()));
            nameOut.setTooltip(new Tooltip(fileOut.getAbsolutePath()));
            setGraphic(content);
        } else {
            setText(null);
            setGraphic(null);
        }
    }
}
