package com.glencoesoftware.convert.tables;

import com.glencoesoftware.convert.tasks.BaseTask;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;
import org.kordamp.ikonli.javafx.FontIcon;

public class TaskStatusTableCell extends TableCell<BaseTask, Void> {
    private final ProgressBar progressBar = new ProgressBar(0.5);

    private final Label mainLabel = new Label();

    private final Tooltip labelTooltip = new Tooltip("Ready");

    private final HBox container = new HBox();

    private final FontIcon finishedIcon = new FontIcon("bi-check");

    private final FontIcon pendingIcon = new FontIcon("bi-circle-fill");
    private final FontIcon warningIcon = new FontIcon("bi-exclamation-triangle-fill");

    {
        container.setAlignment(Pos.CENTER);
        mainLabel.setTooltip(labelTooltip);
        finishedIcon.setIconSize(20);
        pendingIcon.setIconSize(12);
        warningIcon.setIconSize(20);
        finishedIcon.setIconColor(Paint.valueOf("GREEN"));
        pendingIcon.setIconColor(Paint.valueOf("BLUE"));
        warningIcon.setIconColor(Paint.valueOf("ORANGE"));
    }

    @Override
    public void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : this.container);
        if (empty) return;
        this.container.getChildren().clear();
        BaseTask current = getTableView().getItems().get(getIndex());
        labelTooltip.setText("Task OK");
        switch (current.status) {
            case COMPLETED -> {
                this.mainLabel.setText("Completed");
                this.mainLabel.setGraphic(finishedIcon);
                this.labelTooltip.setText("Task successful");
                this.container.getChildren().add(this.mainLabel);
            }
            case RUNNING -> {
                this.mainLabel.setText("00:00:10");
                this.container.getChildren().addAll(this.progressBar, this.mainLabel);
            }
            case ERROR -> {
                this.mainLabel.setText("Error");
                this.mainLabel.setGraphic(warningIcon);
                this.labelTooltip.setText(current.warningMessage);
                this.container.getChildren().add(this.mainLabel);
            }
            default -> {
                this.mainLabel.setText("Pending");
                this.mainLabel.setGraphic(this.pendingIcon);
                this.container.getChildren().add(mainLabel);
            }
        }
    }
}