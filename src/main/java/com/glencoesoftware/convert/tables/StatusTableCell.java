package com.glencoesoftware.convert.tables;

import com.glencoesoftware.convert.tasks.BaseTask;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;
import org.kordamp.ikonli.javafx.FontIcon;

public class StatusTableCell extends TableCell<BaseTask, Void> {
    private final ProgressBar progressBar = new ProgressBar(0.5);

    private final Label mainLabel = new Label();

    private final HBox container = new HBox();

    private final FontIcon finishedIcon = new FontIcon("bi-check");

    private final FontIcon pendingIcon = new FontIcon("bi-circle-fill");

    private final FontIcon cog = new FontIcon("bi-gear-fill");

    {
        finishedIcon.setIconSize(20);
        pendingIcon.setIconSize(20);
        cog.setIconSize(20);
        finishedIcon.setIconColor(Paint.valueOf("GREEN"));
        pendingIcon.setIconColor(Paint.valueOf("BLUE"));
        cog.setIconColor(Paint.valueOf("BLUE"));
    }

    @Override
    public void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : this.container);
        if (empty) return;
        this.container.getChildren().clear();
        BaseTask current = getTableView().getItems().get(getIndex());
        switch (current.status) {
            case COMPLETED -> {
                this.mainLabel.setText("Completed");
                this.mainLabel.setGraphic(finishedIcon);
                this.container.getChildren().add(this.mainLabel);
            }
            case RUNNING -> {
                this.mainLabel.setText("00:00:10");
                this.container.getChildren().addAll(this.progressBar, this.mainLabel);
            }
            default -> {
                this.mainLabel.setText("Pending");
                this.mainLabel.setGraphic(this.pendingIcon);
                this.container.getChildren().add(mainLabel);
            }
        }
    }
}