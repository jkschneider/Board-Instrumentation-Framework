/*
 * ##############################################################################
 * #  Copyright (c) 2018 Intel Corporation
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

import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.widget.widgetbuilder.OnDemandWidgetBuilder;

/**
 * @author Patrick
 */
public class LateCreateTask extends BaseTask {
    private final OnDemandWidgetBuilder builder;
    private final String __Namespace;
    private final String __id;
    private final String __Value;
    private final String sortStr;

    public LateCreateTask(OnDemandWidgetBuilder objBuilder, String namespace, String id, String value, String strSortBy) {
        builder = objBuilder;
        __Namespace = namespace;
        __id = id;
        __Value = value;
        sortStr = strSortBy;
    }

    @Override
    public void PerformTask() {
        if (null != builder) // is null when a Tab
        {
            builder.Build(__Namespace, __id, __Value, sortStr);
            Configuration.getConfig().restoreCursor();
        } else {
            LOGGER.severe("LateCreateTask called, but builder was NULL");
        }
    }
}
