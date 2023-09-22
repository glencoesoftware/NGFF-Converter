package com.glencoesoftware.convert.tables;

import com.glencoesoftware.convert.tasks.BaseTask;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;
import org.kordamp.ikonli.javafx.FontIcon;

public class WorkflowStatusTableCell extends TableCell<BaseWorkflow, Void> {

    private final Label mainLabel = new Label();


    private final FontIcon finishedIcon = new FontIcon("bi-check");

    private final FontIcon pendingIcon = new FontIcon("bi-circle-fill");
    private final FontIcon runningIcon = new FontIcon("bi-circle-fill");
    private final FontIcon failedIcon = new FontIcon("bi-circle-fill");
    private final FontIcon warningIcon = new FontIcon("bi-circle-fill");


    {
        finishedIcon.setIconSize(10);
        pendingIcon.setIconSize(10);
        runningIcon.setIconSize(10);
        failedIcon.setIconSize(10);
        warningIcon.setIconSize(10);
        finishedIcon.setIconColor(Paint.valueOf("GREEN"));
        pendingIcon.setIconColor(Paint.valueOf("BLUE"));
        runningIcon.setIconColor(Paint.valueOf("GREEN"));
        failedIcon.setIconColor(Paint.valueOf("RED"));
        warningIcon.setIconColor(Paint.valueOf("ORANGE"));
    }

    @Override
    public void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : this.mainLabel);
        if (empty) return;
        BaseWorkflow current = getTableView().getItems().get(getIndex());
        switch (current.status.get()) {
            case PENDING -> {
                this.mainLabel.setText("Pending");
                this.mainLabel.setGraphic(this.pendingIcon);
            }
            case COMPLETED -> {
                this.mainLabel.setText("Completed");
                this.mainLabel.setGraphic(this.finishedIcon);
            }
            case RUNNING -> {
                this.mainLabel.setText("Running");
                this.mainLabel.setGraphic(this.runningIcon);
            }
            case FAILED -> {
                this.mainLabel.setText("Failed");
                this.mainLabel.setGraphic(this.failedIcon);
            }
            case WARNING -> {
                this.mainLabel.setText("Error");
                this.mainLabel.setGraphic(this.warningIcon);
            }
            default -> {
                this.mainLabel.setText("Unknown");
                this.mainLabel.setGraphic(this.failedIcon);
            }
        }
    }
}