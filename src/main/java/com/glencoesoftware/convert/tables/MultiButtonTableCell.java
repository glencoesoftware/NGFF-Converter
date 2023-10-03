package com.glencoesoftware.convert.tables;

import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;

import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;


public class MultiButtonTableCell extends TableCell<BaseWorkflow, Void> {

    private final HBox container;
    private final Button showLog = new Button();
    private final Button statusClearComplete = new Button();
    private final Button statusStopRunning = new Button();
    private final Button statusClearPending = new Button();
    private final Button configureTasks = new Button();
    private final Button resetJob = new Button();
    private final FontIcon clearCompleteIcon = new FontIcon("bi-check");
    private final FontIcon logIcon = new FontIcon("bi-terminal");
    private final FontIcon stopRunningIcon = new FontIcon("bi-stop-fill");
    private final FontIcon removeJobIcon = new FontIcon("bi-x");
    private final FontIcon tasksIcon = new FontIcon("bi-list-check");
    private final FontIcon configureIcon = new FontIcon("bi-gear-fill");
    private final FontIcon restartIcon = new FontIcon("bi-arrow-repeat");

    {
        showLog.setGraphic(logIcon);
        statusClearComplete.setGraphic(clearCompleteIcon);
        statusStopRunning.setGraphic(stopRunningIcon);
        statusClearPending.setGraphic(removeJobIcon);
        configureTasks.setGraphic(configureIcon);
        resetJob.setGraphic(restartIcon);

        statusClearComplete.setOnAction(evt -> {
            // delete this row item from TableView items
            getTableView().getItems().remove(getIndex());
        });

        statusClearPending.setOnAction(evt -> {
            // delete this row item from TableView items
            getTableView().getItems().remove(getIndex());
        });

        statusStopRunning.setOnAction(evt -> {
            int index = getIndex();
            // delete this row item from TableView items
            List<BaseWorkflow> jobs = getTableView().getItems();
            BaseWorkflow subject = jobs.get(index);
            System.out.println("Would halt execution of " + subject.firstInput.getName());
        });

        configureTasks.setOnAction(evt -> {
            int index = getIndex();
            // delete this row item from TableView items
            List<BaseWorkflow> jobs = getTableView().getItems();
            BaseWorkflow subject = jobs.get(index);
            System.out.println("Would configure " + subject.firstInput.getName());
            subject.controller.displaySettingsDialog(FXCollections.observableArrayList(subject));
        });

        resetJob.setOnAction(evt -> {
            int index = getIndex();
            // delete this row item from TableView items
            List<BaseWorkflow> jobs = getTableView().getItems();
            BaseWorkflow subject = jobs.get(index);
            System.out.println("Would reset " + subject.firstInput.getName());
            subject.reset();
            getTableView().refresh();
        });

        container = new HBox(5);
        container.setAlignment(Pos.CENTER);
    }

    @Override
    public void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : container);
        if (empty) return;
        this.container.getChildren().clear();
        BaseWorkflow current = getTableView().getItems().get(getIndex());
        switch (current.status.get()) {
            case COMPLETED -> this.container.getChildren().addAll(this.statusClearComplete, this.showLog);
            case RUNNING -> this.container.getChildren().addAll(this.statusStopRunning, this.showLog);
            case FAILED -> this.container.getChildren().addAll(this.resetJob, this.showLog);
            case PENDING, WARNING -> this.container.getChildren().addAll(this.statusClearPending, this.configureTasks);
            default -> System.out.println("Error: " + "No definition for table contents??");
        }
    }

}
