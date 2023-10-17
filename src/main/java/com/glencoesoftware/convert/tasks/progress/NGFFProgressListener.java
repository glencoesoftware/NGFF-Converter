package com.glencoesoftware.convert.tasks.progress;

/**
 * Copyright (c) 2022 Glencoe Software, Inc. All rights reserved.
 *
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */

import com.glencoesoftware.bioformats2raw.Converter;
import com.glencoesoftware.bioformats2raw.IProgressListener;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.text.SimpleDateFormat;
import java.util.Date;

public class NGFFProgressListener implements IProgressListener {

    private final ProgressBar progressBar;
    private final Label labelText;
    private double stepContribution;

    private int currentSeries = 0;

    private int totalSeries = -1;

    private final Converter converter;

    private double progress = 0;

    private String elapsedTimeString = "";

    private AnimationTimer timer;

    /**
     * Create a new progress listener that displays a progress bar.
     *
     */
    public NGFFProgressListener(ProgressBar bar, Label label, Converter subject) {
        progressBar = bar;
        labelText = label;
        converter = subject;
    }

    public void updateBar() {
        Platform.runLater( () -> {
            progressBar.setProgress(progress);
            labelText.setText("Series %d of %d\n%s".formatted(currentSeries + 1, totalSeries, elapsedTimeString));
        });
    }

    public void start() {
        // We do this here because the final series list isn't generated until juuust before we start running
        totalSeries = converter.getSeriesList().size();

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
        timer.stop();
    };

    @Override
    public void notifySeriesStart(int series) {
        System.out.printf("Got series start %d %n", series);
        if (totalSeries == -1) {
            start();
        }
        currentSeries = series;
        progress = 0.0;
        // Todo: figure out total resolutions
        updateBar();
    }

    @Override
    public void notifySeriesEnd(int series) {
        if (series == totalSeries - 1) {
            timer.stop();
        }
        System.out.printf("Got series end %d %n", series);
        progress = 1;
    }

    @Override
    public void notifyResolutionStart(int resolution, int tileCount) {
        System.out.printf("Got res start %d %d%n", resolution, tileCount);
//        pb.setProgress(pb.getProgress() + stepContribution / 5);
    }

    @Override
    public void notifyChunkStart(int plane, int xx, int yy, int zz) {
        System.out.println("Got chunk start");
        progress += 0.001;
        updateBar();
//        progressBar.setProgress(progressBar.getProgress() + 0.01);
        // intentional no-op
    }

    @Override
    public void notifyChunkEnd(int plane, int xx, int yy, int zz) {
    }

    @Override
    public void notifyResolutionEnd(int resolution) {
        System.out.printf("Got res end %d%n", resolution);
    }

}
