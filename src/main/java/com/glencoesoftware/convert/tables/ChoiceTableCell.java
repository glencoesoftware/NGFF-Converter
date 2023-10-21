package com.glencoesoftware.convert.tables;

import com.glencoesoftware.convert.workflows.BaseWorkflow;
import com.glencoesoftware.convert.workflows.ConvertToNGFF;
import com.glencoesoftware.convert.workflows.ConvertToTiff;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import javafx.scene.control.TableCell;


public class ChoiceTableCell extends TableCell<BaseWorkflow, String> {

    private final ComboBox<String> options = new ComboBox<>();

    {
        options.getItems().addAll(ConvertToTiff.getDisplayName(), ConvertToNGFF.getDisplayName());
        options.setOnAction(ext -> {
            int index = getIndex();
            ObservableList<BaseWorkflow> items = getTableView().getItems();
            if (index == -1 | index >= items.size()) {return;}
            BaseWorkflow thisWorkflow = items.get(index);
            if (thisWorkflow.getName().equals(options.getValue())) {
                return;
            }
            BaseWorkflow newWorkflow;
            if (options.getValue().equals("OME-TIFF")) {
                System.out.println("Switching to tiff");
                newWorkflow = new ConvertToTiff(thisWorkflow.controller);
            } else {
                System.out.println("Switching to NGFF");
                newWorkflow = new ConvertToNGFF(thisWorkflow.controller);
            }

            newWorkflow.calculateIO(thisWorkflow.firstInput.getAbsolutePath());
            System.out.println("Changed workflow");
            items.set(index, newWorkflow);
        });

    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        options.setValue(item);
        setGraphic(empty ? null : options);
    }
}
