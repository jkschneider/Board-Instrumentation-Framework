/*
 * ##############################################################################
 * #  Copyright (c) 2016 Intel Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * #  you may not use this file except in compliance with the License.
 * #  You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * #  Unless required by applicable law or agreed to in writing, software
 * #  distributed under the License is distributed on an "AS IS" BASIS,
 * #  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * #  See the License for the specific language governing permissions and
 * #  limitations under the License.
 * ##############################################################################
 * #    File Abstract:
 * #
 * #
 * ##############################################################################
 */
package kutch.biff.marvin.utility;

import java.util.logging.Logger;

import javafx.geometry.Pos;
import kutch.biff.marvin.logger.MarvinLogger;

/**
 * @author Patrick Kutch
 */
public class PanelSideInfo {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private String buttonText;
    private String cSSFile;
    private String styleID;
    private Pos position;
    private boolean buttonOnTop;

    public PanelSideInfo(String loc, String text, String id, String file) {
        buttonText = text;
        styleID = id;
        cSSFile = file;
        buttonOnTop = true;
        position = setButtonAlignment(loc);
    }

    public Pos GetButtonAlignment() {
        return this.position;
    }

    public String getButtonText() {
        if (null == buttonText) {
            return "";
        }
        return buttonText;
    }

    public String getCSSFile() {
        if (null == cSSFile) {
            return "";
        }
        return cSSFile;
    }

    public String getStyleID() {
        return styleID;
    }

    public boolean IsButtonOnTop() {
        return buttonOnTop;
    }

    private Pos setButtonAlignment(String alignString) {
        if (0 == alignString.compareToIgnoreCase("Center")) {
            return Pos.CENTER;
        } else if (0 == alignString.compareToIgnoreCase("N")) {
            return Pos.TOP_CENTER;
        } else if (0 == alignString.compareToIgnoreCase("NE")) {
            return Pos.TOP_RIGHT;
        } else if (0 == alignString.compareToIgnoreCase("E")) {
            return Pos.CENTER_RIGHT;
        } else if (0 == alignString.compareToIgnoreCase("SE")) {
            buttonOnTop = false;
            return Pos.BOTTOM_RIGHT;
        } else if (0 == alignString.compareToIgnoreCase("S")) {
            buttonOnTop = false;
            return Pos.BOTTOM_CENTER;
        } else if (0 == alignString.compareToIgnoreCase("SW")) {
            buttonOnTop = false;
            return Pos.BOTTOM_LEFT;
        } else if (0 == alignString.compareToIgnoreCase("W")) {
            return Pos.CENTER_LEFT;
        } else if (0 == alignString.compareToIgnoreCase("NW")) {
            return Pos.TOP_LEFT;
        } else {
            LOGGER.severe("Invalid FlipPanel Button in config file: " + alignString + ". Ignoring.");
            return Pos.CENTER_LEFT;
        }
    }
}
