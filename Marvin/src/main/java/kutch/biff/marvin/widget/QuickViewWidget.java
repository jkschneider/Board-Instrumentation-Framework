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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.task.IQuickViewSort;
import kutch.biff.marvin.task.QuickViewWidgetSortTask;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.NaturalComparator;

/**
 * @author Patrick Kutch
 */
public class QuickViewWidget extends GridWidget implements IQuickViewSort {

    public enum SortMode {

        None, Ascending, Descending
    }

    ;

    private SortMode _SortMode = SortMode.Descending;
    private int _RowWidth = 5;
    private String _EvenBackgroundStyle = "-fx-background-color:green";
    private String _EvenIDStyle = "fx-font-size: 2.5em";
    private String _EvenDataStyle = "fx-font-size: 1.5em";

    private String oddBackgroundStyle = "fx-background-color:grey";
    private String _OddIDStyle = "fx-font-size: 2.5em";
    private String _OddDataStyle = "fx-font-size: 1.5em";
    private boolean _ShowTitle = true;
    private AtomicInteger sortCount;
//    private int _hGap, _vGap;

    //    private GridWidget _GridWidget;
    private List<Pair<String, List<Object>>> dataPoint; // Minion ID, [objGrid,objID,objValue]
    private HashMap<String, String> dataPointMap;
    private HashMap<String, String> exclusionList;
    private DataManager _dataMgr;

    public QuickViewWidget() {
        // _GridWidget = new GridWidget();
        // _GridWidget = this;
        dataPoint = new ArrayList<>();
        dataPointMap = new HashMap<>(); // for quick lookup as new data comes in
        exclusionList = new HashMap<>(); // For those we do not want to show
//        _hGap = -1;
//        _vGap = -1;
        sortCount = new AtomicInteger();
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        _dataMgr = dataMgr;

        getGridPane().setAlignment(getPosition());
        pane.add(getGridPane(), getColumn(), getRow(), getColumnSpan(), getRowSpan());

        if (gethGap() > -1) {
            getGridPane().setHgap(gethGap());
        }
        if (getvGap() > -1) {
            getGridPane().setVgap(getvGap());
        }

        dataMgr.AddWildcardListener(getMinionID(), getNamespace(), (ObservableValue o, Object oldVal, Object newVal) -> {
            if (IsPaused()) {
                return;
            }

            String strVal = newVal.toString();
            String[] parts = strVal.split(":");
            if (parts.length > 1) // check to see if we have already created the widget
                {
                    String id = parts[0];
                    String Value = parts[1];
                    if (dataPointMap.containsKey(id.toLowerCase())) {
                        return; // already made one for this!
                    }
                    if (exclusionList.containsKey(id.toLowerCase())) {
                        return; // not wanted
                    }
                    dataPointMap.put(id.toLowerCase(), id); // add the key to the map don't care about the stored
                    // value, just the key
                    CreateDataWidget(id, Value); // didn't find it, so go make one
                }
        });
        SetupPeekaboo(dataMgr);
        SetupTaskAction();

        return true;
    }

    private GridWidget CreateDataWidget(String ID, String initialVal) {
        GridWidget objGrid = new GridWidget();
        TextWidget objValueWidget = new TextWidget();
        TextWidget objIDWidget = null;
        if (_ShowTitle) {
            objIDWidget = new TextWidget();
            objIDWidget.setMinionID(ID);
            objIDWidget.SetInitialValue(ID);
            objIDWidget.setRow(0);
            objIDWidget.setColumn(0);
            objGrid.AddWidget(objIDWidget);
            objGrid.setWidth(getWidth());
            objGrid.setHeight(getHeight());
        }

        objValueWidget.SetInitialValue(initialVal);
        objValueWidget.setNamespace(getNamespace());
        objValueWidget.setMinionID(ID);
        objValueWidget.setRow(1);
        objValueWidget.setColumn(0);

        objGrid.AddWidget(objValueWidget);

        objGrid.setRow(0);
        objGrid.setColumn(0);

        objGrid.Create(getGridPane(), _dataMgr); // will register ID and Value Widget
        objGrid.PerformPostCreateActions(getParentGridWidget(), false);
        objGrid.getStylableObject().setVisible(false);
        // _Grid.AddWidget(objGrid);
        dataPoint.add(new Pair<>(ID.toUpperCase(), Arrays.asList(objGrid, objIDWidget, objValueWidget)));
        SetupSort();
        return objGrid;
    }

    @Override
    public String[] GetCustomAttributes() {
        String[] attributes = {"hgap", "vgap"};
        return attributes;
    }

    public String getEvenBackgroundStyle() {
        return _EvenBackgroundStyle;
    }

    public String getEvenDataStyle() {
        return _EvenDataStyle;
    }

    public String getEvenIDStyle() {
        return _EvenIDStyle;
    }

