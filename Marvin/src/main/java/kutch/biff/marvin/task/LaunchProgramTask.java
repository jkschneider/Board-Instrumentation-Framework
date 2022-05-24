/*
 * ##############################################################################
 * #  Copyright (c) 2017 Intel Corporation
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

/**
 * @author Patrick
 */
public class LaunchProgramTask extends BaseTask {
    private String application;

    public LaunchProgramTask() {
        application = null;
    }

    public boolean isValid() {
        return null != application;
    }

    @Override
    public void PerformTask() {
        Runtime rt = Runtime.getRuntime();
        String[] execString = new String[getParams().size() + 1];
        execString[0] = application;
        for (int iLoop = 0; iLoop < getParams().size(); iLoop++) {
            execString[iLoop + 1] = getParams().get(iLoop).toString();
        }

        try {
            rt.exec(execString);
        } catch (Exception ex) {
            LOGGER.severe("Error trying to launch program: " + execString);
        }

    }

    public boolean SetApplication(String strApplication) {
        if (null != application) {
            LOGGER.severe("Application already defined for RunProgram Task");
            return false;
        }
        application = strApplication;
        return true;
    }

}
