package com.glencoesoftware.convert.workflows;

import com.glencoesoftware.convert.tasks.BaseTask;
import com.glencoesoftware.convert.tasks.CreateNGFF;
import com.glencoesoftware.convert.tasks.CreateTiff;
import com.glencoesoftware.convert.tasks.Output;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ConvertToTiff extends BaseWorkflow{
    public ConvertToTiff() {
        ObservableList<BaseTask> tasks = FXCollections.observableArrayList();
        tasks.addAll(new CreateNGFF(), new CreateTiff(), new Output());
        this.setTasks(tasks);
    }

    public static String getDisplayName() {
        return "OME-TIFF";
    }
}
