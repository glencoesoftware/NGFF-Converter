/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert.tables;

import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.tasks.BaseTask;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextAlignment;

public class TaskStatusTableCell extends TableCell<BaseTask, Void> {
    private final Label mainLabel = new Label();

    private final Tooltip labelTooltip = new Tooltip("Ready");

    private final HBox container = new HBox();

    final ProgressBar stoppingBar = new ProgressBar();
    final Label stoppingLabel = new Label("Stopping", stoppingBar);


    {
        container.setAlignment(Pos.CENTER);
        mainLabel.setTooltip(labelTooltip);
        mainLabel.getStyleClass().add("status-cell");
        stoppingLabel.setContentDisplay(ContentDisplay.TOP);
        stoppingLabel.setTextAlignment(TextAlignment.CENTER);
        stoppingBar.setMaxWidth(95);

    }

    @Override
    public void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : container);
        if (empty) return;
        container.getChildren().clear();
        BaseTask current = getTableView().getItems().get(getIndex());
        labelTooltip.setText("Task OK");
        mainLabel.setText(current.getStatusString());
        switch (current.status) {
            case COMPLETED -> {
                mainLabel.setGraphic(JobState.getStatusIcon(current.status, 15));
                labelTooltip.setText("Task successful");
                container.getChildren().add(mainLabel);
            }
            case RUNNING -> container.getChildren().addAll(current.getProgressWidget());
            case STOPPING -> container.getChildren().addAll(stoppingLabel);
            case WARNING -> {
                mainLabel.setGraphic(JobState.getStatusIcon(current.status, 15));
                labelTooltip.setText(current.warningMessage);
                container.getChildren().add(mainLabel);
            }
            case FAILED -> {
                labelTooltip.setText("Task Failed");
                mainLabel.setGraphic(JobState.getStatusIcon(current.status, 15));
                container.getChildren().add(mainLabel);
            }
            default -> {
                mainLabel.setGraphic(JobState.getStatusIcon(current.status, 15));
                container.getChildren().add(mainLabel);
            }
        }
    }
}