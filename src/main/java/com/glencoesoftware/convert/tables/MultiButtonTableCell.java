package com.glencoesoftware.convert.tables;

import com.glencoesoftware.convert.tasks.BaseTask;
import javafx.scene.control.*;

import com.glencoesoftware.convert.tasks.BaseTask;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;


public class MultiButtonTableCell extends TableCell<BaseTask, Void> {

    private final HBox container;

    {
        Button view = new Button("View");
        Button delete = new Button("Delete");

        delete.setOnAction(evt -> {
            // delete this row item from TableView items
            getTableView().getItems().remove(getIndex());
        });

        view.setOnAction(evt -> {
            // call some method with the row item as parameter
            System.out.println("Would view");
        });

        container = new HBox(5, view, delete);
    }

    @Override
    public void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : container);
    }
}
