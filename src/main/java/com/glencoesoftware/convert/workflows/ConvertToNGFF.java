/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
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

    public String getTechnicalName() { return "bioformats2raw"; }

    public FileChooser.ExtensionFilter[] getExtensionFilters() {
        return new FileChooser.ExtensionFilter[]{
                new FileChooser.ExtensionFilter("NGFF Files", "*.zarr", "*.ngff")
        };
    }
}
