package com.glencoesoftware.convert;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

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