    public String getOddDataStyle() {
        return _OddDataStyle;
    }

    public String getOddEvenBackgroundStyle() {
        return oddBackgroundStyle;
    }

    public String getOddIDStyle() {
        return _OddIDStyle;
    }

    // @Override
//    public ObservableList<String> getStylesheets()
//    {
//        return _GridWidget.getStylesheets();
//    }
//
//    @Override
//    public Node getStylableObject()
//    {
//        return _GridWidget.getStylableObject();
//    }
    public int getRowWidth() {
        return _RowWidth;
    }

    public boolean getShowTitle() {
        return _ShowTitle;
    }

    public int getSortCount() {
        synchronized (this) {
            return sortCount.get();
        }
    }

    public SortMode getSortMode() {
        return _SortMode;
    }

    @Override
    public void HandleWidgetSpecificAttributes(FrameworkNode widgetNode) {
        if (widgetNode.hasAttribute("hgap")) {
            try {
                sethGap(Integer.parseInt(widgetNode.getAttribute("hgap")));
                LOGGER.config("Setting hGap for QuickViewWidget :" + widgetNode.getAttribute("hgap"));
            } catch (NumberFormatException ex) {
                LOGGER.warning("hgap for QuickViewWidget invalid: " + widgetNode.getAttribute("hgap") + ".  Ignoring");
            }
        }
        if (widgetNode.hasAttribute("vgap")) {
            try {
                setvGap(Integer.parseInt(widgetNode.getAttribute("vgap")));
                LOGGER.config("Setting vGap for QuickViewWidget :" + widgetNode.getAttribute("vgap"));
            } catch (NumberFormatException ex) {
                LOGGER.warning("vgap for QuickViewWidget invalid: " + widgetNode.getAttribute("vgap") + ".  Ignoring");
            }
        }
        if (widgetNode.hasAttribute("Align")) {
            String str = widgetNode.getAttribute("Align");
            setAlignment(str);
        }

    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node) {
        if ("RowWidth".equalsIgnoreCase(node.getNodeName())) {
            String str = node.getTextContent();
            try {
                setRowWidth(Integer.parseInt(str));
                return true;
            } catch (NumberFormatException ex) {
                LOGGER.severe("Invalid <RowWidth> in QuickViewWidget Widget Definition File : " + str);
            }
        } else if ("ShowID".equalsIgnoreCase(node.getNodeName())) {
            setShowTitle(node.getBooleanValue());
            return true;
        } else if ("EvenBackgroundStyle".equalsIgnoreCase(node.getNodeName())) {
            setEvenBackgroundStyle(node.getTextContent());
            return true;
        } else if ("EvenIDStyle".equalsIgnoreCase(node.getNodeName())) {
            setEvenIDStyle(node.getTextContent());
            return true;
        } else if ("EvenDataStyle".equalsIgnoreCase(node.getNodeName())) {
            setEvenDataStyle(node.getTextContent());
            return true;
        } else if ("OddBackgroundStyle".equalsIgnoreCase(node.getNodeName())) {
            setOddBackgroundStyle(node.getTextContent());
            return true;
        } else if ("OddIDStyle".equalsIgnoreCase(node.getNodeName())) {
            setOddIDStyle(node.getTextContent());
            return true;
        } else if ("OddDataStyle".equalsIgnoreCase(node.getNodeName())) {
            setOddDataStyle(node.getTextContent());
            return true;
        } else if ("ExcludeList".equalsIgnoreCase(node.getNodeName())) {
            String strVal = node.getTextContent();
            for (String exVal : strVal.split(":")) {
                String strClean = exVal.replaceAll("\\s+", ""); // get rid of invalid chars
                LOGGER.info("Addeding QuickViewWidget Exclusion: " + strClean);
                exclusionList.put(strClean.toLowerCase(), "Not Needed");
            }
            for (String exVal : strVal.split(";")) {
                String strClean = exVal.replaceAll("\\s+", ""); // get rid of invalid chars
                LOGGER.info("Addeding QuickViewWidget Exclusion: " + strClean);
                exclusionList.put(strClean.toLowerCase(), "Not Needed");
            }
            return true;
        } else if ("Order".equalsIgnoreCase(node.getNodeName())) {
            String strVal = node.getTextContent();
            if (strVal.equalsIgnoreCase(QuickViewWidget.SortMode.Ascending.toString())) {
                setSortMode(QuickViewWidget.SortMode.Ascending);
            } else if (strVal.equalsIgnoreCase(QuickViewWidget.SortMode.Descending.toString())) {
                setSortMode(QuickViewWidget.SortMode.Descending);
            } else if (strVal.equalsIgnoreCase(QuickViewWidget.SortMode.None.toString())) {
                setSortMode(QuickViewWidget.SortMode.None);
            } else {
                LOGGER.warning("Invalid <Order> Tag in QuickViewWidget Widget File. " + strVal);
            }
            return true;
        }
        return false;
    }

