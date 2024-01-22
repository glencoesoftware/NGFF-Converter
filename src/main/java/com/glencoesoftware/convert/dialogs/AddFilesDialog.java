/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert.dialogs;

import com.glencoesoftware.convert.PrimaryController;
import com.glencoesoftware.convert.tasks.Output;
import com.glencoesoftware.convert.workflows.ConvertToNGFF;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static com.glencoesoftware.convert.PrimaryController.prefName.DEFAULT_FORMAT;
import static com.glencoesoftware.convert.PrimaryController.prefName.SHOW_FORMAT_DLG;
import static com.glencoesoftware.convert.tasks.BaseTask.getSettingContainer;

public class AddFilesDialog {

    private final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(this.getClass());

    public static final Preferences userPreferences = Preferences.userRoot();

    @FXML
    private final ChoiceBox<String> workflowChoiceBox = new ChoiceBox<>();
    @FXML
    private VBox settingsPanel;
    @FXML
    public CheckBox wantDefault;
    @FXML
    public CheckBox shouldNotShow;
    @FXML
    private Label finalLabel;

    public boolean shouldProceed = false;

    private final VBox workflowChoiceContainer = getSettingContainer(workflowChoiceBox,
            "Output format", "Choose the target format for output");


    public void initialize() {
        workflowChoiceBox.getItems().setAll(PrimaryController.installedWorkflows.keySet());
        workflowChoiceBox.setValue(userPreferences.get(DEFAULT_FORMAT.name(), ConvertToNGFF.shortName));
        shouldNotShow.selectedProperty().addListener((obs, oldSelection, newSelection) -> {
            wantDefault.setDisable(newSelection);
            if (newSelection) wantDefault.setSelected(true);
        });
    }

    public void prepareForDisplay() {
        // Get standard settings and apply defaults
        workflowChoiceBox.setValue(userPreferences.get(DEFAULT_FORMAT.name(), ConvertToNGFF.shortName));
        ArrayList<Node> settings = Output.getAddFilesSettings();
        Output.resetWidgets();
        settingsPanel.getChildren().clear();
        settingsPanel.getChildren().add(workflowChoiceContainer);
        settingsPanel.getChildren().addAll(settings);
        settingsPanel.getChildren().addAll(wantDefault, shouldNotShow, finalLabel);
        shouldNotShow.setSelected(userPreferences.getBoolean(SHOW_FORMAT_DLG.name(), false));
        if (shouldNotShow.isSelected()) wantDefault.setSelected(true);
        shouldProceed = false;
    }

    public void handlePostDisplay() {
        if (wantDefault.isSelected()) {
            userPreferences.put(DEFAULT_FORMAT.name(), workflowChoiceBox.getValue());
            try {
                userPreferences.flush();
                Output.setDefaultsFromWidgets();
            } catch (BackingStoreException e) {
                LOGGER.error("Failed to flush settings " + e);
            }
        }
        wantDefault.setSelected(false);
        if (shouldNotShow.isSelected() != userPreferences.getBoolean(SHOW_FORMAT_DLG.name(), false)) {
            userPreferences.putBoolean(SHOW_FORMAT_DLG.name(), shouldNotShow.isSelected());
            try {
                userPreferences.flush();
            } catch (BackingStoreException e) {
                LOGGER.error("Failed to flush settings");
            }
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) settingsPanel.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onApply() {
        shouldProceed = true;
        onClose();
    }

    public String getOutputFormat() {
        return workflowChoiceBox.getValue();
    }
}

