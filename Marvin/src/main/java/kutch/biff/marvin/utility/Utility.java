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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;

import kutch.biff.marvin.logger.MarvinLogger;

/**
 * @author Patrick.Kutch@gmail.com
 */
public final class Utility {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static String keyConjunction = "MarvinKeyJoinerString";

    public static String combineWildcards(String s1, String s2) {
        if (s1.contains("*")) {
            s1 = s1.replace("*", s2);
        }
        return s1;
    }

    public static String generateKey(String s1, String s2) {
        return s1.toUpperCase() + Utility.__KeyConjunction + s2.toUpperCase();
    }

    public static String[] splitKey(String key) {
        String[] parts = key.split(Utility.__KeyConjunction);
        return parts;
    }

    public static boolean ValidateAttributes(String[] validAttributes, FrameworkNode node) {
        boolean retVal = true;

        if (node.hasAttributes()) {
            NamedNodeMap attrs = node.GetNode().getAttributes();

            for (int oLoop = 0; oLoop < attrs.getLength(); oLoop++) {
                boolean found = false;
                Attr attribute = (Attr) attrs.item(oLoop);
                for (int iLoop = 0; iLoop < validAttributes.length; iLoop++) // compare to list of valid
                {
                    if (0 == validAttributes[iLoop].compareToIgnoreCase(attribute.getName())) // 1st check case
                    // independent just for
                    // fun
                    {
                        found = true;
                        break;
                    }
                }
                if (false == found) {
                    if ("CurrentValueAlias".equalsIgnoreCase(attribute.getName())
                            || "CurrentCountAlias".equalsIgnoreCase(attribute.getName())) {
                        LOGGER.warning("Unknown XML Attribute for " + node.getNodeName() + " found: "
                                + attribute.getName() + ". Ignoring.");

                    }
                }
            }
        }

        return retVal;
    }

    public static boolean ValidateAttributes(String[] ValidAttributes, String[] moreAttributes, FrameworkNode node) {
        if (null == moreAttributes) {
            return ValidateAttributes(ValidAttributes, node);
        }
        ArrayList<String> attributes = new ArrayList<>(Arrays.asList(ValidAttributes));
        attributes.addAll(Arrays.asList(moreAttributes));

        return ValidateAttributes(attributes.toArray(new String[attributes.size()]), node);
    }

    private Utility() {
    }

}
