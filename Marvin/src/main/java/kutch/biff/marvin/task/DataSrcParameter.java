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
 * #    Allows you to use a data point (MinionSrc ID + Namespace) as a parameter
 * #    for a MinionTask
 * #
 * ##############################################################################
 */
package kutch.biff.marvin.task;

import java.util.logging.Logger;

import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;

/**
 * @author Patrick Kutch
 */
public class DataSrcParameter extends Parameter {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final String _Namespace;
    private final String _id;
    private final DataManager _DataMgr;

    public DataSrcParameter(String namespace, String id, DataManager dataMgr) {
        _Namespace = namespace;
        _id = id;
        _DataMgr = dataMgr;
        if (null == _id) {
            LOGGER.severe("Task <Param> using Namespace and ID does not have an ID");
        }
        if (null == _Namespace) {
            LOGGER.severe("Task <Param> using Namespace and ID does not have a Namespace");
        }
        if (null == _DataMgr) {
            LOGGER.severe("Null DataManager passed");
        }
    }

    @Override
    public String toString() {
        if (null == _id) {
            LOGGER.severe("Task <Param> using Namespace and ID does not have an ID");
            return null;
        }
        if (null == _Namespace) {
            LOGGER.severe("Task <Param> using Namespace and ID does not have a Namespace");
            return null;
        }
        String data = _DataMgr.GetValue(_id, _Namespace);
        if (null == data) {
            LOGGER.severe("Task <Param> with Namespace:ID of " + _Namespace + ":" + _id
                    + " is still unknown (has not been received from datasrc yet.");
        }
        return data;
    }
}
