/**
 * Copyright (c) 2022 Glencoe Software, Inc. All rights reserved.
 *
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert;


import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.apache.commons.lang.StringUtils;

import java.io.OutputStream;

// We want to capture stdout from workflows into a JavaFX text area.
// We could write the stream directly, but this creates excessive GUI updates.
// Instead, we'll capture lines into a temporary buffer and flush to console.
// Note that Trace-level logs can print thousands of lines in a single operation.
// We only show the last 1000 lines in the GUI.
public class TextAreaStream extends OutputStream
{
    private final TextArea output;
    private final StringBuilder buffer;
    private final int lineLimit = 1000;
    private boolean locked = false;

    public TextAreaStream(TextArea logBox) {
        this.output = logBox;
        this.buffer = new StringBuilder();
    }

    @Override
        public void write(final int i) { buffer.append((char) i); }

    @Override
    public void flush() {
        if (this.locked) {
            // A UI update event already exists in the queue.
            return;
        } else {
            this.locked = true;
        }
        Platform.runLater(() -> {
            String newData = this.buffer.toString();
            int newLines = newData.split("\n").length;
            int existingLines = output.getText().split("\n").length;
            if (newLines > this.lineLimit) {
                int idx = StringUtils.ordinalIndexOf(newData, "\n", newLines - this.lineLimit);
                output.deleteText(0, output.getText().length());
                output.appendText(newData.substring(idx));
            } else if (newLines + existingLines > this.lineLimit) {
                int idx = StringUtils.ordinalIndexOf(output.getText(), "\n", existingLines - this.lineLimit);
                output.deleteText(0, idx+1);
                output.appendText(this.buffer.toString());
            } else {
                output.appendText(this.buffer.toString());
            }

            this.buffer.setLength(0);
            this.locked = false;
        });
    }

    public void forceFlush() {
        Platform.runLater(() -> {
            output.appendText(this.buffer.toString());
            this.buffer.setLength(0);
            this.locked = false;
        });
    }
}

