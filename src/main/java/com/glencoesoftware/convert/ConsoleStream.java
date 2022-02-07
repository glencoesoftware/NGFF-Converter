package com.glencoesoftware.convert;


import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.apache.commons.lang.StringUtils;

import java.io.OutputStream;

// We want to capture stdout from Picocli into a JavaFX text area.
// We could write the stream directly, but this creates excessive GUI updates.
// Instead, we'll capture lines into a temporary buffer and flush to console.
// Would love to do this with a custom PrintWriter class, but that doesn't
// seem to work with picocli, so we'll capture the raw stream instead.
public class ConsoleStream extends OutputStream
{
    private final TextArea output;
    private String buffer;
    private final int lineLimit = 500;

    public ConsoleStream(TextArea logBox)
    {
        this.output = logBox;
        this.buffer = "";
    }

    @Override
        public void write(final int i) {
        this.buffer += String.valueOf((char) i);
    }

    @Override
    public void flush() {
        Platform.runLater(() -> {
            output.appendText(this.buffer);
            this.buffer = "";
            int numLines = output.getText().split("\n").length;
            if (numLines > this.lineLimit) {
                int idx = StringUtils.ordinalIndexOf(output.getText(), "\n", numLines - this.lineLimit);
                output.setText(output.getText(idx + 1, output.getLength()));
            }
        });
    }

}

