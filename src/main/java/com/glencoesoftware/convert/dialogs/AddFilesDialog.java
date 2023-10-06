package com.glencoesoftware.convert.dialogs;

import com.glencoesoftware.convert.PrimaryController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.*;

import java.util.Objects;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class AddFilesDialog {

    private final Dialog<String> dialog;


    private final ChoiceBox<String> workflows;
    private final CheckBox setDefault;
    private final CheckBox doNotShowAgain;

    public AddFilesDialog() {

        dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        //Add buttons to the dialog pane
        dialog.getDialogPane().getButtonTypes().addAll(cancel, save);
        dialog.getDialogPane().setPrefSize(350,250);

        Insets spacer = new Insets(0, 0, 10, 0);

        Label title = new Label("Choose output format");
        title.setFont(Font.font("ARIEL", FontWeight.BOLD, 28));
        title.setPadding(spacer);
        title.setMinWidth(Control.USE_PREF_SIZE);

        ObservableList<String> options = FXCollections.observableArrayList();

        for (PrimaryController.OutputMode mode : PrimaryController.OutputMode.values()) {
            options.add(mode.getDisplayName());
        }


        workflows = new ChoiceBox<>(options);
        workflows.setValue(options.get(0));
        workflows.setMinWidth(330);
        workflows.setMaxWidth(Double.MAX_VALUE);

        setDefault = new CheckBox("Set this format as my default");
        setDefault.setMinWidth(Control.USE_PREF_SIZE);

        doNotShowAgain = new CheckBox("Don't ask me this again");
        doNotShowAgain.setMinWidth(Control.USE_PREF_SIZE);
        doNotShowAgain.setPadding(spacer);
        Label explainer = new Label("(You can always change this later under the File menu)");
        explainer.setPadding(spacer);
        explainer.setMinWidth(Control.USE_PREF_SIZE);

        VBox container = new VBox(10, title, workflows, setDefault, doNotShowAgain, explainer);
        container.setFillWidth(true);
        container.setPadding(new Insets(10));
        dialog.getDialogPane().getChildren().add(container);

        // Only return a result if "OK" was clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton.getButtonData() != ButtonBar.ButtonData.OK_DONE) { return null; }
            return workflows.getValue();
        });
    }

    public PrimaryController.OutputMode show(PrimaryController parent) {
        Preferences prefs = parent.userPreferences;
        boolean shouldShow = prefs.getBoolean(PrimaryController.prefName.SHOW_FORMAT_DLG.name(), true);
        String choice = prefs.get(PrimaryController.prefName.DEFAULT_FORMAT.name(), "OME-NGFF");
        if (shouldShow) {
            if (dialog.getOwner() == null) {
                dialog.initOwner(parent.jobList.getScene().getWindow());
            }
            workflows.setValue(choice);
            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) { return null; }
            choice = result.get();
            if (setDefault.isSelected()) {
                prefs.put(PrimaryController.prefName.DEFAULT_FORMAT.name(), choice);
                try {
                    prefs.flush();
                } catch (BackingStoreException e) {
                    System.out.println("Unable to write preference" + e);
                }
            }
            if (doNotShowAgain.isSelected()) {
                prefs.putBoolean(PrimaryController.prefName.SHOW_FORMAT_DLG.name(), false);
                try {
                    prefs.flush();
                } catch (BackingStoreException e) {
                    System.out.println("Unable to write preference" + e);
                }
            }
        }
        // There is probably a better way to do this
        for (PrimaryController.OutputMode mode : PrimaryController.OutputMode.values()) {
            if (Objects.equals(mode.getDisplayName(), choice)) {
                return mode;
            }
        }
        // User cancelled the dialog or chose nothing??
        return null;
    }
}

