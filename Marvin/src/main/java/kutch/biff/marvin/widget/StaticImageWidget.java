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

import java.io.File;

import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick Kutch
 */
public class StaticImageWidget extends BaseWidget {
    protected ImageView _ImageView;
    private Image image;
    private String srcFile;
    private boolean _PreserveRatio;
    private boolean _ScaleToFit;
    // private boolean _ClickThroughTransparentRegion=false;
//    private Pane _Pane;

    public StaticImageWidget() {
//        _Pane = new Pane();
        image = null;
        _ImageView = null;
        srcFile = null;
        _PreserveRatio = true;
        _ScaleToFit = false;
    }

    @Override
    protected void ConfigureDimentions() {
        if (getHeight() > 0) {
            _ImageView.setFitHeight(getHeight());
        }

        if (getWidth() > 0) {
            _ImageView.setFitWidth(getWidth());

        }
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        if (setupImage()) {
            ConfigureAlignment();
            SetupPeekaboo(dataMgr);
            SetupTaskAction();
            ConfigureDimentions();

            pane.add(_ImageView, getColumn(), getRow(), getColumnSpan(), getRowSpan());

            return ApplyCSS();
        }
        return false;
    }

    public boolean getPreserveRatio() {
        return _PreserveRatio;
    }

    public boolean getScaleToFit() {
        return _ScaleToFit;
    }

    public String getSrcFile() {
        return srcFile;
    }

    @Override
    public javafx.scene.Node getStylableObject() {
//        return _Pane;
        return _ImageView;
    }

    @Override
    public ObservableList<String> getStylesheets() {
        // return _Pane.getStylesheets();
        return _ImageView.getStyleClass();
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node) {
        if ("Source".equalsIgnoreCase(node.getNodeName())) {
            setSrcFile(node.getTextContent());
            return true;
        }
        return false;
    }

    public void setPreserveRatio(boolean preserveRatio) {
        this._PreserveRatio = preserveRatio;
    }

    public void setScaleToFit(boolean scaleToFit) {
        this._ScaleToFit = scaleToFit;
    }

    public void setSrcFile(String strSrcFile) {
        srcFile = strSrcFile;
    }

    /*
     * public boolean GetClickThroughTransparentRegion() { return
     * _ClickThroughTransparentRegion; }
     *
     * public void SetClickThroughTransparentRegion(boolean _CanClickOnTransparent)
     * { this._ClickThroughTransparentRegion = _CanClickOnTransparent; }
     */
    private boolean setupImage() {
        if (null == srcFile) {
            LOGGER.severe("Static Image Widget [ " + this.toString() + "] has no image source.");
            return false;
        }
        String fname = convertToFileOSSpecific(srcFile);
        File file = new File(fname);
        if (file.exists()) {
            String fn = "file:" + fname;
            image = new Image(fn);
            _ImageView = new ImageView(image);
            _ImageView.setPreserveRatio(getPreserveRatio());
            _ImageView.setSmooth(true);

            _ImageView.setPickOnBounds(!GetClickThroughTransparentRegion()); // this allows mouse clicks (for Tasks) to
            // be detected on transparent parts of
            // image
            if (0 == getWidth() && 0 == getHeight()) {
                setWidth(image.getWidth());
                setHeight(image.getHeight());
            }

            return true;
        }
        LOGGER.severe("Image file does not exist:" + srcFile);
        return false;
    }

    @Override
    public void UpdateTitle(String strTitle) {
        LOGGER.warning("Tried to update Title of a StaticImageWidget to " + strTitle);
    }
}
