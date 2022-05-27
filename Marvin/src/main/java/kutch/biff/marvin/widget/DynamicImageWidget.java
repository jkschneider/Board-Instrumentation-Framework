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
import java.util.HashMap;

import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.task.MarvinTask;
import kutch.biff.marvin.utility.CircularList;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.Utility;
import kutch.biff.marvin.widget.dynamicgrid.DynamicTransition;

/**
 * Displays an image, but it can be changed. Is a list of images each with a
 * 'nam' associated with it. DataPoint comes in with a name and is displayed.
 *
 * @author Patrick Kutch
 */
public class DynamicImageWidget extends StaticImageWidget {

    private static int autoAdvanceImageNumber;
    private HashMap<String, String> imageFilenames;
    private HashMap<String, DynamicTransition> transitionMap;
    private HashMap<String, ImageView> imageViewMap;
    private HashMap<String, Long> montorMap;
    // private ArrayList<String> _ImageFileNames;
    private CircularList<String> listID;
    private String currentKey;
    private boolean autoAdvance;
    private boolean autoLoopWithAdvance;
    private int autoAdvanceInterval;
    private ImageView activeView;
    private GridPane basePane;
    private int monitorInterval = 500;
    private final HashMap<String, String> taskMap;

    public DynamicImageWidget() {
        imageFilenames = new HashMap<>();
        transitionMap = new HashMap<>();
        taskMap = new HashMap<>();
        imageViewMap = new HashMap<>();
        montorMap = new HashMap<>();
//        _ImageFileNames = new ArrayList<>();
        currentKey = null;
        listID = new CircularList<>();
        setDefaultIsSquare(false);
        autoAdvance = false;
        autoLoopWithAdvance = false;
        autoAdvanceInterval = 0;
        activeView = null;
        _ImageView = null;
        basePane = new GridPane();

    }

    @Override
    protected boolean ApplyCSS() {
        boolean fRet = true;
        if (null != GetCSS_File()) {
            // getStylesheets().clear();

            LOGGER.config("Applying Stylesheet: " + GetCSS_File() + " to Widget [" + _DefinitionFile + "]");
            // This was a .add(), but changed to Sett all as there was kind of
            // memory leak when I changed style via Minion or MarvinTasks...
            fRet = getStylesheets().setAll(GetCSS_File());
            if (false == fRet) {
                LOGGER.severe("Failed to apply Stylesheet " + GetCSS_File());
                return false;
            }
        }
        if (null != getStyleID()) {
            getStylableObject().setId(getStyleID());
        }

        for (String key : imageFilenames.keySet()) {
            Node objStylable = imageViewMap.get(key);
            fRet = ApplyStyleOverrides(objStylable, getStyleOverride());
        }
        return fRet;
    }

    @Override
    public void ConfigureAlignment() {
        for (String key : imageFilenames.keySet()) {
            Node objStylable = imageViewMap.get(key);
            if (objStylable != null) {
                GridPane.setValignment(objStylable, getVerticalPosition());
                GridPane.setHalignment(objStylable, getHorizontalPosition());
            }
        }
    }

    @Override
    protected void ConfigureDimentions() {
        for (String key : imageFilenames.keySet()) {
            if (getHeight() > 0) {
                imageViewMap.get(key).setFitHeight(getHeight());
            }
            if (getWidth() > 0) {
                imageViewMap.get(key).setFitWidth(getWidth());

            }
        }
    }

