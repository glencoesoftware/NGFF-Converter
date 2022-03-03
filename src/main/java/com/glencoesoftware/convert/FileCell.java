package com.glencoesoftware.convert;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;

public class FileCell extends ListCell<IOPackage> {
    final HBox content;
    final Label nameIn;
    final Label pathIn;
    final Label nameOut;
    final Label pathOut;
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
        pathIn = new Label();
        nameOut = new Label();
        pathOut = new Label();
        pathIn.setFont(pathFont);
        nameIn.setFont(nameFont);
        pathOut.setFont(pathFont);
        nameOut.setFont(nameFont);
        pathIn.setTextFill(Color.GRAY);
        pathOut.setTextFill(Color.GRAY);
        nameIn.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        nameOut.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        pathIn.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
        pathOut.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);

        ok = new FontIcon("bi-play-circle");
        notOk = new FontIcon("bi-exclamation-circle");
        success = new FontIcon("bi-check-circle");
        fail = new FontIcon("bi-x-circle");
        ok.setIconSize(20);
        notOk.setIconSize(20);
        success.setIconSize(20);
        fail.setIconSize(20);
        success.setIconColor(Paint.valueOf("GREEN"));
        notOk.setIconColor(Paint.valueOf("ORANGE"));
        fail.setIconColor(Paint.valueOf("RED"));
        monitor = new Label();
        progress = new ProgressIndicator();
        progress.setPrefSize(20, 20);
        progress.setMinSize(20, 20);
        VBox vBoxIn = new VBox(nameIn, pathIn);
        Region spacer = new Region();
        spacer.setMinWidth(10);
        VBox vBoxOut = new VBox(nameOut, pathOut);
        content = new HBox(monitor, vBoxIn,spacer, vBoxOut);
        HBox.setHgrow(vBoxIn, Priority.ALWAYS);
        HBox.setHgrow(vBoxOut, Priority.ALWAYS);
        content.setMinWidth(0);
        content.setPrefWidth(1);
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
            pathIn.setText(fileIn.getParent());
            nameOut.setText(fileOut.getName());
            pathOut.setText(fileOut.getParent());
            switch (pack.status) {
                case READY -> {
                    monitor.setGraphic(ok);
                    monitor.setTooltip(new Tooltip("Ready to run"));
                }
                case COMPLETED -> {
                    monitor.setGraphic(success);
                    monitor.setTooltip(new Tooltip("Conversion successful"));
                }
                case FAILED -> {
                    monitor.setGraphic(fail);
                    monitor.setTooltip(new Tooltip("Conversion failed"));
                }
                case ERROR -> {
                    monitor.setGraphic(notOk);
                    monitor.setTooltip(new Tooltip("Output file already exists"));
                }
                case RUNNING -> {
                    monitor.setGraphic(progress);
                    progress.setTooltip(new Tooltip("Running"));
                }
                case NOOUTPUT -> {
                    monitor.setGraphic(notOk);
                    monitor.setTooltip(new Tooltip("Run completed with no output"));
                }
                default -> throw new IllegalStateException("Unexpected value: " + pack.status);
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
