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

import eu.hansolo.enzo.gauge.Radial;
import eu.hansolo.enzo.gauge.Radial.TickLabelOrientation;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick Kutch
 */
public class SteelGaugeRadialWidget extends BaseWidget {
    protected static String DumpDimensions(String id, Radial objGauge) {
        String prefWidth = "Pref Width: " + objGauge.getPrefWidth() + " ";
        String prefHeight = "Pref Height: " + objGauge.getPrefHeight() + " ";
        String maxWidth = "MAX Width: " + objGauge.getMaxWidth() + " ";
        String maxHeight = "MAX Width: " + objGauge.getMaxWidth() + " ";
        String minWidth = "MIN Width: " + objGauge.getMinWidth() + " ";
        String minHeight = "MIN Height: " + objGauge.getMinHeight() + " ";
        String currWidth = "Curr Width: " + objGauge.getWidth() + " ";
        String currHeight = "Curr Height: " + objGauge.getHeight() + " ";

        String retString = "Dimensions for " + id + " Widget\n";
        retString += "Current";
        retString += "\t" + currWidth + "\t" + currHeight;
        retString += "\nPreferred";
        retString += "\t" + prefWidth + "\t" + prefHeight;
        retString += "\nMaximum";
        retString += "\t" + maxWidth + "\t" + maxHeight;
        retString += "\nMinimum";
        retString += "\t" + minWidth + "\t" + minHeight;

        return retString;

    }

    private String UnitText;
    private double MinValue;
    private double MaxValue;
    @SuppressWarnings("unused")
    private int DialStartAngle;
    @SuppressWarnings("unused")
    private int DialRangeAngle;
    private double MajorTick;
    private double MinorTick;
    private TickLabelOrientation eOrientation;
    @SuppressWarnings("unused")
    private boolean EnhancedRateText;
    private Radial gauge;
    @SuppressWarnings("unused")
    private GridPane parentGridPane;
    private double initialValue = 0;
    private double MajorTickCount = 0;

    private double MinorTickCount = 0;

    public SteelGaugeRadialWidget() {
        UnitText = "";
        MinValue = 0;
        MaxValue = 0;
        DialStartAngle = 330;
        DialRangeAngle = 300;
        MajorTickCount = 0;
        MinorTickCount = 0;

        MajorTick = 0;
        MinorTick = 0;
        eOrientation = TickLabelOrientation.HORIZONTAL;
        EnhancedRateText = true;
        gauge = new Radial();
        gauge.setAnimationDuration(400);
        // _Gauge.setAnimated(false);

    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        parentGridPane = pane;
        if (false == SetupGauge()) {
            return false;
        }
        gauge.setValue(initialValue);
        SetupPeekaboo(dataMgr);

        pane.add(gauge, getColumn(), getRow(), getColumnSpan(), getRowSpan());

        dataMgr.AddListener(getMinionID(), getNamespace(), (ObservableValue<?> o, Object oldVal, Object newVal) -> {
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
                        setMajorTickCount(majorTickVal);
                    } else {
                        LOGGER.severe("Invalid TickCount:Major ->" + node.getAttribute("Major"));
                        return false;
                    }
                }
                if (node.hasAttribute("Minor")) {
                    minorTickVal = node.getDoubleAttribute("Minor", minorTickVal);
                    if (minorTickVal != -1234) {
                        setMinorTickCount(minorTickVal);
                    } else {
                        LOGGER.severe("Invalid TickCount:Minor ->" + node.getAttribute("Minor"));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void makeNewGauge() {
        Radial oldGauge = gauge;
        gauge = new Radial();
        gauge.setVisible(oldGauge.isVisible());

        GridPane pane = getParentPane();
        pane.getChildren().remove(oldGauge);

        if (false == SetupGauge()) {
            LOGGER.severe("Tried to re-create Radial for Stepped Range, but something bad happened.");
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
            LOGGER.severe("Invalid Default Value data for SteelGaugeRadial  Gauge: " + value);
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

    public void setOrientation(TickLabelOrientation eOrientation) {
        this.eOrientation = eOrientation;
    }

    public void setRangeAngle(int dialEndAngle) {
        this.DialRangeAngle = dialEndAngle;
    }

    public void setUnitText(String unitText) {
        this.UnitText = unitText;
    }

    private boolean SetupGauge() {
        gauge.setMinValue(MinValue);
        gauge.setMaxValue(MaxValue);
        // For some reason there is a bug in this gauge, so going to IGNORE this field
        // for now
//        _Gauge.setStartAngle(DialStartAngle);
//        _Gauge.setAngleRange(DialRangeAngle);
        gauge.setTickLabelOrientation(eOrientation);

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
//        if (null != Sections)
//        {
//            _Gauge.setSections(Sections);
//        }

        gauge.setDecimals(getDecimalPlaces());
        ConfigureDimentions();
        SetupTaskAction();
        ConfigureAlignment();

//        _Gauge.setMouseTransparent(true);
        // LOGGER.config(DumpDimensions("SteelGauge take 2", _Gauge));
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
        makeNewGauge();
    }

}
