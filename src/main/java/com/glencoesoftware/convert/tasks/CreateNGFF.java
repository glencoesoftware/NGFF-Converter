package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.bioformats2raw.Converter;
import picocli.CommandLine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class CreateNGFF extends BaseTask{

    public final Converter converter = new Converter();

    private Method[] paramMethods = null;

    public Method[] getParamMethods() {
        if (this.paramMethods == null) {
            Method[] converterMethods = Converter.class.getDeclaredMethods();
            System.out.println("All methods:");
            System.out.println(Arrays.toString(converterMethods));
            this.paramMethods = Arrays.stream(converterMethods)
                    .filter(m -> Modifier.isPublic(m.getModifiers()) && m.getName().startsWith("set")).toArray(Method[]::new);
        }
        return this.paramMethods;
    }

    public ArrayList<Method> getConfigurableMethods() {
        Method[] allMethods = this.getParamMethods();
        ArrayList<Method> desiredMethods = new ArrayList<Method>();
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

    public void setOutput(String basePath) {
        this.output = Paths.get(basePath, this.outputName + ".zarr").toFile();
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
            int result = converter.call();
            if (result == 0) {
                this.status = taskStatus.COMPLETED;
            } else {
                this.status = taskStatus.FAILED;
            }
        } catch (Exception e) {
            System.out.println("Error");
            this.status = taskStatus.FAILED;
        }
    }
}
