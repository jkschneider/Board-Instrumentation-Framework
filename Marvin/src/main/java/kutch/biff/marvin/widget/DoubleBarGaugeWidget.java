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

import java.util.logging.Logger;

import eu.hansolo.enzo.gauge.AvGauge;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick Kutch
 */
public class DoubleBarGaugeWidget extends BaseWidget {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private double MinValue;
    private double MaxValue;
    private AvGauge gauge;
    private double initialValue;
    private String outerID;
    private String outerNamespace;

    public DoubleBarGaugeWidget() {
        MinValue = 0;
        MaxValue = 0;
        outerID = null;
        outerNamespace = null;
        gauge = new AvGauge();
    }

    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        if (false == SetupGauge()) {
            return false;
        }
        gauge.setInnerValue(initialValue);
        gauge.setOuterValue(initialValue);

        ConfigureAlignment();
        SetupPeekaboo(dataMgr);
        ConfigureDimentions();

        pane.add(gauge, getColumn(), getRow(), getColumnSpan(), getRowSpan());

        dataMgr.AddListener(getMinionID(), getNamespace(), (ObservableValue<?> o, Object oldVal, Object newVal) -> {
            if (IsPaused()) {
                return;
            }

            double newDialValue = 0;
            String strVal = newVal.toString();
            try {
                newDialValue = Double.parseDouble(strVal);
            } catch (NumberFormatException ex) {
                LOGGER.severe("Invalid data for BarGauge received: " + strVal);
                return;
            }
            gauge.setInnerValue(newDialValue);
        });

        if (null == outerID || null == outerNamespace) {
            LOGGER.severe("No Outter Data source defined for BarGauge");
            return false;
        }
        dataMgr.AddListener(outerID, outerNamespace, (ObservableValue<?> o, Object oldVal, Object newVal) -> {
            if (IsPaused()) {
                return;
            }

            double newDialValue = 0;
            String strVal = newVal.toString();
            try {
                newDialValue = Double.parseDouble(strVal);
            } catch (NumberFormatException ex) {
                LOGGER.severe("Invalid data for BarGauge received: " + strVal);
                return;
            }
            gauge.setOuterValue(newDialValue);
        });
        return true;
    }

    @Override
    public javafx.scene.Node getStylableObject() {
        return gauge;
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return gauge.getStylesheets();
    }

    /**
     * Sets range for widget - not valid for all widgets
     *
     * @param rangeNode
     * @return
     */

    @Override
    public boolean HandleValueRange(FrameworkNode rangeNode) {
        double min = -1234.5678;
        double max = -1234.5678;
        if (rangeNode.hasAttribute("Min")) {
            min = rangeNode.getDoubleAttribute("Min", min);
            if (min == -1234.5678) {
                return false;
            }
            this.MinValue = min;
        }
        if (rangeNode.hasAttribute("Max")) {
            max = rangeNode.getDoubleAttribute("Max", max);
            if (max == -1234.5678) {
                return false;
            }
            this.MaxValue = max;
        }
        return true;
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node) {
        if ("InnerMinionSrc".equalsIgnoreCase(node.getNodeName())) {
            if (node.hasAttribute("ID")) {
                setMinionID(node.getAttribute("ID"));
            } else {
                LOGGER.severe("BarGauge defined with invalid InnerMinionSrc, no ID");
                return false;
            }
            if (node.hasAttribute("Namespace")) {
                setNamespace(node.getAttribute("Namespace"));
            } else {
                LOGGER.severe("Conditional defined with invalid InnerMinionSrc, no Namespace");
                return false;
            }
            return true;
        }
        if ("OuterMinionSrc".equalsIgnoreCase(node.getNodeName())) {
            if (node.hasAttribute("ID")) {
                outerID = node.getAttribute("ID");
            } else {
                LOGGER.severe("BarGauge defined with invalid OuterMinionSrc, no ID");
                return false;
            }
            if (node.hasAttribute("Namespace")) {
                outerNamespace = node.getAttribute("Namespace");
            } else {
                LOGGER.severe("Conditional defined with invalid OuterMinionSrc, no Namespace");
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void SetInitialValue(String value) {
        try {
            initialValue = Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            LOGGER.severe("Invalid Default Value data for BarGauge: " + value);
        }
    }

    public void setMaxValue(double maxValue) {
        this.MaxValue = maxValue;
    }

    public void setMinValue(double minValue) {
        initialValue = minValue;
        this.MinValue = minValue;
    }

    private boolean SetupGauge() {
        gauge.setMinValue(MinValue);
        gauge.setMaxValue(MaxValue);

        if (getTitle().length() > 0) {
            gauge.setTitle(getTitle());
        }

        gauge.setDecimals(getDecimalPlaces());

        SetupTaskAction();

                return ApplyCSS();
    }

    @Override
    public void UpdateTitle(String strTitle) {
        gauge.setTitle(getTitle());
    }

}
