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
package kutch.biff.marvin.utility;

import java.util.logging.Logger;

import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.logger.MarvinLogger;

/**
 * @author Patrick Kutch
 */
public class TranslationCalculator {

    protected static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    protected static final Configuration CONFIG = Configuration.getConfig();

    private final DoubleProperty _Scale;
    //    private final Pane _ReferencePane;
    private final Pane workingPane;
    // private final Rectangle clip = new Rectangle();
    private final Pos _Position;
    private final Translate translate;

    public TranslationCalculator(Pane objBasePane, Pane objWorkingPane, DoubleProperty objScaleProperty, Pos position) {
        _Scale = objScaleProperty;

        // _ReferencePane = objBasePane;
        workingPane = objWorkingPane;
        _Position = position;
        translate = new Translate();
        Scale scaleTransform = new Scale();
        scaleTransform.xProperty().bind(_Scale);
        scaleTransform.yProperty().bind(_Scale);

        objWorkingPane.getTransforms().addAll(scaleTransform, translate);
        SetupListeners();
    }

    private double CalcTranslationX() {
        double refWidth;
        double paneWidth;
        double tx;
        double scale;

        scale = _Scale.getValue();

        // refWidth = _ReferencePane.getWidth();
        refWidth = CONFIG.getCanvasWidth();
        paneWidth = workingPane.getWidth();

        if (_Position == Pos.CENTER_RIGHT || _Position == Pos.TOP_RIGHT || _Position == Pos.BOTTOM_RIGHT) {
            tx = (refWidth - (paneWidth * scale)) / scale; // right aligned
        } else if (_Position == Pos.CENTER_LEFT || _Position == Pos.TOP_LEFT || _Position == Pos.BOTTOM_LEFT) {
            tx = 0; // left aligned
        } else if (_Position == Pos.CENTER || _Position == Pos.TOP_CENTER || _Position == Pos.BOTTOM_CENTER) {
            tx = ((refWidth - (paneWidth * scale)) / scale) / 2; // centered
        } else {
            tx = 0;
        }
        if (tx < 0) {
            tx = 0;
        }

        return tx;
    }

    private double CalcTranslationY() {
        double refHeight;
        double paneHeight;
        double ty;
        double scale;

        scale = _Scale.getValue();

        // refHeight = _ReferencePane.getHeight();
        refHeight = CONFIG.getCanvasHeight();
        paneHeight = workingPane.getHeight();
        if (paneHeight == 0 || refHeight == 0) {
            return 0;
        }
        if (_Position == Pos.BOTTOM_CENTER || _Position == Pos.BOTTOM_RIGHT || _Position == Pos.BOTTOM_LEFT) {
            ty = (refHeight - (paneHeight * scale)) / scale; // Bottom aligned
        } else if (_Position == Pos.TOP_CENTER || _Position == Pos.TOP_LEFT || _Position == Pos.TOP_RIGHT) {
            ty = 0; // left aligned
        } else if (_Position == Pos.CENTER || _Position == Pos.CENTER_LEFT || _Position == Pos.CENTER_RIGHT) {
            ty = ((refHeight - (paneHeight * scale)) / scale) / 2; // centered
        } else {
            ty = 0;
        }
        /*
         * if (ty < CONFIG.getTopOffset() * -1) { ty = CONFIG.getTopOffset() * -1; }
         */
        return ty;
    }

    private void Calculate() {
        double tX = CalcTranslationX();
        double tY = CalcTranslationY();
        // Don't think I need this anymor
        translate.setX(tX);
        translate.setY(tY);
    }

    private void SetupListeners() {
        workingPane.widthProperty().addListener((ObservableValue observable, Number number, Number oldNumber) -> {
            Calculate();
        });
        workingPane.heightProperty().addListener((ObservableValue observable, Number number, Number oldNumber) -> {
            Calculate();
        });
        /*
         * _ReferencePane.layoutBoundsProperty().addListener(new
         * ChangeListener<Bounds>() // when things are resized {
         *
         * @Override public void changed(ObservableValue<? extends Bounds> observable,
         * Bounds oldBounds, Bounds bounds) { clip.setWidth(bounds.getWidth());
         * clip.setHeight(bounds.getHeight()); _ReferencePane.setClip(clip);
         * Calculate(); } });
         */
        _Scale.addListener((ObservableValue observableValue, Number number, Number number2) -> {
            Calculate();
        });
    }
}
