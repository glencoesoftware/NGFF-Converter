package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.bioformats2raw.Converter;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import com.glencoesoftware.convert.workflows.ConvertToNGFF;
import picocli.CommandLine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class CreateNGFF extends BaseTask{

    private String name = "Convert to NGFF";

    public CreateNGFF(BaseWorkflow parent) {
        super(parent);
    }

    public String getName() {
        return this.name;
    }

    public final Converter converter = new Converter();
    private Method[] paramMethods = null;
    public Method[] getParamMethods() {
        if (this.paramMethods == null) {
            Method[] converterMethods = Converter.class.getDeclaredMethods();
            System.out.println("All methods:");
            System.out.println(Arrays.toString(converterMethods));
            this.paramMethods = Arrays.stream(converterMethods)
                    .filter(m -> Modifier.isPublic(m.getModifiers()) && m.getName().startsWith("set")).toArray(Method[]::new);
        }
        return this.paramMethods;
    }

    public void setOutput(String basePath) {
        this.output = Paths.get(basePath, this.outputName + ".zarr").toFile();
    }

    private void setupIO() {
        converter.setInputPath(this.input.getAbsolutePath());
        converter.setOutputPath(this.output.getAbsolutePath());
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

    public void run() {
        // Apply GUI configurations first
        setupIO();
        this.status = taskStatus.RUNNING;
        try {
            int result = converter.call();
            if (result == 0) {
                this.status = taskStatus.COMPLETED;
            } else {
                this.status = taskStatus.FAILED;
            }
        } catch (Exception e) {
            System.out.println("Error");
            this.status = taskStatus.FAILED;
        }
    }
}
