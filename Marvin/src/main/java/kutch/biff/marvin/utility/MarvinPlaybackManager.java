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
package kutch.biff.marvin.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;

/**
 * @author Patrick.Kutch@gmail.com
 */
public final class MarvinPlaybackManager {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static final MarvinPlaybackManager _inst = new MarvinPlaybackManager();

    public static MarvinPlayback getMarvinPlayback(String strName) {
        if (!_inst.playbackInstancesMap.containsKey(strName.toUpperCase())) {
            MarvinPlayback mp = new MarvinPlayback(strName);
            _inst.playbackInstancesMap.put(strName.toUpperCase(), mp);
        }
        return _inst.playbackInstancesMap.get(strName.toUpperCase());
    }

    private Map<String, MarvinPlayback> playbackInstancesMap;

    private MarvinPlaybackManager() {
        playbackInstancesMap = new HashMap<>();
    }

    public int getCount() {
        return _inst.playbackInstancesMap.size();
    }

    public void stopAll() {
        for (String key : _inst.playbackInstancesMap.keySet()) {
            MarvinPlayback mp = _inst.playbackInstancesMap.get(key);
            if (null != mp) {
                mp.stopPlayback();
            }
        }
    }
}
