package com.glencoesoftware.convert;

import java.io.File;

public class IOPackage {
    public File fileIn;
    public File fileOut;
    public Boolean status;
    public String info;

    public IOPackage(File in, File out) {
        fileIn = in;
        fileOut = out;
    }
}
