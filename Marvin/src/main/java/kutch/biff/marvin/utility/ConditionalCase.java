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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.TaskManager;

/**
 * @author Patrick Kutch
 *
 *
 * <Conditional Type="CASE"> <MinionSrc Namespace="$(Namespace)" ID=
 * "$(Status).background.warp.core" /> <Case Value="0">MyTask</Value>
 * <Case Value="1">MyTask1</Value> <Case Value="2">MyTask2</Value>
 * <Default>myDefTask</Default> </Conditional>
 */

public class ConditionalCase extends Conditional {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    public static ConditionalCase BuildConditionalCase(FrameworkNode condNode) {
        ConditionalCase objCase = new ConditionalCase(Type.CASE);
        if (objCase.ReadMinionSrc(condNode)) {
            String strValue = null;
            String strTask = null;
            for (FrameworkNode node : condNode.getChildNodes()) {
                if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#Comment".equalsIgnoreCase(node.getNodeName())) {
                    continue;
                }
                if ("MinionSrc".equalsIgnoreCase(node.getNodeName())) {
                    continue;
                }

                if ("Case".equalsIgnoreCase(node.getNodeName())) {
                    if (node.hasAttribute("Value")) {
                        strValue = node.getAttribute("Value");
                    } else {
                        LOGGER.severe("Conditional CASE has <Case> item without a Value defined");
                        objCase = null;
                        break;
                    }
                    strTask = node.getTextContent();
                    if (!objCase.AddNewCaseStatement(strValue, strTask)) {
                        objCase = null;
                        break;
                    }
                } else if ("Default".equalsIgnoreCase(node.getNodeName())) {
                    if (!objCase.SetDefaultTask(node.getTextContent())) {
                        objCase = null;
                        break;
                    }
                } else {
                    LOGGER.config("Unknown Conditional Case item: " + node.getNodeName() + " - ignoring.");
                }
            }
        } else {
            objCase = null;
        }
        return objCase;
    }

    private final TaskManager taskman = TaskManager.getTaskManager();
    private final ArrayList<String> caseValues;
    private final HashMap<String, String> tasks;

    private String defaultTask;

    public ConditionalCase(Type type) {
        super(type, true);
        caseValues = new ArrayList<>();
        tasks = new HashMap<>();
        defaultTask = null;
    }

    protected boolean AddNewCaseStatement(String compareValue, String Task) {
        String strValue;
        if (isCaseSensitive()) {
            strValue = compareValue;
        } else {
            strValue = compareValue.toUpperCase();
        }
        if (tasks.containsKey(strValue)) {
            LOGGER.severe("Conditional CASE has duplicate <Case> value.");
            return false;
        }
        tasks.put(strValue, Task);
        caseValues.add(strValue);

        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ConditionalCase other = (ConditionalCase) obj;
        if (!Objects.equals(this.defaultTask, other.defaultTask)) {
            return false;
        }
        if (!Objects.equals(this.caseValues, other.caseValues)) {
            return false;
        }
        return !(!Objects.equals(this.tasks, other.tasks));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.caseValues);
        hash = 23 * hash + Objects.hashCode(this.tasks);
        hash = 23 * hash + Objects.hashCode(this.defaultTask);
        return hash;
    }

    @Override
    protected void Perform(String rawValue) {
        String strValue = rawValue;
        if (!isCaseSensitive()) {
            strValue = rawValue.toUpperCase();
        }

        for (String strCompare : caseValues) {
            if (strCompare.equals(strValue)) {
                String strTask = tasks.get(strCompare);
                taskman.AddDeferredTask(strTask);
                return;
            }
        }
        if (null != defaultTask) {
            taskman.AddDeferredTask(defaultTask); // no match, so do defautl
        }
    }

    public boolean SetDefaultTask(String strTask) {
        if (null == defaultTask) {
            defaultTask = strTask;
            return true;
        }
        LOGGER.severe("Conditional CASE can't have more than one Default Task.");
        return false;
    }
}