    private int incrementSortCount() {
        synchronized (this) {
            return sortCount.incrementAndGet();
        }
    }

    @Override
    public void PerformSort() {
        if (getSortCount() > 0) {
            setIncrementSortCount(0);
            Sort();
        }
    }

    public void setEvenBackgroundStyle(String evenBackgroundStyle) {
        this._EvenBackgroundStyle = evenBackgroundStyle;
    }

    public void setEvenDataStyle(String evenDataStyle) {
        this._EvenDataStyle = evenDataStyle;
    }

    public void setEvenIDStyle(String evenIDStyle) {
        this._EvenIDStyle = evenIDStyle;
    }

    private void setIncrementSortCount(int newVal) {
        synchronized (this) {
            sortCount.set(newVal);
        }
    }

    public void setOddBackgroundStyle(String oddEvenBackgroundStyle) {
        this.oddBackgroundStyle = oddEvenBackgroundStyle;
    }

    public void setOddDataStyle(String oddDataStyle) {
        this._OddDataStyle = oddDataStyle;
    }

    public void setOddIDStyle(String oddIDStyle) {
        this._OddIDStyle = oddIDStyle;
    }

    public void setRowWidth(int rowWidth) {
        this._RowWidth = rowWidth;
    }

    public void setShowTitle(boolean showTitle) {
        this._ShowTitle = showTitle;
    }

    public void setSortMode(SortMode sortMode) {
        this._SortMode = sortMode;
    }

    private void SetStyle(boolean Odd, GridWidget objGrid, TextWidget objIDWidget, TextWidget objValueWidget) {
        if (Odd) {
            objGrid.setStyleOverride(Arrays.asList(oddBackgroundStyle));
            if (null != objIDWidget) {
                objIDWidget.setStyleOverride(Arrays.asList(_OddIDStyle));
            }
            objValueWidget.setStyleOverride(Arrays.asList(_OddDataStyle));
        } else {
            objGrid.setStyleOverride(Arrays.asList(_EvenBackgroundStyle));
            if (null != objIDWidget) {
                objIDWidget.setStyleOverride(Arrays.asList(_EvenIDStyle));
            }
            objValueWidget.setStyleOverride(Arrays.asList(_EvenDataStyle));
        }
        objGrid.ApplyOverrides();
        if (null != objIDWidget) {
            objIDWidget.ApplyOverrides();
        }
        objValueWidget.ApplyOverrides();
    }

    private void SetupSort() // create a deferred task to do the actual sorting, otherwise when you have 5000
    // datapoints it overlaods the gui
    {
        if (incrementSortCount() < 5) // don't need to stack them on if already in the queue
        {
            QuickViewWidgetSortTask objTask = new QuickViewWidgetSortTask(this);
            TaskManager.getTaskManager().AddPostponedTask(objTask, 500); // just every .5 secs do a sort
        }
    }

    /**
     * Go through and sort the goodies alphabetically
     */
    private void Sort() {
        if (getSortMode() == SortMode.Ascending) {
            Collections.sort(dataPoint, new Comparator<Pair<String, List<Object>>>() {
                @Override
                public int compare(Pair<String, List<Object>> s1, Pair<String, List<Object>> s2) // do alphabetical sort
                {
                    NaturalComparator naturalCompare = new NaturalComparator();
                    return naturalCompare.compare(s1.getKey(), s2.getKey());
                }
            });
        } else if (getSortMode() == SortMode.Descending) {
            Collections.sort(dataPoint, new Comparator<Pair<String, List<Object>>>() {
                @Override
                public int compare(Pair<String, List<Object>> s1, Pair<String, List<Object>> s2) // do alphabetical sort
                {
                    NaturalComparator naturalCompare = new NaturalComparator();
                    return naturalCompare.compare(s2.getKey(), s1.getKey());
                }
            });
        }

        int row = 0;
        int column = 0;
        boolean odd = true;

        getGridPane().getChildren().clear();

        for (Pair<String, List<Object>> pair : dataPoint) {
            GridWidget objGrid = (GridWidget) pair.getValue().get(0);
            TextWidget objID = (TextWidget) pair.getValue().get(1);
            TextWidget objValue = (TextWidget) pair.getValue().get(2);

            objGrid.setRow(row);
            objGrid.setColumn(column);

            // it's not getting added to the right place, its not the right grid...'
            getGridPane().add(objGrid.getStylableObject(), column, row);
            objGrid.getStylableObject().setVisible(true);

            SetStyle(odd, objGrid, objID, objValue);
            odd = !odd;

            if (++column >= getRowWidth()) {
                row++;
                column = 0;
            }
        }
    }
}
