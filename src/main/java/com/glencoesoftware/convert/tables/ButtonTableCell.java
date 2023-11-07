package com.glencoesoftware.convert.tables;

import com.glencoesoftware.convert.tasks.BaseTask;
import javafx.collections.FXCollections;
import javafx.scene.control.*;

import javafx.scene.control.TableCell;
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
            case RUNNING, QUEUED, COMPLETED, FAILED -> configureButton.setDisable(true);
            case READY, WARNING -> configureButton.setDisable(false);
        }

    }
}
