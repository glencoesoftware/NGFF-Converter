/**
 * Copyright (c) 2022 Glencoe Software, Inc. All rights reserved.
 *
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

// Not implemented yet, should be a dialog to modify job parameters.
public class EditJob {

    @FXML
    private Label inputPathLabel;
    @FXML
    private TextField outputPathField;

    @FXML
    public void setData(IOPackage data){
        inputPathLabel.setText(data.fileIn.getAbsolutePath());
        outputPathField.setText(data.fileOut.getAbsolutePath());
    }


    @FXML
    private void selectOutputFile() {

    }

    @FXML
    private void onSave() {

    }
    @FXML
    private void onCancel() {

    }


}
