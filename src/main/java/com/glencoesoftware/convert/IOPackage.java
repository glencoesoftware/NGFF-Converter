package com.glencoesoftware.convert;

import java.io.File;

public class IOPackage {
    public final File fileIn;
    public File fileOut;
    public String status;

    public IOPackage(File in, File out, boolean overwrite) {
        fileIn = in;
        fileOut = out;
        if (fileOut.exists() && !overwrite) {
            status = "error";
        } else {
            status = "ready";
        }
    }
}
