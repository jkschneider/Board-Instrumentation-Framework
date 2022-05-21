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
package kutch.biff.marvin.task;

import java.util.Random;

/**
 * @author Patrick.Kutch@gmail.com
 */
public class WalkDataListTask extends BaseTask {

    private int _interval;
    private int _RepeatCount;
    private int currLoopCount;
    private int currIndex;
    private double fluxRangeLower;
    private double fluxRangeUpper;
    private double rangeMin;
    private double rangeMax;
    private String[] dataSet;
    private String _Namespace;
    private String _id;
    private Random rndObj;

    public WalkDataListTask(String namespace, String id, String[] dataset, int interval, int repeatCount,
                            double lowerFlux, double upperFlux) {
        _Namespace = namespace;
        _id = id;
        dataSet = dataset;
        _interval = interval;
        _RepeatCount = repeatCount;
        currLoopCount = 1;
        currIndex = 0;
        fluxRangeLower = lowerFlux;
        fluxRangeUpper = upperFlux;

        boolean first = true;

        if (fluxRangeLower != fluxRangeUpper) {
            try {
                for (String val : dataSet) {
                    double dVal = Double.parseDouble(val);
                    if (first) {
                        first = false;
                        rangeMin = dVal;
                        rangeMax = dVal;
                    } else if (dVal < rangeMin) {
                        rangeMin = dVal;
                    } else if (dVal > rangeMax) {
                        rangeMax = dVal;
                    }
                }
                rndObj = new Random();
            } catch (NumberFormatException ex) {
            }
        }
    }

    private String calcFluxValue(String dataPoint) {
        try {
            double dVal = Double.parseDouble(dataPoint);
            double modifier = rndObj.doubles(fluxRangeLower, fluxRangeUpper).iterator().next();
            dVal += modifier;
            if (dVal < rangeMin || dVal > rangeMax) {
                // modified value falls outside of data range, so don't modify
            } else {
                return Double.toString(dVal);
            }
        } catch (NumberFormatException ex) {
        }
        return dataPoint;
    }

    @Override
    public void PerformTask() {
        MarvinTask mt = new MarvinTask();
        String dataPoint = dataSet[currIndex];
        if (null != rndObj) {
            dataPoint = calcFluxValue(dataPoint);
        }
        mt.AddDataset(_id, _Namespace, dataPoint);
        TaskManager.getTaskManager().AddDeferredTaskObject(mt);
        currIndex++;
        if (currIndex >= dataSet.length) // went through them all
        {
            currLoopCount++;
            currIndex = 0;
            if (_RepeatCount == 0 || currLoopCount <= _RepeatCount) {
                // let fall through
            } else {
                return; // no more
            }
        }
        // call this task again in interval time
        TASKMAN.AddPostponedTask(this, _interval);
    }
}
