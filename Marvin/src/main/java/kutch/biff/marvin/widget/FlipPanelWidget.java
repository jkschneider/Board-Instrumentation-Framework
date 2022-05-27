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

import eu.hansolo.enzo.flippanel.FlipPanel;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.PanelSideInfo;
import kutch.biff.marvin.widget.widgetbuilder.WidgetBuilder;

/**
 * @author Patrick Kutch
 */
public class FlipPanelWidget extends BaseWidget {

    private GridWidget backGrid;
    private GridWidget _FrontGrid;
    private FlipPanel panel;
    private GridPane frontBaseGridPane;
    private GridPane backBaseGridPane;
    private Orientation _Orientation;
    private boolean front2Back = true;
    private PanelSideInfo _FrontInfo;
    private PanelSideInfo _BackInfo;
    private VBox vFront;
    private VBox vBack;
    private Button frontBtn;
    private Button backBtn;
    private double _AnimationDuration;

    public FlipPanelWidget() {
        backGrid = null;
        _FrontGrid = null;
        panel = null;
        frontBtn = null;
        backBtn = null;
        _Orientation = Orientation.HORIZONTAL;
        _AnimationDuration = 700;
        setDefaultIsSquare(false);
    }

    @Override
    protected boolean ApplyCSS() {
        // super.ApplyCSS(); // can do CSS for entire widget here, and below is for
        // individual sides
        backGrid.setWidgetInformation(getDefinintionFileDirectory(), null, "FlipPanel");
        _FrontGrid.setWidgetInformation(getDefinintionFileDirectory(), null, "FlipPanel");
        if (null == _FrontGrid.GetCSS_File()) {
            _FrontGrid.setBaseCSSFilename(getBaseCSSFilename());
        }
        if (null == backGrid.GetCSS_File()) {
            backGrid.setBaseCSSFilename(getBaseCSSFilename());
        }

        _FrontGrid.ApplyCSS();
        backGrid.ApplyCSS();

        vFront.getStylesheets().clear();
        vBack.getStylesheets().clear();

        if (null != getStyleID()) {
            vFront.setId(getStyleID());
            vBack.setId(getStyleID());
        }
        if (null != GetCSS_File()) {
            vFront.getStylesheets().add(GetCSS_File());
            vBack.getStylesheets().add(GetCSS_File());
        }
        ApplyStyleOverrides(vFront, getStyleOverride());
        ApplyStyleOverrides(vBack, getStyleOverride());
        ApplyStyleOverrides(vFront, _FrontGrid.getStyleOverride());
        ApplyStyleOverrides(vBack, backGrid.getStyleOverride());
        return true;
    }

    @Override
    public boolean Create(GridPane parentPane, DataManager dataMgr) {
        SetParent(parentPane);
        if (null == backGrid || null == _FrontGrid) {
            LOGGER.severe("Flip Panel needs both front and back definition.");
            return false;
        }
        if (false == SetupPanel(dataMgr)) {
            return false;
        }

        ConfigureDimentions();
        parentPane.add(panel, getColumn(), getRow(), getColumnSpan(), getRowSpan()); // is a cycle since this is the
        // parent of tab

        SetupPeekaboo(dataMgr);

        dataMgr.AddListener(getMinionID(), getNamespace(), (ObservableValue<?> o, Object oldVal, Object newVal) -> {
            String strVal = newVal.toString();
            Orientation orientation = getRequestedOrientation(strVal);
            if ("Flip".equalsIgnoreCase(strVal)) {
                DoFlip();
            } else if ("Front".equalsIgnoreCase(strVal)) {
                DoFlip(false, getOrientation());
            } else if ("Back".equalsIgnoreCase(strVal)) {
                DoFlip(true, getOrientation());
            } else if (strVal.length() > 4 && "Flip:".equalsIgnoreCase(strVal.substring(0, 5))) // Flip, but with a
                // direction
                {
                    if (null != orientation) {
                        DoFlip(front2Back, orientation);
                    }

                } else if (strVal.length() > 4 && "Front:".equalsIgnoreCase(strVal.substring(0, 5))) // Flip, but with a
                // direction
                {
                    if (null != orientation) {
                        DoFlip(false, orientation);
                    }

                } else if (strVal.length() > 4 && "Back:".equalsIgnoreCase(strVal.substring(0, 5))) // Flip, but with a
                // direction
                {
                    if (null != orientation) {
                        DoFlip(true, orientation);
                    }
                }

        });

        return ApplyCSS();
    }

