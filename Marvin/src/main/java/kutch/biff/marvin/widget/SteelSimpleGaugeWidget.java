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

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.List;

import eu.hansolo.enzo.common.Section;
import eu.hansolo.enzo.gauge.SimpleGauge;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick Kutch
 */
public class SteelSimpleGaugeWidget extends BaseWidget {
    private String UnitText;
    private double MinValue;
    private double MaxValue;
    private List<Section> Sections;
    private List<Pair<Double, Double>> sectionPercentages;

    private SimpleGauge gauge;
    private double initialValue = 0;

    public SteelSimpleGaugeWidget() {
        UnitText = "";
        MinValue = 0;
        MaxValue = 0;
        Sections = null;
        gauge = new SimpleGauge();
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
            } catch (Exception ex) {
                LOGGER.severe("Invalid data for Simple Gauge received: " + strVal);
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
        SimpleGauge oldGauge = gauge;
        gauge = new SimpleGauge();
        gauge.setVisible(oldGauge.isVisible());

        GridPane pane = getParentPane();
        pane.getChildren().remove(oldGauge);

        if (false == SetupGauge()) {
            LOGGER.severe("Tried to re-create SteelGaugeWidget for Stepped Range, but something bad happened.");
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
        this.MinValue = minValue;
    }

    public void setPercentageSections(List<Pair<Double, Double>> Sections) {
        this.sectionPercentages = Sections;
    }

    public void setSections(List<Section> objSections) {
        this.Sections = objSections;
    }

    private void setSectionsFromPercentages() {
        if (null == sectionPercentages) {
            return;
        }
        List<Section> sections = new ArrayList<>();
        double range = abs(MaxValue - MinValue);
        for (Pair<Double, Double> sect : sectionPercentages) {
            double start;
            double end;
            start = MinValue + sect.getKey() / 100 * range;
            end = MinValue + sect.getValue() / 100 * range;
            sections.add(new Section(start, end));
        }
        setSections(sections);
    }

    public void setUnitText(String unitText) {
        this.UnitText = unitText;
    }

    private boolean SetupGauge() {
        gauge.setMinValue(MinValue);
        gauge.setMaxValue(MaxValue);
        if (null != getUnitsOverride()) {
            gauge.setUnit(getUnitsOverride());
            LOGGER.config("Overriding Widget Units Text to " + getUnitsOverride());
        } else if (UnitText.length() > 0) {
            gauge.setUnit(UnitText);
        }
        gauge.setAnimationDuration(800); // for some reason default is 3000, and that is too long, causes issues.

        if (getTitle().length() > 0) {
            gauge.setTitle(getTitle());
        }
        if (getHeight() > 0) {
            gauge.setPrefHeight(getHeight());
            gauge.setMaxHeight(getHeight());
        }
        if (getWidth() > 0) {
            gauge.setPrefHeight(getWidth());
            gauge.setMaxWidth(getWidth());
        }
        setSectionsFromPercentages();
        if (null != Sections) {
            gauge.setSections(Sections);
        }
        gauge.setDecimals(getDecimalPlaces());
        ConfigureDimentions();
        ConfigureAlignment();
        SetupTaskAction();

                return ApplyCSS();
    }

    @Override
    public boolean SupportsSteppedRanges() {
        return true;
    }

    @Override
    public void UpdateTitle(String strTitle) {
        gauge.setTitle(strTitle);
    }

    @Override
    public void UpdateValueRange() {
        makeNewGauge();
    }
}
