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

import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebView;
import kutch.biff.marvin.datamanager.DataManager;

/**
 * @author Patrick Kutch
 */
public class WebWidget extends BaseWidget {
    private WebView browser;
    private boolean reverseContent;
    private String currentContent;
    private String hackedFile;

    public WebWidget() {
        browser = new WebView();
        reverseContent = false;
        currentContent = "";
        hackedFile = null;
    }

    private void BadContent(String strContent) {
        LOGGER.warning("Received bad WebWidget content: " + strContent);
    }

    @Override
    protected void ConfigureDimentions() {
        if (getHeight() > 0) {
            browser.setPrefHeight(getHeight());
        }

        if (getWidth() > 0) {
            browser.setPrefWidth(getWidth());
        }
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        ConfigureDimentions();
        if (reverseContent) {
            browser.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        }

        ConfigureAlignment();
        SetupPeekaboo(dataMgr);
        SetContent(currentContent);

        pane.add(browser, getColumn(), getRow(), getColumnSpan(), getRowSpan());

        dataMgr.AddListener(getMinionID(), getNamespace(), (ObservableValue o, Object oldVal, Object newVal) -> {
            if (IsPaused()) {
                return;
            }
            hackedFile = null;
            SetContent(newVal.toString());
        });

        // check status of loading
        browser.getEngine().getLoadWorker().stateProperty().addListener((ObservableValue ov, Worker.State oldState, Worker.State newState) -> {
            if (newState == State.FAILED) {
                // browser requires absolute path, so let's try to provide that, in case a
                // relative one was provided
                if ("file:".equalsIgnoreCase(currentContent.substring(0, 5))) {
                    if (hackedFile == null) {
                        Path currentRelativePath = Paths.get("");
                        String workDir = currentRelativePath.toAbsolutePath().toString() + java.io.File.separator;

                        hackedFile = currentContent;
                        currentContent = "file:" + workDir + currentContent.substring(5);
                        SetContent(currentContent);
                        return;
                    }
                    currentContent = hackedFile;
                }

                LOGGER.info("Error loading web widget content: " + currentContent);
            }
        });

        SetupTaskAction();
        return ApplyCSS();
    }

    @Override
    public Node getStylableObject() {
        return browser;
    }

    @Override
    public ObservableList<String> getStylesheets() {

        return browser.getStylesheets();
    }

    private void SetContent(String strContent) {
        if (strContent.length() < 10) {
            BadContent(strContent);
            return;
        }

        if ("http".equalsIgnoreCase(strContent.substring(0, 4))) {
            browser.getEngine().load(strContent);
        } else if ("file:".equalsIgnoreCase(strContent.substring(0, 5))) {
            browser.getEngine().load(strContent);
        } else {
            browser.getEngine().loadContent(strContent);
        }
        currentContent = strContent;
    }

    @Override
    public void SetInitialValue(String value) {
        currentContent = value;
    }

    public void SetReverseContent(boolean newVal) {
        reverseContent = newVal;
    }

    @Override
    public void UpdateTitle(String strTitle) {
        LOGGER.warning("Tried to update Title of a WebWidget to " + strTitle);
    }

}