    /**
     * @param pane
     * @param dataMgr
     * @return
     */
    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        if (setupImages()) {
            ConfigureAlignment();
            for (String key : this.imageFilenames.keySet()) {
                ImageView obj = imageViewMap.get(key);
                basePane.add(obj, 0, 0);
            }
            pane.add(basePane, getColumn(), getRow(), getColumnSpan(), getRowSpan());
            SetupPeekaboo(dataMgr);

            if (autoAdvance) {
                if (null == getMinionID() || null == getNamespace()) {
                    String id = Integer.toBinaryString(DynamicImageWidget._AutoAdvanceImageNumber);
                    DynamicImageWidget._AutoAdvanceImageNumber++;

                    if (null == getMinionID()) {
                        setMinionID(id);
                    }
                    if (null == getNamespace()) {
                        setNamespace(id);
                    }
                }
                MarvinTask mt = new MarvinTask();
                mt.AddDataset(getMinionID(), getNamespace(), "Next");
                TASKMAN.AddPostponedTask(mt, autoAdvanceInterval);
            }
            if (!montorMap.isEmpty()) {
                MarvinTask mt = new MarvinTask();
                mt.AddDataset(getMinionID(), getNamespace(), "Monitor");
                TASKMAN.AddPostponedTask(mt, monitorInterval);
            }

            DynamicImageWidget objDynaImg = this;

            dataMgr.AddListener(getMinionID(), getNamespace(), (ObservableValue<?> o, Object oldVal, Object newVal) -> {
                boolean changeOcurred = false;
                if (IsPaused()) {
                    return;
                }

                String strVal = newVal.toString().replaceAll("(\\r|\\n)", "");

                String key;

                if ("Next".equalsIgnoreCase(strVal)) // go to next image in the list
                    {
                        key = listID.GetNext();

                    } else if ("Previous".equalsIgnoreCase(strVal)) // go to previous image in the list
                    {
                        key = listID.GetPrevious();
                    } else if ("Monitor".equalsIgnoreCase(strVal)) {
                    changeOcurred = MonitorForFilechange();
                    if (changeOcurred) {
                        key = currentKey;

                    }
                    key = currentKey;

                    MarvinTask mt = new MarvinTask();
                    mt.AddDataset(getMinionID(), getNamespace(), "Monitor");
                    TASKMAN.AddPostponedTask(mt, monitorInterval);
                } else {
                    key = strVal; // expecting an ID
                    listID.get(key); // just to keep next/prev alignment
                }
                key = key.toLowerCase();
                if (imageFilenames.containsKey(key)) {
                    if (!key.equalsIgnoreCase(currentKey) || changeOcurred) // no reason to re-load if it is
                        // already loaded
                        {
                            DynamicTransition objTransition = null;

                            ImageView startView = null;
                            ImageView nextView = null;

                            if (transitionMap.containsKey(currentKey)) {
                                DynamicTransition objCurrentTransition = transitionMap.get(currentKey);
                                if (objCurrentTransition.stillPlaying()) {
                                    objCurrentTransition.stopTransition();
                                }
                            }

                            if (transitionMap.containsKey(key)) {
                                objTransition = transitionMap.get(key);
                                startView = imageViewMap.get(currentKey);
                            } else {
                                imageViewMap.get(currentKey).setVisible(false);
                            }

                            currentKey = key;

                            if (null != objTransition && null != startView) {
                                nextView = imageViewMap.get(currentKey);
                                objTransition.Transition(objDynaImg, startView, nextView);
                            } else {
                                imageViewMap.get(currentKey).setVisible(true);
                            }
                        }
                } else {
                    LOGGER.warning("Received unknown ID: [" + strVal + "] for dynamic Image#" + getName() + ": ["
                            + getNamespace() + ":" + getMinionID() + "]");
                    return;
                }
                if (autoAdvance) {
                    if (!autoLoopWithAdvance && listID.IsLast(key)) {
                        autoAdvance = false;
                        return;
                    }
                    MarvinTask mt = new MarvinTask();
                    mt.AddDataset(getMinionID(), getNamespace(), "Next");
                    TASKMAN.AddPostponedTask(mt, autoAdvanceInterval);
                }
            });
            SetupTaskAction();

            return ApplyCSS();
        }
        return false;
    }

    public GridPane GetContainerPane() {
        return basePane;
    }

    @Override
    public javafx.scene.Node getStylableObject() {
//        return _Pane;
        return null;
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node) {
        String Id = null;
        String fileName = null;

        if ("Initial".equalsIgnoreCase(node.getNodeName())) {
            Utility.ValidateAttributes(new String[]{"ID"}, node);
            if (node.hasAttribute("ID")) {
                currentKey = node.getAttribute("ID").toLowerCase();
                return true;
            } else {
                LOGGER.severe("Dynamic Image Widget incorrectly defined Initial Image, no ID.");
                return false;
            }
        }
        if ("AutoAdvance".equalsIgnoreCase(node.getNodeName())) {
            /*
             * <AutoAdvance Frequency='1000' Loop='False'/>
             */
            if (node.hasAttribute("Frequency")) {
                autoAdvanceInterval = node.getIntegerAttribute("Frequency", -1);
                if (autoAdvanceInterval < 100) {
                    LOGGER.severe("Frequency specified for DynamicImage is invalid: " + node.getAttribute("Frequency"));
                    return false;
                }

                if (node.hasAttribute("Loop")) {
                    autoLoopWithAdvance = node.getBooleanAttribute("Loop");
                }
                autoAdvance = true;
                return true;
            }
            return false;
        }

        if ("Image".equalsIgnoreCase(node.getNodeName())) {
            Utility.ValidateAttributes(new String[]{"Source", "ID", "Monitor", "Task"}, node);
            if (node.hasAttribute("Source")) {
                fileName = node.getAttribute("Source");
            } else {
                LOGGER.severe("Dynamic Image Widget has no Source for Image");
                return false;
            }
            if (node.hasAttribute("ID")) {
                Id = node.getAttribute("ID");

                if (imageFilenames.containsKey(Id.toLowerCase())) {
                    LOGGER.severe("Dynamic Image Widget has repeated Image ID: " + Id);
                    return false;
                }
                Id = Id.toLowerCase();
            } else {
                LOGGER.severe("Dynamic Image Widget has no ID for Image");
                return false;
            }
            if (node.hasAttribute("Task")) {
                taskMap.put(Id, node.getAttribute("Task"));
            }
            String fname = convertToFileOSSpecific(fileName);
            if (null == fname) {
                return false;
            }
            File file = new File(fname);

            if (file.exists()) {
                String fn = "file:" + fname;
                // Image img = new Image(fn);
                imageFilenames.put(Id, fn);
                listID.add(Id);
                if (node.hasAttribute("Monitor")) {
                    if (node.getBooleanAttribute("Monitor")) {
                        montorMap.put(Id, file.lastModified());
                    }
                }

            } else {
                LOGGER.severe("Dynamic Image Widget - missing Image file: " + fileName);
                return false;
            }
            if (node.hasChild("Transition")) {
                DynamicTransition objTransition = DynamicTransition.ReadTransitionInformation(node);
                if (null != objTransition) {
                    this.transitionMap.put(Id, objTransition);
                }
            }
        }

        return true;
    }

    private boolean MonitorForFilechange() {
        boolean retVal = false;
        for (String Id : montorMap.keySet()) {
            File fp = new File(imageFilenames.get(Id).substring("file:".length())); // is stored as url, so skip the
            // 1st part
            if (fp.lastModified() != montorMap.get(Id)) {
                montorMap.put(Id, fp.lastModified());
                LOGGER.info("Monitoring " + imageFilenames.get(Id) + " updated");
                ImageView objImageView = new ImageView(imageFilenames.get(Id));
                objImageView.setPreserveRatio(getPreserveRatio());
                objImageView.setSmooth(true);
                objImageView.setPickOnBounds(!GetClickThroughTransparentRegion());
                objImageView.setVisible(imageViewMap.get(Id).isVisible());
                objImageView.setFitWidth(imageViewMap.get(Id).getFitWidth());
                objImageView.setFitHeight(imageViewMap.get(Id).getFitHeight());
                basePane.getChildren().remove(imageViewMap.get(Id));

                imageViewMap.put(Id, objImageView);
                basePane.add(objImageView, 0, 0);

                retVal = true;
            }
        }
        return retVal;
    }

    @Override
    public void OnResumed() {
        if (autoAdvance) {
            if (null == getMinionID() || null == getNamespace()) {
                String ID = Integer.toBinaryString(DynamicImageWidget._AutoAdvanceImageNumber);
                DynamicImageWidget._AutoAdvanceImageNumber++;

                if (null == getMinionID()) {
                    setMinionID(ID);
                }
                if (null == getNamespace()) {
                    setNamespace(ID);
                }
            }
            MarvinTask mt = new MarvinTask();
            mt.AddDataset(getMinionID(), getNamespace(), "Next");
            TASKMAN.AddPostponedTask(mt, autoAdvanceInterval);
        }
    }

    @Override
    public boolean PerformPostCreateActions(GridWidget objParentGrid, boolean updateToolTipOnly) {
        if (updateToolTipOnly) {
            if (CONFIG.isDebugMode()) {
                _ToolTip = this.toString();
            }
            if (_ToolTip != null && null != getStylableObject()) {
                HandleToolTipInit();
                for (String key : imageFilenames.keySet()) {
                    ImageView objView = imageViewMap.get(key);
                    Tooltip.install(objView, _objToolTip);
                }
            }
            return true;
        }

        _WidgetParentGridWidget = objParentGrid;
        if (CONFIG.isDebugMode()) {
            _ToolTip = this.toString();
        }
        if (_ToolTip != null) {
            HandleToolTipInit();
            for (String key : imageFilenames.keySet()) {
                ImageView objView = imageViewMap.get(key);
                Tooltip.install(objView, _objToolTip);
            }
        }
        FireDefaultPeekaboo();

        return handlePercentageDimentions();
        // return true;
    }

    private boolean setupImages() {
        for (String key : imageFilenames.keySet()) {
            ImageView objImageView = new ImageView(imageFilenames.get(key));
            objImageView.setPreserveRatio(getPreserveRatio());
            objImageView.setSmooth(true);
            objImageView.setPickOnBounds(!GetClickThroughTransparentRegion());
            objImageView.setVisible(false);
            imageViewMap.put(key, objImageView);
        }

        if (currentKey == null) {
            LOGGER.severe("No Initial Image setup for Dynamic Image Widget.");
            return false;
        } else if (imageFilenames.containsKey(currentKey)) {
            activeView = imageViewMap.get(currentKey);
            activeView.setVisible(true);
        } else {
            LOGGER.severe("Initial key not valid for dynamic image widget: " + currentKey);
            return false;
        }
        ConfigureDimentions();
        return true;
    }

    @Override
    public EventHandler<MouseEvent> SetupTaskAction() {
        BaseWidget objWidget = this;
        if (null != getTaskID() || CONFIG.isDebugMode()) // only do if a task to setup, or if debug mode
        {
            EventHandler<MouseEvent> eh = (MouseEvent event) -> {
                if (event.isShiftDown() && CONFIG.isDebugMode()) {
                    LOGGER.info(objWidget.toString(true));
                } else if (taskMap.containsKey(currentKey) && CONFIG.getAllowTasks()) {
                    TASKMAN.PerformTask(taskMap.get(currentKey));
                } else if (null != getTaskID() && CONFIG.getAllowTasks()) {
                    TASKMAN.PerformTask(getTaskID());
                }
            };
            for (String key : imageFilenames.keySet()) {
                imageViewMap.get(key).setOnMouseClicked(eh);
            }

            return eh;
        }
        return null;
    }
}
