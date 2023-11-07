/**
 * Copyright (c) 2022 Glencoe Software, Inc. All rights reserved.
 *
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert;

import ch.qos.logback.classic.Level;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.TableView;
import org.slf4j.LoggerFactory;

class WorkflowRunner extends Task<Integer> {
    private final TableView<BaseWorkflow> jobList;
    private final PrimaryController parent;
    public boolean interrupted = false;
    private static final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(WorkflowRunner.class);

    public WorkflowRunner(PrimaryController controller) {
        LOGGER.setLevel(Level.DEBUG);
        parent = controller;
        jobList = parent.jobList;
    }

    @Override
    protected Integer call() {
        int count = 0;
        int totalJobs = 0;
        // Set everything to queued status
        for (BaseWorkflow job : jobList.getItems()) {
            job.prepareToActivate();
            if (job.status.get() == JobState.status.QUEUED) {
                totalJobs++;
            }
        }
        // Give the progress bar a very small value to trigger display.
        parent.updateProgress(0.01);
        // Start executing jobs
        for (BaseWorkflow job : jobList.getItems()) {
            if (interrupted || (job.status.get() == JobState.status.COMPLETED) ||
                    (job.status.get() == JobState.status.FAILED)) {
                continue;
            }

            job.status.set(JobState.status.RUNNING);
            Platform.runLater(() -> {
                jobList.getSelectionModel().select(job);
                jobList.refresh();
            });

            LOGGER.info("Running " + job.firstInput.getName());
            parent.updateStatus("Working on %s job %d of %d".formatted(job.firstInput.getName(), count + 1, totalJobs));
            job.execute();

            switch (job.status.get()) {
                case COMPLETED -> {
                    LOGGER.info("Successfully created: " + job.finalOutput.getName() + "\n");
                    parent.updateStatus(job.firstInput.getName() + " completed");
                }
                case FAILED -> {
                    parent.updateStatus(job.firstInput.getName() + " failed");
                    if (interrupted) {
                        LOGGER.info("User aborted job: " + job.finalOutput.getName() + "\n");
                    } else {
                        LOGGER.info("Job failed, see logs \n");
                    }
                }
                default -> LOGGER.info("Job status is invalid????: " + job.status);
            }
            count++;
            parent.updateProgress((double) count / totalJobs);

            Platform.runLater(jobList::refresh);
        }
        String finalStatus = String.format("Completed conversion of %s files.", count);
        Platform.runLater(() -> {
            LOGGER.info(finalStatus);
            parent.runCompleted();
        });
        return 0;
    }
}
