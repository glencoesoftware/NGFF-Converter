package com.glencoesoftware.convert.tables;

import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
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
        this.mainLabel.setText(current.getStatusString());
        this.mainLabel.setGraphic(JobState.getStatusIcon(current.status.get(),15));
        if (!current.statusText.isEmpty()) {
            this.mainLabel.setTooltip(new Tooltip(current.statusText));
        } else {
            this.mainLabel.setTooltip(null);
        }
    }
}