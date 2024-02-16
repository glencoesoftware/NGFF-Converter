/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert.dialogs;

import com.glencoesoftware.bioformats2raw.Converter;
import com.glencoesoftware.convert.App;
import com.glencoesoftware.pyramid.PyramidFromDirectoryWriter;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import loci.formats.ImageReader;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class AboutDialog {
    @FXML
    private Label guiVersion;
    @FXML
    private Label b2rVersion;
    @FXML
    private Label r2oVersion;
    @FXML
    private Label bfVersion;
    @FXML
    public void initialize() {
        guiVersion.setText(guiVersion.getText() + App.version);
        bfVersion.setText(bfVersion.getText() + ImageReader.class.getPackage().getImplementationVersion());
        b2rVersion.setText(b2rVersion.getText() + Converter.class.getPackage().getImplementationVersion());
        r2oVersion.setText(r2oVersion.getText() +
                PyramidFromDirectoryWriter.class.getPackage().getImplementationVersion());
    }

    @FXML
    public void licenseLink() {
        try {
            Desktop.getDesktop().browse(
                    new URI("https://github.com/glencoesoftware/NGFF-Converter/blob/main/LICENSE.txt"));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
