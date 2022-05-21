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
package kutch.biff.marvin.widget;

import java.text.NumberFormat;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick Kutch
 */
public class TextWidget extends BaseWidget {
    private String textString;
    private Label textControl;
    protected Group _Group;
    private boolean _ScaleToFitBounderies;
    private boolean numericFormat;
    private boolean monetaryFormat;
    private boolean percentFormat;
    private String suffix = "";
    boolean DecimalsSet;

    public TextWidget() {
        textString = null;
        textControl = new Label();
        textControl.setAlignment(Pos.CENTER);
        _ScaleToFitBounderies = false;
        setDefaultIsSquare(false);
    }

    @Override
    public void ConfigureAlignment() {
        super.ConfigureAlignment();
        textControl.setAlignment(getPosition());
        GridPane.setValignment(_Group, getVerticalPosition());
        GridPane.setHalignment(_Group, getHorizontalPosition());
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        if (null != textString) {
            textControl.setText(textString);
        }

        ConfigureDimentions();
        textControl.setScaleShape(getScaleToFitBounderies());

        // By adding it to a group, the underlying java framework will properly
        // clip and resize bounderies if rotated
        _Group = new Group(textControl);
        ConfigureAlignment();
        SetupPeekaboo(dataMgr);

        pane.add(_Group, getColumn(), getRow(), getColumnSpan(), getRowSpan());

        dataMgr.AddListener(getMinionID(), getNamespace(), (ObservableValue o, Object oldVal, Object newVal) -> {
            if (IsPaused()) {
                return;
            }

            textString = newVal.toString();
            if (DecimalsSet) {
                try {
                    String fmtString = "%." + Integer.toString(getDecimalPlaces()) + "f";
                    float fVal = Float.parseFloat(textString);
                    textString = String.format(fmtString, fVal);
                } catch (Exception ex) {

                }
            }
            if (numericFormat) {
                try {
                    if (textString.contains(".")) // good bet it's a float
                        {
                            textString = NumberFormat.getNumberInstance().format(Float.parseFloat(textString));
                        } else {
                        textString = NumberFormat.getNumberInstance().format(Long.parseLong(textString));
                    }
                } catch (Exception ex) {
                    // System.out.println(Ex.toString());
                }
            } else if (monetaryFormat) {
                if (textString.contains(".")) // good bet it's a float
                    {
                        textString = NumberFormat.getCurrencyInstance().format(Float.parseFloat(textString));
                    } else {
                    textString = NumberFormat.getCurrencyInstance().format(Long.parseLong(textString));

                }
            } else if (percentFormat) {
                if (textString.contains(".")) // good bet it's a float
                    {
                        textString = NumberFormat.getPercentInstance().format(Float.parseFloat(textString));
                    } else {
                    textString = NumberFormat.getPercentInstance().format(Long.parseLong(textString));

                }
            }
            textString += suffix;

            if (textString.length() < 2) // seems a single character won't display - bug in Java
                {
                    textString += " ";
                }
            textControl.setText(textString);

        });

        SetupTaskAction();
        return ApplyCSS();
    }

    @Override
    public javafx.scene.Node getRemovableNode() {
        return _Group;
    }

    public boolean getScaleToFitBounderies() {
        return _ScaleToFitBounderies;
    }

    @Override
    public javafx.scene.Node getStylableObject() {
        return textControl;
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return textControl.getStylesheets();
    }

    @Override
    public void HandleCustomStyleOverride(FrameworkNode styleNode) {
        if (styleNode.hasAttribute("ScaleToShape")) {
            String str = styleNode.getAttribute("ScaleToShape");
            if (0 == str.compareToIgnoreCase("True")) {
                setScaleToFitBounderies(true);
            } else if (0 == str.compareToIgnoreCase("False")) {
                setScaleToFitBounderies(false);
            } else {
                LOGGER.severe(
                        "Invalid StyleOVerride Elment ScaleToShape for Text .  ScaleToShape should be True or False, not:"
                                + str);
            }
        }
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node) {
        if ("Format".equalsIgnoreCase(node.getNodeName())) {
            if (node.hasAttribute("Type")) {
                String type = node.getAttribute("Type");
                if ("Number".equalsIgnoreCase(type)) {
                    numericFormat = true;
                } else if ("Money".equalsIgnoreCase(type)) {
                    monetaryFormat = true;
                } else if ("Percent".equalsIgnoreCase(type)) {
                    percentFormat = true;
                } else {
                    LOGGER.warning("Unknown Text Widget Format type: " + type);
                }
            } else {
                LOGGER.severe("Text widget Format option has no Type");
                return true;
            }
            if (node.hasAttribute("Suffix")) {
                suffix = node.getAttribute("Suffix");
            }
            return true;
        }
        return false;
    }

    @Override
    public void setDecimalPlaces(int decimalPlaces) {
        super.setDecimalPlaces(decimalPlaces);
        this.DecimalsSet = true;
    }

    @Override
    public void SetInitialValue(String value) {
        textString = value;
    }

    public void setScaleToFitBounderies(boolean scaleToFitBounderies) {
        this._ScaleToFitBounderies = scaleToFitBounderies;
    }

    @Override
    public void UpdateTitle(String strTitle) {
        LOGGER.warning("Tried to update Title of a TextWidget to " + strTitle);
    }

}
