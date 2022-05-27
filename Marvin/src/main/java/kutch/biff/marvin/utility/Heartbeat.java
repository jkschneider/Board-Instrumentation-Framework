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

import java.util.Timer;
import java.util.TimerTask;

import kutch.biff.marvin.task.TaskManager;

/**
 * @author Patrick Kutch
 */
public class Heartbeat extends TimerTask {
    //    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final TaskManager taskman = TaskManager.getTaskManager();
    private final int _interval;
    private final Timer heartbeatTimer;
    private final String heartbeatTaskID;
    private boolean threadNameSet;

    public Heartbeat(int interval) {
        heartbeatTaskID = taskman.CreateWatchdogTask();
        _interval = interval * 1000;
        heartbeatTimer = new Timer();
        threadNameSet = false;
    }

    @Override
    public void run() {
        if (false == threadNameSet) {
            threadNameSet = true;
            Thread.currentThread().setName("Heartbeat Thread");
        }
        TaskManager.getTaskManager().PerformTask(heartbeatTaskID);
    }

    public void Start() {
        heartbeatTimer.scheduleAtFixedRate(this, 100, _interval);
    }

    public void Stop() {
        heartbeatTimer.cancel();
    }
}
