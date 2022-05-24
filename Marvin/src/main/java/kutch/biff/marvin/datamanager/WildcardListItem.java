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

package kutch.biff.marvin.datamanager;

import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;

/**
 * @author Patrick Kutch
 */
public class WildcardListItem {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    private String _WildCard;
    private DataSet dataSet;

    public WildcardListItem(String wildCard) {
        this._WildCard = wildCard;
        this.dataSet = new DataSet();
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public String getWildCard() {
        return _WildCard;
    }

    public boolean Matches(String wildcard) {
        try {
            return wildcard.toUpperCase().matches(_WildCard.toUpperCase()); // does the RegEx match

        } catch (Exception ex) {
            LOGGER.severe("Invalid RegEx wildcard pattern: " + _WildCard);
        }

        return false;
    }

}
