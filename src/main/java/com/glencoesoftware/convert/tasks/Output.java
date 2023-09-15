package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.bioformats2raw.Converter;
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
        this.output = Paths.get(basePath, this.outputName + ".ome.tiff").toFile();
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

        @Override
        public ArrayList<Method> getConfigurableMethods () {
            return null;
        }
    }
