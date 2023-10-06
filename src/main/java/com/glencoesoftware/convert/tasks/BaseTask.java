package com.glencoesoftware.convert.tasks;

import ch.qos.logback.classic.Level;
import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.scene.Node;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;

public abstract class BaseTask {

    public BaseWorkflow parent;
    public final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(this.getClass());

    public JobState.status status = JobState.status.READY;

    public File input = null;
    public File output = null;

    public String outputName = "";

    public String warningMessage = "";

    public BaseTask(BaseWorkflow parent) {
        this.parent = parent;
        // We want some basic log messages to always show
        LOGGER.setLevel(Level.INFO);
    }

    public File getInput(){
        return this.input;
    }

    public void setInput(File input){
        this.input = input;
        this.outputName = FilenameUtils.getBaseName(input.getAbsolutePath());
    }

    // Get status as a string for display
    public String getStatusString() {
        return StringUtils.capitalize(this.status.toString().toLowerCase());
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

    // Trigger creation and population of widget nodes
    public void prepareWidgets() {
        generateNodes();
        updateNodes();
    }

    // Construct the widgets for the UI
    abstract public void generateNodes();

    // Populate widgets with settings from the engine
    abstract public void updateNodes();

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

    // Should store current task settings as default values for each (where applicable)
    abstract public void setDefaults();

    // Load default values saved previously
    abstract public void applyDefaults();

    // Copy values from supplied instance's widgets to this instance's widgets
    abstract public void cloneValues(BaseTask source);

    // Should receive global overwrite setting and apply that to the engine if necessary.
    public void setOverwrite(boolean shouldOverwrite) {
    }

}
