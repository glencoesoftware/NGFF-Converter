package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.bioformats2raw.Converter;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import com.glencoesoftware.pyramid.PyramidFromDirectoryWriter;
import picocli.CommandLine;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

public class Output extends BaseTask {
    // Virtual task to enable display of global config and cleanup of intermediates
    private boolean overwrite = false;

    public Output(BaseWorkflow parent) {
        super(parent);
    }

    public String getName() {
        return "Output";
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean getOverwrite() {
        return this.overwrite;
    }

    public void setOutput(String basePath) {
        // Todo: Conform to desired workflow output type
        this.output = Paths.get(basePath, this.input.getName()).toFile();
        this.parent.finalOutput = this.output;
        // Get the previous task and update it's output
        this.parent.tasks.get(this.parent.tasks.size() - 2).output = this.output;
    }

    public void updateStatus() {
        System.out.println("Doing status update");
        if (this.status == taskStatus.COMPLETED) {
            return;
        }
        if (this.output == null | this.input == null) {
            this.status = taskStatus.ERROR;
            this.warningMessage = "I/O not configured";
        } else if (this.output.exists() && !this.overwrite) {
            this.status = taskStatus.ERROR;
            this.warningMessage = "Output file already exists";
        } else {
            this.status = taskStatus.PENDING;
        }
    }


    private void setupIO() {
        this.output = this.input;
    }

    public void run() {
        // Apply GUI configurations first
        setupIO();
        this.status = taskStatus.RUNNING;
        try {
            if (!Objects.equals(this.input.getAbsolutePath(), this.output.getAbsolutePath())) {
                Files.copy(this.input.toPath(), this.output.toPath(), ATOMIC_MOVE);
            }
            this.status = taskStatus.COMPLETED;
        } catch (IOException e) {
            this.status = taskStatus.FAILED;
            System.out.println("Failed to copy");
            System.out.println(e);
        }
        }

    }
