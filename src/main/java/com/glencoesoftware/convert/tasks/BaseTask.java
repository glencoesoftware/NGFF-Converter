/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert.tasks;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.glencoesoftware.convert.App;
import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;

// Abstract base class for Tasks.
public abstract class BaseTask {

    public final BaseWorkflow parent;
    public final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(this.getClass());

    public JobState.status status = JobState.status.READY;

    public File input = null;
    public File output = null;
    public String outputName = "";
    public String warningMessage = "";
    protected final ProgressBar progressBar = new ProgressBar();
    protected final Label progressLabel = new Label("Preparing");
    protected final Label timerLabel = new Label("");
    protected final VBox progressContainer = new VBox(progressLabel, progressBar, timerLabel);

    abstract public String getName();

    public BaseTask(BaseWorkflow parent) {
        this.parent = parent;
        // We want some basic log messages to always show
        LOGGER.setLevel(Level.INFO);
        progressContainer.setAlignment(Pos.CENTER);
        progressContainer.setSpacing(1);
        progressLabel.setTextAlignment(TextAlignment.CENTER);
        progressBar.setMaxWidth(95);
    }

    public VBox getProgressWidget() {
        return progressContainer;
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
    abstract public void calculateOutput(String basePath);

    // Execute the task
    abstract public void run();

    // Optional method to be run just before all jobs in the workflow start. Use to validate settings,
    // make last minute changes, etc.
    public void prepareToRun() {}

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
    abstract public void setOverwrite(boolean shouldOverwrite);

    // Clear any loaded settings and apply the defaults. Primarily needed to call reset() methods on executors.
    abstract public void resetToDefaults();

    // Clear any loaded settings and apply the defaults. Primarily needed to call reset() methods on executors.
    abstract public void exportSettings(JsonGenerator generator) throws IOException;

    // Clear any loaded settings and apply the defaults. Primarily needed to call reset() methods on executors.
    abstract public void importSettings(JsonNode mainNode);

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

    public static HBox getFileSelectWidget(TextField pathField, String title, boolean newFile,
                                           File defaultDir, FileChooser.ExtensionFilter[] extensionFilters) {

        FontIcon browseButton = new FontIcon("bi-folder-fill");
        browseButton.setIconSize(16);
        browseButton.getStyleClass().add("file-browser-button");
        browseButton.onMouseClickedProperty().set(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(title);
            File startDir = null;
            if (pathField.getText() != null && !pathField.getText().isEmpty()) {
                File currentDir = new File(pathField.getText());
                fileChooser.setInitialFileName(currentDir.getName());
                if (currentDir.exists()) startDir = currentDir.getParentFile();
            } else if (defaultDir != null) startDir = defaultDir;
            fileChooser.setInitialDirectory(startDir);
            if (extensionFilters != null) fileChooser.getExtensionFilters().addAll(extensionFilters);
            File selectedFile;
            if (newFile) selectedFile = fileChooser.showSaveDialog(App.getScene().getWindow());
            else selectedFile = fileChooser.showOpenDialog(App.getScene().getWindow());
            if (selectedFile != null) {
                pathField.setText(selectedFile.getAbsolutePath());
            }
        });

        FontIcon resetButton = new FontIcon("bi-x-circle-fill");
        resetButton.setIconSize(16);
        resetButton.getStyleClass().add("file-browser-button");
        resetButton.onMouseClickedProperty().set(e -> pathField.clear());
        HBox container = new HBox(5, pathField, browseButton, resetButton);
        HBox.setHgrow(pathField, Priority.ALWAYS);
        container.setAlignment(Pos.CENTER_LEFT);
        return container;
    }

    public static HBox getDirectorySelectWidget(TextField pathField, String title, File defaultDir) {
        FontIcon browseButton = new FontIcon("bi-folder-fill");
        browseButton.setIconSize(16);
        browseButton.getStyleClass().add("file-browser-button");
        browseButton.onMouseClickedProperty().set(e -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle(title);
            File startDir = null;
            if (pathField.getText() != null && !pathField.getText().isEmpty()) {
                File currentDir = new File(pathField.getText());
                if (currentDir.exists()) startDir = currentDir.getParentFile();
            } else if (defaultDir != null) startDir = defaultDir;
            dirChooser.setInitialDirectory(startDir);
            File selectedDir = dirChooser.showDialog(App.getScene().getWindow());
            if (selectedDir != null) {
                pathField.setText(selectedDir.getAbsolutePath());
            }
        });

        FontIcon resetButton = new FontIcon("bi-x-circle-fill");
        resetButton.setIconSize(16);
        resetButton.getStyleClass().add("file-browser-button");
        resetButton.onMouseClickedProperty().set(e -> pathField.clear());
        HBox container = new HBox(5, pathField, browseButton, resetButton);
        HBox.setHgrow(pathField, Priority.ALWAYS);
        container.setAlignment(Pos.CENTER_LEFT);
        return container;
    }
}
