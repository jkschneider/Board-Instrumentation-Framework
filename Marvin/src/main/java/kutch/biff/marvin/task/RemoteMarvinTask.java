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

import static kutch.biff.marvin.configuration.ConfigurationReader.GetConfigReader;

import java.util.Random;
import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;

/**
 * @author Patrick Kutch
 */
public class RemoteMarvinTask extends BaseTask {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static int requestNumber;
    private final TaskManager taskman = TaskManager.getTaskManager();
    private String _TaskID;
    private String marvinID; // remote marvin id

    public RemoteMarvinTask(String id, String taskID) {
        marvinID = id;
        _TaskID = taskID;
        if (RemoteMarvinTask._RequestNumber == 0) {
            Random r = new Random(TaskManager.getTaskManager().getNumberOfTasks()); // use # of tasks for a random seed
            RemoteMarvinTask._RequestNumber = r.nextInt(1000) + 1;
        }
    }

    @Override
    public boolean getMustBeInGUIThread() {
        return false;
    }

    @Override
    public void PerformTask() {
        String strTask = getDataValue(_TaskID);
        if (null == strTask) {
            return;
        }
        String sendBuffer = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        sendBuffer += "<Marvin Type=\"RemoteMarvinTask\">";
        sendBuffer += "<Version>1.0</Version>";
        sendBuffer += "<Requester>" + GetConfigReader().getConfiguration().GetApplicationID() + " :"
                + GetConfigReader().getConfiguration().getAddress() + "</Requester>";
        sendBuffer += "<RequestNumber>" + Integer.toString(requestNumber) + "</RequestNumber>";
        sendBuffer += "<MarvinID>" + marvinID + "</MarvinID>";
        sendBuffer += "<Task>" + strTask + "</Task>";
        sendBuffer += "</Marvin>";

        RemoteMarvinTask._RequestNumber += 1;

        LOGGER.info("Sending RemoteMarvinAdminTask :" + strTask);
        taskman.SendToAllOscars(sendBuffer.getBytes());
    }

}
