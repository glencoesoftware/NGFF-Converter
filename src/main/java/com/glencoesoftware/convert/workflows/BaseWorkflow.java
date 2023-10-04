package com.glencoesoftware.convert.workflows;

import com.glencoesoftware.convert.App;
import com.glencoesoftware.convert.PrimaryController;
import com.glencoesoftware.convert.tasks.BaseTask;
import com.glencoesoftware.convert.tasks.Output;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

public abstract class BaseWorkflow {

    // Keep a reference to the parent controller
    public PrimaryController controller = null;

    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public void setSelected(boolean val) {
        this.selected.set(val);
    }

    public BooleanProperty getSelected() {
        return this.selected;
    }

    static String getDisplayName() { return null; };

    public String getName() {
        return getDisplayName();
    }

    private static final ch.qos.logback.classic.Logger LOGGER =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(App.class);

    public enum workflowStatus {PENDING, RUNNING, COMPLETED, FAILED, WARNING}

    public ObjectProperty<workflowStatus> status = new SimpleObjectProperty<workflowStatus>(
            workflowStatus.PENDING);

    public String statusText = "";

    public File firstInput = null;

    public File finalOutput = null;

    public String workingDirectory = null;

    public String getInput() {
        if (firstInput == null) {
            return null;
        }
        return firstInput.getName();
    }

    public IntegerProperty currentStage = new SimpleIntegerProperty(-1);

    public boolean allowOverwrite = false;

    public ObservableList<BaseTask> tasks;
    protected void setTasks(ObservableList<BaseTask> tasks) {
        this.tasks = tasks;
    }

    public void respondToUpdate() {
        for (BaseTask task : this.tasks) {
            task.updateStatus();
            System.out.println("New status for "+ task.getName()  + " is " + task.getStatus());

        }
    }

    public BaseTask getNextStage(){
        if (this.status.get() == workflowStatus.COMPLETED) {
            return null;
        }
        int nextIndex = this.currentStage.get() + 1;
        if (nextIndex >= this.tasks.size()) {
            return null;
        }
        return this.tasks.get(nextIndex);
    }

    public BaseTask getLastStage(){
        if (this.status.get() == workflowStatus.COMPLETED) {
            return null;
        }
        if (this.currentStage.get() == 0){
            return null;
        }
        return this.tasks.get(this.currentStage.get() - 1);
    }

    public BaseTask getCurrentStage(){
        if (this.status.get() == workflowStatus.COMPLETED) {
            return null;
        }
        return this.tasks.get(this.currentStage.get());
    }

    public void calculateIO(String inputPath, String outputBasePath, String workingDir) {
        // Run through each task and determine the input/output file paths to feed into each-other.
        this.workingDirectory = workingDir;
        String targetDir = workingDir;
        this.firstInput = new File(inputPath);
        File workingInput = this.firstInput;
        for (int i = 0; i < this.tasks.size(); i++) {
            if (i == this.tasks.size() - 2) {
                // This is the last "real" task, use the final output destination rather than the temp dir
                targetDir = outputBasePath;
            }
            BaseTask task = this.tasks.get(i);
            if (task instanceof Output) {
                // This is the virtual output task that lets the user configure output names.
                // Carry forward i/o from last task
                task.input = workingInput;
                task.output = workingInput;
            } else {
                task.setInput(workingInput);
                task.setOutput(targetDir);
            }
            workingInput = task.getOutput();
        }
        this.finalOutput = workingInput;
        if (this.finalOutput.exists() && !this.allowOverwrite) {
            this.status.set(workflowStatus.WARNING);
            this.statusText = "Output path already exists: " + this.finalOutput.getAbsolutePath();
        } else {
            this.status.set(workflowStatus.PENDING);
            this.statusText = "";
        }
        LOGGER.info("Path calculation complete. Final output will be:");
        LOGGER.info(workingInput.getAbsolutePath());
        this.respondToUpdate();
    }

    public void reset() {
        this.currentStage.set(-1);
        this.status.set(workflowStatus.PENDING);
        for (BaseTask task : this.tasks) {
            task.status = BaseTask.taskStatus.PENDING;
            task.updateStatus();
        }
    }

    public void execute() throws Exception {
        // Presumes input/outputs are pre-calculated
        this.status.set(workflowStatus.RUNNING);
        this.currentStage.set(0);
//        this.calculateIO();
        for (BaseTask task : this.tasks) {
            LOGGER.info("Running task ");
            LOGGER.info(String.valueOf(task));
            task.run();
            LOGGER.info("Completed");
            this.currentStage.set(this.currentStage.get() + 1);
            if (task.status != BaseTask.taskStatus.COMPLETED) {
                this.status.set(workflowStatus.FAILED);
                this.currentStage.set(-1);
                return;
            }
        }
        // Cleanup intermediates
        for (int i = 0; i < this.tasks.size() - 1; ++i) {
            BaseTask task = this.tasks.get(i);
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
        this.status.set(workflowStatus.COMPLETED);
        this.currentStage.set(-1);
    }
}