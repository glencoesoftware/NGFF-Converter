package com.glencoesoftware.convert.tables;

import com.glencoesoftware.convert.App;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;

import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.*;


public class MultiButtonTableCell extends TableCell<BaseWorkflow, Void> {

    private final HBox container;
    private final Button showLog = new Button();
    private final Button removeJob = new Button();
    private final Button stopRunning = new Button();
    private final Button configureTasks = new Button();
    private final Button resetJob = new Button();
    private final Button showFile = new Button();
    private final FontIcon removeIcon = new FontIcon("bi-trash-fill");
    private final FontIcon logIcon = new FontIcon("bi-terminal-fill");
    private final FontIcon stopRunningIcon = new FontIcon("bi-stop-fill");
    private final FontIcon configureIcon = new FontIcon("bi-gear-fill");
    private final FontIcon restartIcon = new FontIcon("bi-arrow-repeat");
    private final FontIcon openDirIcon = new FontIcon("bi-folder-symlink-fill");

    {
        showLog.setGraphic(logIcon);
        showLog.setTooltip(new Tooltip("Show execution logs"));
        showLog.setOnAction(evt -> getTableRow().getItem().showLogBox());

        removeJob.setGraphic(removeIcon);
        removeJob.setTooltip(new Tooltip("Remove from list"));
        removeJob.setOnAction(evt -> {
            // delete this row item from TableView items
            getTableView().getItems().remove(getIndex());
        });

        stopRunning.setGraphic(stopRunningIcon);
        stopRunning.setTooltip(new Tooltip("Stop execution"));
        stopRunning.setOnAction(evt -> {
            // Stop an ongoing run
            try {
                App.controller.runCancel();
            } catch (InterruptedException e) {
                System.out.println("Failed to stop " + e);
                throw new RuntimeException(e);
            }
            // Todo: stop single task?
        });

        configureTasks.setGraphic(configureIcon);
        configureTasks.setTooltip(new Tooltip("Configure job settings"));
        configureTasks.setOnAction(evt -> {
            BaseWorkflow subject = getTableRow().getItem();
            subject.controller.displaySettingsDialog(FXCollections.observableArrayList(subject), 0);
        });

        resetJob.setGraphic(restartIcon);
        resetJob.setTooltip(new Tooltip("Reset job to run again"));
        resetJob.setOnAction(evt -> {
            BaseWorkflow subject = getTableRow().getItem();
            subject.reset();
            getTableView().refresh();
        });

        showFile.setGraphic(openDirIcon);
        showFile.setTooltip(new Tooltip("Open containing folder"));
        showFile.setOnAction(evt -> {
            Desktop desktop = Desktop.getDesktop();
            desktop.browseFileDirectory(getTableRow().getItem().finalOutput);
        });


        container = new HBox(5);
        container.setAlignment(Pos.CENTER);
        // Add a bit of spacing on the right for the scrollbar
        container.setPadding(new Insets(0,10,0,0));
    }

    @Override
    public void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : container);
        if (empty) return;
        this.container.getChildren().clear();
        BaseWorkflow current = getTableView().getItems().get(getIndex());
        switch (current.status.get()) {
            case COMPLETED -> this.container.getChildren().addAll(this.showLog, this.showFile, this.removeJob);
            case RUNNING, QUEUED -> this.container.getChildren().addAll(this.showLog, this.stopRunning);
            case FAILED -> this.container.getChildren().addAll(this.showLog, this.resetJob, this.removeJob);
            case WARNING, READY -> this.container.getChildren().addAll(
                    this.showLog, this.configureTasks, this.removeJob);
        }
    }

}
