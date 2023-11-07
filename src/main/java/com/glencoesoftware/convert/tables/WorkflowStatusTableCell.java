package com.glencoesoftware.convert.tables;

import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;

public class WorkflowStatusTableCell extends TableCell<BaseWorkflow, Void> {

    private final Label mainLabel = new Label();

    {
        mainLabel.getStyleClass().add("status-cell");
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