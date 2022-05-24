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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.Node;
import javafx.util.Pair;
import kutch.biff.marvin.Marvin;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.task.DynamicTabWidgetSortTask;
import kutch.biff.marvin.task.TaskManager;

/**
 * @author Patrick Kutch
 */
public class DynamicTabWidget extends TabWidget {
    private static ArrayList<DynamicTabWidget> dynaTabs = new ArrayList<>();
    private static int index = 1;
    private static boolean Enabled;
    private static int MaxWidth = 7;
    private static String titleStr = "Unregistered Datapoints";
    private static String TitleStyle = "-fx-font-size: 3.5em;";
    private static String Even_Background = "-fx-background-color:green";
    private static String Even_ID = "-fx-font-size: 2.5em;";
    private static String Even_Value = "-fx-font-size: 1.5em;";
    private static String Odd_Background = "-fx-background-color:grey";
    private static String Odd_ID = "-fx-font-size: 2.5em;-fx-text-fill:black";
    private static String Odd_Value = "-fx-font-size: 1.5em;";

    public static String getEven_Background() {
        return DynamicTabWidget.Even_Background;
    }

    public static String getEven_ID() {
        return DynamicTabWidget.Even_ID;
    }

    public static String getEven_Value() {
        return DynamicTabWidget.Even_Value;
    }

    public static int getMaxWidth() {
        return DynamicTabWidget.MaxWidth;
    }

    public static String getOdd_Background() {
        return DynamicTabWidget.Odd_Background;
    }

    public static String getOdd_ID() {
        return DynamicTabWidget.Odd_ID;
    }

    public static String getOdd_Value() {
        return DynamicTabWidget.Odd_Value;
    }

    public static DynamicTabWidget getTab(String tabID, DataManager dataMgr) {
        for (DynamicTabWidget tab : dynaTabs) {
            if (tab.getMinionID().equalsIgnoreCase(tabID)) {
                return tab;
            }
        }
        DynamicTabWidget objTabWidget = new DynamicTabWidget(tabID);
        objTabWidget.setTitle(tabID);
        dynaTabs.add(objTabWidget);

        TextWidget objTitleWidget = new TextWidget();

        objTitleWidget.SetInitialValue(DynamicTabWidget.getTitleStr());
        objTitleWidget.setRow(0);
        objTitleWidget.setColumn(0);
        objTitleWidget.setColumnSpan(DynamicTabWidget.getMaxWidth());
        objTitleWidget.setAlignment("Center");
        objTitleWidget.setStyleOverride(Arrays.asList(DynamicTabWidget.getTitleStyle()));
        objTabWidget.AddWidget(objTitleWidget);

        objTabWidget.Create(Marvin.GetBaseTabPane(), dataMgr, index);

        index++;
        return objTabWidget;
    }

    public static String getTitleStr() {
        return DynamicTabWidget.TitleStr;
    }

    public static String getTitleStyle() {
        return DynamicTabWidget.TitleStyle;
    }

    public static boolean isEnabled() {
        return DynamicTabWidget.Enabled;
    }

    public static void setEnabled(boolean enabled) {
        DynamicTabWidget.Enabled = enabled;
    }

    public static void setEven_Background(String evenBackground) {
        DynamicTabWidget.Even_Background = evenBackground;
    }

    public static void setEven_ID(String evenID) {
        DynamicTabWidget.Even_ID = evenID;
    }

    public static void setEven_Value(String evenValue) {
        DynamicTabWidget.Even_Value = evenValue;
    }

    public static void setMaxWidth(int maxWidth) {
        DynamicTabWidget.MaxWidth = maxWidth;
    }

    public static void setOdd_Background(String oddBackground) {
        DynamicTabWidget.Odd_Background = oddBackground;
    }

    public static void setOdd_ID(String oddID) {
        DynamicTabWidget.Odd_ID = oddID;
    }

    public static void setOdd_Value(String oddValue) {
        DynamicTabWidget.Odd_Value = oddValue;
    }

    public static void setTitleStr(String newTitle) {
        DynamicTabWidget.TitleStr = newTitle;
    }

    public static void setTitleStyle(String titleStyle) {
        DynamicTabWidget.TitleStyle = titleStyle;
    }

    private List<Pair<String, List<Object>>> dataPoint; // Minion ID, [objGrid,objID,objValue]

    private AtomicInteger sortCount;

    public DynamicTabWidget(String tabID) {
        super(tabID);
        dataPoint = new ArrayList<>();
        sortCount = new AtomicInteger();
        sortCount.set(0);
    }

