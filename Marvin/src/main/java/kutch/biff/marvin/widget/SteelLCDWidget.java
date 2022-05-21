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

import eu.hansolo.enzo.lcd.Lcd;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick Kutch
 */
public class SteelLCDWidget extends BaseWidget {
    private static double aspectRatio = 2.75;
    private Lcd lcd;
    private String UnitText;
    private boolean ShowMeasuredMax;
    private boolean ShowMeasuredMin;
    private double MinValue;
    private double MaxValue;
    private boolean keepAspectRatio;
    private boolean textMode;
    private String initialValue = "";

    public SteelLCDWidget() {
        lcd = new Lcd();
        keepAspectRatio = true;
        textMode = false;

        UnitText = "";
        lcd.setAnimationDuration(300);
    }

    @Override
    protected void ConfigureDimentions() {
        if (keepAspectRatio) {
            if (getWidth() > 0) {
                setHeight(getWidth() / aspectRatio);
            } else if (getHeight() > 0) {
                setWidth(getHeight() * aspectRatio);
            }
        }
        super.ConfigureDimentions();
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        if (false == SetupLCD()) {
            return false;
        }
        if (initialValue.length() > 0) {
            SetValue(initialValue);
        }

        ConfigureAlignment();
        ConfigureDimentions();

        pane.add(lcd, getColumn(), getRow(), getColumnSpan(), getRowSpan());
        SetupPeekaboo(dataMgr);

        dataMgr.AddListener(getMinionID(), getNamespace(), (ObservableValue o, Object oldVal, Object newVal) -> {
            if (IsPaused()) {
                return;
            }
            SetValue(newVal.toString());
        });
        SetupTaskAction();

        return ApplyCSS();
    }

    public double getMaxValue() {
        return MaxValue;
    }

    public double getMinValue() {
        return MinValue;
    }

    @Override
    public javafx.scene.Node getStylableObject() {
        return lcd;
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return lcd.getStylesheets();
    }

    public boolean getTextMode() {
        return textMode;
    }

    public String getUnitText() {
        return UnitText;
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

    public boolean isKeepAspectRatio() {
        return keepAspectRatio;
    }

    public boolean isShowMeasuredMax() {
        return ShowMeasuredMax;
    }

    public boolean isShowMeasuredMin() {
        return ShowMeasuredMin;
    }

    @Override
    public void SetInitialValue(String value) {
        initialValue = value;
    }

    public void setKeepAspectRatio(boolean keepAspectRation) {
        this.keepAspectRatio = keepAspectRation;
    }

    public void setMaxValue(double maxValue) {
        this.MaxValue = maxValue;
    }

    public void setMinValue(double minValue) {
        this.MinValue = minValue;
    }

    public void setShowMeasuredMax(boolean showMeasuredMax) {
        this.ShowMeasuredMax = showMeasuredMax;
    }

    public void setShowMeasuredMin(boolean showMeasuredMin) {
        this.ShowMeasuredMin = showMeasuredMin;
    }

    public void setTextMode(boolean newMode) {
        textMode = newMode;
        lcd.setTextMode(newMode);
        if (newMode) {
            lcd.setDecimals(0);
        } else {
            lcd.setDecimals(getDecimalPlaces());
        }
    }

    public void setUnitText(String unitText) {
        this.UnitText = unitText;
    }

    private boolean SetupLCD() {
        if (!textMode) {
            lcd.setMinMeasuredValueVisible(ShowMeasuredMin);
            lcd.setMaxMeasuredValueVisible(ShowMeasuredMax);
            lcd.setMaxValue(getMaxValue());
            lcd.setKeepAspect(keepAspectRatio);
            lcd.setDecimals(getDecimalPlaces());
        }

        if (getTitle().length() > 0) {
            lcd.setTitle(getTitle());
        }
        if (null != getUnitsOverride()) {
            lcd.setUnit(getUnitsOverride());
            LOGGER.config("Overriding Widget Units Text to " + getUnitsOverride());
        } else if (UnitText.length() > 0) {
            lcd.setUnit(UnitText);
        }

        lcd.setCrystalOverlayVisible(true); // 'rgainy, LCD like overlay, very subtle
        return true;
    }

    public void SetValue(String newVal) {
//        if (true || !_TextMode)
        if (!textMode) {
            double newDialValue;
            String strVal = newVal;
            try {
                newDialValue = Double.parseDouble(strVal);
                setTextMode(false);
            } catch (NumberFormatException ex) {
                lcd.setText(strVal);
                return;
            }
            lcd.setValue(newDialValue);
        } else {
            lcd.setText(newVal);
        }
    }

    @Override
    public void UpdateTitle(String strTitle) {
        lcd.setTitle(strTitle);
    }

}
