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

import static kutch.biff.marvin.configuration.Configuration.getConfig;
import static kutch.biff.marvin.configuration.ConfigurationReader.GetConfigReader;

import java.util.List;
import java.util.logging.Logger;

import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.widget.TabWidget;

/**
 * @author Patrick Kutch
 */
public class MarvinAdminTask extends BaseTask {

    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final String task;
    private final String _Data;
    private final String _id;

    public MarvinAdminTask(String id, String Task, String data) {
        _id = id;
        task = Task;
        _Data = data;
    }

    @Override
    public boolean getMustBeInGUIThread() {
        return true;
    }

    @Override
    public void PerformTask() {
        if ("SetActiveTab".equalsIgnoreCase(task)) {
            SetTab();
        } else if ("SetTabVisibility".equalsIgnoreCase(task)) {
            SetVisible();
        } else if ("Terminate".equalsIgnoreCase(task)) {
            TerminateMarvin();
        } else if ("RefreshData".equalsIgnoreCase(task)) {
            WatchdogTask.ForceRefresh();
        } else {
            LOGGER.warning("Asked to perform a MarvinAdminTask of [" + task + "] for Task ID: " + _id
                    + ".  However that is not a valid task.");
        }
    }

    private void SetTab() {
        LOGGER.info("Performing Set Tab MarvinAdminTask, to Tab: " + _Data);
        List<TabWidget> tabs = GetConfigReader().getTabs();
        for (int iLoop = 0; iLoop < tabs.size(); iLoop++) {
            TabWidget tabWidget = tabs.get(iLoop);

            if (tabWidget.getMinionID().equalsIgnoreCase(_Data)) {
                SingleSelectionModel<Tab> selectionModel = getConfig().getPane().getSelectionModel();
                selectionModel.select(tabWidget.getTabIndex());

                return;
            }
        }
        LOGGER.warning("Invalid Tab ID [" + _Data + "] specified for a SetActiveTab MarvinAdminTask");
    }

    /**
     * Make a tab visible or invisible
     */
    private void SetVisible() {
        LOGGER.info("Performing Set Visible Tab MarvinAdminTask, to Tab: " + _Data);
        if (_Data.contains(":")) // could be Volume or JumpTo
        {
            String[] parts = _Data.split(":");
            boolean fVisible;
            if (parts.length > 1) {
                String strTabID = parts[0];
                String strVisibility = parts[1];
                if ("true".equalsIgnoreCase(strVisibility)) {
                    fVisible = true;
                } else if ("false".equalsIgnoreCase(strVisibility)) {
                    fVisible = false;
                } else {
                    LOGGER.severe("MarvinAdminTask received invalid command --> " + _Data);
                    return;
                }

                List<TabWidget> tabs = GetConfigReader().getTabs();
                int invisCount = 0;
                for (int iLoop = 0; iLoop < tabs.size(); iLoop++) {
                    TabWidget tab = tabs.get(iLoop);
                    if (tab.getMinionID().equalsIgnoreCase(strTabID)) // Minion ID is the Tab ID
                    {
                        if (fVisible) {
                            if (tab.isVisible()) {
                                LOGGER.info("Asked to set tab ID " + strTabID + " visible, but it is already visible");
                                return;
                            }
                            getConfig().getPane().getTabs().add(tab.getTabIndex() - invisCount, tab.getTabControl());

                            tab.setVisible(fVisible);
                            return;
                        }
                        // else make invisible
                        if (!tab.isVisible()) {
                            LOGGER.info("Asked to set tab ID " + strTabID + " invisible, but it is already invisible");
                            return;
                        }
                        getConfig().getPane().getTabs().remove(iLoop - invisCount);
                        tab.setVisible(fVisible);
                        return;
                    } else {
                        if (!tab.isVisible()) {
                            invisCount++; // make sure and insert at right point, could be 'invisible' tabs ahead of us,
                            // so the index would be off.
                        }

                    }
                }
            }
        } else {
            LOGGER.severe("Received invalid MarvinAdminTask : " + _Data);
            return;
        }

        LOGGER.warning("Invalid Tab ID [" + _Data + "] specified for a SetActiveTab MarvinAdminTask");
    }

    private void TerminateMarvin() {
        LOGGER.info("Terminating Marvin based upon MarvinAdminTask");
        System.exit(0);
    }
}
