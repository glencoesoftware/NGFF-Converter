/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * JavaFX App
 */
public class App extends Application {

    public static Scene scene;

    public static PrimaryController controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("primary.fxml"));
        Parent primary = fxmlLoader.load();
        scene = new Scene(primary, 1024, 600);
        controller = fxmlLoader.getController();
        stage.setScene(scene);
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) { version = "DEV"; }
        stage.setTitle("NGFF-Converter - %s".formatted(version));
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("main-icon.png")));
        stage.getIcons().add(icon);
        stage.setOnCloseRequest(event -> Platform.exit());
        stage.show();
    }

    @Override
    public void stop() throws InterruptedException {
        if (controller.isRunning) {
            controller.runCancel();
        }
    }

    public static Scene getScene() {
        return scene;
    }

    public static void main(String[] args) {
        launch();
    }

}
