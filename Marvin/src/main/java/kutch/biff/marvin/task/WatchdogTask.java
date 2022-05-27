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

import java.util.Random;
import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.version.Version;

/**
 * @author Patrick Kutch
 */
public class WatchdogTask extends BaseTask {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static WatchdogTask objSingleton;

    public static void ForceRefresh() {
        if (null != WatchdogTask.objSingleton) {
            WatchdogTask.objSingleton.FirstWatchdogMessage = true;
            WatchdogTask.objSingleton.PerformTask();
        }
    }

    // Backdoor kludge to improve initial startup time
    public static void OnInitialOscarConnection() {
        if (null != WatchdogTask.objSingleton) {
            WatchdogTask.objSingleton.PerformTask();
        }
    }

    private TaskManager taskman = TaskManager.getTaskManager();

    private boolean firstWatchdogMessage;

    private Random rnd = new Random();

    public WatchdogTask() {
        firstWatchdogMessage = true; // tells Oscar to send a refresh to Minions
        WatchdogTask.objSingleton = this;
    }

    @Override
    public void PerformTask() {
        String sendBuffer = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        sendBuffer += "<Marvin Type=\"WatchdogTimer\">";
        sendBuffer += "<Version>1.0</Version>";
        sendBuffer += "<MarvinVersion>" + Version.getVersion() + "</MarvinVersion>";
        sendBuffer += "<UniqueID>" + Integer.toString(rnd.nextInt()) + "</UniqueID>";
        sendBuffer += "<Port>" + Integer.toString(getConfig().getPort()) + "</Port>";
        if (firstWatchdogMessage) {
            sendBuffer += "<RefreshRequested>True</RefreshRequested>";
        }
        sendBuffer += "</Marvin>";
        LOGGER.info("Sending Watchdog re-arm (Heartbeat)");
        boolean retVal = taskman.SendToAllOscars(sendBuffer.getBytes());

        if (firstWatchdogMessage && retVal) {
            firstWatchdogMessage = false; // only reset flag after successful transmit
            LOGGER.info("Sent Request Refresh message");
        }
    }
}
