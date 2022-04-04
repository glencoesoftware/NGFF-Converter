package com.glencoesoftware.convert;

import java.io.File;

public class IOPackage {
    public final File fileIn;
    public File fileOut;
    public PrimaryController.jobStatus status;
    public PrimaryController.OutputMode outputMode;

    public IOPackage(File in, File out, boolean overwrite, PrimaryController.OutputMode mode) {
        outputMode = mode;
        fileIn = in;
        fileOut = out;
        if (fileOut.exists() && !overwrite) {
            status = PrimaryController.jobStatus.ERROR;
        } else {
            status = PrimaryController.jobStatus.READY;
        }
    }
}
