/*
 * ##############################################################################
 * #  Copyright (c) 2017 Intel Corporation
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
package kutch.biff.marvin.task;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Patrick
 */
public class DesktopTask extends BaseTask {
    private static String[] validActions = {"BROWSE", "MAIL", "OPEN", "EDIT", "PRINT"};
    private static boolean desktopSupportChecked;
    private String action;
    private String document;

    public DesktopTask() {
        action = null;
        document = null;
    }

    boolean isValid() {
        return document != null;
    }

    @Override
    public void PerformTask() {
        Desktop desktop = Desktop.getDesktop();

        if ("OPEN".equalsIgnoreCase(action)) {
            File file = new File(document);
            try {
                desktop.open(file);
            } catch (IOException ex) {
                Logger.getLogger(DesktopTask.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
        }
        if ("Browse".equalsIgnoreCase(action)) {
            URI uri = null;
            try {
                uri = new URI(document);
                desktop.browse(uri);
            } catch (Exception ex) {
                Logger.getLogger(DesktopTask.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
        }

        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
        // Tools | Templates.
    }

    public boolean SetAction(String strAction) {
        if (null != action) {
            LOGGER.severe("Action already defined for Desktop Task");
            return false;
        }
        if (Arrays.asList(validActions).contains(strAction.toUpperCase())) { // should also do a Desktop.isSupported(action)
            action = strAction;
            return true;
        }

        return false;
    }

    public boolean SetDocument(String strDocument) {
        if (!Desktop.isDesktopSupported()) {
            if (!DesktopTask.DesktopSupportChecked) {
                DesktopTask.DesktopSupportChecked = true;
                LOGGER.severe("This system does not support the DekstopTask capability.");
                return false;
            }
        }

        if (null != document) {
            LOGGER.severe("Document already defined for Desktop Task");
            return false;
        }
        document = strDocument;

        return true;
    }

}
