package com.glencoesoftware.convert.workflows;

import com.glencoesoftware.convert.PrimaryController;
import com.glencoesoftware.convert.tasks.BaseTask;
import com.glencoesoftware.convert.tasks.CreateNGFF;
import com.glencoesoftware.convert.tasks.CreateTiff;
import com.glencoesoftware.convert.tasks.Output;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;

import java.io.File;

public class ConvertToTiff extends BaseWorkflow{

    public ConvertToTiff(PrimaryController controller, File input) {
        super(controller, input);
        ObservableList<BaseTask> tasks = FXCollections.observableArrayList();
        tasks.addAll(new CreateNGFF(this), new CreateTiff(this), new Output(this));
        this.setTasks(tasks);
    }

    public static String shortName = "OME-TIFF";
    public static String fullName = "Convert to OME-TIFF";

    public String getShortName() { return shortName; }

    public String getFullName() { return fullName; }


    public FileChooser.ExtensionFilter[] getExtensionFilters() {
        return new FileChooser.ExtensionFilter[]{
                new FileChooser.ExtensionFilter("OME-TIFF Files", "*.ome.tiff", "*.ome.tif"),
                new FileChooser.ExtensionFilter("TIFF Files", "*.tif", "*.tiff")};
    }


}
