/*
 * ##############################################################################
 * #  Copyright (c) 2019 Intel Corporation
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
package kutch.biff.marvin.datamanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javafx.beans.value.ChangeListener;
import javafx.util.Pair;
import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.DynamicDebugWidgetTask;
import kutch.biff.marvin.task.LateCreateTask;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.utility.DynamicItemInfoContainer;
import kutch.biff.marvin.utility.GenerateDatapointInfo;
import kutch.biff.marvin.utility.Glob;
import kutch.biff.marvin.utility.Utility;
import kutch.biff.marvin.widget.widgetbuilder.OnDemandTabBuilder;
import kutch.biff.marvin.widget.widgetbuilder.OnDemandWidgetBuilder;

/**
 * @author Patrick.Kutch@gmail.com
 */
public class DataManager {
    private static DataManager dataManager;
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    public static DataManager getDataManager() {
        return dataManager;
    }

    private ConcurrentHashMap<String, DataSet> dataMap;
    private Map<String, List<GenerateDatapointInfo>> proxyIDMap;
    private ConcurrentHashMap<String, List<WildcardListItem>> wildcardDataMap;
    private final Queue<Pair<DynamicItemInfoContainer, OnDemandWidgetBuilder>> onDemandQueue; // a queue solves some of
    // my concurency issues
    private final Queue<GenerateDatapointInfo> generateDatapointList;
    private long updateCount;
    private long unassignedDataPoints;

    private boolean dynamicTabRegistered;

    public DataManager() {
        dataMap = new ConcurrentHashMap<>();
        wildcardDataMap = new ConcurrentHashMap<>();
        dataManager = this;
        updateCount = 0;
        unassignedDataPoints = 0;
        onDemandQueue = new ConcurrentLinkedQueue<>();
        generateDatapointList = new ConcurrentLinkedQueue<>();
        proxyIDMap = new HashMap<>();
    }

    public boolean AddGenerateDatapointInfo(GenerateDatapointInfo genInfo) {
        String key;
        if (null == genInfo.getProxyID()) {
            key = this.toString();
        } else {
            key = genInfo.getProxyID().toUpperCase();

        }
        if (!proxyIDMap.containsKey(key)) {
            proxyIDMap.put(key, new ArrayList<>());
        }

        proxyIDMap.get(key).add(genInfo); // add to list
        generateDatapointList.add(genInfo);
        return true;
    }

    public void AddListener(String id, String namespace, ChangeListener<?> listener) {
        if (null == id || null == namespace) {
            return;
        }

        String Key = Utility.generateKey(namespace, id);

        if (false == dataMap.containsKey(Key)) {
            dataMap.put(Key, new DataSet());
        }

        dataMap.get(Key).addListener(listener);
    }

    public void AddOnDemandWidgetCriterea(DynamicItemInfoContainer criterea, OnDemandWidgetBuilder objBuilder) {
        onDemandQueue.add(new Pair<>(criterea, objBuilder));

        if (objBuilder instanceof OnDemandTabBuilder) {
            dynamicTabRegistered = true; // flag so can know if something is registered for startup check for any tabs
        }
    }

    public void AddWildcardListener(String ID, String Namespace, ChangeListener<?> listener) {
        if (null == ID || null == Namespace) {
            LOGGER.severe("Wildcard listener has no Namespace or RegEx pattern");
            return;
        }

        // LOGGER.info("Adding Wildcard Listener for [" + Namespace + "] ID: " + ID);
        String Key = Namespace.toUpperCase();

        if (false == wildcardDataMap.containsKey(Key)) {
            WildcardListItem item = new WildcardListItem(ID);
            List<WildcardListItem> list = new ArrayList<>();
            list.add(item);
            wildcardDataMap.put(Key, list);
        }

        for (WildcardListItem wcNode : wildcardDataMap.get(Key)) // go through the list for the namespace
        {
            if (wcNode.getWildCard().equalsIgnoreCase(ID)) {
                wcNode.getDataSet().addListener(listener);
                return; // found match no need to keep going, each pattern is unique but not repeated
            }
        }
        // Not found, so add a new listener
        WildcardListItem item = new WildcardListItem(ID);
        item.getDataSet().addListener(listener);
        wildcardDataMap.get(Key).add(item);
    }

    public void ChangeValue(String ID, String Namespace, String Value) {
        boolean onDemandItemFound = false;
        boolean onDemandTabFound = false;
        synchronized (this) {
            for (Pair<DynamicItemInfoContainer, OnDemandWidgetBuilder> entry : onDemandQueue) {
                if (entry.getKey().Matches(Namespace, ID, Value)) {
                    LateCreateTask objTask = new LateCreateTask(entry.getValue(), Namespace, ID, Value,
                            entry.getKey().getLastMatchedSortStr());
                    TaskManager.getTaskManager().AddDeferredTaskObject(objTask);
                    onDemandItemFound = true;
                    if (entry.getValue() instanceof OnDemandTabBuilder) {
                        onDemandTabFound = true;
                    }
                }
            }
            if (onDemandItemFound) {
                Configuration.getConfig().setCursorToWait();
            }
            // go and handle any GenerateDatapoint stuff
            for (GenerateDatapointInfo info : generateDatapointList) {
                if (info.Matches(Namespace, ID)) {
                    info.BuildDatapoint(Namespace, ID);
                }
            }
        }

        synchronized (this) {
            String Key = Utility.generateKey(Namespace, ID);

            updateCount++;

            boolean inWildcard = HandleWildcardChangeValue(ID, Namespace, Value);

            if (false == dataMap.containsKey(Key)) {
                dataMap.put(Key, new DataSet());

                if (false == inWildcard) {
                    unassignedDataPoints++;

                    // LOGGER.info("Received Data update not associated with a widget: " + Namespace
                    // + " : " + ID + " [" + Value + "]");
                    // nifty stuff to dynamically add a tab to show 'unregistered' data points.
                    if (kutch.biff.marvin.widget.DynamicTabWidget.isEnabled()) {
                        DynamicDebugWidgetTask objTask = new DynamicDebugWidgetTask(Namespace, ID, Value);
                        TaskManager.getTaskManager().AddPostponedTask(objTask, 0);
                    }
                }
            }
            if (dataMap.containsKey(Key)) // if didn't exist, is created above
            {
                dataMap.get(Key).setLatestValue(Value);
            }
        }
        if (onDemandTabFound) {
//            ApplyOnDemandTabStyle objTask = new ApplyOnDemandTabStyle();
//            TaskManager.getTaskManager().AddPostponedTask(objTask, 1000);
        }
    }

