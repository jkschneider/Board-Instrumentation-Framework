/*
##############################################################################
#  Copyright (c) 2018 Intel Corporation
# 
# Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
##############################################################################
#    File Abstract: 
#
##############################################################################
 */
package kutch.biff.marvin.widget;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/*
<Widget File="Text\File.xml" row="0" column="0">
    <MinionSrc Namespace="foo" ID="Bar.*"/>
    <File>MyOut.txt</File>
    <Mode>Overwrite</Mode>
    <Format>KeyPair-ID-Value</Format>
</Widget>

 */

/**
 * @author Patrick
 */
public class FileWriterWidget extends BaseWidget {

    public enum WriteFormat {
        KeyPairIDValue, KeyPairNamespaceIDValue
    }

    ;

    public enum WriteMode {
        Append, Overwrite
    }

    ;

    private static HashMap<String, HashMap<String, String>> fileToDataMap = new HashMap<>();
    private HashMap<String, String> dataPointMap;
    private String outFile;
    private WriteMode writeMode;
    private WriteFormat writeFormat;
    private String prefixStr;

    public FileWriterWidget() {
        dataPointMap = null; // new HashMap<>(); // for quick lookup as new data comes in
        outFile = null;
        writeMode = WriteMode.Overwrite;
        writeFormat = WriteFormat.KeyPairIDValue;
        prefixStr = "";
    }

    private void Append() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile, true));
            writeMap(writer);
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(FileWriterWidget.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);

        if (null == outFile) {
            LOGGER.severe("Invalid FileWriterWidget, no <File> specified.");
            return false;
        }

        dataMgr.AddWildcardListener(getMinionID(), getNamespace(), (ObservableValue<?> o, Object oldVal, Object newVal) -> {
            if (IsPaused()) {
                return;
            }

            String strVal = newVal.toString();
            String[] parts = strVal.split(":");
            if (parts.length > 1) // check to see if we have already created the widget
                {
                    String id = parts[0];
                    String Value = parts[1];
                    String key = prefixStr;
                    if (writeFormat == WriteFormat.KeyPairNamespaceIDValue) {
                        key += getNamespace() + ".";
                    }
                    key += id;

                    dataPointMap.put(key, Value);
                    if (writeMode == WriteMode.Append) {
                        Append();
                    } else if (writeMode == WriteMode.Overwrite) {
                        Overwrite();
                    }
                }
        });
        SetupPeekaboo(dataMgr);

        return true;
    }

    @Override
    public Node getStylableObject() {
        return null;
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return null;
    }

    /**
     * @param node
     * @return
     */
    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node) {
        if ("File".equalsIgnoreCase(node.getNodeName())) {
            outFile = node.getTextContent();
            if (!fileToDataMap.containsKey(outFile)) {
                dataPointMap = new HashMap<>();
                fileToDataMap.put(outFile, dataPointMap);
            } else {
                dataPointMap = fileToDataMap.get(outFile);
            }

            return true;
        } else if ("Format".equalsIgnoreCase(node.getNodeName())) {
            String strVal = node.getTextContent();
            if ("KeyPair-ID-Value".equalsIgnoreCase(strVal)) {
                writeFormat = WriteFormat.KeyPairIDValue;
            } else if ("KeyPairIDValue".equalsIgnoreCase(strVal)) {
                writeFormat = WriteFormat.KeyPairIDValue;
            } else if ("KeyPair-Namespace-ID-Value".equalsIgnoreCase(strVal)) {
                writeFormat = WriteFormat.KeyPairNamespaceIDValue;
            } else if ("KeyPairNamespaceIDValue".equalsIgnoreCase(strVal)) {
                writeFormat = WriteFormat.KeyPairNamespaceIDValue;
            } else {
                LOGGER.severe("Invalid <Format> in FileWriterWidget : " + strVal);
                return false;
            }
            if (node.hasAttribute("Prefix")) {
                prefixStr = node.getAttribute("Prefix");
            }
            return true;
        } else if ("Mode".equalsIgnoreCase(node.getNodeName())) {
            String strVal = node.getTextContent();
            if ("Append".equalsIgnoreCase(strVal)) {
                writeMode = WriteMode.Append;
            } else if ("Overwrite".equalsIgnoreCase(strVal)) {
                writeMode = WriteMode.Overwrite;
            } else {
                LOGGER.severe("Invalid <Mode> in FileWriterWidget : " + strVal);
                return false;
            }
            return true;
        }
        return false;
    }

    private void Overwrite() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
            writer.write("");
            writeMap(writer);
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(FileWriterWidget.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void UpdateTitle(String newTitle) {

    }

    private void writeMap(BufferedWriter writer) throws IOException {
        for (String key : dataPointMap.keySet()) {
            // writer.append(_prefixStr);
            // if (_writeFormat == WriteFormat.KeyPairNamespaceIDValue)
            // {
//                writer.append(getNamespace());
//            }
            writer.append(key + "=");
            writer.append(dataPointMap.get(key));
            writer.newLine();
        }
    }

}
