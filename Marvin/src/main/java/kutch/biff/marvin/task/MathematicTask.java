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
 * #    Performs simple mathematic operations on a data point
 * ##############################################################################
 */
package kutch.biff.marvin.task;

/**
 * @author Patrick
 */
public class MathematicTask extends PulseTask {
    private double value;
    private boolean validVal;
    private String operation;

    public MathematicTask() {

    }

    @Override
    public boolean isValid() {
        return super.isValid() && validVal && operation != null;
    }

    @Override
    public void PerformTask() {
        String currValueStr;
        double doubleVal;

        synchronized (TASKMAN.getDataMgr()) {
            currValueStr = TASKMAN.getDataMgr().GetValueForMath(_ID, _Namespace);

            if (null == currValueStr) {
                LOGGER.warning(
                        "Mathematic Task failed [" + getTaskID() + "] beause the data point does not exist (yet).");
                return;
            }

            try {
                doubleVal = Double.parseDouble(currValueStr);
            } catch (NumberFormatException ex) {
                LOGGER.warning("Attempted Mathematic Task on Non numrice Data point: [" + _Namespace + ":" + _ID
                        + "] = " + currValueStr);
                return;
            }
            double newVal = 0.0;
            if ("Add".equalsIgnoreCase(operation)) {
                newVal = doubleVal + value;
            } else if ("Subtract".equalsIgnoreCase(operation)) {
                newVal = doubleVal - value;
            } else if ("Multiply".equalsIgnoreCase(operation)) {
                newVal = doubleVal * value;
            } else {
                LOGGER.warning("Unknown Error processing Mathematic Task on Non numrice Data point: [" + _Namespace
                        + ":" + _ID + "]");
                return;
            }
            int intVal = (int) newVal;
            TASKMAN.getDataMgr().ChangeValue(_ID, _Namespace, Integer.toString(intVal));
        }
    }

    public boolean SetOperation(String strOper) {
        if ("Add".equalsIgnoreCase(strOper) || "Subtract".equalsIgnoreCase(strOper)
                || "Multiply".equalsIgnoreCase(strOper)) {
            operation = strOper;
        }

        return null != operation;
    }

    public boolean setValue(String strValue) {
        try {
            value = Double.parseDouble(strValue);
            validVal = true;
        } catch (NumberFormatException ex) {

        }
        return validVal;
    }
}
