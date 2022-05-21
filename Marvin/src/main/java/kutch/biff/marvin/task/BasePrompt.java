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

package kutch.biff.marvin.task;

import java.util.logging.Logger;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.BaseWidget;

/**
 * @author Patrick Kutch
 */
public abstract class BasePrompt {
    protected static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private String _dlgTitle;
    private String _Message;
    private String _Prompt;
    private String promptedValue;
    private String _id;
    private double _Width;
    private double _Height;
    private String _StyleOverride;
    private String _CssFile;

    public BasePrompt(String id) {
        _dlgTitle = null;
        _Message = null;
        _Prompt = null;
        promptedValue = null;
        _id = id;
        _Width = 0;
        _Height = 0;
        _CssFile = null;
        _StyleOverride = null;
    }

    private Stage CreateDialog() {
        Stage dialog = new Stage();
        dialog.setTitle(getDlgTitle());
        dialog.initStyle(StageStyle.UTILITY);
        dialog.initModality(Modality.APPLICATION_MODAL);
        Pane dlgPane = SetupDialog(dialog);
        Scene objScene = new Scene(dlgPane);

        String cssFile = Configuration.getConfig().getCSSFile();
        if (null != getCssFile()) {
            cssFile = getCssFile();
        }

        if (null != cssFile) {
            String osIndepFN = BaseWidget.convertToFileOSSpecific(cssFile);
            String strCSS = BaseWidget.convertToFileURL(osIndepFN);

            if (null != strCSS) {
                try {
                    if (false == objScene.getStylesheets().add(strCSS)) {
                        // LOGGER.severe("Problems with application stylesheet: " +
                        // Configuration.getConfig().getConfiguration().getCSSFile());
                    }
                } catch (Exception ex) {
                    // LOGGER.severe("Problems with application stylesheet: " +
                    // Configuration.getConfig().getConfiguration().getCSSFile());
                }
            }
        }
        if (null != getStyleOverride()) {
            dlgPane.setStyle(getStyleOverride());
        }

        if (null == dlgPane) {
            return null;
        }

        dialog.setScene(objScene);

        return dialog;
    }

    public String getCssFile() {
        return _CssFile;
    }

    public String getDlgTitle() {
        return _dlgTitle;
    }

    public double getHeight() {
        return _Height;
    }

    public String getMessage() {
        return _Message;
    }

    public String getPrompt() {
        return _Prompt;
    }

    public String GetPromptedValue() {
        return promptedValue;
    }

    public String getStyleOverride() {
        return _StyleOverride;
    }

    public double getWidth() {
        return _Width;
    }

    public boolean HandlePromptSpecificConfig(FrameworkNode baseNode) {
        return false;
    }

    public boolean PerformPrompt() {
        promptedValue = null;
        Stage objStage = CreateDialog();
        if (null == objStage) {
            return false;
        }

        CreateDialog().showAndWait();

        return null != GetPromptedValue();
    }

    public void setCssFile(String _CssFile) {
        this._CssFile = _CssFile;
    }

    public void setDlgTitle(String dlgTitle) {
        this._dlgTitle = dlgTitle;
    }

    public void setHeight(double height) {
        this._Height = height;
    }

    public void setMessage(String message) {
        this._Message = message;
    }

    public void setPrompt(String prompt) {
        this._Prompt = prompt;
    }

    protected void SetPromptedValue(String newValue) {
        promptedValue = newValue;
    }

    public void setStyleOverride(String styleOverride) {
        this._StyleOverride = styleOverride;
    }

    protected Pane SetupDialog(Stage dialog) {
        return null;
    }

    public void setWidth(double width) {
        this._Width = width;
    }

    @Override
    public String toString() {
        return _id;
    }

}
