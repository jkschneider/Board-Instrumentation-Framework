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

import static kutch.biff.marvin.widget.widgetbuilder.WidgetBuilder.HandlePeekaboo;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javafx.collections.ObservableList;

import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.TranslationCalculator;
import kutch.biff.marvin.widget.widgetbuilder.WidgetBuilder;

/**
 * @author Patrick Kutch
 */
public class TabWidget extends GridWidget {
    public static void ReIndexTabs(TabPane tabPane) {
        TabWidget.sortTabs(tabPane);

        int tabIndex = 0;
        for (Tab tab : tabPane.getTabs()) {
            for (TabWidget tabWidget : ConfigurationReader.GetConfigReader().getTabs()) {
                if (tabWidget.Reindex(tab, tabIndex)) {
                    break;
                }
            }
            tabIndex++;
        }
    }

    private static void sortTabs(TabPane tabPane) {
        List<TabWidget> tabs = ConfigurationReader.GetConfigReader().getTabs();
        Collections.sort(tabs, new Comparator<TabWidget>() {
            @Override
            public int compare(TabWidget o1, TabWidget o2) {
                if (!o1.getCreatedOnDemand() && o2.getCreatedOnDemand()) {
                    return -1;
                }
                if (o1.getCreatedOnDemand() && !o2.getCreatedOnDemand()) {

                    return 0;
                }
                if (!o1.getCreatedOnDemand() && !o2.getCreatedOnDemand()) {
                    return 0;
                }
                if (null == o1.getOnDemandSortBy() && null == o2.getOnDemandSortBy()) {
                    return 0;
                }
                if (null == o1.getOnDemandSortBy() && null != o2.getOnDemandSortBy()) {
                    return 1;
                }
                if (null != o1.getOnDemandSortBy() && null == o2.getOnDemandSortBy()) {
                    return -1;
                }
                return o1.getOnDemandSortBy().compareToIgnoreCase(o2.getOnDemandSortBy());
                // return nc.compare(o1.getTitle(),o2.getTitle());
            }
        });
        int index = 0;
        for (TabWidget tabWidget : tabs) {
            boolean selected = false;
            Tab objTab = tabWidget.getTabControl();
            if (objTab.isSelected()) {
                selected = true;
            }
            tabPane.getTabs().remove(objTab);
            tabPane.getTabs().add(index, objTab);
            if (selected) {
                SingleSelectionModel<Tab> selectionModel = tabPane.getSelectionModel();
                selectionModel.select(index);
            }
            index++;
        }
    }

    private Tab _tab;
    private GridPane baseGridPane; // throw one down in tab to put all the goodies in
    private boolean _IsVisible;
    private int _TabIndex;
    ScrollPane _ScrollPane;
    private boolean useScrollBars;
    private Pane basePane;
    private StackPane stackReference;
    private String taskOnActivate;
    private boolean ignoreFirstSelect;

    private boolean createdOnDemand;

    private String onDemandSortStr;

    public TabWidget(String tabID) {
        super();
        setMinionID(tabID); // isn't really a minion, just re-using field
        baseGridPane = new GridPane(); // can't rely on super class
        _tab = new Tab();
        setBaseCSSFilename("TabDefault.css");
        _IsVisible = true;
        taskOnActivate = null;
        ignoreFirstSelect = false;
        createdOnDemand = false;
        onDemandSortStr = null;
        basePane = new Pane();

        useScrollBars = CONFIG.getEnableScrollBars();

        if (useScrollBars) {
            _ScrollPane = new ScrollPane();
            _ScrollPane.setPannable(true);
            _ScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
            _ScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        }
        _DefinitionFile = "Tab {" + tabID + "}";
        _WidgetType = "Tab";
    }

    /**
     * Create the tab
     *
     * @param tabPane
     * @param dataMgr
     * @param iIndex
     * @return
     */
    public boolean Create(TabPane tabPane, DataManager dataMgr, int iIndex) {
        _TabIndex = iIndex;
        setWidth(CONFIG.getCanvasWidth());
        setHeight(CONFIG.getCanvasHeight());

        baseGridPane.setPadding(new Insets(getInsetTop(), getInsetRight(), getInsetBottom(), getInsetLeft()));

        boolean fSuccess = super.Create(baseGridPane, dataMgr);
        if (fSuccess) {
            _tab.setText(getTitle());
            _tab.setClosable(false);

            // basePane.setStyle("-fx-background-color:yellow");
            stackReference = new StackPane(); // for back filler when translating
//            _stackReference.setStyle("-fx-background-color:red");

            getGridPane().setAlignment(getPosition());

            // stackReference.getChildren().add(this.getGridPane());
            basePane.getChildren().add(stackReference);
            basePane.getChildren().add(baseGridPane);

            stackReference.prefWidthProperty().bind(CONFIG.getCurrentWidthProperty());
            stackReference.prefHeightProperty().bind(CONFIG.getCurrentHeightProperty());
            stackReference.prefHeightProperty().bind(basePane.heightProperty());

            if (useScrollBars) {
                _ScrollPane.setContent(baseGridPane);
                _ScrollPane.setFitToWidth(true);
                _ScrollPane.setFitToHeight(true);

                _tab.setContent(_ScrollPane);
            } else {
                basePane.prefWidthProperty().bind(CONFIG.getCurrentWidthProperty());
                basePane.prefHeightProperty().bind(CONFIG.getCurrentHeightProperty());

                _tab.setContent(basePane);

            }

            new TranslationCalculator(stackReference, baseGridPane, CONFIG.getScaleProperty(), getPosition()); // handles
            // all
            // the
            // resizing/scaling

            tabPane.getTabs().add(_tab);
            SetupPeekaboo(dataMgr);

            ApplyCSS();
            return true;
        }

        return false;
    }

