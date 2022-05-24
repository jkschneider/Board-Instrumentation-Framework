/*
 * ##############################################################################
 * #  Copyright (c) 2019 Intel Corporation
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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.DataPointGenerator;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * <Prompt ID="Volume Selection" Type="ListBox"> <Title>Volume Level</Title>
 * <Message>Select the Volume for this media player</Message> <List>
 * <Item Text="Mute">Volume:0</Item> <Item Text="100%">Volume:100</Item>
 * <Item Text="25%">Volume:25</Item>215 Revision 19.07 Board Instrumentation
 * Framework <Item Text="50%">Volume:50</Item> <Item Text="75%">Volume:75</Item>
 * </List> </Prompt>
 *
 * @author Patrick Kutch
 */
public class Prompt_ListBox extends BasePrompt {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final List<String> displayTextList;
    private final List<String> paramList;
    private final List<List<DataPointGenerator>> _DataPoints;
    private int prevSelection;

    public Prompt_ListBox(String id) {
        super(id);
        displayTextList = new ArrayList<>();
        paramList = new ArrayList<>();
        _DataPoints = new ArrayList<>();
    }

    public void AddListItem(String strDisplayText, String strParam, List<DataPointGenerator> dps) {
        displayTextList.add(strDisplayText);
        paramList.add(strParam);
        _DataPoints.add(dps);
    }

    /**
     * @param baseNode
     * @return
     */
    @Override
    public boolean HandlePromptSpecificConfig(FrameworkNode baseNode) {
        if ("List".equalsIgnoreCase(baseNode.getNodeName())) {
            int itemIndex = 0;
            for (FrameworkNode node : baseNode.getChildNodes(true)) {
                if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#Comment".equalsIgnoreCase(node.getNodeName())) {
                    continue;
                }
                if ("Item".equalsIgnoreCase(node.getNodeName())) {
                    @SuppressWarnings("unused")
                    String strDisplayText;
                    if (node.hasAttribute("Text")) {
                        strDisplayText = node.getAttribute("Text");
                    } else {
                        strDisplayText = node.getTextContent();
                    }
                    /*
                     * Beginning of work, but not yet done
                     */

                    List<DataPointGenerator> dataPoints = null;
                    if (node.hasAttribute("CreateDataPoint")) {
                        dataPoints = ConfigurationReader.ReadDataPointsForTask(itemIndex++, node.getAttribute("CreateDataPoint"),
                                node.getAttribute("Text"), node.getAttribute("Task"));
                    }
                    AddListItem(strDisplayText, node.getTextContent(), dataPoints);
                } else {
                    LOGGER.severe(
                            "Invalid list item in prompt ID: " + this.toString() + " [" + node.getNodeName() + "]");
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    @Override
    protected Pane SetupDialog(Stage dialog) {
        if (paramList.isEmpty()) {
            LOGGER.severe("Listbox Prompt [" + toString() + "] had no <List> items.");
            //return null;
        }
        GridPane root = new GridPane();

        root.setHgap(5.0);
        root.setVgap(5.0);
        root.setPadding(new Insets(5, 5, 5, 5));
        Button btn = new Button();
        btn.setText("OK");
        Label lblMessage = new Label(getMessage());

        lblMessage.setWrapText(true);
        ListView<String> listBox = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList(displayTextList);
        listBox.setItems(items);
        listBox.getSelectionModel().select(prevSelection);

        Text t = new Text(getMessage());
        Double maxWidth = t.getLayoutBounds().getWidth();
        for (String strCheck : items) {
            t = new Text(strCheck);
            if (t.getLayoutBounds().getWidth() > maxWidth) {
                maxWidth = t.getLayoutBounds().getWidth();
            }
        }

        GridPane.setColumnSpan(lblMessage, 2);

        root.add(lblMessage, 0, 0);

        int len = items.size();
        if (len > 8) // only do max of 8 items in height
        {
            len = 8;
        }
        // len++;
        double height = 24 * len; // hack! (but recommended from javafx community

        if (getWidth() > 0) {
            maxWidth = getWidth();
        }
        if (getHeight() > 0) {
            height = getHeight();
        }

        listBox.setPrefHeight(height + 10); // +10 pushes box down enough to not hae scroll
        listBox.setPrefWidth(maxWidth + 30); // add some padding at end

        root.add(listBox, 0, 2);
        GridPane.setHalignment(btn, HPos.CENTER);
        root.add(btn, 0, 3);

        // place on correct screen and center
        int xPos = (int) (Configuration.getConfig().getPrimaryScreen().getVisualBounds().getMinX());
        int yPos = (int) (Configuration.getConfig().getPrimaryScreen().getVisualBounds().getMinY());
        dialog.setX(xPos);
        dialog.setY(yPos);

        dialog.centerOnScreen();

        btn.setOnAction((ActionEvent event) ->
        {
            int selection = listBox.getSelectionModel().getSelectedIndex();
            List<DataPointGenerator> dataPoints = _DataPoints.get(selection);
            if (null != dataPoints) {
                for (DataPointGenerator dp : dataPoints) {
                    dp.generate();
                }
            }
            SetPromptedValue(paramList.get(selection));
            prevSelection = selection;
            dialog.close();
        });

        return root;
    }

}
