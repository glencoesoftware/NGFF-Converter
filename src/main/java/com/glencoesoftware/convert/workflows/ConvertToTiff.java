package com.glencoesoftware.convert.workflows;

import com.glencoesoftware.convert.tasks.BaseTask;
import com.glencoesoftware.convert.tasks.CreateNGFF;
import com.glencoesoftware.convert.tasks.CreateTiff;

public class ConvertToTiff extends BaseWorkflow{
    public ConvertToTiff() {
        this.setTasks(new BaseTask[]{new CreateNGFF(), new CreateTiff()});
    }

    public static String getDisplayName() {
        return "OME-TIFF";
    }
}
