package com.glencoesoftware.convert;

import com.glencoesoftware.bioformats2raw.Converter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import picocli.CommandLine;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class ConverterTask extends Task<Integer> {
    private final List<String> args;
    private final ListView<IOPackage> inputFileList;
    private final TextArea logBox;
    private final TextField statusBox;

    public ConverterTask(List<String> args, ListView<IOPackage> inputFileList, TextField statusBox, TextArea logBox) {
        this.args = args;
        this.inputFileList = inputFileList;
        this.statusBox = statusBox;
        this.logBox = logBox;
    }

    @Override protected Integer call() throws Exception {
        CommandLine runner = new CommandLine(new Converter());
        PrintWriter writer = new PrintWriter(new StringWriter());
        runner.setOut(writer);
        runner.setErr(writer);
        int count = 0;
        for (IOPackage job : inputFileList.getItems()) {
            File in = job.fileIn;
            File out = job.fileOut;
            if (job.status.equals("success") || job.status.equals("error")) {
                continue;
            }

            job.status = "running";
            Platform.runLater(() -> {
                statusBox.setText("Working on " + in.getName());
                logBox.appendText("Working on " + in.getName() + "\n");
                inputFileList.refresh();
            });

            // Construct args list
            ArrayList<String> params = new ArrayList<>(args);
            params.add(0, out.getAbsolutePath());
            params.add(0, in.getAbsolutePath());
            String[] fullParams = params.toArray(new String[args.size()]);
            Platform.runLater(() -> {
                logBox.appendText("Executing with args " + Arrays.toString(fullParams) + "\n");
            });

            boolean argsValid;
            // Validate args
            try {
                runner.parseArgs(fullParams);
                argsValid = true;
            } catch(Exception e) {
                Platform.runLater(() -> {logBox.appendText(e + "\n");});
                argsValid = false;
            }

            final int result;
            if (argsValid) {
                result = runner.execute(fullParams);
            } else {
                result = 9;
            }

            Platform.runLater(() -> {
                if (result == 0 && out.exists()) {
                    job.status = "success";
                    logBox.appendText("Successfully created " + out.getName() + "\n\n");
                } else if (result == 0) {
                    job.status = "noOutput";
                    logBox.appendText("Job completed without output " + out.getName() + "\n\n");
                } else if (result == 9) {
                    job.status = "fail";
                    logBox.appendText("Input arguments were invalid for " + out.getName() + "\n\n");
                } else {
                    job.status = "fail";
                    logBox.appendText("Failed with Exit Code " + result + "\n\n");
                }
                inputFileList.refresh();
            });
            if (result == 0) { count++; };
        }
        int finalCount = count;
        String finalStatus = String.format("Completed conversion of %s files.", finalCount);
        Platform.runLater(() -> {
            statusBox.setText(finalStatus);
        });
        Platform.runLater(() -> {
            logBox.appendText(finalStatus + "\n");
        });
        return 0;
    }
}