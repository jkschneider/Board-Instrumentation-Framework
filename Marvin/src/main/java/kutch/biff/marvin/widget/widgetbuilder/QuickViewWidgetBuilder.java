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
package kutch.biff.marvin.widget.widgetbuilder;

import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.QuickViewWidget;

/**
 * @author Patrick Kutch
 */
public final class QuickViewWidgetBuilder {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    public static QuickViewWidget Build(FrameworkNode masterNode, String widgetDefFilename) {
        QuickViewWidget _widget = new QuickViewWidget();
        for (FrameworkNode node : masterNode.getChildNodes()) {
            if (BaseWidget.HandleCommonDefinitionFileConfig(_widget, node)) {
            } else if ("#comment".equalsIgnoreCase(node.getNodeName())) {
            } else if ("RowWidth".equalsIgnoreCase(node.getNodeName())) {
                String str = node.getTextContent();
                try {
                    _widget.setRowWidth(Integer.parseInt(str));
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Invalid <RowWidth> in QuickViewWidget Widget Definition File : " + str);
                    return null;
                }
            } else if ("EvenBackgroundStyle".equalsIgnoreCase(node.getNodeName())) {
                _widget.setEvenBackgroundStyle(node.getTextContent());
            } else if ("EvenIDStyle".equalsIgnoreCase(node.getNodeName())) {
                _widget.setEvenIDStyle(node.getTextContent());
            } else if ("EvenDataStyle".equalsIgnoreCase(node.getNodeName())) {
                _widget.setEvenDataStyle(node.getTextContent());
            } else if ("OddBackgroundStyle".equalsIgnoreCase(node.getNodeName())) {
                _widget.setOddBackgroundStyle(node.getTextContent());
            } else if ("OddIDStyle".equalsIgnoreCase(node.getNodeName())) {
                _widget.setOddIDStyle(node.getTextContent());
            } else if ("OddDataStyle".equalsIgnoreCase(node.getNodeName())) {
                _widget.setOddDataStyle(node.getTextContent());
            } else if ("Order".equalsIgnoreCase(node.getNodeName())) {
                String strVal = node.getTextContent();
                if (strVal.equalsIgnoreCase(QuickViewWidget.SortMode.Ascending.toString())) {
                    _widget.setSortMode(QuickViewWidget.SortMode.Ascending);
                } else if (strVal.equalsIgnoreCase(QuickViewWidget.SortMode.Descending.toString())) {
                    _widget.setSortMode(QuickViewWidget.SortMode.Descending);
                } else if (strVal.equalsIgnoreCase(QuickViewWidget.SortMode.None.toString())) {
                    _widget.setSortMode(QuickViewWidget.SortMode.None);
                } else {
                    LOGGER.severe("Invalid <Order> Tag in QuickViewWidget Widget Definition File. " + strVal);
                    return null;
                }
            } else {
                LOGGER.severe("Invalid QuickViewWidget Widget Definition File.  Unknown Tag: " + node.getNodeName());
                return null;
            }

        }
        return _widget;
    }

    private QuickViewWidgetBuilder() {
    }

}
