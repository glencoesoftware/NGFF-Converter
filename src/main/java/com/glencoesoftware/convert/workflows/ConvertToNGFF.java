package com.glencoesoftware.convert.workflows;

import com.glencoesoftware.convert.tasks.BaseTask;
import com.glencoesoftware.convert.tasks.CreateNGFF;
import com.glencoesoftware.convert.tasks.CreateTiff;
import com.glencoesoftware.convert.tasks.Output;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ConvertToNGFF extends BaseWorkflow {


    public ConvertToNGFF() {
        ObservableList<BaseTask> tasks = FXCollections.observableArrayList();
        tasks.addAll(new CreateNGFF(this), new Output(this));
        this.setTasks(tasks);
    }

    public static String getDisplayName() {
        return "OME-NGFF";
    }

    public String getName() {
        return getDisplayName();
    }


}