    private void DoFlip() {
        try {
            panel.setFlipDirection(getOrientation());
            if (front2Back) {
                panel.flipToBack();
            } else {
                panel.flipToFront();
            }
            front2Back = !front2Back;
        } catch (Exception ex) // latest Enzo build works, but sometimes has an exeption in here
        {
        }
    }

    private void DoFlip(boolean toBack, Orientation orientation) {
        if (null != orientation) // am on one side and want to go to other
        {
            panel.setFlipDirection(orientation);
            if (front2Back) {
                panel.flipToBack();
            } else {
                panel.flipToFront();
            }
            front2Back = !front2Back;
        }
    }

    public double getAnimationDuration() {
        return _AnimationDuration;
    }

    public PanelSideInfo getBackInfo() {
        return _BackInfo;
    }

    public GridWidget getFrontGrid() {
        return _FrontGrid;
    }

    public PanelSideInfo getFrontInfo() {
        return _FrontInfo;
    }

    public Orientation getOrientation() {
        return _Orientation;
    }

    private Orientation getRequestedOrientation(String strRequest) {
        String[] parts = strRequest.split(":");
        if (parts.length > 1) {
            if ("Horizontal".equalsIgnoreCase(parts[1])) {
                return Orientation.HORIZONTAL;
            }
            if ("Vertical".equalsIgnoreCase(parts[1])) {
                return Orientation.VERTICAL;
            }
            LOGGER.warning("Received invalid action for Flip Panel: " + strRequest);
        }
        return null;
    }

