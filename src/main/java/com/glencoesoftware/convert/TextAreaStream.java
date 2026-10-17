/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert;


import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.apache.commons.lang3.StringUtils;

import java.io.OutputStream;

// We want to capture stdout from workflows into a JavaFX text area.
// We could write the stream directly, but this creates excessive GUI updates.
// Instead, we'll capture lines into a temporary buffer and flush to console.
public class TextAreaStream extends OutputStream
{
    private final TextArea output;
    private final StringBuffer buffer;

    public TextAreaStream(TextArea logBox) {
        this.output = logBox;
        this.buffer = new StringBuffer();
    }

    @Override
    public void write(final int i) {
      buffer.append((char) i);
    }

    @Override
    public void write(byte[] b) {
      buffer.append(new String(b));
    }

    @Override
    public void flush() {
      Platform.runLater(() -> {
          output.setText(this.buffer.toString());
      });
    }

}
