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
package kutch.biff.marvin.utility;

import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.BaseTask;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.widget.BaseWidget;

/**
 * @author Patrick
 */
public class MarvinLocalData {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final ConfigurationReader config = ConfigurationReader.GetConfigReader();
    private final TaskManager taskman = TaskManager.getTaskManager();
    private final String namespace = "MarvinLocalNamespace";
    private final long startTime = System.currentTimeMillis();
    private final int _interval;
    private ScheduledExecutorService executor;
    private boolean stopSignalled;

    public MarvinLocalData(int interval) {
        _interval = interval;
        if (_interval > 0) {
            stopSignalled = false;
            Runnable myRunnable = () -> {
                if (false == stopSignalled) {
                    PerformMagic();
                }
            };

            executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(myRunnable, 0, 1, TimeUnit.SECONDS);
            DoStaticMagic();
        }
    }

    private void DoStaticMagic() {
        taskman.getDataMgr().ChangeValue("TaskCount", namespace, Integer.toString(BaseTask.getTaskCount()));
        if (config.getConfiguration().GetApplicationID().length() > 0) {
            taskman.getDataMgr().ChangeValue("MarvinID", namespace, config.getConfiguration().GetApplicationID());
        } else {
            taskman.getDataMgr().ChangeValue("MarvinID", namespace, "Not Set");
        }
    }

    private String GetTimeString(long seconds) {
        long sec = seconds % 60;
        long minutes = seconds % 3600 / 60;
        long hours = seconds % 86400 / 3600;
        long days = seconds / 86400;

        String strRet = String.format("%02d", sec);
        if (seconds > 60) {
            strRet = String.format("%02d", minutes) + ":" + strRet;
        }
        if (seconds > 3600) {
            strRet = String.format("%02d", hours) + ":" + strRet;
        }
        if (seconds > 86400) {
            strRet = Long.toString(days) + ":" + strRet;
        }

        return strRet;
    }

    private void PerformMagic() {
        taskman.getDataMgr().ChangeValue("Datapoints", namespace,
                Integer.toString(DataManager.getDataManager().NumberOfRegisteredDatapoints()));
        long runtime = (System.currentTimeMillis() - startTime) / 1000;
        taskman.getDataMgr().ChangeValue("RuntimeSecs", namespace, Long.toString(runtime));
        taskman.getDataMgr().ChangeValue("RuntimeFormatted", namespace, GetTimeString(runtime));
        LocalTime now = LocalTime.now(ZoneId.systemDefault());
        taskman.getDataMgr().ChangeValue("LocalTime", namespace, GetTimeString(now.toSecondOfDay()));
        taskman.getDataMgr().ChangeValue("WidgetCount", namespace, Integer.toString(BaseWidget.getWidgetCount()));
        taskman.getDataMgr().ChangeValue("DataUpdateCount", namespace,
                Long.toString(taskman.getDataMgr().getUpdateCount()));
        taskman.getDataMgr().ChangeValue("UnassignedDatapointCount", namespace,
                Long.toString(taskman.getDataMgr().getUnassignedCount()));
        taskman.getDataMgr().ChangeValue("TasksExecutedCount", namespace, Long.toString(taskman.GetPerformedCount()));
        taskman.getDataMgr().ChangeValue("PendingTasksCount", namespace, Long.toString(taskman.GetPendingTaskCount()));
        long freeMem = Runtime.getRuntime().freeMemory();
        String kBMemStr = NumberFormat.getNumberInstance(Locale.US).format(freeMem / 1024);
        String bytesStr = NumberFormat.getNumberInstance(Locale.US).format(freeMem);
        taskman.getDataMgr().ChangeValue("FreeMemB", namespace, bytesStr);
        taskman.getDataMgr().ChangeValue("FreeMemKB", namespace, kBMemStr);
    }

    public void Shutdown() {
        stopSignalled = true;
        executor.shutdown();
    }
}