    public boolean DynamicTabRegistered() {
        return dynamicTabRegistered;
    }

    public Queue<GenerateDatapointInfo> getGenerateDatapointList() {
        return generateDatapointList;
    }

    public Queue<Pair<DynamicItemInfoContainer, OnDemandWidgetBuilder>> getOnDemandList() {
        return onDemandQueue;
    }

    public int getQueuedSize() {
        int tSize = 0;
        for (Map.Entry<String, DataSet> entry : dataMap.entrySet()) {
            DataSet objData = entry.getValue();
            tSize += objData.getSize();
        }
        return tSize;
    }

    public long getUnassignedCount() {
        return unassignedDataPoints;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    public String GetValue(String ID, String Namespace) {
        synchronized (this) {
            if (null == ID || null == Namespace) {
                LOGGER.severe("Wildcard listener has no Namespace or RegEx pattern");
                return null;
            }

            String Key = Utility.generateKey(Namespace, ID);

            if (dataMap.containsKey(Key)) {
                return dataMap.get(Key).getLatestValue();
            }
            return null;
        }
    }

    public String GetValueForMath(String ID, String Namespace) {
        synchronized (this) {
            if (null == ID || null == Namespace) {
                LOGGER.severe("Wildcard listener has no Namespace or RegEx pattern");
                return null;
            }

            String Key = Utility.generateKey(Namespace, ID);

            if (dataMap.containsKey(Key)) {
                return dataMap.get(Key).getLatestValueForMath();
            }
            return null;
        }
    }

    private boolean HandleWildcardChangeValue(String ID, String Namespace, String Value) {
        String Key = Namespace.toUpperCase();
        boolean retVal = false;
        if (wildcardDataMap.containsKey(Key)) {
            for (WildcardListItem wcNode : wildcardDataMap.get(Key)) // go through the list for the namespace
            {
                if (wcNode.Matches(ID)) {
                    wcNode.getDataSet().setLatestValue(ID + ":" + Value); // need 2 pass ID here, since it's a RegEx
                    retVal = true;
                }
            }
        }

        return retVal;
    }

    public int NumberOfRegisteredDatapoints() {
        return dataMap.size();
    }

    public int PerformUpdates() {
        int updatesPerformed = 0;
        for (Map.Entry<String, DataSet> entry : dataMap.entrySet()) {
            updatesPerformed += entry.getValue().Update();
        }
        if (!wildcardDataMap.isEmpty()) {
            for (String Key : wildcardDataMap.keySet()) // go through each namespace
            {
                for (WildcardListItem wcNode : wildcardDataMap.get(Key)) // go through the list for the namespace
                {
                    updatesPerformed += wcNode.getDataSet().Update();
                }
            }
        }
        return updatesPerformed;
    }

    public int PulseDataPoint(String namespaceCriterea, String idCriterea) {
        int count = 0;
        if (null == namespaceCriterea || null == idCriterea) {
            return count;
        }

        String strCompare = Utility.generateKey(namespaceCriterea, idCriterea);

        for (String Key : dataMap.keySet()) {
            if (Glob.check(strCompare, Key)) {
                String[] parts = Utility.splitKey(Key);
                if (parts.length != 2) {
                    LOGGER.severe("Unknown problem trying to perform PulseDataPoint. Key=" + Key);
                } else {
                    String Namespace = parts[0];
                    String ID = parts[1];
                    String Value = dataMap.get(Key).getLatestValue();
                    ChangeValue(ID, Namespace, Value);
                    count++;
                }
            }
        }

        return count;
    }

    public void RemoveListener(ChangeListener<?> listener) {
        // super inefficient.....
        for (String key : dataMap.keySet()) {
            dataMap.get(key).removeListener(listener);
        }
    }

    public void RemoveListener(String ID, String Namespace, ChangeListener<?> listener) {
        if (null == ID || null == Namespace) {
            return;
        }

        String Key = Utility.generateKey(Namespace, ID);

        if (dataMap.containsKey(Key)) {
            synchronized (this) {
                dataMap.get(Key).removeListener(listener);
            }
        }
    }

    public void UpdateGenerateDatapointProxy(String proxyID, String newNamespaceCriteria, String newIDCriterea,
                                             String newListEntry) {
        if (!proxyIDMap.containsKey(proxyID.toUpperCase())) {
            LOGGER.warning("Unknown ProxyID: " + proxyID);
            return;
        }

        LOGGER.info("Updating proxy [" + proxyID + "] to [" + newNamespaceCriteria + ":" + newIDCriterea + "]");
        for (GenerateDatapointInfo genInfo : proxyIDMap.get(proxyID.toUpperCase())) {
            genInfo.ProxyReset(newNamespaceCriteria, newIDCriterea, newListEntry);
        }
    }
}
