/*
 * ##############################################################################
 * #  Copyright (c) 2018 Intel Corporation
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
 * ###
 */
package kutch.biff.marvin.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.DynamicItemInfoContainer;
import kutch.biff.marvin.utility.DynamicItemInfoContainer.SortMethod;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.widgetbuilder.OnDemandGridBuilder;

/**
 * @author Patrick.Kutch@gmail.com
 */
public class OnDemandGridWidget extends GridWidget {
    private String strPrimaryGrowth = "HZ";
    private String strSecondaryGrowth = "VT";
    private int newLineCount = 1;
    @SuppressWarnings("unused")
    private int currentLineCount;
    private int nextPositionX;
    private int nextPositionY;
    private DynamicItemInfoContainer criterea;
    private List<Pair<BaseWidget, String>> addedGridList;

    public OnDemandGridWidget(DynamicItemInfoContainer onDemandInfo) {
        criterea = onDemandInfo;
        addedGridList = new ArrayList<>();
    }

    public boolean AddOnDemandWidget(BaseWidget objWidget, String sortStr) {
        Pair<Integer, Integer> position = getNextPosition();

        objWidget.setColumn(position.getKey());
        objWidget.setRow(position.getValue());
        objWidget.setWidth(getWidth());
        objWidget.setHeight(getHeight());

        if (objWidget.Create(getGridPane(), DataManager.getDataManager())) {
            if (objWidget.PerformPostCreateActions(this, false)) {
                addedGridList.add(new Pair<>(objWidget, sortStr));
                if (criterea.getSortByMethod() != SortMethod.NONE) {
                    resortWidgets(); // OddEven Style applied in here
                } else {
                    criterea.ApplyOddEvenStyle(objWidget, addedGridList.size());
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean Create(GridPane parentPane, DataManager dataMgr) {
        super.Create(parentPane, dataMgr);
        setupPositioning();
        OnDemandGridBuilder objBuilder = new OnDemandGridBuilder(this);
        dataMgr.AddOnDemandWidgetCriterea(criterea, objBuilder);
        // grab ALL aliases to use when contained widgets created
        return true;
    }

    public DynamicItemInfoContainer getCriterea() {
        return criterea;
    }

    private Pair<Integer, Integer> getNextPosition() {
        Pair<Integer, Integer> retObj = null;
        if ("HZ".equals(strPrimaryGrowth)) {
            nextPositionX++;
            if (nextPositionX >= newLineCount) {
                currentLineCount++;
                nextPositionX = 0;
                if ("VT".equals(strSecondaryGrowth)) {
                    nextPositionY++;
                } else {
                    nextPositionY--;
                }
            }
        } else if ("ZH".equals(strPrimaryGrowth)) {
            nextPositionX--;
            if (nextPositionX < 0) {
                currentLineCount++;
                nextPositionX = newLineCount - 1;
                if ("VT".equals(strSecondaryGrowth)) {
                    nextPositionY++;
                } else {
                    nextPositionY--;
                }
            }
        } else if ("VT".equals(strPrimaryGrowth)) {
            nextPositionY++;
            if (nextPositionY >= newLineCount) {
                currentLineCount++;
                nextPositionY = 0;
                if ("HZ".equals(strSecondaryGrowth)) {
                    nextPositionX++;
                } else {
                    nextPositionX--;
                }
            }
        } else if ("TV".equals(strPrimaryGrowth)) {
            nextPositionY--;
            if (nextPositionY < 0) {
                currentLineCount++;
                nextPositionY = newLineCount - 1;
                if ("HZ".equals(strSecondaryGrowth)) {
                    nextPositionX++;
                } else {
                    nextPositionX--;
                }
            }
        }

        retObj = new Pair<>(nextPositionX, nextPositionY);
        return retObj;
    }

    public boolean ReadGrowthInfo(FrameworkNode growthNode) {
        String strPrimary = growthNode.getAttribute("Primary");
        String strSecondary = growthNode.getAttribute("Secondary");
        newLineCount = growthNode.getIntegerAttribute("NewLineCount", 1);
        boolean primaryIsHorizontal = false;
        boolean secondaryIsHorizontal = false;

        if (null == strPrimary) {
            strPrimary = "HZ";
            primaryIsHorizontal = true;
        } else if ("Horizontal".equalsIgnoreCase(strPrimary) || "HZ".equalsIgnoreCase(strPrimary)) {
            strPrimaryGrowth = "HZ";
            primaryIsHorizontal = true;
        } else if ("latnoziroh".equalsIgnoreCase(strPrimary) || "ZH".equalsIgnoreCase(strPrimary)) {
            strPrimaryGrowth = "ZH";
            primaryIsHorizontal = true;
        } else if ("Vertical".equalsIgnoreCase(strPrimary) || "VT".equalsIgnoreCase(strPrimary)) {
            strPrimaryGrowth = "VT";
        } else if ("lacitrev".equalsIgnoreCase(strPrimary) || "TV".equalsIgnoreCase(strPrimary)) {
            strPrimaryGrowth = "TV";
        } else {
            LOGGER.severe("Unknown primary Growth Direction for OnDemand grid: " + strPrimary);
            return false;
        }

        if (null == strSecondary) {
            if (primaryIsHorizontal) {
                strSecondary = "VT";
                secondaryIsHorizontal = false;
            } else {
                strSecondaryGrowth = "HZ";
                secondaryIsHorizontal = true;
            }
        } else if ("Horizontal".equalsIgnoreCase(strSecondary) || "HZ".equalsIgnoreCase(strSecondary)) {
            strSecondaryGrowth = "HZ";
            secondaryIsHorizontal = true;
        } else if ("latnoziroh".equalsIgnoreCase(strSecondary) || "ZH".equalsIgnoreCase(strSecondary)) {
            strSecondaryGrowth = "ZH";
            secondaryIsHorizontal = true;
        } else if ("Vertical".equalsIgnoreCase(strSecondary) || "VT".equalsIgnoreCase(strSecondary)) {
            strSecondaryGrowth = "VT";
        } else if ("lacitrev".equalsIgnoreCase(strSecondary) || "TV".equalsIgnoreCase(strSecondary)) {
            strSecondaryGrowth = "TV";
        } else {
            LOGGER.severe("Unknown secondary Growth Direction for OnDemand grid: " + strSecondary);
            return false;
        }

        if (primaryIsHorizontal == secondaryIsHorizontal) {
            LOGGER.severe(
                    "Primary and secondary Growth Direction for OnDemand grid must differ, one must be horizontal, the other vertical");
            return false;
        }

        return true;
    }

    private void resortWidgets() {
//        getGridPane().getChildren().removeAll(getGridPane().getChildren());
        getGridPane().getChildren().clear();
        sortGrids();

        setupPositioning();
        int widgetNum = 0;
        for (Pair<BaseWidget, String> tuple : addedGridList) {
            widgetNum++;
            BaseWidget objWidget = tuple.getKey();
            Pair<Integer, Integer> position = getNextPosition();
            getGridPane().add(objWidget.getStylableObject(), position.getKey(), position.getValue());
            criterea.ApplyOddEvenStyle(objWidget, widgetNum);
        }
    }

    private void setupPositioning() {
        if ("HZ".equals(strPrimaryGrowth)) {
            nextPositionX = -1;
            if ("VT".equals(strSecondaryGrowth)) {
                nextPositionY = 0;
            } else {
                nextPositionY = 125;
            }
        } else if ("ZH".equals(strPrimaryGrowth)) {
            nextPositionX = newLineCount - 1;
            if ("VT".equals(strSecondaryGrowth)) {
                nextPositionY = 0;
            } else {
                nextPositionY = 125;
            }
        } else if ("VT".equals(strPrimaryGrowth)) {
            nextPositionY = -1;
            if ("HZ".equals(strSecondaryGrowth)) {
                nextPositionX = -1;
            } else {
                nextPositionX = 125;
            }
        } else if ("TV".equals(strPrimaryGrowth)) {
            nextPositionY = newLineCount - 1;
            if ("HZ".equals(strSecondaryGrowth)) {
                nextPositionX = -1;
            } else {
                nextPositionX = 125;
            }
        }
    }

    private void sortGrids() {
        Collections.sort(addedGridList, new Comparator<Pair<BaseWidget, String>>() {
            @Override
            public int compare(Pair<BaseWidget, String> o1, Pair<BaseWidget, String> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
    }
}
