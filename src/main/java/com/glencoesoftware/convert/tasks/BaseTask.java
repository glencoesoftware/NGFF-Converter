package com.glencoesoftware.convert.tasks;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;

public abstract class BaseTask {
    public static enum taskStatus {PENDING, RUNNING, COMPLETED, FAILED}

    public taskStatus status = taskStatus.PENDING;

    public File input = null;
    public File output = null;

    public String outputName = "";

    public String outputExtension = "";

    public ArrayList<String> parameters = new ArrayList<>();

    public File getInput(){
        return this.input;
    };

    public void setInput(File input){
        this.input = input;
        this.outputName = FilenameUtils.getBaseName(input.getAbsolutePath());
    };

    public String getStatus() {
        return this.status.toString();
    };

    abstract public String getName();

    public File getOutput(){
        return this.output;
    };

    abstract public void setOutput(String basePath);

    abstract public void run();

    abstract ArrayList<Method> getConfigurableMethods();

}
