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
public class QuickViewLCDWidget extends GridWidget implements IQuickViewSort {

    public enum SortMode {

        None, Ascending, Descending
    }

    ;

    private SortMode _SortMode = SortMode.Descending;
    private int _RowWidth = 5;
    private String _EvenBackgroundStyle = "-fx-background-color:green";
    private String evenStyle;
    private String evenStyleID = "";

    private String oddBackgroundStyle = "fx-background-color:grey";
    private String oddStyle;
    private String oddStyleID = "";

    //    private GridWidget _GridWidget;
    private List<Pair<String, SteelLCDWidget>> dataPoint; // Minion ID, LCDWidget
    private DataManager _dataMgr;
    // private int _hGap, _vGap;
    private HashMap<String, String> exclusionList;
    private HashMap<String, String> dataPointMap;
    private AtomicInteger sortCount;

    public QuickViewLCDWidget() {
        dataPoint = new ArrayList<>();
//        _hGap = -1;
//        _vGap = -1;
        exclusionList = new HashMap<>(); // For those we do not want to show
        dataPointMap = new HashMap<>(); // for quick lookup as new data comes in
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
            if (parts.length > 1) {
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

    private SteelLCDWidget CreateDataWidget(String ID, String initialVal) {
        SteelLCDWidget objWidget = new SteelLCDWidget();

        objWidget.setTextMode(false);
        objWidget.setMinionID(ID);
        objWidget.setNamespace(getNamespace());
        objWidget.setTitle(ID);
        objWidget.SetValue(initialVal);
        objWidget.setRow(0);
        objWidget.setColumn(0);
        objWidget.setDecimalPlaces(2);
        objWidget.setMaxValue(999999999);

        objWidget.setWidgetInformation(getDefinintionFileDirectory(), "", "huh");

        objWidget.getStylableObject().setVisible(false); // becomes visible when sorted
        objWidget.setWidth(getWidth());
        objWidget.setHeight(getHeight());

        objWidget.Create(getGridPane(), _dataMgr);
        objWidget.PerformPostCreateActions(getParentGridWidget(), false);

        dataPoint.add(new Pair<>(ID.toUpperCase(), objWidget));
        SetupSort();

        return objWidget;
    }

    @Override
    public String[] GetCustomAttributes() {
        String[] attributes = {"hgap", "vgap"};
        return attributes;
    }

    public String getEvenBackgroundStyle() {
        return _EvenBackgroundStyle;
    }

    public String getEvenStyle() {
        return evenStyle;
    }

    public String getEvenStyleID() {
        return evenStyleID;
    }

    public String getOddEvenBackgroundStyle() {
        return oddBackgroundStyle;
    }

    public String getOddStyle() {
        return oddStyle;
    }

    public String getOddStyleID() {
        return oddStyleID;
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
                LOGGER.severe("Invalid <RowWidth> in QuickViewLCDWidget Widget Definition File : " + str);
            }
        } else if ("EvenBackgroundStyle".equalsIgnoreCase(node.getNodeName())) {
            setEvenBackgroundStyle(node.getTextContent());
            return true;
        } else if ("EvenStyle".equalsIgnoreCase(node.getNodeName())) {
            String ID = "";
            if (node.hasAttribute("ID")) {
                ID = node.getAttribute("ID");
            }
            setEvenStyle(ID, node.getTextContent());
            return true;
        } else if ("OddBackgroundStyle".equalsIgnoreCase(node.getNodeName())) {
            setOddBackgroundStyle(node.getTextContent());
            return true;
        } else if ("OddStyle".equalsIgnoreCase(node.getNodeName())) {
            String ID = "";
            if (node.hasAttribute("ID")) {
                ID = node.getAttribute("ID");
            }
            setOddStyle(ID, node.getTextContent());
            return true;
        } else if ("Order".equalsIgnoreCase(node.getNodeName())) {
            String strVal = node.getTextContent();
            if (strVal.equalsIgnoreCase(QuickViewWidget.SortMode.Ascending.toString())) {
                setSortMode(QuickViewLCDWidget.SortMode.Ascending);
            } else if (strVal.equalsIgnoreCase(QuickViewWidget.SortMode.Descending.toString())) {
                setSortMode(QuickViewLCDWidget.SortMode.Descending);
            } else if (strVal.equalsIgnoreCase(QuickViewWidget.SortMode.None.toString())) {
                setSortMode(QuickViewLCDWidget.SortMode.None);
            } else {
                LOGGER.warning("Invalid <Order> Tag in QuickViewLCDWidget Widget File. " + strVal);
            }
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

    public void setEvenStyle(String ID, String file) {
        evenStyleID = ID;
        evenStyle = file;
    }

    private void setIncrementSortCount(int newVal) {
        synchronized (this) {
            sortCount.set(newVal);
        }
    }

    public void setOddBackgroundStyle(String oddEvenBackgroundStyle) {
        this.oddBackgroundStyle = oddEvenBackgroundStyle;
    }

    public void setOddStyle(String ID, String File) {
        oddStyleID = ID;
        oddStyle = File;
    }

    public void setRowWidth(int rowWidth) {
        this._RowWidth = rowWidth;
    }

    public void setSortMode(SortMode sortMode) {
        this._SortMode = sortMode;
    }

    private void SetStyle(boolean Odd, SteelLCDWidget objWidget) {
        if (Odd) {
            objWidget.setBaseCSSFilename(oddStyle);
            objWidget.setStyleID(oddStyleID);
        } else {
            objWidget.setBaseCSSFilename(evenStyle);
            objWidget.setStyleID(evenStyleID);
        }
        objWidget.ApplyCSS();
        objWidget.ApplyOverrides();

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
            Collections.sort(dataPoint, new Comparator<Pair<String, SteelLCDWidget>>() {
                @Override
                public int compare(Pair<String, SteelLCDWidget> s1, Pair<String, SteelLCDWidget> s2) // do alphabetical
                // sort
                {
                    NaturalComparator naturalCompare = new NaturalComparator();
                    return naturalCompare.compare(s1.getKey(), s2.getKey());
                }
            });
        } else if (getSortMode() == SortMode.Descending) {
            Collections.sort(dataPoint, new Comparator<Pair<String, SteelLCDWidget>>() {
                @Override
                public int compare(Pair<String, SteelLCDWidget> s1, Pair<String, SteelLCDWidget> s2) // do alphabetical
                // sort
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

        for (Pair<String, SteelLCDWidget> pair : dataPoint) {
            SteelLCDWidget objLCD = pair.getValue();

            objLCD.setRow(row);
            objLCD.setColumn(column);

            // it's not getting added to the right place, its not the right grid...'
            getGridPane().add(objLCD.getStylableObject(), column, row);

            SetStyle(odd, objLCD);
            odd = !odd;
            objLCD.getStylableObject().setVisible(true);

            if (++column >= getRowWidth()) {
                row++;
                column = 0;
            }
        }
    }

}
