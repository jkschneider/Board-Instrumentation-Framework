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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javafx.application.Platform;
import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.network.Client;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.Utility;

/**
 * @author Patrick Kutch
 */
public class TaskManager {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static TaskManager taskManager;
    private static ArrayList<String> onStartupList;
    private static ArrayList<String> onConnectedList;

    private static boolean warningAboutManyTasksSent;

    public static TaskManager getTaskManager() {
        if (null == taskManager) {
            taskManager = new TaskManager();
        }
        return taskManager;
    }

    private ConcurrentHashMap<String, TaskList> taskMap;
    private ConcurrentHashMap<String, Client> clientMap;
    private DataManager _DataMgr;
    private final ArrayList<String> deferredTasks;
    private final ArrayList<String> timedTasks;
    private final ArrayList<PostponedTask> postponedTasks;
    private final ArrayList<PostponedTask> postponedTasksNew;
    private final ArrayList<ITask> postponedTaskObjectThatMustBeRunInGuiThreadList;
    private long tasksPerformed;

    private int loopsWithManyTasks;

    public TaskManager() {
        taskMap = new ConcurrentHashMap<>();
        _DataMgr = null;
        clientMap = null;
        timedTasks = new ArrayList<>();
        deferredTasks = new ArrayList<>();
        postponedTasks = new ArrayList<>();
        postponedTasksNew = new ArrayList<>();
        postponedTaskObjectThatMustBeRunInGuiThreadList = new ArrayList<>();
        tasksPerformed = 0;
        loopsWithManyTasks = 0;
    }

    // this is where a task comes in on a worker thread (like remote marvin)
    public void AddDeferredTask(String newTask) {
        if (null == newTask) {
            LOGGER.severe("Sent null task ID to add Deferred Task");
            return;
        }
        synchronized (deferredTasks) {
            deferredTasks.add(newTask);
        }
    }

    public void AddDeferredTaskObject(ITask objTask) {
        synchronized (postponedTaskObjectThatMustBeRunInGuiThreadList) {
            postponedTaskObjectThatMustBeRunInGuiThreadList.add(objTask);
        }
    }

    /**
     * Puts a new task in the internal collection
     *
     * @param TaskID  Unique ID
     * @param objTask the task object
     * @return true if success
     */
    private boolean AddNewTask(String TaskID, TaskList objTask, boolean onStartup, boolean onConnected) {
        if (null == taskMap) {
            taskMap = new ConcurrentHashMap<>();
        }

        if (false == taskMap.containsKey(TaskID.toUpperCase())) {
            taskMap.put(TaskID.toUpperCase(), objTask);
            if (onStartup) {
                if (null == onStartupList) {
                    onStartupList = new ArrayList<>();
                }
                onStartupList.add(TaskID);
            }
            if (onConnected) {
                if (null == onConnectedList) {
                    onConnectedList = new ArrayList<>();
                }
                onConnectedList.add(TaskID);
            }

            if (objTask.GetInterval() > 0.0) {
                timedTasks.add(TaskID);
            }

            return true;
        }
        LOGGER.config("Duplicate Task with ID of " + TaskID + " found. Ignoring.");
        return true;
    }

    public void AddOnStartupTask(String TaskID, BaseTask objTaskToPerform) {
        if (false == taskMap.containsKey(TaskID.toUpperCase())) {
            TaskList objTask = new TaskList();
            objTask.AddTaskItem(objTaskToPerform);
            taskMap.put(TaskID.toUpperCase(), objTask);
            if (null == onStartupList) {
                onStartupList = new ArrayList<>();
            }
            onStartupList.add(TaskID);
        }
    }

    public void AddPostponedTask(ITask objTask, long period) {
        synchronized (postponedTasks) {
            postponedTasks.add(new PostponedTask(objTask, period));
        }
    }

    // Sometimes there are postponed tasks to be added while processing postponed
    // tasks. Since you can't go and add stuff to the _PostponedTask list while it
    // is being processed, add to the temp one.
    public void AddPostponedTaskThreaded(ITask objTask, long Period) {
        synchronized (postponedTasksNew) {
            postponedTasksNew.add(new PostponedTask(objTask, Period));
        }
    }

    private ChainedTask BuildChainedTaskItem(String taskID, FrameworkNode taskNode) {
        if (taskNode.hasAttribute("ID")) {
            return new ChainedTask(taskNode.getAttribute("ID"));
        }

        return null;
    }

    private DataSetFileTask BuildDataSetFileTaskItem(String taskID, FrameworkNode taskNode) {
        /**
         * <TaskItem Type="DataSetFile" File="Foo.csv" DataRate="1000">
         * <Options RepeatCount="forever"> <!-- Forever, or count -->
         * <Range Min="0" Max="9.3"/> <Flux>10</Flux> <Flux>1%</Flux> </Options>
         * </TaskItem>
         */

        int dataRate;
        String fileName;

//	<TaskItem Type="DataSetFile" File="Foo.csv" DataRate="1000">
//			<Options RepeatCount="forever"> <!-- Forever, or count -->                
        if (false == taskNode.hasAttribute("File")) {
            LOGGER.severe("Task with ID: " + taskID + " contains an invalid definition. File Required");
            return null;
        }
        if (false == taskNode.hasAttribute("DataRate")) {
            LOGGER.severe("Task with ID: " + taskID + " contains an invalid definition. DataRate Required");
            return null;
        }

        fileName = taskNode.getAttribute("File");
        dataRate = taskNode.getIntegerAttribute("DataRate", -3232);
        if (dataRate < 100) {
            LOGGER.severe("Task with ID: " + taskID + " contains an invalid DataRate  of "
                    + taskNode.getAttribute("DataRate"));
            return null;

        }

        DataSetFileTask objTask = new DataSetFileTask(fileName, dataRate);

        if (taskNode.hasChild("Options")) {
            FrameworkNode optionsNode = taskNode.getChild("Options");
            if (optionsNode.hasAttribute("RepeatCount")) {
                int repeatCount = optionsNode.getIntegerAttribute("RepeatCount", 0);
                if (repeatCount < 1) {
                    LOGGER.severe("DataSetFileTask with ID: " + taskID + " contains an invalid RepeatCount  of "
                            + taskNode.getAttribute("RepeatCount"));
                    return null;
                }
                objTask.setRepeatCount(repeatCount);
            }
            if (optionsNode.hasChild("RandomFluxRange")) // <RandomFluxRange Lower=\"-.24\" Upper=\".4\"/>"))
            {
                FrameworkNode fluxNode = optionsNode.getChild("RandomFluxRange");
                Double lower;
                Double upper;
                if (!(fluxNode.hasAttribute("Lower") && fluxNode.hasAttribute("Upper"))) {
                    LOGGER.severe("DataSetFileTask with ID: " + taskID
                            + " specified RandomFlux range without Lower and Upper values");
                    return null;
                }

                lower = fluxNode.getDoubleAttribute("Lower", -323232);
                if (lower == 323232) {
                    LOGGER.severe("DataSetFileTask with ID: " + taskID + " specified Invalid RandomFlux lower: "
                            + fluxNode.getAttribute("Lower"));
                    return null;
                }
                upper = fluxNode.getDoubleAttribute("Upper", -323232);
                if (lower == 323232) {
                    LOGGER.severe("DataSetFileTask with ID: " + taskID + " specified Invalid RandomFlux upper: "
                            + fluxNode.getAttribute("upper"));
                    return null;
                }
                objTask.setFluxRange(lower, upper);
            }
        }

        return objTask;
    }

