package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.bioformats2raw.Converter;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import com.glencoesoftware.pyramid.PyramidFromDirectoryWriter;
import picocli.CommandLine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class CreateTiff extends BaseTask {

    private final PyramidFromDirectoryWriter converter = new PyramidFromDirectoryWriter();

    public CreateTiff(BaseWorkflow parent) {
        super(parent);
    }

    private String name = "Convert to TIFF";

    public String getName() {
        return this.name;
    }


    public void setOutput(String basePath) {
        this.output = Paths.get(basePath, this.outputName + ".ome.tiff").toFile();
    }

    private void setupIO() {
        converter.setInputPath(this.input.getAbsolutePath());
        converter.setOutputPath(this.output.getAbsolutePath());
    }

    public void run() {
        // Apply GUI configurations first
        setupIO();
        this.status = taskStatus.RUNNING;
        try {
            converter.call();
            this.status = taskStatus.COMPLETED;
        } catch (Exception e) {
            this.status = taskStatus.FAILED;
            System.out.println("Failed");
        }
    }

    public void updateStatus() {
        if (this.status == taskStatus.COMPLETED) { return; }
        if (this.output == null | this.input == null) {
            this.status = taskStatus.ERROR;
            this.warningMessage = "I/O not configured";
        } else {
            this.status = taskStatus.PENDING;
        }
    };


}
