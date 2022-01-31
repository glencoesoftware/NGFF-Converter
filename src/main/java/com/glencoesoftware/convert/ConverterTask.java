package com.glencoesoftware.convert;

import com.glencoesoftware.bioformats2raw.Converter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
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
        System.out.println("Working");
        CommandLine runner = new CommandLine(new Converter());
        int count = 0;
        for (IOPackage job : inputFileList.getItems()) {
            File in = job.fileIn;
            File out = job.fileOut;
            if (!job.status.equals("ready")) {
                Platform.runLater(() -> {
                    System.out.println("Invalid input " + in.getName());
                    inputFileList.refresh();
                });
                continue;
            }

            job.status = "running";
            Platform.runLater(() -> {
                statusBox.setText("Working on " + in.getName());
                System.out.println("Would update List status");
                inputFileList.refresh();
            });

            // Construct args list

            ArrayList<String> params = new ArrayList<>(args);

            params.add(0, out.getAbsolutePath());
            params.add(0, in.getAbsolutePath());
            int result = runner.execute(params.toArray(new String[args.size()]));
            Platform.runLater(() -> {
                System.out.println("Would update UI");
                statusBox.setText("Completed task " + out);
                if (result == 0) {

                    job.status = "success";
                    statusBox.setText("Successfully created" + out.getName());
                } else {
                    job.status = "fail";
                    statusBox.setText("Failed with Exit Code " + result);
                }
                inputFileList.refresh();
            });
            if (result == 0) { count++; };
        }
        int finalCount = count;
        Platform.runLater(() -> {
            statusBox.setText(String.format("Completed conversion of %s files.", finalCount));
        });
        return 1;
    }
}