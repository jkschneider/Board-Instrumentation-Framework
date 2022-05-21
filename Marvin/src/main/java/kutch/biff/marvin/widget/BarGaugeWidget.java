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

import eu.hansolo.enzo.gauge.FlatGauge;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick Kutch
 */
public class BarGaugeWidget extends BaseWidget {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private double MinValue;
    private double MaxValue;
    private FlatGauge gauge;
    private double initialValue;
    private String UnitText;

    public BarGaugeWidget() {
        UnitText = "";
        MinValue = 0;
        MaxValue = 0;
        gauge = new FlatGauge();
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        if (false == SetupGauge()) {
            return false;
        }
        gauge.setValue(initialValue);

        ConfigureAlignment();
        SetupPeekaboo(dataMgr);
        ConfigureDimentions();

        pane.add(gauge, getColumn(), getRow(), getColumnSpan(), getRowSpan());

        dataMgr.AddListener(getMinionID(), getNamespace(), (ObservableValue o, Object oldVal, Object newVal) -> {
            if (IsPaused()) {
                return;
            }

            double newDialValue = 0;
            String strVal = newVal.toString();
            try {
                newDialValue = Double.parseDouble(strVal);
            } catch (NumberFormatException ex) {
                LOGGER.info("Ignoring invalid data for BarGauge received: " + strVal);
                return;
            }
            gauge.setValue(newDialValue);
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

    public void setUnitText(String unitText) {
        this.UnitText = unitText;
    }

    private boolean SetupGauge() {
        gauge.setMinValue(MinValue);
        gauge.setMaxValue(MaxValue);

        if (getTitle().length() > 0) {
            gauge.setTitle(getTitle());
        }
        if (null != getUnitsOverride()) {
            gauge.setUnit(getUnitsOverride());
            LOGGER.config("Overriding Widget Units Text to " + getUnitsOverride());
        } else if (UnitText.length() > 0) {
            gauge.setUnit(UnitText);
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