    @Override
    public javafx.scene.Node getStylableObject() {
        return panel;
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return panel.getStylesheets();
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node) {
        if (super.HandleWidgetSpecificSettings(node)) // see if anything in GridWidget (like padding override)
        {
            return true;
        }
        if ("Front".equalsIgnoreCase(node.getNodeName())) {
            // _FrontGrid = WidgetBuilder.ReadGridInfo(node, new GridWidget());
            _FrontGrid = WidgetBuilder.BuildGrid(node, true);
            return _FrontGrid != null;
        }
        if ("Back".equalsIgnoreCase(node.getNodeName())) {
            // _BackGrid = WidgetBuilder.ReadGridInfo(node, new GridWidget());
            backGrid = WidgetBuilder.BuildGrid(node, true);
            return backGrid != null;
        }
        if ("AnimationDuration".equalsIgnoreCase(node.getNodeName())) {
            String str = node.getTextContent();
            try {
                setAnimationDuration(Double.parseDouble(str));
                return true;
            } catch (NumberFormatException ex) {
                LOGGER.severe("Invlid value for <AnimationDuration> tag for FlipPanel Widget");
            }
        }
        if ("RotationOverride".equalsIgnoreCase(node.getNodeName())) {
            String str = node.getTextContent();
            if (0 == str.compareToIgnoreCase("Horizontal")) {
                setOrientation(Orientation.HORIZONTAL);
            } else if (0 == str.compareToIgnoreCase("Vertical")) {
                setOrientation(Orientation.VERTICAL);
            } else {
                LOGGER.severe(
                        "Invalid Orientation in FlipPanel Orientation overvide. Should be Horizontal or Vertical, not : "
                                + str);
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean PerformPostCreateActions(GridWidget objParentGrid, boolean updateToolTipOnly) {
        if (updateToolTipOnly) {
            if (CONFIG.isDebugMode()) {
                _ToolTip = this.toString();
            }
            if (_ToolTip != null && null != getStylableObject()) {
                HandleToolTipInit();
                Tooltip.install(vFront, _objToolTip);
                Tooltip.install(vBack, _objToolTip);
            }
            return true;
        }

        if (CONFIG.isDebugMode()) {
            _ToolTip = this.toString();
        }
        if (_ToolTip != null) {
            HandleToolTipInit();
            Tooltip.install(vFront, _objToolTip);
            Tooltip.install(vBack, _objToolTip);
        }
        FireDefaultPeekaboo();

        return true;
    }

    public void setAnimationDuration(double animationDuration) {
        this._AnimationDuration = animationDuration;
    }

    public void setBackInfo(PanelSideInfo backInfo) {
        this._BackInfo = backInfo;
    }

    public void setFrontGrid(GridWidget frontGrid) {
        this._FrontGrid = frontGrid;
    }

    public void setFrontInfo(PanelSideInfo frontInfo) {
        this._FrontInfo = frontInfo;
    }

    public void setOrientation(Orientation _Orientation) {
        this._Orientation = _Orientation;
    }

    private boolean SetupPanel(DataManager dataMgr) {
        boolean retVal = true;
        panel = new FlipPanel(_Orientation);

        panel.setFlipTime(getAnimationDuration());
        vFront = new VBox();
        vBack = new VBox();

        frontBaseGridPane = new GridPane();
        backBaseGridPane = new GridPane();

        if (false == _FrontGrid.Create(frontBaseGridPane, dataMgr)
                || !_FrontGrid.PerformPostCreateActions(getFrontGrid(), false)) {
            return false;
        }
        if (false == backGrid.Create(backBaseGridPane, dataMgr)
                || !backGrid.PerformPostCreateActions(backGrid, false)) {
            return false;
        }
        frontBaseGridPane.setAlignment(_FrontGrid.getPosition());
        backBaseGridPane.setAlignment(backGrid.getPosition());
        frontBtn = SetupPanelButton(_FrontInfo);
        backBtn = SetupPanelButton(_BackInfo);

        vFront.getChildren().add(frontBaseGridPane);
        vBack.getChildren().add(backBaseGridPane);
        if (null != frontBtn) {
            if (_FrontInfo.IsButtonOnTop()) {
                vFront.getChildren().add(0, frontBtn);
            } else {
                vFront.getChildren().add(1, frontBtn);
            }
        }
        if (null != backBtn) {
            if (_BackInfo.IsButtonOnTop()) {
                vBack.getChildren().add(0, backBtn);
            } else {
                vBack.getChildren().add(1, backBtn);
            }
        }

        if (null != _FrontInfo) {
            vFront.setAlignment(_FrontInfo.GetButtonAlignment());
        }
        if (null != _BackInfo) {
            vBack.setAlignment(_BackInfo.GetButtonAlignment());
        }

        panel.getFront().getChildren().add(vFront);
        panel.getBack().getChildren().add(vBack);

        return retVal;
    }

    private Button SetupPanelButton(PanelSideInfo info) {
        if (null == info) {
            return null;
        }
        Button button = new Button(info.getButtonText());
        String strFile = "";

        strFile = strFile + getDefinintionFileDirectory() + File.separatorChar + info.getCSSFile();
        File file = new File(strFile);

        if (false == file.exists()) {
            LOGGER.severe("Unable to locate FlipPanel Stylesheet: " + strFile);
        } else {
            LOGGER.config("Applying Stylesheet: " + GetCSS_File() + " to Widget.");
            String url = BaseWidget.convertToFileURL(strFile);
            if (false == button.getStylesheets().add(url)) {
                // return null;
            }
            if (null != info.getStyleID()) {
                button.setId(info.getStyleID());
            }
        }
        button.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> this.DoFlip());

        return button;
    }

    @Override
    public void UpdateTitle(String strTitle) {
        LOGGER.warning("Tried to update Title of a FlipPanel Widget to " + strTitle);
    }
}
