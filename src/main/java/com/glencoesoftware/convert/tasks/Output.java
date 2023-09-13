package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.bioformats2raw.Converter;
import com.glencoesoftware.pyramid.PyramidFromDirectoryWriter;
import picocli.CommandLine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class Output extends BaseTask {
    // Virtual task to enable display of global config and cleanup of intermediates
    private boolean overwrite = false;

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean getOverwrite(){
        return this.overwrite;
    }

    public void setOutput(String basePath) {
        this.output = Paths.get(basePath, this.outputName + ".ome.tiff").toFile();
    }

    private void setupIO() {
        return;
    }

    public void run() {
        // Apply GUI configurations first
        setupIO();
        this.status = taskStatus.RUNNING;
        try {
            this.status = taskStatus.COMPLETED;
        } catch (Exception e) {
            this.status = taskStatus.FAILED;
            System.out.println("Failed");
        }
    }

    @Override
    public ArrayList<Method> getConfigurableMethods() {
        return null;
    }
}
