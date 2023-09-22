package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;

public abstract class BaseTask {

    public BaseWorkflow parent;
    public static enum taskStatus {PENDING, RUNNING, COMPLETED, FAILED, ERROR}

    public taskStatus status = taskStatus.PENDING;

    public File input = null;
    public File output = null;

    public String outputName = "";

    public String warningMessage = "";

    public ArrayList<String> parameters = new ArrayList<>();

    public File getInput(){
        return this.input;
    };

    public BaseTask(BaseWorkflow parent) {
        this.parent = parent;
    }

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

    abstract public void updateStatus();

}
