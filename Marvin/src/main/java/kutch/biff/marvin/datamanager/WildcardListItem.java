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
 *
 * @author Patrick Kutch
 */
public class WildcardListItem
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    private String _WildCard;
    private DataSet _DataSet;

    public WildcardListItem(String _WildCard)
    {
        this._WildCard = _WildCard;
        this._DataSet = new DataSet();
    }
    
    public boolean Matches(String Wildcard)
    {
        try
        {
            return Wildcard.toUpperCase().matches(_WildCard.toUpperCase()); // does the RegEx match
            
        }
        catch (Exception ex)
        {
            LOGGER.severe("Invalid RegEx wildcard pattern: " + _WildCard);
        }
        
        return false;
    }

    public String getWildCard()
    {
        return _WildCard;
    }

    public DataSet getDataSet()
    {
        return _DataSet;
    }
    
}
