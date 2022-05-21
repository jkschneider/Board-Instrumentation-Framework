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
package kutch.biff.marvin.task;

import java.util.ArrayList;
import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.BaseWidget;

/**
 * @author Patrick Kutch
 */
public class PromptManager {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static PromptManager promptManager;

    public static PromptManager getPromptManager() {
        if (null == promptManager) {
            promptManager = new PromptManager();
        }
        return promptManager;
    }

    private ArrayList<BasePrompt> prompts;

    public PromptManager() {
        prompts = new ArrayList<>();
    }

    public boolean addPrompt(String id, BasePrompt prompt) {
        if (null == getPrompt(id)) {
            prompts.add(prompt);

        } else {
            LOGGER.warning("Tried to add duplicate Prompt: " + id + ". Ignoring.");
        }
        return true;
    }

    public boolean CreatePromptObject(String ID, FrameworkNode taskNode) {
        String strType = null;
        String strTitle = null;
        String strMessage = null;
        BasePrompt objPrompt = null;

        if (taskNode.hasAttribute("Type")) {
            strType = taskNode.getAttribute("Type");
        } else {
            LOGGER.severe("Invalid Prompt, no Type ID=" + ID);
            return false;
        }

        if ("ListBox".equalsIgnoreCase(strType)) {
            objPrompt = new Prompt_ListBox(ID);
        } else if ("InputBox".equalsIgnoreCase(strType)) {
            objPrompt = new Prompt_InputBox(ID);
        } else {
            LOGGER.severe("Invalid Prompt Object, Type[" + strType + "] is unknown ID=" + ID);
            return false;
        }

        if (taskNode.hasAttribute("Width")) {
            objPrompt.setWidth(BaseWidget.parsePercentWidth(taskNode, "Width"));
        }
        if (taskNode.hasAttribute("Height")) {
            objPrompt.setHeight(BaseWidget.parsePercentWidth(taskNode, "Height"));
        }

        for (FrameworkNode node : taskNode.getChildNodes(true)) {
            if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#Comment".equalsIgnoreCase(node.getNodeName())) {
                continue;
            }
            if ("StyleSheet".equalsIgnoreCase(node.getNodeName())) {
                objPrompt.setCssFile(node.getTextContent());
            } else if ("StyleOverride".equalsIgnoreCase(node.getNodeName())) {
                String styleList = "";

                for (FrameworkNode itemNode : node.getChildNodes()) {
                    if ("#Text".equalsIgnoreCase(itemNode.getNodeName())
                            || "#comment".equalsIgnoreCase(node.getNodeName())) {
                        continue;
                    }
                    if ("Item".equalsIgnoreCase(itemNode.getNodeName())) {
                        styleList += node.getTextContent() + ";";
                    } else {
                        LOGGER.severe("Unknown Tag under <Prompt> <StyleOverride>: " + itemNode.getNodeName());
                        return false;
                    }
                }
                objPrompt.setStyleOverride(styleList);
            } else if ("Title".equalsIgnoreCase(node.getNodeName())) {
                strTitle = node.getTextContent();
            } else if ("Message".equalsIgnoreCase(node.getNodeName())) {
                strMessage = node.getTextContent();
            } else if (objPrompt.HandlePromptSpecificConfig(node)) {
                continue;
            } else {
                LOGGER.config("Unknown tag in <Prompt> ID[" + ID + "] :" + node.getNodeName());
            }
        }
        if (null == strTitle) {
            LOGGER.severe("Prompt ID[+" + ID + "] has no title.");
            return false;
        }
        if (null == strMessage) {
            LOGGER.severe("Prompt ID[+" + ID + "] has no message.");
            return false;
        }
        objPrompt.setDlgTitle(strTitle);
        objPrompt.setMessage(strMessage);
        return addPrompt(ID, objPrompt);
    }

    public BasePrompt getPrompt(String ID) {
        for (BasePrompt prompt : prompts) {
            if (ID.equalsIgnoreCase(prompt.toString())) {
                return prompt;
            }
        }
        return null;
    }

}
