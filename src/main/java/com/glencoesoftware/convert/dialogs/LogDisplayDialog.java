/**
 * Copyright (c) 2022 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert.dialogs;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.glencoesoftware.convert.PrimaryController;
import com.glencoesoftware.convert.TextAreaAppender;
import com.glencoesoftware.convert.TextAreaStream;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class LogDisplayDialog {

    @FXML
    public TextArea logBox;
    public TextAreaStream stream;
    public PrimaryController parentController;
    public Label title;

    private TextAreaAppender<ILoggingEvent> logBoxAppender;

    public void setTitle(String newTitle) {
        title.setText(newTitle);
    }

    @FXML
    public void initialize() {
        stream = new TextAreaStream(logBox);
    }

    public TextAreaAppender<ILoggingEvent> getAppender() {
        if (logBoxAppender == null) {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            PatternLayoutEncoder ple = new PatternLayoutEncoder();
            ple.setPattern("%date [%thread] %-5level %logger{36} - %msg%n");
            ple.setContext(lc);
            ple.start();
            logBoxAppender = new TextAreaAppender<>();
            logBoxAppender.setChildOutputStream(stream);
            logBoxAppender.setEncoder(ple);
            logBoxAppender.setContext(lc);
            logBoxAppender.start();
        }
        return logBoxAppender;
    }

    public void setParent(PrimaryController parent) {
        parentController = parent;
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