    private DesktopTask BuildDesktopTaskItem(String taskID, FrameworkNode taskNode) {
        /**
         * <TaskList ID="TestDesktop"> <TaskItem Type="Desktop">
         * <Document Action="Open">foo.html</Document> </TaskItem> </TaskList>
         *
         */
        DesktopTask objDesktopTask = new DesktopTask();

        for (FrameworkNode node : taskNode.getChildNodes()) {
            if ("Document".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"Action"}, node);

                if (!objDesktopTask.SetDocument(node.getTextContent())) {
                    LOGGER.severe("Desktop task has invalid document: " + node.getTextContent());
                    return null;
                }
                if (node.hasAttribute("Action")) {
                    if (!objDesktopTask.SetAction(node.getAttribute("Action"))) {
                        LOGGER.severe("Desktop task has invalid Actiont: " + node.getAttribute("Action"));
                    }
                } else {
                    objDesktopTask.SetAction("Open");
                }
            }
        }

        if (!objDesktopTask.isValid()) {
            objDesktopTask = null;
            LOGGER.severe("Task with ID: " + taskID + " contains an invalid Desktop Task.");
        }
        return objDesktopTask;
    }

    private LaunchProgramTask BuildLaunchApplicationTaskItem(String taskID, FrameworkNode taskNode) {
        /**
         * <TaskList ID="TestLaunch"> <TaskItem Type="LaunchProgram">
         * <Application>foo.exe</Document> <Param>1</Param> <Param>2</Param> </TaskItem>
         * </TaskList> *
         */
        LaunchProgramTask objRunProgramTask = new LaunchProgramTask();
        objRunProgramTask.setParams(GetParameters(taskNode));

        for (FrameworkNode node : taskNode.getChildNodes()) {
            if ("Application".equalsIgnoreCase(node.getNodeName())) {
                if (!objRunProgramTask.SetApplication(node.getTextContent())) {
                    LOGGER.severe("LaunchProgram task has invalid Application: " + node.getTextContent());
                    return null;
                }
            }
        }

        if (!objRunProgramTask.isValid()) {
            objRunProgramTask = null;
            LOGGER.severe("Task with ID: " + taskID + " contains an invalid LaunchProgram Task.");
        }
        return objRunProgramTask;
    }

    private MarvinAdminTask BuildMarvinAdminTaskItem(String taskID, FrameworkNode taskNode) {
        /**
         * * Example Task <TaskItem Type="MarvinAdmin">
         * <Task ID="TabChange" Data="DemoTab-Indicators"/> </TaskItem>
         */
        String id;
        String data;
        id = "";
        data = "";
        boolean taskFound = false;
        for (FrameworkNode node : taskNode.getChildNodes()) {
            if ("Task".equalsIgnoreCase(node.getNodeName())) {
                taskFound = true;
                Utility.ValidateAttributes(new String[]{"Data", "ID"}, node);

                if (node.hasAttribute("ID")) {
                    id = node.getAttribute("ID");
                } else {
                    LOGGER.severe("Invalid MarvinAdminTask with Task ID: " + taskID + " - no ID specified.");
                    return null;
                }
                if (node.hasAttribute("Data")) {
                    data = node.getAttribute("Data");
                } else {
                    data = "";
                }
            }
        }
        if (!taskFound) {
            LOGGER.severe("Invalid MarvinAdminTask with Task ID: " + taskID + " - no Task specified.");
            return null;
        }
        MarvinAdminTask objTask = new MarvinAdminTask(taskID, id, data);

        return objTask;
    }

    /**
     * Read the parameters from the xml file for a Oscar task
     *
     * @param taskID   ID of the task
     * @param taskNode XML node
     * @return A OscarTask object
     */
    private MarvinPlaybackTask BuildMarvinPlaybackTaskItem(String taskID, FrameworkNode taskNode) {
        // objMinionTask.setParams(GetParameters(taskNode));
        MarvinPlaybackTask pbT = null;
        String playerID = null;
        MarvinPlaybackTask.PlaybackAction action = MarvinPlaybackTask.PlaybackAction.INVALID;

        ArrayList<Parameter> params = GetParameters(taskNode);

        for (FrameworkNode node : taskNode.getChildNodes()) {
            if ("Task".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"PlayerID"}, node);
                if (node.hasAttribute("PlayerID")) {
                    playerID = node.getAttribute("PlayerID");
                } else {
                    LOGGER.severe("MarvinPlayback Task [" + taskID + "] defined without PlayerID");
                    return null;
                }
                String strAction = node.getTextContent();
                switch (strAction.toUpperCase()) {
                    case "PLAY":
                        action = MarvinPlaybackTask.PlaybackAction.PLAY;
                        break;

                    case "STOP":
                        action = MarvinPlaybackTask.PlaybackAction.STOP;
                        if (null != params) {
                            LOGGER.severe("MarvinPlayback Task [" + taskID
                                    + "] Invalid.  No Params allowed for task of " + strAction);
                            return null;
                        }
                        break;

                    case "PAUSE":
                        action = MarvinPlaybackTask.PlaybackAction.PAUSE;
                        if (null != params) {
                            LOGGER.severe("MarvinPlayback Task [" + taskID
                                    + "] Invalid.  No Params allowed for task of " + strAction);
                            return null;
                        }
                        break;

                    case "RESUME":
                        action = MarvinPlaybackTask.PlaybackAction.RESUME;
                        if (null != params) {
                            LOGGER.severe("MarvinPlayback Task [" + taskID
                                    + "] Invalid.  No Params allowed for task of " + strAction);
                            return null;
                        }
                        break;

                    case "SET OPTIONS":
                        action = MarvinPlaybackTask.PlaybackAction.SET_OPTIONS;
                        if (null == params) {
                            LOGGER.severe("MarvinPlayback Task [" + taskID
                                    + "] Invalid.  No Params specified for task of " + strAction);
                            return null;
                        }
                        break;

                    case "PLAY FILE":
                        action = MarvinPlaybackTask.PlaybackAction.PLAY_FILE;
                        if (null == params) {
                            LOGGER.severe("MarvinPlayback Task [" + taskID
                                    + "] Invalid.  No Params specified for task of " + strAction);
                            return null;
                        }
                        break;

                    case "LOAD FILE":
                        action = MarvinPlaybackTask.PlaybackAction.LOAD_FILE;
                        if (null == params) {
                            LOGGER.severe("MarvinPlayback Task [" + taskID
                                    + "] Invalid.  No Params specified for task of " + strAction);
                            return null;
                        }
                        break;
                    default:
                        LOGGER.severe("MarvinPlayback Task [" + taskID + "] defined with invalid Task: " + strAction);
                        return null;
                }
            }
        }
        pbT = new MarvinPlaybackTask(playerID, action);

        if (null != params) {
            for (Parameter p : params) {
                if (!p.toString().contains("=")) {
                    LOGGER.severe("MarvinPlayback Task [" + taskID + "] defined with invalid Param: " + p.toString());
                    return null;
                }
                String[] parts = p.toString().split("=");
                String what = parts[0];
                if ("File".equalsIgnoreCase(what)) {
                    if (!pbT.set_fileName(parts[1])) {
                        return null;
                    }
                } else if ("Speed".equalsIgnoreCase(what)) {
                    try {
                        pbT.set_Speed(Double.parseDouble(parts[1]));
                    } catch (Exception e) {
                        LOGGER.severe(
                                "MarvinPlayback Task [" + taskID + "] defined with invalid Param: " + p.toString());
                        return null;
                    }
                } else if ("Repeat".equalsIgnoreCase(what)) {
                    if ("true".equalsIgnoreCase(parts[1])) {
                        pbT.set_Loop(true);
                    } else if ("false".equalsIgnoreCase(parts[1])) {
                        pbT.set_Loop(false);
                    } else {
                        LOGGER.severe(
                                "MarvinPlayback Task [" + taskID + "] defined with invalid Param: " + p.toString());
                        return null;
                    }
                }
            }
        }

        return pbT;
    }

    /**
     * Read the parameters from the xml file for a Marvin (local) task
     *
     * @param taskID   ID of the task
     * @param taskNode XML node
     * @return A MarvinTask object
     */
    private MarvinTask BuildMarvinTaskItem(String taskID, FrameworkNode taskNode) {
        /**
         * * Example Task <TaskItem Type="Marvin">
         * <DataToInsert ID="Text" Namespace="PK Laptop" Data="First Text"/> </TaskItem>
         */
        MarvinTask objMarvinTask = new MarvinTask();
        objMarvinTask.setParams(GetParameters(taskNode));
        if (objMarvinTask.getParams() != null && objMarvinTask.getParams().isEmpty()) {
            return null; // means had problem loading params.
        }

        for (FrameworkNode node : taskNode.getChildNodes()) {
            if ("DataToInsert".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"Namespace", "ID", "Data"}, node);
                if (node.hasAttribute("Namespace") && node.hasAttribute("ID")) {
                    String strData = null;
                    if (node.hasAttribute("Data")) {
                        strData = node.getAttribute("Data");
                    } else {
                        for (FrameworkNode dataNode : node.getChildNodes()) {
                            if ("Data".equalsIgnoreCase(dataNode.getNodeName())) {
                                strData = dataNode.getTextContent();
                                break;
                            }
                        }
                    }
                    if (strData != null) {
                        objMarvinTask.AddDataset(node.getAttribute("ID"), node.getAttribute("Namespace"), strData);
                    } else {
                        LOGGER.severe("Task with ID: " + taskID + " contains an invalid Marvin Task - no Data defined");
                    }

                } else {
                    LOGGER.severe("Task with ID: " + taskID + " contains an invalid Marvin Task");
                    return null;
                }
            } else {
                LOGGER.severe("Task with ID: " + taskID + " contains an unknown tag: " + node.getNodeName());
            }
        }

        if (!objMarvinTask.isValid()) {
            objMarvinTask = null;
            LOGGER.severe("Task with ID: " + taskID + " contains an invalid Marvin Task");
        }

        return objMarvinTask;
    }

    private PulseTask BuildMathematicTaskTaskItem(String taskID, FrameworkNode taskNode) {
        /**
         * * Example Task <TaskItem Type="Mathematic">
         * <MarvinDataPoint ID="Text" Namespace="PK Laptop"/>
         * <Operation Value=".1">Add</Operation> Subtract, Multiply </TaskItem>
         */
        boolean errorLogged = false;
        MathematicTask objTask = new MathematicTask();

        for (FrameworkNode node : taskNode.getChildNodes()) {
            if ("MarvinDataPoint".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"Namespace", "ID"}, node);
                if (node.hasAttribute("Namespace") && node.hasAttribute("ID")) {
                    objTask.SetNamespaceAndID(node.getAttribute("Namespace"), node.getAttribute("ID"));
                } else {
                    LOGGER.severe("Task with ID: " + taskID
                            + " contains an invalid Mathematic Task - no Namespace and ID defined in MarvinDataPoint");
                    errorLogged = true;
                }
            } else if ("Operation".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"Value"}, node);
                if (node.hasAttribute("Value")) {
                    if (!objTask.setValue(node.getAttribute("Value"))) {
                        LOGGER.severe(
                                "Task with ID: " + taskID + " contains an invalid Mathematic Task Operation Value: "
                                        + node.getAttribute("Value"));
                        errorLogged = true;
                    }
                }
                String strOperationType = node.getTextContent();
                if (!objTask.SetOperation(strOperationType)) {
                    LOGGER.severe("Task with ID: " + taskID + " contains an invalid Mathematic Task Operation : "
                            + strOperationType);
                    errorLogged = true;
                }

            }
        }
        if (!objTask.isValid()) {
            objTask = null;
            if (!errorLogged) {
                LOGGER.severe("Task with ID: " + taskID
                        + " contains an invalid Arithmatic Task - no MarvinDataPoint defined");
            }
        }
        return objTask;
    }

    private PulseTask BuildDeltaValueTaskItem(String taskID, FrameworkNode taskNode) {
        /**
         * * Example Task <TaskItem Type="DeltaValue">
         * <MarvinDataPoint ID="AppStartupSeconds" Namespace="PK Laptop"/>
         * <MarvinDataPoint ID="Currseconds" Namespace="PK Laptop"/>
         * <Operation ID="Uptime" Namespace="PK Laptop">Delta | DeltaPercent</Operation>
         * </TaskItem>
         */
        boolean errorLogged = false;
        DeltaValueTask objTask = new DeltaValueTask();

        boolean firstDPFound = false;
        boolean secondDPFound = false;
        for (FrameworkNode node : taskNode.getChildNodes()) {
            if ("MarvinDataPoint".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"Namespace", "ID"}, node);
                if (node.hasAttribute("Namespace") && node.hasAttribute("ID")) {
                    if (!firstDPFound) {
                        firstDPFound = true;
                        objTask.SetFirstDatapoint(node.getAttribute("Namespace"), node.getAttribute("ID"));
                    } else if (!secondDPFound) {
                        secondDPFound = true;
                        objTask.SetSecondDatapoint(node.getAttribute("Namespace"), node.getAttribute("ID"));
                    } else {
                        LOGGER.severe("Task with ID: " + taskID
                                + " contains an invalid Delta Task - more than 2 MarvinDataPoints defined. Ignoring extras");
                        errorLogged = true;
                    }
                } else {
                    LOGGER.severe("Task with ID: " + taskID
                            + " contains an invalid Mathematic Task - no Namespace and ID defined in MarvinDataPoint");
                    errorLogged = true;
                }
            } else if ("Operation".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"Namespace", "ID"}, node);
                if (node.hasAttribute("Namespace") && node.hasAttribute("ID")) {
                    objTask.SetNamespaceAndID(node.getAttribute("Namespace"), node.getAttribute("ID"));
                } else {
                    LOGGER.severe(
                            "Task with ID: " + taskID + " contains an invalid Delta Task Operation.  Namespace and ID required ");
                    errorLogged = true;
                }

                String strOperationType = node.getTextContent();
                if (!objTask.SetOperation(strOperationType)) {
                    LOGGER.severe("Task with ID: " + taskID + " contains an invalid Delta Task Task Operation : "
                            + strOperationType);
                    errorLogged = true;
                }
            }
        }
        if (!objTask.isValid()) {
            objTask = null;
            if (!errorLogged) {
                LOGGER.severe("Task with ID: " + taskID
                        + " contains an invalid Delta Task - no MarvinDataPoint defined");
            }
        }
        return objTask;
    }


    /**
     * Read the parameters from the xml file for a Minion task
     *
     * @param taskID   ID of the task
     * @param taskNode XML node
     * @return A MinionTask object
     */
    private MinionTask BuildMinionTaskItem(String taskID, FrameworkNode taskNode) {
        /**
         * * Example Task <TaskItem Type="Minion">
         * <Actor Namespace="fubar" ID="EnableRSS"/> <Param>16</Param>
         */

        MinionTask objMinionTask = new MinionTask();
        objMinionTask.setParams(GetParameters(taskNode));
        if (objMinionTask.getParams() != null && objMinionTask.getParams().isEmpty()) {
            return null; // means had problem loading params.
        }

        for (FrameworkNode node : taskNode.getChildNodes()) {
            if ("Actor".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"Namespace", "ID"}, node);

                if (node.hasAttribute("Namespace") && node.hasAttribute("ID")) {
                    objMinionTask.setID(node.getAttribute("ID"));
                    objMinionTask.setNamespace(node.getAttribute("Namespace"));
                } else {
                    LOGGER.severe("Task with ID: " + taskID + " contains an invalid Minion Task");
                    return null;
                }
            }
        }

        return objMinionTask;
    }

    private OscarBindTask BuildOscarBindTask(String taskID, FrameworkNode taskNode) {
        /**
         * <TaskList ID="ConnectToOscar"> <TaskItem Type="OscarBind">
         * <ConnectInfo IP="myOscar.myCompany" port="1234" key="My hash key"/>
         * </TaskItem> </TaskList>
         *
         */
        String address;
        int port;
        String Key;

        if (taskNode.hasChild("ConnectionInfo")) {
            FrameworkNode connInfo = taskNode.getChild("ConnectionInfo");
            if (connInfo.hasAttribute("IP")) {
                address = connInfo.getAttribute("IP");
            } else if (connInfo.hasAttribute("Address")) {
                address = connInfo.getAttribute("Address");
            } else {
                LOGGER.severe(
                        "Task with ID: " + taskID + " contains an invalid OscarBind Task - no Address/IP specified");
                return null;
            }
            if (connInfo.hasAttribute("Port")) {
                port = connInfo.getIntegerAttribute("Port", -1);
                if (port == -1) {
                    LOGGER.severe(
                            "Task with ID: " + taskID + " contains an invalid OscarBind Task - invalid Port specified");
                    return null;
                }
            } else {
                LOGGER.severe("Task with ID: " + taskID + " contains an invalid OscarBind Task - no Port specified");
                return null;
            }
            if (connInfo.hasAttribute("Key")) {
                Key = connInfo.getAttribute("Key");
            } else {
                LOGGER.severe("Task with ID: " + taskID + " contains an invalid OscarBind Task - no Key specified");
                return null;
            }
        } else {
            LOGGER.severe("Task with ID: " + taskID + " contains an invalid definition for OscarBind Task");
            return null;
        }

        OscarBindTask objTask = new OscarBindTask(address, port, Key);
        objTask.setParams(GetParameters(taskNode));

        return objTask;
    }

    /**
     * Read the parameters from the xml file for a Oscar task
     *
     * @param taskID   ID of the task
     * @param taskNode XML node
     * @return A OscarTask object
     */
    private OscarTask BuildOscarTaskItem(String taskID, FrameworkNode taskNode) {
        /**
         * * Example Task <TaskItem Type="Oscar"> <Task OscarID="Fubar">Load File</Task>
         * <Param>mysave.glk</Param> </TaskItem>
         */
        OscarTask objOscarTask = new OscarTask();
        objOscarTask.setParams(GetParameters(taskNode));
        if (objOscarTask.getParams() != null && objOscarTask.getParams().isEmpty()) {
            return null; // means had problem loading params.
        }

        for (FrameworkNode node : taskNode.getChildNodes()) {
            if ("Task".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"OscarID"}, node);

                if (node.hasAttribute("OscarID")) {
                    objOscarTask.setOscarID(node.getAttribute("OscarID"));
                    objOscarTask.setTaskID(node.getTextContent());
                } else {
                    LOGGER.severe("Task with ID: " + taskID + " contains an invalid Oscar Task");
                    return null;
                }
            }
        }
        return objOscarTask;
    }

    private PulseTask BuildPulseTaskItem(String taskID, FrameworkNode taskNode) {
        /**
         * * Example Task <TaskItem Type="Pulse">
         * <MarvinDataPoint ID="Text" Namespace="PK Laptop"/> </TaskItem>
         */
        boolean errorLogged = false;
        PulseTask objPulseTask = new PulseTask();

        for (FrameworkNode node : taskNode.getChildNodes()) {
            if ("MarvinDataPoint".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"Namespace", "ID"}, node);
                if (node.hasAttribute("Namespace") && node.hasAttribute("ID")) {
                    objPulseTask.SetNamespaceAndID(node.getAttribute("Namespace"), node.getAttribute("ID"));
                } else {
                    LOGGER.severe("Task with ID: " + taskID
                            + " contains an invalid Pulse Task - no Namespace and ID defined in MarvinDataPoint");
                    errorLogged = true;
                }
            }
        }
        if (!objPulseTask.isValid()) {
            objPulseTask = null;
            if (!errorLogged) {
                LOGGER.severe(
                        "Task with ID: " + taskID + " contains an invalid Pulse Task - no MarvinDataPoint defined");
            }
        }
        return objPulseTask;
    }

    private RandomTask BuildRandomTaskItem(String taskID, FrameworkNode taskNode) {
        /**
         * * Example Task <TaskItem Type="Random">
         *
         * <Task>TaskID</Task> <Task Weight="50">TaskID2</Task> <Task>TaskID3</Task>
         * </TaskItem>
         */
        RandomTask objRandomTask = new RandomTask();

        double totalWeight = 0;

        for (FrameworkNode node : taskNode.getChildNodes()) {
            if ("Task".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"Weight"}, node);
                String strTaskID;
                double weight = 0;
                if (node.hasAttribute("Weight")) {
                    weight = node.getDoubleAttribute("Weight", 0);
                    if (weight >= 100 || weight <= 0) {
                        LOGGER.severe("RandomTask [" + taskID + "] has a Task with an invalid weight: "
                                + node.getAttribute("Weight"));
                        return null;
                    }
                    totalWeight += weight;
                }

                strTaskID = node.getTextContent();

                objRandomTask.AddTask(strTaskID, weight);
            }
        }
        if (totalWeight > 100) {
            LOGGER.severe("RandomTask [" + taskID + "] has a cummulative weight of > 100.");
            return null;
        }
        return objRandomTask;
    }

    private RemoteMarvinTask BuildRemoteMarvinTaskItem(String taskID, FrameworkNode taskNode) {
        /**
         * * Example Task <TaskItem Type="MarvinAdmin"> <Task ID="TabChange"/>
         * </TaskItem>
         */
        String remoteTaskID;
        String remoteMarvinID;
        remoteTaskID = "";
        remoteMarvinID = "";
        for (FrameworkNode node : taskNode.getChildNodes()) {
            if ("Task".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"ID"}, node);

                if (node.hasAttribute("ID")) {
                    remoteTaskID = node.getAttribute("ID");
                } else {
                    LOGGER.severe("Invalid MarvinAdminTask with Task ID: " + taskID + " - no ID specified.");
                    return null;
                }
            }
            if ("MarvinID".equalsIgnoreCase(node.getNodeName())) {
                remoteMarvinID = node.getTextContent();
            }
        }

        RemoteMarvinTask objTask = new RemoteMarvinTask(remoteMarvinID, remoteTaskID);

        return objTask;
    }

    private SaveScreenshotTask BuildSaveScreenshotTaskItem(String taskID, FrameworkNode taskNode) {
        /*
         * <TaskItem Type="SaveScreenshot" DestFile="Foo.csv" SavePolidy="Overwrite"/>
         * <!-- overwrite,sequence or prompt -->
         */
        if (false == taskNode.hasAttribute("DestFile")) {
            LOGGER.severe(
                    "SaveScreenShotTask with ID: " + taskID + " contains an invalid definition. DestFile Required");
            return null;
        }
        String strFilename = taskNode.getAttribute("DestFile");

        String strMode = "overwrite";
        SaveScreenshotTask.SaveMode mode;
        if (taskNode.hasAttribute("SavePolicy")) {
            strMode = taskNode.getAttribute("SavePolicy");
        }
        if ("Overwrite".equalsIgnoreCase(strMode)) {
            mode = SaveScreenshotTask.SaveMode.OVERWRITE;
        } else if ("SEQUENCE".equalsIgnoreCase(strMode)) {
            mode = SaveScreenshotTask.SaveMode.SEQUENCE;
        }
        /*
         * else if (strMode.equalsIgnoreCase("Prompt")) { mode =
         * SaveScreenshotTask.SaveMode.PROMPT; }
         */
        else {
            LOGGER.severe("SaveScreenShotTask with ID: " + taskID + " contains an invalid definition. SavePolicy = "
                    + strMode);
            return null;
        }

        SaveScreenshotTask objTask = new SaveScreenshotTask(strFilename, mode);
        return objTask;
    }

    private UpdateProxyTask BuildUpdateProxyTask(String taskID, FrameworkNode taskNode) {
        UpdateProxyTask task = null;
        for (FrameworkNode node : taskNode.getChildNodes()) {
            if ("Proxy".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"Namespace", "ID", "ProxyID", "ListEntry"}, node);
                if (!node.hasAttribute("ProxyID")) {
                    LOGGER.severe("ProxyTask requires ProxyID");
                    return null;
                }
                if (!(node.hasAttribute("Namespace") || node.hasAttribute("ID"))) {
                    LOGGER.severe("ProxyTask requires Namespace or ID or both");
                    return null;
                }

                task = new UpdateProxyTask(node.getAttribute("ProxyID"));
                if (node.hasAttribute("Namespace")) {
                    task.setNamespaceMask(node.getAttribute("Namespace"));
                }
                if (node.hasAttribute("ID")) {
                    task.setIDMask(node.getAttribute("ID"));
                }
                if (node.hasAttribute("ListEntry")) {
                    task.setListEntry(node.getAttribute("ListEntry"));
                }
            } else {
                LOGGER.severe("Unknown tag in ProxyTask:" + node.getNodeName());
                return null;
            }
        }
        return task;
    }

    public boolean CreateTask(String ID, FrameworkNode masterNode) {
        boolean retVal = false;
        boolean OnStartup = false;
        boolean OnConnected = false;
        double intervalTime = 0;

        TaskList objTask = null;

        if (masterNode.hasAttribute("Stepped") && masterNode.getBooleanAttribute("Stepped")) {
            SteppedTaskList objSteppedTask = new SteppedTaskList();

            if (masterNode.hasAttribute("LoopTasks")) {
                objSteppedTask.setLooped(masterNode.getBooleanAttribute("LoopTasks"));
            }
            objTask = objSteppedTask;
        } else {
            objTask = new TaskList();
        }
        if (masterNode.hasAttribute("Interval")) {
            intervalTime = masterNode.getDoubleAttribute("Interval", -1.0);
            if (intervalTime < 0.0) {
                intervalTime = 0.0;
            }
            intervalTime = intervalTime * 1000.0; // specified in seconds, checked in ms
            objTask.SetInterval(intervalTime);
        }

        if (masterNode.hasAttribute("PerformOnStartup")) {
            OnStartup = masterNode.getBooleanAttribute("PerformOnStartup");
        }

        if (masterNode.hasAttribute("PerformOnConnect")) {
            OnConnected = masterNode.getBooleanAttribute("PerformOnConnect");
        }

        for (FrameworkNode node : masterNode.getChildNodes()) {
            long postpone = 0;
            if (0 == node.getNodeName().compareToIgnoreCase("#text")
                    || 0 == node.getNodeName().compareToIgnoreCase("#comment")) {
                continue;
            }

            if ("TaskItem".equalsIgnoreCase(node.getNodeName())) {
                if (false == node.hasAttribute("Type")) {
                    LOGGER.severe("Task with ID: " + ID + " contains a TaskItem with no Type");
                    continue;
                }
                if (node.hasAttribute("Postpone")) {
                    postpone = ReadTaskPostpone(node.getAttribute("Postpone"));
                }

                String taskType = node.getAttribute("Type");
                BaseTask objTaskItem = null;
                if (0 == taskType.compareToIgnoreCase("Oscar")) {
                    objTaskItem = BuildOscarTaskItem(ID, node);
                } else if ("OscarBind".equalsIgnoreCase(taskType)) {
                    objTaskItem = BuildOscarBindTask(ID, node);
                } else if (0 == taskType.compareToIgnoreCase("Minion")) {
                    objTaskItem = BuildMinionTaskItem(ID, node);
                } else if (0 == taskType.compareToIgnoreCase("Marvin")) {
                    objTaskItem = BuildMarvinTaskItem(ID, node);
                } else if (0 == taskType.compareToIgnoreCase("Mathematic")) {
                    objTaskItem = BuildMathematicTaskTaskItem(ID, node);
                } else if (0 == taskType.compareToIgnoreCase("DeltaValue")) {
                    objTaskItem = BuildDeltaValueTaskItem(ID, node);
                } else if (0 == taskType.compareToIgnoreCase("DataPulse")) {
                    objTaskItem = BuildPulseTaskItem(ID, node);
                } else if (0 == taskType.compareToIgnoreCase("OtherTask")) {
                    objTaskItem = BuildChainedTaskItem(ID, node);
                } else if (0 == taskType.compareToIgnoreCase("MarvinAdmin")) {
                    objTaskItem = BuildMarvinAdminTaskItem(ID, node);
                } else if (0 == taskType.compareToIgnoreCase("RemoteMarvinTask")) {
                    objTaskItem = BuildRemoteMarvinTaskItem(ID, node);
                } else if ("RandomTask".equalsIgnoreCase(taskType) || "Random".equalsIgnoreCase(taskType)) {
                    objTaskItem = BuildRandomTaskItem(ID, node);
                } else if ("Desktop".equalsIgnoreCase(taskType)) {
                    objTaskItem = BuildDesktopTaskItem(ID, node);
                } else if ("UpdateProxy".equalsIgnoreCase(taskType)) {
                    objTaskItem = BuildUpdateProxyTask(ID, node);
                } else if ("DataSetFile".equalsIgnoreCase(taskType)) {
                    objTaskItem = BuildDataSetFileTaskItem(ID, node);
                } else if ("SaveScreenshot".equalsIgnoreCase(taskType)) {
                    objTaskItem = BuildSaveScreenshotTaskItem(ID, node);
                } else if ("MarvinPlayback".equalsIgnoreCase(taskType)) {
                    objTaskItem = BuildMarvinPlaybackTaskItem(ID, node);
                } else if ("LaunchApplication".equalsIgnoreCase(taskType) || "LaunchApp".equalsIgnoreCase(taskType)
                        || "LaunchProgram".equalsIgnoreCase(taskType) || "RunProgram".equalsIgnoreCase(taskType)
                        || "RunApp".equalsIgnoreCase(taskType)) {
                    objTaskItem = BuildLaunchApplicationTaskItem(ID, node);
                } else {
                    LOGGER.severe("Task with ID: " + ID + " contains a TaskItem of unknown Type of " + taskType + ".");
                }
                if (null != objTaskItem) {
                    objTaskItem.setPostponePeriod(postpone);
                    objTask.AddTaskItem(objTaskItem);
                    retVal = true;
                }
            }
        }
        if (retVal) {
            if (false == AddNewTask(ID, objTask, OnStartup, OnConnected)) {
                retVal = false;
            }
        } else {
            LOGGER.severe("Task with ID: " + ID + " is invalid.");
        }
        return retVal;
    }

    public String CreateWatchdogTask() {
        String ID = "Watchdog Task";
        WatchdogTask objWatchdog = new WatchdogTask();
        TaskList objTask = new TaskList();
        objTask.AddTaskItem(objWatchdog);

        AddNewTask(ID, objTask, false, false);

        return ID;
    }

    public DataManager getDataMgr() {
        return _DataMgr;
    }

    public int getNumberOfTasks() {
        if (null == taskMap) {
            return 0;
        }
        return taskMap.size();
    }

    /**
     * Common function for tasks, reads the <Param> sections
     *
     * @param taskNode
     * @return an array of the parameters
     */
    private ArrayList<Parameter> GetParameters(FrameworkNode taskNode) {
        ArrayList<Parameter> Params = null;

        for (FrameworkNode node : taskNode.getChildNodes()) {
            if (0 == node.getNodeName().compareToIgnoreCase("#text")
                    || 0 == node.getNodeName().compareToIgnoreCase("#comment")) {
                if ("Param".equalsIgnoreCase(node.getNodeName())) {
                    if (null == Params) {
                        Params = new ArrayList<>();
                    }
                    if (node.getTextContent().length() > 0) {
    //		    if (node.hasAttributes())
    //		    {
    //                        String y = node.getAttributeList();
    //                        String x = node.toString();
    //			LOGGER.severe("Specified a value and an attribute for <Param>.  They are mutually exclusive.");
    //		    }

                        Params.add(new Parameter(node.getTextContent()));
                    } else if (node.hasAttribute("Namespace") || node.hasAttribute("id")) {
                        if (!node.hasAttribute("Namespace")) {
                            LOGGER.severe(
                                    "Specified a <Param> with intent to use Namespace & ID, but did not specify Namespace.");
                            Params.clear(); // return an empty, but non-null list to know there was a problem.
                            break;
                        }
                        if (!node.hasAttribute("id")) {
                            LOGGER.severe("Specified a <Param> with intent to use Namespace & ID, but did not specify ID.");
                            Params.clear(); // return an empty, but non-null list to know there was a problem.
                            break;
                        }
                        String ns = node.getAttribute("Namespace");
                        String ID = node.getAttribute("ID");
                        DataSrcParameter param = new DataSrcParameter(ns, ID, getDataMgr());
                        Params.add(param);
                        LOGGER.info("Creating <Param> with input from Namespace=" + ns + " and ID=" + ID);
                    } else {
                        Params.clear(); // return an empty, but non-null list to know there was a problem.
                        LOGGER.severe("Empty <Param> in task");
                        break;
                    }
                } else {
                    List<String> validTags = Arrays.asList("TASK", "DATATOINSERT", "ACTOR", "ConnectionInfo");
                    String strTag = node.getNodeName();
                    boolean invalidTag = true;
                    for (String validTag : validTags) {
                        if (validTag.equalsIgnoreCase(strTag)) {
                            invalidTag = false;
                            break;
                        }
                    }
                    if (invalidTag) {
                        LOGGER.warning("Possible unknown tag in task: " + strTag);
                    }
                }
            }
        }

        return Params;
    }

    public long GetPendingTaskCount() {
        long retVal = 0;
        synchronized (deferredTasks) // make a quick copy to reduce time in synchronized block
        {
            retVal += deferredTasks.size();
        }
        synchronized (postponedTaskObjectThatMustBeRunInGuiThreadList) {
            retVal = postponedTaskObjectThatMustBeRunInGuiThreadList.size();
        }

        synchronized (postponedTasks) {
            retVal += postponedTasks.size();
        }
        return retVal;
    }

    public long GetPerformedCount() {
        return tasksPerformed;
    }

    public int GetPostPonedTaskCount() {
        int retVal;
        synchronized (postponedTaskObjectThatMustBeRunInGuiThreadList) {
            retVal = postponedTaskObjectThatMustBeRunInGuiThreadList.size();
        }
        return retVal;
    }

    /**
     * A Oscar as come online and announced itself, so register it (in a map)
     *
     * @param OscarID - Unique ID
     * @param Address Where it
     * @param Port    is from
     */
    public void OscarAnnouncementReceived(String oscarID, String Address, int Port, String oscarVersion) {
        if (null == clientMap) {
            clientMap = new ConcurrentHashMap<>();
        }
        oscarID = oscarID.toLowerCase();
        if (clientMap.containsKey(oscarID)) { // already exists, check to see if is same
            Client objClient = clientMap.get(oscarID);
            if (0 == objClient.getAddress().compareTo(Address) && objClient.getPort() == Port) {
                // they are the same, just got another announcment from same Oscar as before
            } else {
                clientMap.remove(oscarID);
                clientMap.put(oscarID, new Client(Address, Port));
                if (0 == objClient.getAddress().compareTo(Address)) { // going to assume old Oscar died, and a new one started on different port
                    LOGGER.info("New Oscar [" + oscarID + "] Connection made [" + Address + "," + Integer.toString(Port)
                            + "] Replacing the on on port " + Integer.toString(objClient.getPort()));
                } else // same ID, but from different IP address
                {
                    LOGGER.severe(
                            "New Oscar [" + oscarID + "] Connection made [" + Address + "," + Integer.toString(Port)
                                    + "].  This OscarID was already used.  Using new connection from now on.");
                }
            }
        } else // brand new Oscar
        {
            clientMap.put(oscarID, new Client(Address, Port));
            LOGGER.info("New Oscar [" + oscarID + "] Connection made [" + Address + "," + Integer.toString(Port)
                    + "] Ocscar Version: " + oscarVersion);
            PerformOnConnectedTasks(); // Just do it every time a new connection is made, might be a bit redundant, but
            // not too bad
        }
    }

    public void PerformDeferredTasks() {
        // boolean fDone = false;
        int size = 0;
        ArrayList<String> localDeferredTasksToRun = new ArrayList<>();
        ArrayList<ITask> localPostponedTaskstoRun = new ArrayList<>();

        synchronized (deferredTasks) // make a quick copy to reduce time in synchronized block
        {
            size = deferredTasks.size();
            if (size > 0) {
                localDeferredTasksToRun.addAll(deferredTasks);
                deferredTasks.clear();
            }
        }

        if (size > 256 && !warningAboutManyTasksSent) {
            if (loopsWithManyTasks++ > 5) {
                LOGGER.warning(" There are " + size
                        + " Tasks queued up to be performed. That is a lot - you MAY have a circular logic bomb in <Conditionals>.  This is the last warning for this potential problem.");
                warningAboutManyTasksSent = true;
            } else {
                LOGGER.warning(" There are " + size
                        + " Tasks queued up to be performed. That is a lot - you MAY have a circular logic bomb in <Conditionals>.");
            }
        }

        // String Task;
        Platform.runLater(() -> { // go run this in a GUI thread
            //
            for (String strTask : localDeferredTasksToRun) {
                PerformTask(strTask);
            }
            for (String strTask : timedTasks) {
                TaskList objTaskList = taskMap.get(strTask.toUpperCase());
                if (objTaskList.ReadyForIntervalExecuation()) {
                    PerformTask(strTask);
                }
            }
        });
        // Now go and process all of the postponed tasks that need to be done in gui
        // thread

        synchronized (postponedTaskObjectThatMustBeRunInGuiThreadList) {
            size = postponedTaskObjectThatMustBeRunInGuiThreadList.size();
            if (size > 0) {
                localPostponedTaskstoRun.addAll(postponedTaskObjectThatMustBeRunInGuiThreadList);
                postponedTaskObjectThatMustBeRunInGuiThreadList.clear();
            }
        }

        Platform.runLater(() -> { // go run this in a GUI thread
            //
            for (ITask objTask : localPostponedTaskstoRun) {
                objTask.PerformTask();
            }
            if (localPostponedTaskstoRun.size() > 0) {
                Configuration.getConfig().requestImmediateRefresh();
            }
        });

        PerformPostponedTasks();
    }

    /**
     * @return
     */
    public int PerformOnConnectedTasks() {
        WatchdogTask.OnInitialOscarConnection(); // Send the 'Refresh' message NOW rather than after the watchdog
        // interval
        int RetVal = RunThroughList(onConnectedList);
        if (RetVal > 0) {
            LOGGER.info("Performed [" + Integer.toString(RetVal) + "] tasks after connection establshed");
        }
        return RetVal;
    }

    public int PerformOnStartupTasks() {
        int RetVal = RunThroughList(onStartupList);
        LOGGER.info("Performed [" + Integer.toString(RetVal) + "] tasks after startup");

        return RetVal;
    }

    /**
     * Go through and perform the postponed tasks
     */
    public void PerformPostponedTasks() {
        ArrayList<PostponedTask> toRunList = null;
        synchronized (postponedTasks) {
            for (PostponedTask objTask : postponedTasks) {
                if (objTask.ReadyToPerform()) {
                    if (null == toRunList) {
                        toRunList = new ArrayList<>();
                    }
                    toRunList.add(objTask); // Add it to a temp list, don't want to run here in sync'd loop
                }
            }
            if (null != toRunList) {
                for (PostponedTask objTask : toRunList) {
                    postponedTasks.remove(objTask);
                }
            }
        }

        if (null != toRunList) // nuken
        {
            for (PostponedTask objTask : toRunList) {
                objTask.Perform();
            }
        }

        synchronized (postponedTasksNew) // New postponed tasks came in while during processing of postponed tasks
        // thread
        {
            synchronized (postponedTasks) {
                postponedTasks.addAll(postponedTasksNew); // add to postponed list for processing next time
            }
            postponedTasksNew.clear();
        }
    }

    /**
     * Called by a widget or menu item to go do a task
     *
     * @param TaskID - the TASK ID, that is associated with 1 or more task items
     * @return true if success, else false
     */
    public boolean PerformTask(String TaskID) {
        if (false == TaskExists(TaskID)) {
            LOGGER.severe("Asked to perform a task [" + TaskID + "] that doesn't exist.");
            return false;
        }
        tasksPerformed++;
        TaskList objTaskList = taskMap.get(TaskID.toUpperCase());

        if (null != objTaskList) {
            return objTaskList.PerformTasks();
        }
        LOGGER.severe("Tasklist [" + TaskID + "] came back null.");
        return false;
    }

    private long ReadTaskPostpone(String strPostpone) {
        long Postpone = 0;
        try {
            Postpone = Integer.parseInt(strPostpone);
            if (Postpone < 0) {
                LOGGER.severe("Invalid Postpone value: " + strPostpone + ".  Setting to 0");
                Postpone = 0;
            }

        } catch (NumberFormatException ex) {
            if (strPostpone.contains(":")) // could be a random range!
            {
                String[] parts = strPostpone.split(":");

                if (2 == parts.length) {
                    String strRange1 = parts[0];
                    String strRange2 = parts[1];
                    int bound1 = (int) ReadTaskPostpone(strRange1);
                    int bound2 = (int) ReadTaskPostpone(strRange2);
                    if (bound1 > bound2) {
                        int tLong = bound1;
                        bound1 = bound2;
                        bound2 = tLong;
                    }
                    Random rand = new Random();

                    Postpone = (long) (bound1 + rand.nextInt((bound2 - bound1) + 1));

                }
            } else {
                LOGGER.severe("Invalid Postpone value: " + strPostpone + ".  Setting to 0");
                Postpone = 0;
            }

        }
        return Postpone;
    }

    private int RunThroughList(ArrayList<String> list) {
        int RetVal = 0;
        if (null != list) {
            for (String taskID : list) {
                PerformTask(taskID);
            }
            RetVal = list.size();
        }
        return RetVal;
    }

    /**
     * Sends a packet to each and every Oscar registered
     *
     * @param sendData
     */
    protected boolean SendToAllOscars(byte[] sendData) {
        if (null == clientMap || clientMap.isEmpty()) {
            LOGGER.info("Marvin tried to send something to Oscar, but there are no Oscar's available.");
            return false;
        }
        ArrayList<String> badList = null;
        Iterator<String> reader = this.clientMap.keySet().iterator();
        while (reader.hasNext()) {
            String key = reader.next();
            Client client = clientMap.get(key);
            if (null == client || false == client.send(sendData)) {
                if (null == badList) // if either null (should not happen or failure to send (could happen)
                {
                    badList = new ArrayList<>(); // make a list of keys to nuke
                }
                badList.add(key);
            } else {
                client.send(sendData); // send it again - it is UDP traffic, so not guaranteed. Minion will worry about
                // duplicates
            }
        }
        if (null != badList) // something went wrong, nuke them.
        {
            for (String key : badList) {
                clientMap.remove(key);
                LOGGER.info("Unable to send data to Oscar with ID:" + key);
            }
        }
        return true;
    }

    /**
     * * Sends a datapacket to a specific Oscar
     *
     * @param OscarID  - Which Oscar to send packet to
     * @param sendData - Data 2 send
     */
    protected void SendToOscar(String OscarID, byte[] sendData) {
        if (null == OscarID) {
            LOGGER.severe("SendToOscar fn received NULL OscarID.");
            return;
        }

        OscarID = OscarID.toLowerCase();
        if (null == clientMap || clientMap.isEmpty()) {
            // LOGGER.info("Marvin tried to send something to Oscar, but there are no
            // Oscar's available.");
            return;
        }
        if (clientMap.containsKey(OscarID)) {
            Client client = clientMap.get(OscarID);
            if (null == client || false == client.send(sendData)) {
                clientMap.remove(OscarID); // something not right with this sucker, so nuke it.
                LOGGER.info("Unable to send data to Oscar with ID:" + OscarID);
            } else { // success
                client.send(sendData); // send it again, just in case, as it is UDP - Other end needs to take care not
                // to repeat
                LOGGER.info("Sent Packet to Oscar with ID=" + OscarID);
            }
        } else {
            LOGGER.info("Asked to send data to unknown (not yet established connection) Oscar with ID of: " + OscarID);
        }
    }

    public void setDataMgr(DataManager dataMgr) {
        this._DataMgr = dataMgr;
    }

    /**
     * helper routine to see if a Task with the given ID already exists
     *
     * @param TaskID
     * @return true if the TaskList with the TaskID already exists, else false
     */
    public boolean TaskExists(String TaskID) {
        if (null == TaskID) {
            return false;
        }
        if (null != taskMap) {
            return taskMap.containsKey(TaskID.toUpperCase());
        }
        return false;
    }

    public boolean VerifyTasks() {
        boolean returnVal = true;
        HashMap<String, String> knownBad = new HashMap<>();
        for (String strKey : taskMap.keySet()) {
            TaskList objTaskList = taskMap.get(strKey);
            for (BaseTask objTask : objTaskList.GetTasks()) {
                String taskID = objTask.getTaskID_ForVerification();
                if (null != taskID) {
                    if (taskMap.containsKey(taskID.toUpperCase())) {
                        // all good
                    } else if (false == knownBad.containsKey(taskID.toUpperCase())) {
                        knownBad.put(taskID.toUpperCase(), taskID);
                        LOGGER.warning("Task with ID " + taskID + " specified, but not defined anywhere.");
                        returnVal = false;
                    }

                }
            }
        }

        for (int iIndex = 0; iIndex < ConfigurationReader.GetConfigReader().getTabs().size(); iIndex++) {
            ArrayList<String> taskIDs = ConfigurationReader.GetConfigReader().getTabs().get(iIndex).GetAllWidgetTasks();
            for (String taskID : taskIDs) {
                if (taskMap.containsKey(taskID.toUpperCase())) {
                    // all good
                } else if (false == knownBad.containsKey(taskID.toUpperCase())) {
                    knownBad.put(taskID.toUpperCase(), taskID);
                    LOGGER.warning("Task with ID " + taskID + " specified, but not defined anywhere.");
                    returnVal = false;
                }
            }
        }

        return returnVal;
    }
}
