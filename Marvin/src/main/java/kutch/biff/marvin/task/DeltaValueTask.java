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

public class DeltaValueTask extends PulseTask {

    private String value1ID;
    private String value2ID;
    private String value1NS;
    private String value2NS;
    private boolean validVal;
    private String operation;

    public DeltaValueTask() {

    }

    public void SetFirstDatapoint(String ns, String id) {
        value1ID = id;
        value1NS = ns;
    }

    public void SetSecondDatapoint(String NS, String ID) {
        value2ID = ID;
        value2NS = NS;
    }

    @Override
    public boolean isValid() {
        validVal = value1ID != null && value2ID != null && value2NS != null && value1NS != null;

        return super.isValid() && validVal && operation != null;
    }

    @Override
    public void PerformTask() {
        String value1Str;
        String value2Str;
        double doubleVal1;
        double doubleVal2;

        synchronized (TASKMAN.getDataMgr()) {
            value1Str = TASKMAN.getDataMgr().GetValueForMath(value1ID, value1NS);
            value2Str = TASKMAN.getDataMgr().GetValueForMath(value2ID, value2NS);
            if (null == value1Str || null == value2Str) {
                LOGGER.warning(
                        "DeltaValue Task failed [" + getTaskID() + "] beause the data point does not exist (yet).");
                return;
            }

            try {
                doubleVal1 = Double.parseDouble(value1Str);
            } catch (NumberFormatException ex) {
                LOGGER.warning("Attempted DeltaValue Task on Non numrice Data point: [" + value1NS + ":" + value1ID
                        + "] = " + value1Str);
                return;
            }
            try {
                doubleVal2 = Double.parseDouble(value2Str);
            } catch (NumberFormatException ex) {
                LOGGER.warning("Attempted DeltaValue Task on Non numrice Data point: [" + value2NS + ":" + value2ID
                        + "] = " + value2Str);
                return;
            }
            double newVal = 0.0;
            if ("Difference".equalsIgnoreCase(operation)) {
                newVal = Math.abs(doubleVal1 - doubleVal2);
            } else if ("PercentDifference".equalsIgnoreCase(operation)) {
                double diff = doubleVal1 - doubleVal2;

                newVal = diff / doubleVal1;

                newVal = newVal * -100;
            } else if ("FactorDifference".equalsIgnoreCase(operation)) {
                double diff = doubleVal1 - doubleVal2;

                newVal = (diff / doubleVal1 * -1) + 1;
            } else if ("PercentDifferenceAbs".equalsIgnoreCase(operation)) {
                double diff = Math.abs(doubleVal1 - doubleVal2);

                newVal = diff / doubleVal1;

                newVal = (newVal * 100) + 1;
            } else if ("FactorDifferenceAbs".equalsIgnoreCase(operation)) {
                double diff = Math.abs(doubleVal1 - doubleVal2);

                newVal = diff / doubleVal1;
            } else {
                LOGGER.warning("Unknown Error processing DeltaValue Task on Non numrice Data point: [" + _Namespace
                        + ":" + _ID + "]");
                return;
            }

            TASKMAN.getDataMgr().ChangeValue(_ID, _Namespace, Double.toString(newVal));
        }
    }

    public boolean SetOperation(String strOper) {
        if ("Difference".equalsIgnoreCase(strOper) || "PercentDifference".equalsIgnoreCase(strOper) || "FactorDifference".equalsIgnoreCase(strOper)) {
            operation = strOper;
        }

        return null != operation;
    }

}
