package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.scene.Node;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;

public abstract class BaseTask {

    public BaseWorkflow parent;
    public enum taskStatus {PENDING, RUNNING, COMPLETED, FAILED, ERROR}

    public taskStatus status = taskStatus.PENDING;

    public File input = null;
    public File output = null;

    public String outputName = "";

    public String warningMessage = "";

    public BaseTask(BaseWorkflow parent) {
        this.parent = parent;
    }

    public File getInput(){
        return this.input;
    }

    public void setInput(File input){
        this.input = input;
        this.outputName = FilenameUtils.getBaseName(input.getAbsolutePath());
    }

    // Get status as a string for display
    public String getStatus() {
        return this.status.toString();
    }

    // Get task name for display
    abstract public String getName();

    public File getOutput(){
        return this.output;
    }

    // Set output to be in the directory specified
    abstract public void setOutput(String basePath);

    // Execute the task
    abstract public void run();

    // Recalculate the task's status. Check for issues in settings, etc.
    abstract public void updateStatus();

    // Generate a list of JavaFX widgets bound to task settings. Will be placed in a VBox.
    // StandardSettings are always shown in the configurator interface
    abstract public ArrayList<Node> getStandardSettings();

    // Generate a list of JavaFX widgets bound to task settings. Will be placed in a VBox.
    // AdvancedSettings are only shown in the "advanced" mode of the configurator interface
    abstract public ArrayList<Node> getAdvancedSettings();

    // Used when the user clicks 'Apply' on the settings config window. Should take set values from the JavaFX widgets
    // and apply them to the task engine itself. I.e. settings entered by the user don't get applied to the task until
    // they hit the 'Apply' button.
    abstract public void applySettings();

    // Get an array of settings values from the widgets above. Used in multi-mode to copy settings between tasks of the
    // same type. Object list should hold values in a set order.
    abstract public Object[] getValues();

    // Takes an object array from getValues and applies those settings to the widgets.
    abstract public void setValues(Object[] objects);

    // Should store current task settings as default values for each (where applicable)
    abstract public void setDefaults();

    // Load default values saved previously
    abstract public void applyDefaults();

}
