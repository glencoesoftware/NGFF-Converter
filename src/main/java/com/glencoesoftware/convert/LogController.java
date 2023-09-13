/**
 * Copyright (c) 2022 Glencoe Software, Inc. All rights reserved.
 *
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
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
    public MainController parentController;

    @FXML
    public void initialize() {
        stream = new ConsoleStream(logBox);
        ConsoleStreamAppender.setStaticOutputStream(stream);
    }

    public void setParent(MainController parent) {
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
