package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.bioformats2raw.Converter;
import com.glencoesoftware.pyramid.PyramidFromDirectoryWriter;
import picocli.CommandLine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class CreateTiff extends BaseTask {

    private final PyramidFromDirectoryWriter converter = new PyramidFromDirectoryWriter();

    private String name = "Convert to TIFF";

    public String getName() {
        return this.name;
    }


    public void setOutput(String basePath) {
        this.output = Paths.get(basePath, this.outputName + ".ome.tiff").toFile();
    }

    private void setupIO() {
        converter.setInputPath(this.input.getAbsolutePath());
        converter.setOutputPath(this.output.getAbsolutePath());
    }

    public void run() {
        // Apply GUI configurations first
        setupIO();
        this.status = taskStatus.RUNNING;
        try {
            converter.call();
            this.status = taskStatus.COMPLETED;
        } catch (Exception e) {
            this.status = taskStatus.FAILED;
            System.out.println("Failed");
        }
    }

    @Override
    public ArrayList<Method> getConfigurableMethods() {
        Method[] allMethods = Arrays.stream(PyramidFromDirectoryWriter.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && m.getName().startsWith("set"))
                .toArray(Method[]::new);
        ArrayList<Method> desiredMethods = new ArrayList<>();
        for (Method m : allMethods) {
            Annotation t = m.getAnnotation(CommandLine.Option.class);
            if (t == null) {
                System.out.println("No option config detected for " + m.getName());
            } else {
                System.out.println("Found Option config for " + m.getName());
                desiredMethods.add(m);
            }
        }
        return desiredMethods;
    }
}
