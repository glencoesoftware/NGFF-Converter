/**
 * Copyright (c) 2023 Glencoe Software, Inc. All rights reserved.
 * This software is distributed under the terms described by the LICENSE.txt
 * file you can find at the root of the distribution bundle.  If the file is
 * missing please request a copy by contacting info@glencoesoftware.com
 */
package com.glencoesoftware.convert;

import javafx.scene.effect.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.HashMap;
import java.util.Map;

public class JobState {

    public enum status {READY, QUEUED, RUNNING, COMPLETED, FAILED, WARNING}

    public static Map<status, String> iconCodes = new HashMap<>();
    public static Map<status, String> iconColors = new HashMap<>();

    static {
        iconCodes.put(status.READY, "bi-circle-fill");
        iconCodes.put(status.QUEUED, "bi-circle-fill");
        iconCodes.put(status.RUNNING, "bi-play-circle-fill");
        iconCodes.put(status.COMPLETED, "bi-check-circle-fill");
        iconCodes.put(status.FAILED, "bi-x-circle-fill");
        iconCodes.put(status.WARNING, "bi-exclamation-circle-fill");

        iconColors.put(status.READY, "#2980B8");
        iconColors.put(status.QUEUED, "#2980B8");
        iconColors.put(status.RUNNING, "#1D2433");
        iconColors.put(status.COMPLETED, "#057204");
        iconColors.put(status.FAILED, "#E02D3C");
        iconColors.put(status.WARNING, "ORANGE");
    }

    public static FontIcon getStatusIcon(status statusCode, int size) {
        FontIcon icon = new FontIcon(iconCodes.get(statusCode));
        icon.setIconSize(size);
        icon.setIconColor(Paint.valueOf(iconColors.get(statusCode)));
        icon.setEffect(new DropShadow(2, Color.valueOf("LIGHTGREY")));
        return icon;
    }

    public static FontIcon getCircleIcon(status statusCode, int size) {
        FontIcon icon = new FontIcon("bi-circle-fill");
        icon.setIconSize(size);
        icon.setIconColor(Paint.valueOf(iconColors.get(statusCode)));
        return icon;

    }

}
