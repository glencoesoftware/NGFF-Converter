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
    private final MainController parent;
    public boolean interrupted = false;
    private static final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(ConverterTask.class);

    public WorkflowRunner(MainController controller) {
        LOGGER.setLevel(Level.DEBUG);
        parent = controller;
        jobList = parent.jobList;
    }

    @Override
    protected Integer call() throws Exception {
        int count = 0;
        for (BaseWorkflow job : jobList.getItems()) {
            if (interrupted || (job.status.get() == BaseWorkflow.workflowStatus.COMPLETED) ||
                    (job.status.get() == BaseWorkflow.workflowStatus.FAILED)) {
                continue;
            }
            LOGGER.info("Setup job with new config");

            job.status.set(BaseWorkflow.workflowStatus.RUNNING);
            Platform.runLater(() -> {
                LOGGER.info("Working on " + job.firstInput.getName());
                jobList.refresh();
            });

            LOGGER.info("Running new model pipeline");
            job.execute();
            LOGGER.info("Completed");

            //Todo: Cleanup intermediates?

            switch (job.status.get()) {
                case COMPLETED -> LOGGER.info("Successfully created: " + job.finalOutput.getName() + "\n");
                case FAILED -> {
                    if (interrupted) {
                        LOGGER.info("User aborted job: " + job.finalOutput.getName() + "\n");
                    } else {
                        LOGGER.info("Job failed, see logs \n");
                    }
                }
                default -> LOGGER.info("Job status is invalid????: " + job.status);
            }

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
