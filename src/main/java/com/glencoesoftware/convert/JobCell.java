/**
 * Copyright (c) 2022 Glencoe Software, Inc. All rights reserved.
 *
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert;

import com.glencoesoftware.convert.tasks.CreateNGFF;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import com.glencoesoftware.convert.workflows.ConvertToNGFF;
import com.glencoesoftware.convert.workflows.ConvertToTiff;
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

public class JobCell extends ListCell<BaseWorkflow> {
    final HBox content;

    final CheckBox selected;

    final Label nameIn;

    final Tooltip fullPath;

    final ChoiceBox<String> formatChoice;
    final Label monitor;
    final FontIcon ok;
    final FontIcon notOk;
    final FontIcon success;
    final FontIcon fail;
    final ProgressIndicator progress;

    final Button showLogButton;

    final Button removeButton;

    final Button resetButton;

    final Button settingsButton;

    final HBox actionButtons;

    public JobCell() {
        super();
        Font nameFont = new Font(14);

        selected = new CheckBox();

        nameIn = new Label();
        nameIn.setFont(nameFont);
        nameIn.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        fullPath = new Tooltip();
        Tooltip.install(nameIn, fullPath);

        formatChoice = new ChoiceBox<>();
//        formatChoice.getItems().addAll(ConvertToTiff.getDisplayName(), ConvertToNGFF.getDisplayName());


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

        settingsButton = new Button("");
        settingsButton.setGraphic(ok);
        resetButton = new Button("");
        resetButton.setGraphic(notOk);
        showLogButton = new Button("");
        showLogButton.setGraphic(success);
        removeButton = new Button("");
        removeButton.setGraphic(fail);

        actionButtons = new HBox();
        actionButtons.getChildren().addAll(settingsButton, resetButton);

        content = new HBox(selected, nameIn, formatChoice, monitor, actionButtons);
        content.setMinWidth(0);
        content.setPrefWidth(1);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setSpacing(10);
    }

    @Override
    public void updateItem(BaseWorkflow job, boolean empty) {
        super.updateItem(job, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else if (job != null) {
            File fileIn = job.firstInput;
            File fileOut = job.finalOutput;
            nameIn.setText(fileIn.getName());
            fullPath.setText(fileIn.getAbsolutePath());
            actionButtons.getChildren().clear();
            switch (job.status.get()) {
                case PENDING -> {
                    monitor.setGraphic(ok);
                    monitor.setTooltip(new Tooltip("Ready to run"));
                    actionButtons.getChildren().addAll(settingsButton, removeButton);
                }
                case COMPLETED -> {
                    monitor.setGraphic(success);
                    monitor.setTooltip(new Tooltip("Conversion successful"));
                    actionButtons.getChildren().addAll(settingsButton, removeButton, showLogButton);
                }
                case FAILED -> {
                    monitor.setGraphic(fail);
                    monitor.setTooltip(new Tooltip("Conversion failed"));
                    actionButtons.getChildren().addAll(settingsButton, removeButton, resetButton, showLogButton);
                }
                case WARNING -> {
                    monitor.setGraphic(notOk);
                    monitor.setTooltip(new Tooltip("Output file already exists"));
                    actionButtons.getChildren().addAll(settingsButton, removeButton);

                }
                case RUNNING -> {
                    monitor.setGraphic(progress);
                    progress.setTooltip(new Tooltip("Running"));
                    actionButtons.getChildren().addAll(removeButton, showLogButton);
                }
                default -> throw new IllegalStateException("Unexpected value: " + job.status);
            }
            setGraphic(content);
        } else {
            setText(null);
            setGraphic(null);
        }
    }
}