    public boolean getCreatedOnDemand() {
        return createdOnDemand;
    }

    @Override
    protected GridPane getGridPane() {
        return baseGridPane;
    }

    public String getOnDemandSortBy() {
        return onDemandSortStr;
    }

    @Override
    public javafx.scene.Node getStylableObject() {
        return basePane;
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return basePane.getStylesheets();
    }

    public Tab getTabControl() {
        return _tab;
    }

    public int getTabIndex() {
        return _TabIndex;
    }

    public boolean isVisible() {
        return _IsVisible;
    }

    public boolean LoadConfiguration(FrameworkNode doc) {
        if (doc.getChildNodes().isEmpty()) {
            LOGGER.severe("No Widgets Defined for Tab: " + getTitle());
            return false;
        }
        for (FrameworkNode node : doc.getChildNodes()) {
            if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#Comment".equalsIgnoreCase(node.getNodeName())) {
                continue;
            }

            if ("Title".equalsIgnoreCase(node.getNodeName())) {
                setTitle(node.getTextContent());
            } else if ("Peekaboo".equalsIgnoreCase(node.getNodeName())) {
                if (!HandlePeekaboo(this, node)) {
                    return false;
                }
            } else if ("StyleOverride".equalsIgnoreCase(node.getNodeName())) {
                if (false == WidgetBuilder.HandleStyleOverride(this, node)) {
                    return false;
                }
            } else if ("ClickThroughTransparent".equalsIgnoreCase(node.getNodeName())) {
                SetClickThroughTransparentRegion(node.getBooleanValue());
                if (node.hasAttribute("Propagate") && node.getBooleanAttribute("Propagate")) {
                    setExplicitPropagate(true);
                }
            } else if ("Widget".equalsIgnoreCase(node.getNodeName()) || "Grid".equalsIgnoreCase(node.getNodeName())
                    || "DynamicGrid".equalsIgnoreCase(node.getNodeName())) {
                Widget widget = WidgetBuilder.Build(node);

                if (null != widget) {
                    _Widgets.add(widget);
                } else {
                    return false;
                }
            } else if ("GridMacro".equalsIgnoreCase(node.getNodeName())
                    || "MacroGrid".equalsIgnoreCase(node.getNodeName())) {
                if (!WidgetBuilder.ReadGridMacro(node)) {
                    return false;
                }
            } else if ("For".equalsIgnoreCase(node.getNodeName())) {
                List<Widget> repeatList = WidgetBuilder.BuildRepeatList(node);
                if (null == repeatList) {
                    return false;
                }
                _Widgets.addAll(repeatList);
            } else if ("TaskList".equalsIgnoreCase(node.getNodeName())) {
                ConfigurationReader.ReadTaskList(node);
            } else if ("GenerateDataPoint".equalsIgnoreCase(node.getNodeName())) {
                if (!ConfigurationReader.ReadGenerateDataPoints(node)) {
                    // return null;
                }
            } else if ("Prompt".equalsIgnoreCase(node.getNodeName())) {
                ConfigurationReader.ReadPrompt(node);
            } else if ("AliasList".equalsIgnoreCase(node.getNodeName())) {
                // already deal with someplace else
            } else if (false == HandleWidgetSpecificSettings(node)) {
                LOGGER.warning("Unknown Entry: " + node.getNodeName() + " in Tab ID= " + getMinionID());
            }
        }

        return true;
    }

    @Override
    public boolean PerformPostCreateActions(GridWidget parentGrid, boolean updateToolTipOnly) {
        if (getHeight() == 0 && this.getHeightPercentOfParentGrid() == 0) {
            setHeightPercentOfParentGrid(100);
        }
        if (null != taskOnActivate) {
            if (_tab.isSelected()) {
                ignoreFirstSelect = true; // 1st tab will get the selection changed notification on startup, ignore it
            }
            _tab.setOnSelectionChanged((Event e) ->
            {
                if (_tab.isSelected()) {
                    if (!ignoreFirstSelect) {
                        TASKMAN.PerformTask(taskOnActivate);
                    }
                }
                ignoreFirstSelect = false;
            });
        }

        return super.PerformPostCreateActions(parentGrid, updateToolTipOnly);
    }

    public boolean Reindex(Tab compare, int newIndex) {
        if (compare == _tab) {
            _TabIndex = newIndex;
            return true;
        }
        return false;
    }

    public void setCreatedOnDemand() {
        createdOnDemand = true;
    }

    public void setOnActivateTask(String taskID) {
        taskOnActivate = taskID;
    }

    public void setOnDemandSortBy(String sortStr) {
        onDemandSortStr = sortStr;
    }

    public void setVisible(boolean _IsVisible) {
        this._IsVisible = _IsVisible;
    }

}
