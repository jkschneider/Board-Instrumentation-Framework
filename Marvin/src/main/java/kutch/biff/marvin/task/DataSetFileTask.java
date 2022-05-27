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

import static kutch.biff.marvin.widget.BaseWidget.convertToFileOSSpecific;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Patrick.Kutch@gmail.com
 */
public class DataSetFileTask extends BaseTask {
    private int _interval;
    private int repeatCount;
    private String strFileName;
    private double fluxRangeLower;
    private double fluxRangeUpper;

    public DataSetFileTask(String inpFile, int interval) {
        repeatCount = 0;
        strFileName = inpFile;
        _interval = interval;
        fluxRangeLower = fluxRangeUpper = 0;
    }

    private int HandleDataFile(String inpFile) {
        int addedCount = 0;
        List<String[]> dataSets = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(inpFile))) {
            String line = "";
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }
                dataSets.add(line.split(","));
                addedCount += 1;
            }
        } catch (IOException ex) {
            Logger.getLogger(DataSetFileTask.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (String[] dataList : dataSets) {
            if (dataList.length < 3) {
                LOGGER.severe("Invalid datalist DataSetFileTask in file " + inpFile + ". List: " + dataList.toString());
                continue;
            }
            String namespace = dataList[0];
            String id = dataList[1];
            String[] dataPoints = new String[dataList.length - 2];
            for (int index = 2; index < dataList.length; index++) {
                dataPoints[index - 2] = dataList[index];
            }
            WalkDataListTask dlTask = new WalkDataListTask(namespace, id, dataPoints, _interval, repeatCount,
                    fluxRangeLower, fluxRangeUpper);
            TASKMAN.AddDeferredTaskObject(dlTask);
        }

        return addedCount;
    }

    @Override
    public void PerformTask() {
        String fname = getDataValue(strFileName);
        fname = convertToFileOSSpecific(strFileName);
        @SuppressWarnings("unused")
        int count = HandleDataFile(fname);
    }

    public void setFluxRange(double lower, double upper) {
        fluxRangeLower = lower;
        fluxRangeUpper = upper;
    }

    public void setRepeatCount(int count) {
        repeatCount = count;
    }
}
