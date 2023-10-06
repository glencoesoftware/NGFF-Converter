package com.glencoesoftware.convert;

import ch.qos.logback.core.OutputStreamAppender;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TextAreaAppender<E> extends OutputStreamAppender<E> {

    private final DelegatingOutputStream DELEGATING_OUTPUT_STREAM = new DelegatingOutputStream(null);

    @Override
    public void start() {
        setOutputStream(DELEGATING_OUTPUT_STREAM);
        super.start();
    }

    public void setChildOutputStream(OutputStream outputStream) {
        DELEGATING_OUTPUT_STREAM.setOutputStream(outputStream);
    }

    private static class DelegatingOutputStream extends FilterOutputStream {
        // We don't need to write to the console itself
        public DelegatingOutputStream(OutputStream out){
            super(new OutputStream() {
                @Override
                public void write(int b) throws IOException {}
            });
        }

        void setOutputStream(OutputStream outputStream) {
            this.out = outputStream;
        }
    }
}