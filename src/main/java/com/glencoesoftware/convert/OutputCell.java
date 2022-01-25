package com.glencoesoftware.convert;

import javafx.scene.paint.*;
import javafx.geometry.*;
import java.io.File;
import javafx.scene.control.ListCell;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.CheckBox;
import javafx.scene.text.*;

import java.io.File;

public class OutputCell extends ListCell<File> {
    private HBox content;
    private Text name;
    private Text path;
    private CheckBox status;

    public OutputCell() {
        super();
        name = new Text();
        path = new Text();
        path.setFont(new Font(10));
        name.setFont(new Font(14));
        path.setFill(Color.GRAY);
        status = new CheckBox();
        status.setSelected(true);
        VBox vBox = new VBox(name, path);
        content = new HBox(status, vBox);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setSpacing(10);
        content.setBackground(null);
    }

    @Override
    public void updateItem(File file, boolean empty) {
        super.updateItem(file, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else if (file != null) {
            name.setText(file.getName());
            path.setText(file.getParent());
            setGraphic(content);
        } else {
            setText(null);
            setGraphic(null);
        };
    }
}
