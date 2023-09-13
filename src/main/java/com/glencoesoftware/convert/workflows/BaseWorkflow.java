package com.glencoesoftware.convert.workflows;

import com.glencoesoftware.convert.App;
import com.glencoesoftware.convert.tasks.BaseTask;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

public abstract class BaseWorkflow {

    static String getDisplayName() {
        return null;
    }

    private static final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(App.class);

    public enum workflowStatus {PENDING, RUNNING, COMPLETED, FAILED, WARNING}

    public workflowStatus status = workflowStatus.PENDING;

    public String statusText = "";

    public File firstInput = null;

    public File finalOutput = null;

    public int currentStage = 0;

    public boolean allowOverwrite = false;

    private BaseTask[] tasks;

    protected void setTasks(BaseTask[] tasks) {
        this.tasks = tasks;
    }

    public BaseTask getNextStage(){
        if (this.status == workflowStatus.COMPLETED) {
            return null;
        }
        int nextIndex = this.currentStage + 1;
        if (nextIndex >= this.tasks.length) {
            return null;
        }
        return this.tasks[nextIndex];
    }

    public BaseTask getLastStage(){
        if (this.status == workflowStatus.COMPLETED) {
            return null;
        }
        if (this.currentStage == 0){
            return null;
        }
        return this.tasks[this.currentStage - 1];
    }

    public BaseTask getCurrentStage(){
        if (this.status == workflowStatus.COMPLETED) {
            return null;
        }
        return this.tasks[this.currentStage];
    }

    public void calculateIO(String inputPath, String outputBasePath, String workingDir) {
        // Run through each task and determine the input/output file paths to feed into each-other.
        String targetDir = workingDir;
        this.firstInput = new File(inputPath);
        File workingInput = this.firstInput;
        for (int i = 0; i < this.tasks.length; i++) {
            if (i == this.tasks.length - 1) {
                // This is the last task, use the final output destination rather than the temp dir
                targetDir = outputBasePath;
            }
            BaseTask task = this.tasks[i];
            task.setInput(workingInput);
            task.setOutput(targetDir);
            workingInput = task.getOutput();
        }
        this.finalOutput = workingInput;
        if (this.finalOutput.exists() && !this.allowOverwrite) {
            this.status = workflowStatus.WARNING;
            this.statusText = "Output path already exists: " + this.finalOutput.getAbsolutePath();
        } else {
            this.status = workflowStatus.PENDING;
            this.statusText = "";
        }
        LOGGER.info("Path calculation complete. Final output will be:");
        LOGGER.info(workingInput.getAbsolutePath());
    }

    public void execute() throws Exception {
        // Presumes input/outputs are pre-calculated
        this.status = workflowStatus.RUNNING;
//        this.calculateIO();
        for (BaseTask task : this.tasks) {
            LOGGER.info("Running task ");
            LOGGER.info(String.valueOf(task));
            task.run();
            LOGGER.info("Completed");
            if (task.status != BaseTask.taskStatus.COMPLETED) {
                this.status = workflowStatus.FAILED;
                return;
            }
        }
        // Cleanup intermediates
        for (int i = 0; i < this.tasks.length - 1; ++i) {
            BaseTask task = this.tasks[i];
            File output = task.getOutput();
            if (!Objects.equals(output, this.finalOutput)) {
                if (!output.exists()){
                    continue;
                }
                if (output.isDirectory()) {
                    FileUtils.deleteDirectory(output);
                } else {
                    FileUtils.delete(output);
                }
            }
        }
        this.status = workflowStatus.COMPLETED;
    }
}