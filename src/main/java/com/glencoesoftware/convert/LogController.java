package com.glencoesoftware.convert;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class LogController {

    @FXML
    public TextArea logBox;
    public ConsoleStream stream;
    public Button logFileButton;
    public PrimaryController parentController;

    @FXML
    public void initialize() {
        stream = new ConsoleStream(logBox);
        ConsoleStreamAppender.setStaticOutputStream(stream);
    }

    public void setParent(PrimaryController parent) {
        parentController = parent;
    }

    public void onToggleLogging() {
        if (parentController.fileAppender != null) {
            parentController.wantLogToFile.setSelected(!parentController.wantLogToFile.isSelected());
        }
        parentController.toggleFileLogging();
    }

    @FXML
    public void copyLogs() {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(
                        new StringSelection(logBox.getText()),
                        null
                );
    }

    @FXML
    public void clearLogs() {
        logBox.clear();
    }

}
