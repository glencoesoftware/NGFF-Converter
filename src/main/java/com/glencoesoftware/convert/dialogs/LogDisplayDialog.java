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
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class LogDisplayDialog {

    private Stage stage;
    @FXML
    public TextArea logBox;
    public TextAreaStream stream;
    public PrimaryController parentController;
    public Label title;
    public HBox topPane;

    private TextAreaAppender<ILoggingEvent> logBoxAppender;

    public void setTitle(String newTitle) {
        title.setText(newTitle);
    }

    private double xOffset;
    private double yOffset;

    @FXML
    private FontIcon resizeIcon;


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

    public void registerStage(Stage mainStage) {
        stage = mainStage;
        topPane.setOnMousePressed(event -> {
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        });

        topPane.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
        });

        resizeIcon.setOnMousePressed(event -> {
            xOffset = stage.getWidth() - event.getSceneX();
            yOffset = stage.getHeight() - event.getSceneY();
        });

        resizeIcon.setOnMouseDragged(event -> {
            stage.setWidth(Math.max(400.0, event.getScreenX() - stage.getX() + xOffset));
            stage.setHeight(Math.max(400.0, event.getScreenY() - stage.getY() + yOffset));
        });
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

    @FXML
    public void closeLogs() {
        stage.close();
    }
}
