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
import eu.hansolo.enzo.gauge.Gauge;
import eu.hansolo.enzo.gauge.Gauge.TickLabelOrientation;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick Kutch
 */
public class SteelGaugeWidget extends BaseWidget {

    private String UnitText;
    private double MinValue;
    private double MaxValue;
    private int DialStartAngle;
    private int DialRangeAngle;
    private double MajorTick;
    private double MinorTick;
    private double MajorTickCount;
    private double MinorTickCount;
    private TickLabelOrientation eOrientation;
    private boolean EnhancedRateText;
    private boolean Shadowed;
    private boolean ShowMeasuredMax;
    private boolean ShowMeasuredMin;
    private List<Section> Sections;
    private List<Pair<Double, Double>> sectionPercentages;
    private Gauge gauge; // remember that you need to disable mouse action in gaugeskin
    // knob.setOnMousePressed(event -> ~ line 324
    // private GridPane _ParentGridPane;
    private double initialValue;

    public SteelGaugeWidget() {
        UnitText = "";
        MinValue = 0;
        MaxValue = 0;
        DialStartAngle = 270;
        DialRangeAngle = 270;
        MajorTick = 0;
        MinorTick = 0;
        MajorTickCount = 0;
        MinorTickCount = 0;
        eOrientation = TickLabelOrientation.HORIZONTAL;
        EnhancedRateText = true;
        Shadowed = true;
        ShowMeasuredMax = true;
        ShowMeasuredMin = true;
        Sections = null;
        gauge = new Gauge();
        gauge.setAnimationDuration(400);
        sectionPercentages = null;
        // _Gauge.setAnimated(false);

    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        // _ParentGridPane = pane;
        if (false == SetupGauge()) {
            return false;
        }
        gauge.setValue(initialValue);
        initialSteppedRangeSetup(MinValue, MaxValue);

        pane.add(gauge, getColumn(), getRow(), getColumnSpan(), getRowSpan());

        SetupPeekaboo(DataManager.getDataManager());
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
                LOGGER.severe("Invalid data for Gauge received: " + strVal);
                return;
            }

