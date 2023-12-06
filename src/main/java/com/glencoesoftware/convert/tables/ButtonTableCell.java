/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert.tables;

import com.glencoesoftware.convert.tasks.BaseTask;
import javafx.collections.FXCollections;
import javafx.scene.control.*;

import org.kordamp.ikonli.javafx.FontIcon;


public class ButtonTableCell extends TableCell<BaseTask, Void> {

    private final Button configureButton = new Button("");

    private final FontIcon cog = new FontIcon("bi-gear-fill");


    {
        cog.setIconSize(16);
        cog.getStyleClass().add("icon-graphic");
        configureButton.getStyleClass().add("icon-button");
        configureButton.setGraphic(cog);
        configureButton.setPrefWidth(32);
        configureButton.setPrefHeight(32);

        configureButton.setOnAction(ext -> {
            BaseTask task = getTableView().getItems().get(getIndex());
            task.parent.controller.displaySettingsDialog(FXCollections.observableArrayList(task.parent), getIndex());
        });

    }

    @Override
    public void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : configureButton);
        if (empty) return;
        BaseTask current = getTableView().getItems().get(getIndex());
        switch (current.status) {
            case RUNNING, QUEUED, COMPLETED, FAILED, STOPPING -> configureButton.setDisable(true);
            case READY, WARNING -> configureButton.setDisable(false);
        }

    }
}
