package com.glencoesoftware.convert;

import java.io.IOException;
import java.io.OutputStream;

import javafx.scene.control.TextArea;

public class ConsoleLogger extends OutputStream
{

    private TextArea output;

    public ConsoleLogger(TextArea field)
    {
        this.output = field;
        output.setText("Ready");
    }

    @Override
    public void write(int i) throws IOException
    {
        output.appendText(String.valueOf((char) i));
    }

    @Override
    public void flush() throws IOException {
        System.out.println("Flushing DATA RIGHT NOW");
    }

}
