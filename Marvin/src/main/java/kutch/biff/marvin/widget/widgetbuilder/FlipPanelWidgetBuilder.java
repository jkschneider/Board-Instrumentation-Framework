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

import javafx.geometry.Orientation;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.PanelSideInfo;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.FlipPanelWidget;

/**
 * @author Patrick Kutch
 */
public final class FlipPanelWidgetBuilder {

    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    public static FlipPanelWidget Build(FrameworkNode masterNode, String widgetDefFilename) {
        FlipPanelWidget _panel = new FlipPanelWidget();
        for (FrameworkNode node : masterNode.getChildNodes()) {
            if (BaseWidget.HandleCommonDefinitionFileConfig(_panel, node)) {
                continue;
            }
            if ("AnimationDuration".equalsIgnoreCase(node.getNodeName())) {
                String str = node.getTextContent();
                try {
                    _panel.setAnimationDuration(Double.parseDouble(str));
                } catch (Exception ex) {
                    LOGGER.severe("Invlid value for <AnimationDuration> tag for FlipPanel Widget");
                    return null;
                }
            } else if ("RotationDirection".equalsIgnoreCase(node.getNodeName())) {
                String str = node.getTextContent();
                if (0 == str.compareToIgnoreCase("Horizontal")) {
                    _panel.setOrientation(Orientation.HORIZONTAL);
                } else if (0 == str.compareToIgnoreCase("Vertical")) {
                    _panel.setOrientation(Orientation.VERTICAL);
                } else {
                    LOGGER.severe(
                            "Invalid Orientation in FlipPanel Widget Definition File. Should be Horizontal or Vertical, not : "
                                    + str);
                    return null;
                }
            } else if ("FrontButton".equalsIgnoreCase(node.getNodeName())
                    || "BackButton".equalsIgnoreCase(node.getNodeName())) {
                String btnText = null;
                String styleFile = null;
                String styleID = null;
                String location = null;

                if (node.hasAttribute("Text")) {
                    btnText = node.getAttribute("Text");
                }
                if (node.hasAttribute("Position")) {
                    location = node.getAttribute("Position");
                } else {
                    LOGGER.severe("No Position set for FlipPanel Button");
                    return null;
                }
                for (FrameworkNode iNode : node.getChildNodes()) {
                    if ("#text".equalsIgnoreCase(iNode.getNodeName())
                            || "#comment".equalsIgnoreCase(iNode.getNodeName())) {
                        continue;
                    }
                    if ("Style".equalsIgnoreCase(iNode.getNodeName())) {
                        styleFile = iNode.getTextContent();
                        if (iNode.hasAttribute("ID")) {
                            styleID = iNode.getAttribute("ID");
                        }
                    }
                }
                if (null == location || null == styleFile) {
                    LOGGER.severe("Invalid Flip Panel side definition :" + node.getNodeName());
                    return null;
                }

                PanelSideInfo panel = new PanelSideInfo(location, btnText, styleID, styleFile);
                if ("FrontButton".equalsIgnoreCase(node.getNodeName())) {
                    _panel.setFrontInfo(panel);
                } else {
                    _panel.setBackInfo(panel);
                }
            } else {
                LOGGER.severe("Invalid FlipPanel Widget Definition File.  Unknown Tag: " + node.getNodeName());
                return null;
            }
        }
        return _panel;
    }

    private FlipPanelWidgetBuilder() {
    }

}
