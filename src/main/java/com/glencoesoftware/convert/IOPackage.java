/**
 * Copyright (c) 2022 Glencoe Software, Inc. All rights reserved.
 *
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert;

import com.glencoesoftware.convert.tasks.CreateNGFF;
import com.glencoesoftware.convert.tasks.CreateTiff;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import com.glencoesoftware.convert.workflows.ConvertToNGFF;
import com.glencoesoftware.convert.workflows.ConvertToTiff;

import java.io.File;

public class IOPackage {
    public final File fileIn;
    public File fileOut;
    public PrimaryController.jobStatus status;
    public PrimaryController.OutputMode outputMode;
    public BaseWorkflow workflow;

    public IOPackage(File in, File out, boolean overwrite, PrimaryController.OutputMode mode) {
        outputMode = mode;
        fileIn = in;
        fileOut = out;
        if (fileOut.exists() && !overwrite) {
            status = PrimaryController.jobStatus.ERROR;
        } else {
            status = PrimaryController.jobStatus.READY;
        }
//        if (mode == PrimaryController.OutputMode.TIFF) {
//            this.workflow = new ConvertToTiff();
//        } else {
//            this.workflow = new ConvertToNGFF();
//        }
    }
}
