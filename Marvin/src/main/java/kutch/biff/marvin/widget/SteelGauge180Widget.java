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

import eu.hansolo.enzo.gauge.OneEightyGauge;
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
public class SteelGauge180Widget extends BaseWidget {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private String UnitText;
    private double MinValue;
    private double MaxValue;
    private OneEightyGauge gauge;
    private double initialValue;

    public SteelGauge180Widget() {
        UnitText = "";
        MinValue = 0;
        MaxValue = 0;
        gauge = new OneEightyGauge();
        gauge.setAnimationDuration(400);
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        if (false == SetupGauge()) {
            return false;
        }
        gauge.setValue(initialValue);

        SetupPeekaboo(dataMgr);

        pane.add(gauge, getColumn(), getRow(), getColumnSpan(), getRowSpan());

        dataMgr.AddListener(getMinionID(), getNamespace(), (ObservableValue o, Object oldVal, Object newVal) -> {
            if (IsPaused()) {
                return;
            }

            double newDialValue = 0;
            String strVal = newVal.toString();
            try {
                newDialValue = Double.parseDouble(strVal);
                HandleSteppedRange(newDialValue);
            } catch (NumberFormatException ex) {
                LOGGER.severe("Invalid data for 180  Gauge received: " + strVal);
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

    protected void HandleSteppedRange(double newValue) {
        if (SupportsSteppedRanges()) {
            if (getExceededMaxSteppedRange(newValue)) {
                MaxValue = getNextMaxSteppedRange(newValue);
                UpdateValueRange();
            } else if (getExceededMinSteppedRange(newValue)) {
                MinValue = getNextMinSteppedRange(newValue);
                UpdateValueRange();
            }
        }
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

    private void makeNewGauge() {
        OneEightyGauge oldGauge = gauge;
        gauge = new OneEightyGauge();
        gauge.setVisible(oldGauge.isVisible());

        GridPane pane = getParentPane();
        pane.getChildren().remove(oldGauge);

        if (false == SetupGauge()) {
            LOGGER.severe("Tried to re-create OneEightyGauge for Stepped Range, but something bad happened.");
            gauge = oldGauge;
            return;
        }
        pane.add(gauge, getColumn(), getRow(), getColumnSpan(), getRowSpan());
        ApplyCSS();
    }

    @Override
    public void SetInitialValue(String value) {
        try {
            initialValue = Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            LOGGER.severe("Invalid Default Value data for 180  Gauge: " + value);
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
        ConfigureAlignment();
        ConfigureDimentions();

        gauge.setDecimals(getDecimalPlaces());
        gauge.setAnimated(true);

        SetupTaskAction();

                return ApplyCSS();
    }

    @Override
    public boolean SupportsSteppedRanges() {
        return true;
    }

    @Override
    public void UpdateTitle(String strTitle) {
        gauge.setTitle(getTitle());
    }

    @Override
    public void UpdateValueRange() {
        makeNewGauge();
    }

}
