/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert.tasks.progress;

import com.glencoesoftware.bioformats2raw.IProgressListener;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ConverterProgressListener implements IProgressListener {

    private final ProgressBar progressBar;
    private final Label labelText;
    private final Label timerText;
    private int totalSeries = -1;
    private int currentSeries = 0;
    private long totalChunks = -1;
    private double completedChunks = 0;
    private String elapsedTimeString = "";
    private AnimationTimer timer;

    /**
     * Create a new progress listener that displays a progress bar.
     *
     */
    public ConverterProgressListener(ProgressBar bar, Label label, Label timer) {
        progressBar = bar;
        labelText = label;
        timerText = timer;
    }

    public void updateBar() {
        Platform.runLater( () -> {
            progressBar.setProgress(completedChunks / totalChunks);
            if (totalSeries < 100)
                labelText.setText("Series %d of %d".formatted(currentSeries + 1, totalSeries));
            else if (totalSeries < 1000)
                labelText.setText("Series %d/%d".formatted(currentSeries + 1, totalSeries));
            else
                labelText.setText("%d/%d".formatted(currentSeries + 1, totalSeries));
            timerText.setText(elapsedTimeString);
        });
    }

    public void start() {
        long startTime = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("mm:ss");
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                elapsedTimeString = dateFormat.format(new Date(System.currentTimeMillis() - startTime));
                    updateBar();
                }
            };
        timer.start();
    }

    public void stop() {
        // Stop the animation timer if it was started
        if (timer != null) timer.stop();
    }

    @Override
    public void notifyStart(int seriesCount, long chunkCount) {
        totalSeries = seriesCount;
        totalChunks = chunkCount;
        start();
    }

    @Override
    public void notifySeriesStart(int series, int resolutionCount, int chunkCount) {
        currentSeries = series;
    }

    @Override
    public void notifySeriesEnd(int series) {
        if (series == totalSeries - 1) {
            timer.stop();
        }
    }

    @Override
    public void notifyResolutionStart(int resolution, int tileCount) {
        // intentional no-op
    }

    @Override
    public void notifyChunkStart(int plane, int xx, int yy, int zz) {
        // intentional no-op
    }

    @Override
    public void notifyChunkEnd(int plane, int xx, int yy, int zz) {
        // N.b. we don't trigger a bar refresh here to avoid excessive updates.
        completedChunks += 1;
    }

    @Override
    public void notifyResolutionEnd(int resolution) {
        // intentional no-op
    }

}
