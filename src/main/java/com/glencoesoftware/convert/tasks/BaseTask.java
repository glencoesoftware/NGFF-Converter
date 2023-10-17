package com.glencoesoftware.convert.tasks;

import ch.qos.logback.classic.Level;
import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;

// Abstract base class for Tasks.
public abstract class BaseTask {

    public BaseWorkflow parent;
    public final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(this.getClass());

    public JobState.status status = JobState.status.READY;

    public File input = null;
    public File output = null;

    public String outputName = "";

    public String warningMessage = "";

    protected final ProgressBar progressBar = new ProgressBar();
    protected final Label progressLabel = new Label("Running", progressBar);

    abstract public String getName();

    public BaseTask(BaseWorkflow parent) {
        this.parent = parent;
        // We want some basic log messages to always show
        LOGGER.setLevel(Level.INFO);
        progressLabel.setContentDisplay(ContentDisplay.TOP);
        progressBar.setMaxWidth(95);
    }

    public Label getProgressWidget() {
        return progressLabel;
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

    // Task name for display
    public static String name = "Template Task";

    public File getOutput(){
        return this.output;
    }

    // Set output to be in the directory specified
    abstract public void setOutput(String basePath);

    // Execute the task
    abstract public void run();

    // Recalculate the task's status. Check for issues in settings, etc.
    abstract public void updateStatus();

    // Populate widgets with settings from the engine
    abstract public void prepareForDisplay();

    // Fetch a list of JavaFX widgets bound to task settings. Will be placed in a VBox.
    // StandardSettings are always shown in the configurator interface
    abstract public ArrayList<Node> getStandardSettings();

    // Fetch a list of JavaFX widgets bound to task settings. Will be placed in a VBox.
    // AdvancedSettings are only shown in the "advanced" mode of the configurator interface
    abstract public ArrayList<Node> getAdvancedSettings();

    // Used when the user clicks 'Apply' on the settings config window. Should take set values from the JavaFX widgets
    // and apply them to the task engine itself. I.e. settings entered by the user don't get applied to the task until
    // they hit the 'Apply' button.
    abstract public void applySettings();

    // Should store current task settings as default values for each (where applicable)
    abstract public void setDefaults() throws BackingStoreException;

    // Load default values saved previously
    abstract public void applyDefaults();

    // Copy values from supplied instance's widgets to this instance's widgets
    abstract public void cloneValues(BaseTask source);

    // Should receive global overwrite setting and apply that to the engine if necessary.
    public void setOverwrite(boolean shouldOverwrite) {
    }

    public static VBox getSettingContainer(Node node, String headerText, String tooltipText) {
        return new VBox(5, getSettingHeader(headerText, tooltipText), node);
    }


    public static VBox getSettingContainer(ChoiceBox<?> node, String headerText, String tooltipText) {
        VBox container = new VBox(5, getSettingHeader(headerText, tooltipText), node);
        node.setMaxWidth(Double.MAX_VALUE);
        return container;
    }

    public static VBox getSettingContainer(TextField node, String headerText, String tooltipText) {
        VBox container = new VBox(5, getSettingHeader(headerText, tooltipText), node);
        node.setMaxWidth(Double.MAX_VALUE);
        return container;
    }

    // Create a bordered container for grouping settings together
    public static VBox getSettingGroupContainer() {
        VBox container = new VBox(5);
        container.getStyleClass().add("setting-subcontainer");
        return container;

    }


    // Get a nice-looking capsule for the setting, including a header and tooltip
    public static Label getSettingHeader(String labelText, String tooltipText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("setting-label");
//        label.setTextFill(labelColor);
        if (!tooltipText.isEmpty()) {
            FontIcon help = new FontIcon("bi-question-circle");
            help.getStyleClass().add("help-icon");
            help.setIconSize(14);
            label.setGraphic(help);
            label.setContentDisplay(ContentDisplay.RIGHT);
            Tooltip tooltip = new Tooltip(tooltipText);
            tooltip.setAutoHide(true);
            label.setTooltip(tooltip);
            // User can click the 'help' icon to see the tooltip (without having to hover)
            help.setOnMouseClicked(e -> tooltip.show(label, e.getScreenX(), e.getScreenY()));
        }
        return label;
    }

    // Clear any loaded settings and apply the defaults. Primarily needed to call reset() methods on executors.
    abstract public void resetToDefaults();

}
