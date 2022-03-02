package com.glencoesoftware.convert;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.PrintStream;


public class LogController {

    @FXML
    public TextArea logBox;

    @FXML
    public void initialize() {
        ConsoleStream console = new ConsoleStream(logBox);
        PrintStream printStream = new PrintStream(console, true);
        System.setOut(printStream);
        System.setErr(printStream);
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
