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

/**
 * @author Patrick
 */
public class RandomTask extends BaseTask {
    class RandomSet {

        public String TaskID;
        public double Weight;
    }

    private final TaskManager taskman = TaskManager.getTaskManager();

    private ArrayList<RandomSet> taskList;
    private boolean wieghtAdjusted;

    public RandomTask() {
        taskList = new ArrayList<>();
        wieghtAdjusted = false;
    }

    public void AddTask(String strTaskID, double weight) {
        RandomSet objSet = new RandomSet();
        objSet.TaskID = strTaskID;
        objSet.Weight = weight;
        taskList.add(objSet);
    }

    private void AdjustWeight() {
        if (wieghtAdjusted) {
            return;
        }
        wieghtAdjusted = true;
        double totalWeight = 0.0d;
        int notWeightedCount = 0;
        for (RandomSet objRandomSet : taskList) {
            totalWeight += objRandomSet.Weight;
            if (0 == objRandomSet.Weight) {
                notWeightedCount++;
            }
        }
        if (notWeightedCount > 0) {
            double defaultWeight = (101.0d - totalWeight) / notWeightedCount;
            if (defaultWeight < 1.0d) {
                defaultWeight = 1.0d;
            }
            for (RandomSet objRandomSet : taskList) {
                if (0 == objRandomSet.Weight) {
                    objRandomSet.Weight = defaultWeight;
                }
            }
        }

    }

    @Override
    public void PerformTask() {
        double totalWeight = 0.0d;
        AdjustWeight();
        for (RandomSet objRandomSet : taskList) {
            totalWeight += objRandomSet.Weight;
        }
// Now choose a random item
        int randomIndex = -1;
        double random = Math.random() * totalWeight;
        for (int index = 0; index < taskList.size(); ++index) {
            random -= taskList.get(index).Weight;
            if (random <= 0.0d) {
                randomIndex = index;
                break;
            }
        }
        String strTaskToRun = taskList.get(randomIndex).TaskID;
        taskman.PerformTask(strTaskToRun);
    }

}
