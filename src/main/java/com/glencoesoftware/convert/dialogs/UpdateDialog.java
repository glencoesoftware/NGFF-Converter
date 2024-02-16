/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert.dialogs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glencoesoftware.convert.App;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import java.awt.Desktop;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.control.HyperlinkLabel;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static com.glencoesoftware.convert.PrimaryController.prefName.LAST_UPDATE;

public class UpdateDialog {

    private final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(this.getClass());

    public static final Preferences userPreferences = Preferences.userRoot();

    public boolean updateAvailable = false;

    @FXML
    private VBox container;

    @FXML
    public CheckBox shouldCheckForUpdates;
    @FXML
    private Label finalLabel;

    @FXML
    public void initialize() {
        shouldCheckForUpdates.setSelected(userPreferences.getLong(LAST_UPDATE.name(), 0) != -1);
    }

    public void doUpdateCheck() throws BackingStoreException, IOException {
        String currentVersion = App.version;
        long lastUpdate = userPreferences.getLong(LAST_UPDATE.name(), 0);
        URL uri = new URL("https://downloads.glencoesoftware.com/public/NGFF-Converter/metadata/version.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode response = mapper.readTree(uri);
        String latestVersion = response.get("currentversion").textValue();

        container.getChildren().clear();
        // Prepare UI
        if (compareVersion(latestVersion, currentVersion)) {
            updateAvailable = true;
            Label header = new Label("A new version of NGFF-Converter is available (v%s)".formatted(latestVersion));
            header.getStyleClass().add("main-text");
            HyperlinkLabel link = new HyperlinkLabel("[Click here] to visit the download page");
            link.setOnAction(event -> {
                try {
                    Desktop.getDesktop().browse(new URI("https://downloads.glencoesoftware.com/public/NGFF-Converter/latest/"));
                } catch (IOException | URISyntaxException e) {
                    LOGGER.error("Failed to open URL");
                    throw new RuntimeException(e);
                }
            });
            link.getStyleClass().add("link-label");
            link.getStyleClass().add("main-text");
            container.getChildren().addAll(header, link);
        } else {
            Label text = new Label("You are currently using the latest NGFF-Converter release\n(v%s)".formatted(currentVersion));
            text.getStyleClass().add("main-text");
            container.getChildren().add(text);
        }

        if (lastUpdate != -1) {
            // Get current date in days
            long currentDate = new Date().getTime() / 86400000;
            userPreferences.putLong(LAST_UPDATE.name(), currentDate);
            userPreferences.flush();
        }
    }

    public void show() {
        Stage stage = (Stage) finalLabel.getScene().getWindow();
        stage.showAndWait();
        handlePostDisplay();
    }

    private boolean compareVersion(String latestVersion, String currentVersion) {
        LOGGER.debug("Comparing versions %s & %s".formatted(currentVersion, latestVersion));
        // Don't bother with deep comparison if we're on current
        if (latestVersion.equals(currentVersion)) return false;
        // Don't check with dev versions
        if (currentVersion.equals("DEV")) return false;
        // Versions differ, figure out which is newer
        // Strip any RC/Snapshot tags
        if (currentVersion.contains("-")) currentVersion = currentVersion.substring(0, currentVersion.indexOf("-"));
        // Consider numeric version numbers
        String[] currentVersionParts = currentVersion.split("\\.");
        String[] latestVersionParts = latestVersion.split("\\.");
        if (currentVersionParts.length != latestVersionParts.length) {
            LOGGER.error("Failed to compare versions %s & %s".formatted(currentVersion, latestVersion));
            return false;
        }
        for (int i = 0; i < currentVersionParts.length; i++){
            int currentNum = Integer.parseInt(currentVersionParts[i]);
            int latestNum = Integer.parseInt(latestVersionParts[i]);
            if (latestNum < currentNum) return false;
        }
        return true;
    }

    public void handlePostDisplay() {
        if (!shouldCheckForUpdates.isSelected()) {
            userPreferences.putLong(LAST_UPDATE.name(), -1);
            try {
                userPreferences.flush();
            } catch (BackingStoreException e) {
                LOGGER.error("Failed to write settings");
            }
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) container.getScene().getWindow();
        stage.close();
    }

}
