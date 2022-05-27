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

import java.util.Random;
import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;

/**
 * @author Patrick Kutch
 */
public class MinionTask extends BaseTask {
    protected static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private TaskManager taskman = TaskManager.getTaskManager();
    private String _Namespace;
    private String _id;
    private Random rnd = new Random();

    public MinionTask() {
        _Namespace = null;
        _id = null;
    }

    public String getID() {
        return _id;
    }

    @Override
    public boolean getMustBeInGUIThread() {
        return false;
    }

    public String getNamespace() {
        return _Namespace;
    }

    @Override
    public void PerformTask() {
        String strNamespace = getDataValue(getNamespace());
        String strID = getDataValue(getID());
        String strParamList = "";
        if (strID == null || strNamespace == null) {
            return;
        }
        String sendBuffer = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        sendBuffer += "<Marvin Type=\"MinionTask\">";
        sendBuffer += "<Version>1.0</Version>";
        sendBuffer += "<UniqueID>" + Integer.toString(rnd.nextInt()) + "</UniqueID>";
        sendBuffer += "<Task Namespace=\"" + strNamespace + "\" ";
        sendBuffer += "ID=\"" + strID + "\"/>";
        if (null != getParams()) {
            for (Parameter param : getParams()) {
                String strParam = param.toString();
                if (null == strParam) {
                    return;
                }

                sendBuffer += "<Param>" + strParam + "</Param>";
                if (strParamList.length() == 0) {
                    strParamList = strParam;
                } else {
                    strParamList += "," + strParam;
                }
            }
        }

        sendBuffer += "</Marvin>";

        taskman.SendToAllOscars(sendBuffer.getBytes());
        LOGGER.info("Sending Minion Task [" + getNamespace() + ":" + getID() + "] Params: " + strParamList);
    }

    public void setID(String id) {
        this._id = id;
    }

    public void setNamespace(String namespace) {
        this._Namespace = namespace;
    }
}
