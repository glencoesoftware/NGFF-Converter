/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert.tables;

import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.PrimaryController;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import javafx.scene.control.TableCell;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


public class ChoiceTableCell extends TableCell<BaseWorkflow, String> {

    private final ComboBox<String> options = new ComboBox<>();

    {
        options.getStyleClass().add("workflow-choice");
        options.getItems().addAll(PrimaryController.installedWorkflows.keySet());
        options.setOnAction(ext -> {
            int index = getIndex();
            ObservableList<BaseWorkflow> items = getTableView().getItems();
            if (index == -1 | index >= items.size()) {return;}
            BaseWorkflow thisWorkflow = items.get(index);
            if (thisWorkflow.getShortName().equals(options.getValue())) {
                return;
            }
            BaseWorkflow newWorkflow;
            thisWorkflow.controller.updateStatus("Switching %s workflow to %s".formatted(
                    thisWorkflow.firstInput.getName(), options.getValue()));

            try {
                // Lookup the job name in the workflow list
                Constructor<? extends BaseWorkflow> jobClass = PrimaryController.installedWorkflows.get(
                        options.getValue()).getConstructor(PrimaryController.class, File.class);
                newWorkflow = jobClass.newInstance(thisWorkflow.controller, thisWorkflow.firstInput);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                // We catch these but they ---should--- never happen
                throw new RuntimeException(e);
            }

            newWorkflow.calculateIO();
            items.set(index, newWorkflow);
        });

    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        options.setValue(item);
        setGraphic(empty ? null : options);
        if (empty) return;
        BaseWorkflow current = getTableView().getItems().get(getIndex());
        JobState.status state = current.status.get();
        if (state != JobState.status.READY && state != JobState.status.WARNING) {
            // Can't change workflow of a completed job
            setGraphic(new Label(item));
        }
    }
}
