package com.glencoesoftware.convert.workflows;

import com.glencoesoftware.convert.tasks.BaseTask;
import com.glencoesoftware.convert.tasks.CreateNGFF;
import com.glencoesoftware.convert.tasks.CreateTiff;

public class ConvertToNGFF extends BaseWorkflow {

    public ConvertToNGFF() {
        this.setTasks(new BaseTask[]{new CreateNGFF()});
    }


    public static String getDisplayName() {
        return "OME-NGFF";
    }
}