            gauge.setValue(newDialValue);
        });

        return ApplyCSS();
    }

    public double getMajorTickCount() {
        return MajorTickCount;
    }

    public double getMinorTickCount() {
        return MinorTickCount;
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
        for (FrameworkNode node : rangeNode.getChildNodes()) {
            if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#comment".equalsIgnoreCase(node.getNodeName())) {
                continue;
            }
            if ("TickCount".equalsIgnoreCase(node.getNodeName())) {
                double majorTickVal = -1234;
                double minorTickVal = -1234;

                if (node.hasAttribute("Major")) {
                    majorTickVal = node.getDoubleAttribute("Major", majorTickVal);
                    if (majorTickVal != -1234) {
                        MajorTickCount = majorTickVal;
                    } else {
                        LOGGER.severe("Invalid TickCount:Major ->" + node.getAttribute("Major"));
                        return false;
                    }
                }
                if (node.hasAttribute("Minor")) {
                    minorTickVal = node.getDoubleAttribute("Minor", minorTickVal);
                    if (minorTickVal != -1234) {
                        MajorTickCount = minorTickVal;
                    } else {
                        LOGGER.severe("Invalid TickCount:Minor ->" + node.getAttribute("Minor"));
                        return false;
                    }
                }
            } else {
                return false;
            }

        }
        return true;
    }

    private void makeNewGauge() {
        Gauge oldGauge = gauge;
        gauge = new Gauge();
        gauge.setVisible(oldGauge.isVisible());

        gauge.setAnimationDuration(400);
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

    public void setDialRangeAngle(int dialRangeAngle) {
        this.DialRangeAngle = dialRangeAngle;
    }

    public void setDialStartAngle(int dialStartAngle) {
        this.DialStartAngle = dialStartAngle;
    }

    public void setEnhancedRateText(boolean enhancedRateText) {
        this.EnhancedRateText = enhancedRateText;
    }

    @Override
    public void SetInitialValue(String value) {
        try {
            initialValue = Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            LOGGER.severe("Invalid Default Value data for SteelGauge: " + value);
        }
    }

    public void setMajorTick(double majorTick) {
        this.MajorTick = majorTick;
    }

    public void setMajorTickCount(double majorTickCount) {
        this.MajorTickCount = majorTickCount;
    }

    public void setMaxValue(double maxValue) {
        this.MaxValue = maxValue;
    }

    public void setMinorTick(double minorTick) {
        this.MinorTick = minorTick;
    }

    public void setMinorTickCount(double minorTickCount) {
        this.MinorTickCount = minorTickCount;
    }

    public void setMinValue(double minValue) {
        this.MinValue = minValue;
    }

    public void setOrientation(Gauge.TickLabelOrientation eOrientation) {
        this.eOrientation = eOrientation;
    }

    public void setPercentageSections(List<Pair<Double, Double>> Sections) {
        this.sectionPercentages = Sections;
    }

    public void setRangeAngle(int dialEndAngle) {
        this.DialRangeAngle = dialEndAngle;
    }

    public void setSections(List<Section> Sections) {
        this.Sections = Sections;
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

    public void setShadowed(boolean shadowed) {
        this.Shadowed = shadowed;
    }

    public void setShowMeasuredMax(boolean showMeasuredMax) {
        this.ShowMeasuredMax = showMeasuredMax;
    }

    public void setShowMeasuredMin(boolean showMeasuredMin) {
        this.ShowMeasuredMin = showMeasuredMin;
    }

    public void setUnitText(String unitText) {
        this.UnitText = unitText;
    }

    private boolean SetupGauge() {
        gauge.setMinValue(MinValue);
        gauge.setMaxValue(MaxValue);
        gauge.setStartAngle(DialStartAngle);
        gauge.setAngleRange(DialRangeAngle);
        gauge.setTickLabelOrientation(eOrientation);
        gauge.setDropShadowEnabled(Shadowed);
        gauge.setMinMeasuredValueVisible(ShowMeasuredMin);
        gauge.setMaxMeasuredValueVisible(ShowMeasuredMax);
        gauge.setPlainValue(!EnhancedRateText);

        if (getTitle().length() > 0) {
            gauge.setTitle(getTitle());
        }
        SetupTicksFromTickCount();
        if (MajorTick > 0) {
            gauge.setMajorTickSpace(MajorTick);
        }
        if (MinorTick > 0) {
            gauge.setMinorTickSpace(MinorTick);
        }
        if (null != getUnitsOverride()) {
            gauge.setUnit(getUnitsOverride());
            LOGGER.config("Overriding Widget Units Text to " + getUnitsOverride());
        } else if (UnitText.length() > 0) {
            gauge.setUnit(UnitText);
        }
        setSectionsFromPercentages();
        if (null != Sections) {
            gauge.setSections(Sections);
        }

        gauge.setDecimals(getDecimalPlaces());

        ConfigureDimentions();

        ConfigureAlignment();
        EventHandler<MouseEvent> eh = SetupTaskAction(); // special because Gauge can be interactive
        if (null == eh) {
            eh = (MouseEvent event) -> {
            };
        }
        gauge.customKnobClickHandlerProperty().set(eh);

        return true;
    }

    private void SetupTicksFromTickCount() {
        double range = abs(this.MaxValue - this.MinValue);
        if (range == 0) {
            return;
        }
        if (MajorTickCount > 0) {
            MajorTick = range / MajorTickCount;
            if (MinorTickCount > 0) {
                MinorTick = MajorTick / MinorTickCount;
            }
        }
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
        gauge.setMinValue(MinValue);
        gauge.setMaxValue(MaxValue);
        makeNewGauge();
    }

}