    public void AddWidget(DataManager dataMgr, String id, String initialVal) {
        GridWidget objGrid = new GridWidget();
        TextWidget objIDWidget = new TextWidget();
        TextWidget objValueWidget = new TextWidget();

        objIDWidget.SetInitialValue(id);
        objIDWidget.setRow(0);
        objIDWidget.setColumn(0);

        objValueWidget.SetInitialValue(initialVal);
        objValueWidget.setNamespace(this.getMinionID()); // Minion ID is the namespace for this tab
        objValueWidget.setMinionID(id);
        objValueWidget.setRow(1);
        objValueWidget.setColumn(0);
        objValueWidget.setScaleToFitBounderies(false);
        // objValueWidget.setWidth(250);

        objGrid.AddWidget(objIDWidget);
        objGrid.AddWidget(objValueWidget);

        objGrid.setRow(0); // going to be moved in 'sort' anyhow
        objGrid.setColumn(0);

        if (!objGrid.Create(this.getGridPane(), dataMgr) || !objGrid.PerformPostCreateActions(this, false)) {
            LOGGER.severe("Unknown failure creating Grid");
        }
        objGrid.getStylableObject().setVisible(false); // make it invisible until sort, makes it quicker

        dataPoint.add(new Pair<>(id.toUpperCase(), Arrays.asList(objGrid, objIDWidget, objValueWidget)));
        SetupSort();
    }

    public int getSortCount() {
        synchronized (this) {
            return sortCount.get();
        }
    }

    private int incrementSortCount() {
        synchronized (this) {
            return sortCount.incrementAndGet();
        }
    }

    public void PerformSort() {
        if (getSortCount() > 0) {
            setIncrementSortCount(0);
            Sort();
        }
    }

    private void setIncrementSortCount(int newVal) {
        synchronized (this) {
            sortCount.set(newVal);
        }
    }

    private void SetStyle(boolean Odd, GridWidget objGrid, TextWidget objIDWidget, TextWidget objValueWidget) {
        if (Odd) {
            objGrid.setStyleOverride(Arrays.asList(DynamicTabWidget.getOdd_Background()));
            objIDWidget.setStyleOverride(Arrays.asList(DynamicTabWidget.getOdd_ID()));
            objValueWidget.setStyleOverride(Arrays.asList(DynamicTabWidget.getOdd_Value()));
        } else {
            objGrid.setStyleOverride(Arrays.asList(DynamicTabWidget.getEven_Background()));
            objIDWidget.setStyleOverride(Arrays.asList(DynamicTabWidget.getEven_ID()));
            objValueWidget.setStyleOverride(Arrays.asList(DynamicTabWidget.getEven_Value()));
        }
        objGrid.ApplyOverrides();
        objIDWidget.ApplyOverrides();
        objValueWidget.ApplyOverrides();
    }

    private void SetupSort() // create a deferred task to do the actual sorting, otherwise when you have 5000
    // datapoints it overlaods the gui
    {
        if (incrementSortCount() < 5) // don't need to stack them on if already in the queue
        {
            DynamicTabWidgetSortTask objTask = new DynamicTabWidgetSortTask(this);
            TaskManager.getTaskManager().AddPostponedTaskThreaded(objTask, 500); // just every 1.5 secs do a sort
        }
    }

    /**
     * Go through and sort the goodies alphabetically
     */
    private void Sort() {
        Collections.sort(dataPoint, new Comparator<Pair<String, List<Object>>>() {
            @Override
            public int compare(Pair<String, List<Object>> s1, Pair<String, List<Object>> s2) // do alphabetical sort
            {
                return s1.getKey().compareToIgnoreCase(s2.getKey());
            }
        });

        int row = 1; // row 0 is title
        int column = 0;
        boolean odd = true;

        Node title = getGridPane().getChildren().get(0); // Title is always first, need 2 save it
        getGridPane().getChildren().clear(); // clear all widgets
        getGridPane().getChildren().add(title); // add title back

        for (Pair<String, List<Object>> pair : dataPoint) // now add the sorted data points
        {
            GridWidget objGrid = (GridWidget) pair.getValue().get(0);
            TextWidget objID = (TextWidget) pair.getValue().get(1);
            TextWidget objValue = (TextWidget) pair.getValue().get(2);

            objGrid.setRow(row);
            objGrid.setColumn(column);

            this.getGridPane().add(objGrid.getStylableObject(), column, row);
            SetStyle(odd, objGrid, objID, objValue);
            odd = !odd;

            if (++column >= getMaxWidth()) {
                row++;
                column = 0;
            }
            objGrid.getStylableObject().setVisible(true);
        }
    }

}
