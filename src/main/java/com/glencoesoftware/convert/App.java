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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * JavaFX App
 */
public class App extends Application {

    public static Scene scene;

    public static PrimaryController controller;

    public static String version;

    public static Image appIcon = new Image(Objects.requireNonNull(App.class.getResourceAsStream("main-icon.png")));

    static {
        version = App.class.getPackage().getImplementationVersion();
        if (version == null) { version = "DEV"; }
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("primary.fxml"));
        Parent primary = fxmlLoader.load();
        scene = new Scene(primary, 1024, 600);
        controller = fxmlLoader.getController();
        stage.setScene(scene);
        stage.setTitle("NGFF-Converter - %s".formatted(version));
        stage.getIcons().add(appIcon);
        stage.setOnCloseRequest(event -> {
            if (controller.jobsRunning()) {
                // Confirm exit if tasks are running
                Alert choice = new Alert(Alert.AlertType.CONFIRMATION);
                choice.initOwner(getScene().getWindow());
                choice.setTitle("Close NGFF-Converter");
                choice.setHeaderText("Conversions are still running!");
                choice.setContentText(
                        "File conversions are still running, are you sure you want to quit?"
                );
                choice.getDialogPane().getStylesheets().add(
                        Objects.requireNonNull(App.class.getResource("Alert.css")).toExternalForm());
                choice.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.CANCEL) {
                        event.consume();
                    } else {
                        stop();
                        Platform.exit();
                    }
                });
            } else {
                stop();
                Platform.exit();
            }
        }
        );
        stage.show();
    }

    @Override
    public void stop() {
        if (controller.jobsRunning()) {
            controller.runCancel();
        }
        executor.shutdownNow();
    }

    public static Scene getScene() {
        return scene;
    }

    public static void main(String[] args) {
        launch();
    }

    public static final ExecutorService executor = Executors.newFixedThreadPool(1, r -> {
        Thread t = new Thread(r, "Converter");
        t.setDaemon(true);
        return t ;
    });

    private static final BlockingQueue<Runnable> queue = ((ThreadPoolExecutor) executor).getQueue();

    public static int queueSize() { return queue.size(); }
}
