package com.glencoesoftware.convert.workflows;

import com.glencoesoftware.convert.PrimaryController;
import com.glencoesoftware.convert.tasks.BaseTask;
import com.glencoesoftware.convert.tasks.CreateNGFF;
import com.glencoesoftware.convert.tasks.Output;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;

import java.io.File;

public class ConvertToNGFF extends BaseWorkflow {

    public ConvertToNGFF(PrimaryController controller, File input) {
        super(controller, input);
        ObservableList<BaseTask> tasks = FXCollections.observableArrayList();
        tasks.addAll(new CreateNGFF(this), new Output(this));
        this.setTasks(tasks);
    }

    public static String shortName = "OME-NGFF";
    public static String fullName = "Convert to OME-NGFF";


    public String getShortName() { return shortName; }

    public String getFullName() { return fullName; }


    public FileChooser.ExtensionFilter[] getExtensionFilters() {
        return new FileChooser.ExtensionFilter[]{
                new FileChooser.ExtensionFilter("NGFF Files", "*.zarr", "*.ngff")
        };
    }
}
