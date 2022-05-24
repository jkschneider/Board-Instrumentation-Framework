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
package kutch.biff.marvin.configuration;

import static kutch.biff.marvin.widget.BaseWidget.convertToFileOSSpecific;
import static kutch.biff.marvin.widget.widgetbuilder.WidgetBuilder.OpenDefinitionFile;

import java.io.File;
import java.io.IOException;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.PromptManager;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.utility.AliasMgr;
import kutch.biff.marvin.utility.Conditional;
import kutch.biff.marvin.utility.DataPointGenerator;
import kutch.biff.marvin.utility.DynamicItemInfoContainer;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.GenerateDatapointInfo;
import kutch.biff.marvin.utility.GenerateDatapointInfo.ListSortMethod;
import kutch.biff.marvin.utility.Utility;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.DynamicTabWidget;
import kutch.biff.marvin.widget.TabWidget;
import kutch.biff.marvin.widget.widgetbuilder.OnDemandTabBuilder;
import kutch.biff.marvin.widget.widgetbuilder.OnDemandWidgetBuilder;
import kutch.biff.marvin.widget.widgetbuilder.WidgetBuilder;

/**
 * @author Patrick.Kutch@gmail.com
 */
public class ConfigurationReader {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static ConfigurationReader configReader;
    private static HashMap<Conditional, Conditional> conditionalMap = new HashMap<>();
    private static List<String> onDemandIDList = new ArrayList<>();

    private static Document _OpenXMLFile(String filename, boolean fReport) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            if (fReport) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            return null;
        }
        String fName = BaseWidget.convertToFileOSSpecific(filename);
        if (null == fName) {
            return null;
        }
        File file = new File(fName);
        if (file.exists()) {
            Document doc;
            try {
                doc = db.parse(file);
            } catch (SAXException | IOException ex) {
                if (fReport) {
                    LOGGER.severe(ex.toString());
                }
                return null;
            }
            if (fReport) {
                // LOGGER.info("Opening XML file: " + filename);
            }
            return doc;
        }
        if (fReport) {
            LOGGER.severe("Missing file: " + filename);
        }
        return null;
    }

    public static ConfigurationReader GetConfigReader() {
        return configReader;
    }

    public static ImageView GetImage(FrameworkNode node) {
        if ("Image".equalsIgnoreCase(node.getNodeName())) {
            String fName = node.getTextContent();
            double imageWidthConstraint = 0.0;
            double imageHeightConstraint = 0.0;

            Utility.ValidateAttributes(new String[]{"Height", "Width"}, node);
            if (node.hasAttribute("Width")) {
                try {
                    imageWidthConstraint = Double.parseDouble(node.getAttribute("Width"));
                } catch (Exception ex) {
                    LOGGER.severe("Widget Image has invalid Width specified: " + node.getAttribute("Width"));
                    return null;
                }
            }
            if (node.hasAttribute("Height")) {
                try {
                    imageHeightConstraint = Double.parseDouble(node.getAttribute("Height"));
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Widget Image has invalid Height specified: " + node.getAttribute("Height"));
                    return null;
                }
            }

            return ConfigurationReader.GetImage(fName, imageHeightConstraint, imageWidthConstraint);
        }
        return null;
    }

    public static ImageView GetImage(String imageFileName, double ImageHeightConstraint, double ImageWidthConstraint) {
        ImageView view = null;
        if (null != imageFileName) {
            String fname = convertToFileOSSpecific(imageFileName);
            File file = new File(fname);
            if (file.exists()) {
                String fn = "file:" + fname;
                Image img = new Image(fn);
                view = new ImageView(img);

                if (ImageHeightConstraint > 0) {
                    view.setFitHeight(ImageHeightConstraint);
                }
                if (ImageWidthConstraint > 0) {
                    view.setFitWidth(ImageWidthConstraint);
                }

            } else {
                LOGGER.severe("Invalid Image File specified for Widget: " + imageFileName);
            }
        }
        return view;
    }

    private static Pair<String, String> getNamespaceAndIdPattern(FrameworkNode node, boolean noID) {
        if (node.hasAttribute("Namespace")) {
            if (node.hasAttribute("ID") || !noID) {
                return new Pair<>(node.getAttribute("Namespace"), node.getAttribute("ID"));
            }
            if (noID) {
                return new Pair<>(node.getAttribute("Namespace"), null);
            }
        } else if (noID) // is ID List
        {
            if (node.hasAttribute("ID")) {
                return new Pair<>(null, node.getAttribute("ID"));
            }
        }
        return null;
    }

    private static boolean InList(List<String> list, String toCheck) {
        for (String item : list) {
            if (item.equalsIgnoreCase(toCheck)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Allows one to define the tab contents (widgets) in an onother file Node that
     * the tab attributes (hgp,vgap,id) must still be in Application.xml
     *
     * @param inputFilename
     * @return Base node with the config goodies
     */
    public static FrameworkNode OpenTabDefinitionFile(String inputFilename) {
        FrameworkNode TabNode = OpenDefinitionFile(inputFilename, "Tab");
        return TabNode;
    }

    public static Document OpenXMLFile(String filename) {
        return _OpenXMLFile(filename, true);
    }

    public static Document OpenXMLFileQuietly(String filename) {
        return _OpenXMLFile(filename, false);
    }

    private static Conditional ReadConditional(FrameworkNode condNode) {
        String strType = null;
        boolean caseSensitive = false;

        Utility.ValidateAttributes(new String[]{"CaseSensitive", "Type"}, condNode);
        if (!condNode.hasAttribute("type")) {
            LOGGER.severe("Conditional defined with no Type");
            return null;
        }
        if (condNode.hasAttribute("CaseSensitive")) {
            caseSensitive = condNode.getBooleanAttribute("CaseSensitive");
        }

        strType = condNode.getAttribute("type");
        Conditional.Type type = Conditional.GetType(strType);
        if (type == Conditional.Type.Invalid) {
            LOGGER.severe("Conditional defined with invalid type: " + strType);
            return null;
        }

        Conditional objConditional = Conditional.BuildConditional(type, condNode, true);
        if (null != objConditional) {
            objConditional.setCaseSensitive(caseSensitive);
        }

        return objConditional;
    }

    public boolean ReadConditionals(FrameworkNode parentNode) {
        for (FrameworkNode condNode : parentNode.getChildNodes("Conditional")) {
            Conditional objCond = ReadConditional(condNode);
            if (null == objCond) {
                return false;
            } else {
                if (conditionalMap.containsKey(objCond)) {
                    LOGGER.config(
                            "Duplicate conditional found.  Might want to check for multiple inclusions of same conditional. Conditional Information: "
                                    + objCond.toString());
                } else {
                    conditionalMap.put(objCond, objCond);
                }
                objCond.Enable();
            }
        }
        return true;
    }

    private static boolean ReadConditionals(Document doc) {
        /*
         * <Conditional type='IF_EQ' CaseSensitive="True"> <MinionSrc ID="myID"
         * Namespace="myNS"/> <Value> <MinionSrc ID="myID" Namespace="myNS"/> </Value>
         * or <Value>44</Value>
         *
         * <Then>Task12</Then> <Else>Task3</Else> </Conditional>
         */
        boolean retVal = true;

        // NodeList conditionals = doc.getElementsByTagName("Conditional");
        List<FrameworkNode> conditionals = FrameworkNode.GetChildNodes(doc, "Conditional");

        if (conditionals.size() < 1) {
            return true;
        }
        for (FrameworkNode condNode : conditionals)// int iLoop = 0; iLoop < conditionals.getLength(); iLoop++)
        {
            // FrameworkNode condNode = new FrameworkNode(conditionals.item(iLoop));
            Conditional objCond = ReadConditional(condNode);
            if (null == objCond) {
                retVal = false;
            } else {
                if (conditionalMap.containsKey(objCond)) {
                    LOGGER.config(
                            "Duplicate conditional found.  Might want to check for multiple inclusions of same conditional. Conditional Information: "
                                    + objCond.toString());
                } else {
                    conditionalMap.put(objCond, objCond);
                }
                objCond.Enable();
            }
        }

        return retVal;
    }

    public static List<DataPointGenerator> ReadDataPointsForTask(int itemIndex, String strInput, String strTaskText,
                                                                 String strTask) {
        List<DataPointGenerator> retList = new ArrayList<>();
        if (strInput.contains("^")) // might be replacement of ^Text or ^Task
        {
            if (strInput.contains("^Text")) {
                strInput = strInput.replace("^Text", strTaskText);
            }
            if (strInput.contains("^Task")) {
                strInput = strInput.replace("^Task", strTask);
            }
            if (strInput.contains("^Index")) {
                String strIndex = Integer.toString(itemIndex);
                strInput = strInput.replace("^Index", strIndex);
            }
        }
        String[] points = strInput.split("\\]");
        for (String point : points) {
            if (point.length() == 0) {
                continue;
            }
            if (point.charAt(0) == ',' && point.charAt(1) == '[') {
                point = point.substring(1);
            }
            if (point.charAt(0) == '[') {
                point = point.substring(1);
                String[] parts = point.split(",", 3);
                if (parts.length != 3) {
                    retList.clear();
                    return retList;
                }
                String namespace = parts[0];
                String ID = parts[1];
                String val = parts[2];
                retList.add(new DataPointGenerator(namespace, ID, val));
            } else {
                retList.clear();
                return retList;
            }
        }

        return retList;
    }

    public static GenerateDatapointInfo ReadGenerateDatapointInfo(FrameworkNode inputNode) {
        ArrayList<Pair<String, String>> maskList = new ArrayList<>();
        ArrayList<Pair<String, String>> excludeList = new ArrayList<>();
        @SuppressWarnings("unused")
        ListSortMethod sortPolicy = ListSortMethod.ASCENDING;
        int precision = -1;
        boolean hasListEntry = false;
        int listEntry = 0;

        if (!inputNode.hasAttribute("Method")) {
            LOGGER.severe("GenerateDatapoint did not specify the Method of generate. ");
            return null;
        }

        boolean isGenerateNamespaceList = "MakeNamespaceList".equalsIgnoreCase(inputNode.getAttribute("Method"))
                || "MakeIDList".equalsIgnoreCase(inputNode.getAttribute("Method"));
        String strMethod = inputNode.getAttribute("Method");

        Pair<String, String> genDPInfo = getNamespaceAndIdPattern(inputNode, false);
        Pair<ValueRange, String> rangeInfo = WidgetBuilder.ReadMinionSrcIndexInfo(inputNode);
        if (null == genDPInfo) {
            LOGGER.severe("Invalid GenerateDatapoint.");
            return null;
        }

        for (FrameworkNode node : inputNode.getChildNodes(true)) {
            if ("InputPattern".equalsIgnoreCase(node.getNodeName())) {
                Pair<String, String> input = getNamespaceAndIdPattern(node, isGenerateNamespaceList);
                if (null == input) {
                    LOGGER.severe(String.format("Invalid GenerateDatapoint %s:%s -->%s", genDPInfo.getKey(),
                            genDPInfo.getValue(), node.getAttributeList()));
                    return null;
                }
                maskList.add(input);
            } else if ("ExcludePattern".equalsIgnoreCase(node.getNodeName())) {
                Pair<String, String> exclude = getNamespaceAndIdPattern(node, isGenerateNamespaceList);
                if (null == exclude) {
                    LOGGER.severe(String.format("Invalid GenerateDatapoint %s:%s -->%s", genDPInfo.getKey(),
                            genDPInfo.getValue(), node.getAttributeList()));
                    return null;
                }
                excludeList.add(exclude);
            } else if ("ListEntry".equalsIgnoreCase(node.getNodeName())) {
                try {
                    listEntry = node.getIntegerContent();
                    hasListEntry = true;
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Invalid ListEntry specified for <GenerateDatapoint>: " + node.getTextContent());
                    return null;
                }
            } else if ("Decimals".equalsIgnoreCase(node.getNodeName())) {
                try {
                    precision = node.getIntegerContent();
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Invalid Decimals specified for <GenerateDatapoint>: " + node.getTextContent());
                    return null;
                }
            } else if ("Refresh".equalsIgnoreCase(node.getNodeName())) { // handle below
            } else if ("Sort".equalsIgnoreCase(node.getNodeName())) {
                if ("MakeList".equalsIgnoreCase(strMethod) || "MakeNamespaceList".equalsIgnoreCase(strMethod)
                        || "MakeIDList".equalsIgnoreCase(strMethod)) {
                    String strSort = node.getTextContent();
                    if ("Ascending".equalsIgnoreCase(strSort)) {
                        sortPolicy = ListSortMethod.ASCENDING;
                    } else if ("Descending".equalsIgnoreCase(strSort)) {
                        sortPolicy = ListSortMethod.DESCENDING;
                    } else if ("None".equalsIgnoreCase(strSort)) {
                        sortPolicy = ListSortMethod.NONE;
                    } else {
                        LOGGER.severe("Invalid Sort Method for Generate Datapoint: " + strSort);
                        return null;
                    }
                } else {
                    LOGGER.warning("Specified Sort Method for Generate Datapoint, however " + strMethod
                            + " does not support sorting.  Ignoring.");
                }
            } else {
                LOGGER.severe("Unknown entry in <GenerateDatapoint>: " + node.getNodeName());
                return null;
            }
        }
        GenerateDatapointInfo info = new GenerateDatapointInfo(genDPInfo.getKey(), genDPInfo.getValue(), maskList,
                excludeList, rangeInfo.getKey(), rangeInfo.getValue());

        if (hasListEntry) {
            if (!info.setListEntry(listEntry)) {
                LOGGER.severe("Invalid ListEntry specified for <GenerateDatapoint>: " + Integer.toString(listEntry));
                return null;
            }
            if (inputNode.hasAttribute("Separator")) {
                String token = inputNode.getAttribute("Separator");
                info.setSplitToken(token);
            } else {
                info.setSplitToken(",");
            }
        }

        if ("Add".equalsIgnoreCase(inputNode.getAttribute("Method"))) {
            info.setMethod(GenerateDatapointInfo.GenerateMethod.ADD);
        } else if ("Average".equalsIgnoreCase(inputNode.getAttribute("Method"))) {
            info.setMethod(GenerateDatapointInfo.GenerateMethod.AVERAGE);
        } else if ("MakeList".equalsIgnoreCase(inputNode.getAttribute("Method"))) {
            info.setMethod(GenerateDatapointInfo.GenerateMethod.MAKE_LIST);
        } else if ("MakeNamespaceList".equalsIgnoreCase(inputNode.getAttribute("Method"))) {
            info.setMethod(GenerateDatapointInfo.GenerateMethod.MAKE_NAMESPACE_LIST);
        } else if ("MakeIDList".equalsIgnoreCase(inputNode.getAttribute("Method"))) {
            info.setMethod(GenerateDatapointInfo.GenerateMethod.MAKE_ID_LIST);
        } else if ("Proxy".equalsIgnoreCase(inputNode.getAttribute("Method"))) {
            info.setMethod(GenerateDatapointInfo.GenerateMethod.PROXY);
            if (inputNode.hasAttribute("ProxyID")) {
                String proxyID = inputNode.getAttribute("ProxyID");
                info.setProxyID(proxyID);
            } else {
                LOGGER.warning(
                        "GenerateDatapoint [Proxy] did not have a ProxyID, you will be unable to change it with a task.");
                String proxyID = Long.toString(new Random().nextLong());
                info.setProxyID(proxyID);
            }
        } else if ("SplitList".equalsIgnoreCase(inputNode.getAttribute("Method"))) {
            info.setMethod(GenerateDatapointInfo.GenerateMethod.SPLIT_LIST);
            if (inputNode.hasAttribute("Separator")) {
                String token = inputNode.getAttribute("Separator");
                info.setSplitToken(token);
            } else {
                info.setSplitToken(",");
            }
        } else if ("GetListSize".equalsIgnoreCase(inputNode.getAttribute("Method"))) {
            info.setMethod(GenerateDatapointInfo.GenerateMethod.GET_LIST_SIZE);
        } else if ("MakeIndexList".equalsIgnoreCase(inputNode.getAttribute("Method"))) {
            info.setMethod(GenerateDatapointInfo.GenerateMethod.MAKE_INDEX_LIST);
        } else {
            LOGGER.severe("Invalid Method specified for GenerateDatapoint: " + inputNode.getAttribute("Method"));
            return null;
        }
        if (inputNode.hasAttribute("Scale")) {
            try {
                double scale = Double.parseDouble(inputNode.getAttribute("Scale"));
                info.setScale(scale);
            } catch (Exception ex) {

            }
        }
        if (inputNode.hasChild("Refresh")) {
            FrameworkNode rfNode = inputNode.getChild("Refresh");
            if (rfNode.hasAttribute("Frequency")) {
                int freq = rfNode.getIntegerAttribute("Frequency", -1);
                if (freq > 0) {
                    info.setMinFrequency(freq);
                } else {
                    return null;
                }
            } else {
                LOGGER.severe("<GenerateDatapoint> specified <Refresh> without a Frequency.");
                return null;
            }
            if (rfNode.hasAttribute("Policy")) {
                String strPolicy = rfNode.getAttribute("Policy");
                if ("REMOVE".equalsIgnoreCase(strPolicy)) {
                    info.setPolicy(GenerateDatapointInfo.RefreshPolicy.REMOVE);
                } else if ("REUSE".equalsIgnoreCase(strPolicy)) {
                    info.setPolicy(GenerateDatapointInfo.RefreshPolicy.REUSE);
                } else if ("ZERO_OUT".equalsIgnoreCase(strPolicy)) {
                    info.setPolicy(GenerateDatapointInfo.RefreshPolicy.ZERO_OUT);
                } else {
                    LOGGER.severe("<GenerateDatapoint> specified invalid <Refresh> without a policy: " + strPolicy);
                    return null;
                }
            } else {
                LOGGER.severe("<GenerateDatapoint> specified <Refresh> without a Policy.");
                return null;
            }
        }

        info.setPrecision(precision);
        return info;
    }

    public static boolean ReadGenerateDataPoints(FrameworkNode node) {
        GenerateDatapointInfo info = ConfigurationReader.ReadGenerateDatapointInfo(node);
        if (null == info) {
            return false;
        }
        return DataManager.getDataManager().AddGenerateDatapointInfo(info);
    }

    /**
     * *
     *
     * @param sourceNode
     * @return
     */
    public static DynamicItemInfoContainer ReadOnDemandInfo(FrameworkNode sourceNode) {
        ArrayList<String> namespaceMaskList = new ArrayList<>();
        ArrayList<String> namespaceExcludeList = new ArrayList<>();
        ArrayList<String> idMaskList = new ArrayList<>();
        ArrayList<String> idExcludeList = new ArrayList<>();

        for (FrameworkNode dynaNode : sourceNode.getChildNodes(true)) {
            if ("NamespaceTriggerPattern".equalsIgnoreCase(dynaNode.getNodeName())) {
                String mask = dynaNode.getTextContent();
                if (InList(namespaceMaskList, mask)) {
                    LOGGER.warning("On Demand item specified duplicate NamespaceTriggerPattern.  Ignoring.");
                } else {
                    namespaceMaskList.add(dynaNode.getTextContent());
                }
            } else if ("NamespaceTriggerExcludePattern".equalsIgnoreCase(dynaNode.getNodeName())) {
                String Exclude = dynaNode.getTextContent();
                if (InList(namespaceExcludeList, Exclude)) {
                    LOGGER.warning("On Demand item specified duplicate NamespaceTriggerExcludePattern.  Ignoring.");
                } else {
                    namespaceExcludeList.add(dynaNode.getTextContent());
                }

                if (InList(namespaceMaskList, Exclude)) {
                    LOGGER.warning(
                            "On Demand item specified duplicate NamespaceTriggerExcludePattern that matches NamespaceTriggerExcludePattern.");
                }
            } else if ("IDTriggerPattern".equalsIgnoreCase(dynaNode.getNodeName())) {
                String Mask = dynaNode.getTextContent();
                if (InList(idMaskList, Mask)) {
                    LOGGER.warning("On Demand item specified duplicate IDTriggerPattern.  Ignoring.");
                } else {
                    idMaskList.add(dynaNode.getTextContent());
                }

            } else if ("IDTriggerExcludePattern".equalsIgnoreCase(dynaNode.getNodeName())) {
                String Exclude = dynaNode.getTextContent();
                if (InList(idExcludeList, Exclude)) {
                    LOGGER.warning("On Demand item specified duplicate IDTriggerExcludePattern.  Ignoring.");
                } else {
                    idExcludeList.add(dynaNode.getTextContent());
                }

                if (InList(idMaskList, Exclude)) {
                    LOGGER.warning(
                            "On Demand item specified duplicate IDTriggerExcludePattern that matches IDTriggerExcludePattern.");
                }
            } else if ("StyleOverride-Odd".equalsIgnoreCase(dynaNode.getNodeName())
                    || "StyleOverride-Even".equalsIgnoreCase(dynaNode.getNodeName())) {
     if ("Growth".equalsIgnoreCase(dynaNode.getNodeName())) {

                } else {
                    LOGGER.warning("Invalid Tag found in <OnDemand>: " + dynaNode.getNodeName());
                }

            }
        }
        if (namespaceMaskList.isEmpty() && idMaskList.isEmpty()) {
            LOGGER.severe("On Demand item did not specify a namespace or ID Trigger pattern");
            return null;
        }

        Pair<ArrayList<String>, ArrayList<String>> namespaceCriterea;
        namespaceCriterea = new Pair<>(namespaceMaskList, namespaceExcludeList);
        Pair<ArrayList<String>, ArrayList<String>> idCriterea = new Pair<>(idMaskList, idExcludeList);

        DynamicItemInfoContainer dynaInfo = new DynamicItemInfoContainer(namespaceCriterea, idCriterea);

        dynaInfo.ReadStyles(sourceNode);

        if (sourceNode.hasAttribute("SortBy")) {
            String strSortBy = sourceNode.getAttribute("SortBy");
            if ("Namespace".equalsIgnoreCase(strSortBy)) {
                dynaInfo.setSortByMethod(DynamicItemInfoContainer.SortMethod.NAMESPACE);
            } else if ("ID".equalsIgnoreCase(strSortBy)) {
                dynaInfo.setSortByMethod(DynamicItemInfoContainer.SortMethod.ID);
            } else if ("Value".equalsIgnoreCase(strSortBy)) {
                dynaInfo.setSortByMethod(DynamicItemInfoContainer.SortMethod.VALUE);
            } else if ("None".equalsIgnoreCase(strSortBy)) {
                dynaInfo.setSortByMethod(DynamicItemInfoContainer.SortMethod.NONE);
            } else {
                LOGGER.severe("OnDemand item specified invalid SortBy: " + strSortBy + ". Ignoring.");
                dynaInfo.setSortByMethod(DynamicItemInfoContainer.SortMethod.NONE);
            }
        }

        if (sourceNode.hasAttribute("TriggeredIdToken")) {
            String strToken = sourceNode.getAttribute("TriggeredIdToken");
            dynaInfo.setToken(strToken);
        }

        return dynaInfo;
    }

    public static boolean ReadPrompt(FrameworkNode promptNode) {
        PromptManager promptman = PromptManager.getPromptManager();
        boolean retVal = true;

        Utility.ValidateAttributes(new String[]{"ID", "Type", "Height", "Width"}, promptNode);

        if (false == promptNode.hasAttribute("ID")) {
            LOGGER.warning("Prompt defined with no ID, ignoring");
            retVal = false;
        } else if (false == promptman.CreatePromptObject(promptNode.getAttribute("ID"), promptNode)) {
            retVal = false;
        }

        return retVal;
    }

    private static boolean ReadPrompts(Document doc) {
        boolean retVal = true;

        NodeList prompts = doc.getElementsByTagName("Prompt");
        if (prompts.getLength() < 1) {
            return true;
        }
        for (int iLoop = 0; iLoop < prompts.getLength(); iLoop++) {
            FrameworkNode promptNode = new FrameworkNode(prompts.item(iLoop));
            if (!ReadPrompt(promptNode)) {
                retVal = false;
                break;
            }
        }

        return retVal;
    }

    public static TabWidget ReadTab(FrameworkNode node, TabWidget tab, String id) {
        FrameworkNode tabNode = null;
        AliasMgr.getAliasMgr().PushAliasList(true);
        AliasMgr.getAliasMgr().AddAlias("TabID", id); // Make TabID an alias = to the ID :-)

        if (node.hasAttribute("File")) // can externally define widgets within
        {
            WidgetBuilder.StartReadingExternalFile(node);
            tabNode = OpenTabDefinitionFile(node.getAttribute("File"));
            if (null == tabNode) {
                LOGGER.severe("Invalid tab definition file: " + node.getAttribute("File"));
                return null;
            }
            if (tabNode.hasAttribute("OnDemandTask")) {
                tab.setOnDemandTask(tabNode.getAttribute("OnDemandTask"));
            }

            AliasMgr.getAliasMgr().AddAliasFromAttibuteList(node,
                    new String[]{"ID", "File", "Align", "hgap", "vgap", "Task"});

            if (false == AliasMgr.ReadAliasFromExternalFile(node.getAttribute("File"))) {
                return null;
            }
            if (!ConfigurationReader.ReadTasksFromExternalFile(node.getAttribute("File"))) {
                return null;
            }
        } else {
            Utility.ValidateAttributes(new String[]{"ID", "File", "Align", "hgap", "vgap", "Task"}, node);
            tabNode = node;
        }
        if (false == tab.LoadConfiguration(tabNode)) {
            return null;
        }
        if (node.hasAttribute("File")) {
            WidgetBuilder.DoneReadingExternalFile();
        }

        if (node.hasAttribute("Align")) {
            String str = node.getAttribute("Align");
            tab.setAlignment(str);
        }
        if (node.hasAttribute("task")) {
            tab.setOnActivateTask(node.getAttribute("task"));
        }
        if (node.hasAttribute("OnDemandTask")) {
            tab.setOnDemandTask(node.getAttribute("OnDemandTask"));
        }
        if (node.hasAttribute("hgap")) {
            try {
                if (!tab.parsehGapValue(node)) {
                    LOGGER.config("Setting hGap for Tab ID=" + tab.getMinionID() + " to " + node.getAttribute("hgap"));
                }
                // tab.sethGap(Integer.parseInt(node.getAttribute("hgap")));
            } catch (Exception ex) {
                LOGGER.warning("Tab ID=" + tab.getMinionID() + " has invalid hgap.  Ignoring");
            }
        }
        if (node.hasAttribute("vgap")) {
            try {
                if (!tab.parsevGapValue(node)) {
                    LOGGER.config("Setting vGap for Tab ID=" + tab.getMinionID() + " to " + node.getAttribute("vgap"));
                }
                // tab.setvGap(Integer.parseInt(node.getAttribute("vgap")));
                // LOGGER.config("Setting vGap for Tab ID=" + tab.getMinionID() + " to " +
                // node.getAttribute("vgap"));
            } catch (Exception ex) {
                LOGGER.warning("Tab ID=" + tab.getMinionID() + " has invalid vgap.  Ignoring");
            }
        }
        return tab;
    }

    /***
     * Reads at outtermost level of file
     *
     * @param doc
     * @return
     */
    private static boolean ReadTaskAndConditionals(Document doc) {
        boolean retVal = true;

        List<FrameworkNode> taskListNodes = FrameworkNode.GetChildNodes(doc, "TaskList");

        if (taskListNodes.size() < 1) {
            // LOGGER.info("No Tasks defined in config file.");
            // return true;
        }
        for (FrameworkNode taskNode : taskListNodes) {
            retVal = ConfigurationReader.ReadTaskList(taskNode);
            if (!retVal) {
                break;
            }
        }

        if (retVal) {
            retVal = ReadConditionals(doc);
        }

        return retVal;
    }

    public static boolean ReadTaskList(FrameworkNode taskNode) {
        TaskManager taskman = TaskManager.getTaskManager();
        boolean retVal = true;

        String taskID = null;
        String externFile = null;
        FrameworkNode nodeToPass = taskNode;

        Utility.ValidateAttributes(new String[]{"ID", "File", "PerformOnStartup", "PerformOnConnect", "stepped", "interval", "LoopTasks"},
                taskNode);
        if (false == taskNode.hasAttribute("ID")) {
            LOGGER.warning("Task defined with no ID, ignoring");
            return false;
        }
        taskID = taskNode.getAttribute("ID");
        if (taskNode.hasAttribute("File")) {
            externFile = taskNode.getAttribute("File");
            if (!ConfigurationReader.ReadTasksFromExternalFile(externFile)) // could also be tasks defined in external
            // file
            {
                return false;
            }
            Document externDoc = OpenXMLFile(externFile); // TODO, likely need to make path OS independent in
            // OpenXMLFile app
            if (externDoc != null) {
                nodeToPass = new FrameworkNode((Node) externDoc);
            } else {
                retVal = false; // something wrong with file, already notified in OpenXMLFile. Continue
                // processing, looking for more issues
            }
        }
        if (taskman.CreateTask(taskID, nodeToPass)) {

        } else {
            retVal = false;
        }
        return retVal;
    }

    public static boolean ReadTasksFromExternalFile(String filename) {
        Document doc = OpenXMLFile(filename);

        if (null != doc) {
            if (ReadPrompts(doc)) {
                return ReadTaskAndConditionals(doc);
            }
        }
        return false;
    }

    private Configuration configuration;

    private final TaskManager TASKMAN = TaskManager.getTaskManager();

    private List<TabWidget> _tabs;

    public ConfigurationReader() {
        configReader = this;
    }

    private void CalculateScaling() {
        if (null == configuration) {
            return;
        }
        if (configuration.isAutoScale() && configuration.getCreationHeight() > 0
                && configuration.getCreationWidth() > 0) {
            Rectangle2D visualBounds = configuration.getPrimaryScreen().getVisualBounds();

            double appWidth = visualBounds.getWidth();
            double appHeight = visualBounds.getHeight();
            // if app size specified
            if (getConfiguration().getWidth() > 0) {
                appWidth = getConfiguration().getWidth();
            }
            if (getConfiguration().getHeight() > 0) {
                appHeight = getConfiguration().getHeight();
            }
            double createWidth = (double) configuration.getCreationWidth();
            double createHeight = (double) configuration.getCreationHeight();
            double widthScale;
            double heightScale;
            double appScale;

            Font defFont = Font.getDefault();
            LOGGER.info("System Font info: " + defFont.toString());

            if (appWidth == createWidth && appHeight == createHeight) {
                LOGGER.info(
                        "Attempting to automatically scale, however current resolution is the same as specified <CreationSize>");
                return;
            }

            widthScale = appWidth / createWidth;
            heightScale = appHeight / createHeight;

            appScale = widthScale;
            if (heightScale < widthScale) // pick the smaller of the 2
            {
                appScale = heightScale;
            }
            configuration.setScaleFactor(appScale);

            String strCurr = " screen resolution: [" + Double.toString(appWidth) + "x" + Double.toString(appHeight)
                    + "]";
            String strCreated = " created screen resolution: [" + Double.toString(createWidth) + "x"
                    + Double.toString(createHeight) + "]";
            LOGGER.info("AutoScale set to " + Double.toString(appScale) + strCurr + strCreated);
        } else {
            configuration.setAutoScale(false);
        }
    }

    private void FetchDimenstions(FrameworkNode node) {
        try {
            if (node.hasAttribute("Width")) {
                int appWidth = Integer.parseInt(node.getAttribute("Width"));
                configuration.setWidth(appWidth);
            }
            if (node.hasAttribute("Height")) {
                int appHeight = Integer.parseInt(node.getAttribute("Height"));
                configuration.setHeight(appHeight);
            }
        } catch (Exception ex) {
        }

    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public List<TabWidget> getTabs() {
        return _tabs;
    }

    private boolean HandleMenuStyleOverride(MenuItem menu, FrameworkNode menuNode) {
        List<String> styles = new ArrayList<>();

        FrameworkNode styleNode;
        if (menuNode.hasChild("StyleOverride")) {
            styleNode = menuNode.getChild("StyleOverride");
        } else {
            return false;
        }

        for (FrameworkNode node : styleNode.getChildNodes()) {
            if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#comment".equalsIgnoreCase(node.getNodeName())) {
                continue;
            }
            if ("Item".equalsIgnoreCase(node.getNodeName())) {
                styles.add(node.getTextContent());
            } else {
                LOGGER.severe("Unknown Tag under <StyleOverride>: " + node.getNodeName());
                return false;
            }
        }
        String styleString = "";
        for (String style : styles) {
            styleString += style + ";";
        }

        menu.setStyle(styleString);

        return true;
    }

    /**
     * Reads the XML attributes of the <Application> tag
     *
     * @param appNode
     */
    private void ReadAppAttributes(FrameworkNode appNode) {
        Utility.ValidateAttributes(
                new String[]{"mode", "Width", "Height", "Scale", "ID", "EnableScrollBars", "MarvinLocalData"

                }, appNode);
        if (appNode.hasAttribute("ID")) {
            configuration.SetApplicationID(appNode.getAttribute("ID"));
            LOGGER.config("Setting Application ID to: " + configuration.GetApplicationID());
        }
        if (appNode.hasAttribute("EnableScrollBars")) {
            LOGGER.config("Setting Scroll bars per configuration");
            configuration.setEnableScrollBars(appNode.getBooleanAttribute("EnableScrollBars"));

        }
        if (appNode.hasAttribute("MarvinLocalData")) {
            if ("enable".equalsIgnoreCase(appNode.getAttribute("MarvinLocalData"))
                    || "enabled".equalsIgnoreCase(appNode.getAttribute("MarvinLocalData"))) {
                configuration.setMarvinLocalDatafeed(true);
            }
        }
        if (appNode.hasAttribute("mode")) {
            if (0 == appNode.getAttribute("mode").compareToIgnoreCase("kiosk")) {
                configuration.setKioskMode(true);
            } else {
                configuration.setKioskMode(false);
            }
            if (0 == appNode.getAttribute("mode").compareToIgnoreCase("Debug")) {
                configuration.setDebugMode(true);
            } else {
                configuration.setDebugMode(false);
            }
        }

        if (appNode.hasAttribute("Width")) {
            String strWidth = appNode.getAttribute("Width");
            try {
                configuration.setWidth((int) Double.parseDouble(strWidth));
            } catch (Exception ex) {
                LOGGER.severe("Invalid Application Width : " + strWidth + " ignoring");
            }
        }
        if (appNode.hasAttribute("Height")) {
            String strHeight = appNode.getAttribute("Height");
            try {
                configuration.setHeight((int) Double.parseDouble(strHeight));
            } catch (Exception ex) {
                LOGGER.severe("Invalid Application Height : " + strHeight + " ignoring");
            }
        }
        if (appNode.hasAttribute("Scale")) {
            String strScale = appNode.getAttribute("Scale");
            if ("Auto".equalsIgnoreCase(strScale)) {
                configuration.setAutoScale(true);
            } else {
                try {
                    configuration.setScaleFactor(Double.parseDouble(strScale));
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Invalid Application Scale factor: " + strScale + " ignoring");
                }
            }
        }
    }

    public Configuration ReadAppConfigFile(String filename) {
        Document doc = OpenXMLFile(filename);
        if (null != doc) {
            AliasMgr.getAliasMgr().SetCurrentConfigFile(filename);
            if (null == configuration) {
                configuration = new Configuration();
            }
            Configuration.getConfig().SetDoNotReportAliasErrors(true);
            if (false == ReadAppSettings(doc, false)) {
                return null;
            }
            Configuration.getConfig().SetDoNotReportAliasErrors(false);

            if (false == ReadTaskAndConditionals(doc)) {
                configuration = null;
            } else if (false == ReadPrompts(doc)) {
                configuration = null;
            }
        }
        if (null != configuration) {
            CalculateScaling();
        }
        return configuration;
    }

    private boolean ReadAppMenu(FrameworkNode menuNode, boolean basicInfoOnly) {
        if (null != configuration.getMenuBar()) {
            return true;
        }
        if (menuNode.hasAttribute("Show")) {
            String strVal = menuNode.getAttribute("Show");
            if ("True".equalsIgnoreCase(strVal)) {
                configuration.setShowMenuBar(true);
                if (!basicInfoOnly) {
                    LOGGER.config("Show Menu Bar = TRUE");
                }
            } else {
                configuration.setShowMenuBar(false);
                if (!basicInfoOnly) {
                    LOGGER.config("Show Menu Bar = FALSE");
                }
            }
        }
        if (basicInfoOnly) {
            return true;
        }
        MenuBar objMenuBar = new MenuBar();

        for (FrameworkNode node : menuNode.getChildNodes()) {
            if ("Menu".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"Title"}, node);
                if (node.hasAttribute("Title")) {
                    Menu objMenu = ReadMenu(node.getAttribute("Title"), node);
                    if (null != objMenu) {
                        HandleMenuStyleOverride(objMenu, node);

                        objMenuBar.getMenus().add(objMenu);
                        if (node.hasChild("Image")) {
                            ImageView iv = ConfigurationReader.GetImage(node.getChild("Image"));
                            if (null != iv) {
                                objMenu.setGraphic(iv);
                            }
                        }
                    } else {
                        return false;
                    }
                } else {
                    LOGGER.severe("Invalid Menu defined, no Title");
                    return false;
                }

            }
        }
        configuration.setMenuBar(objMenuBar);
        return true;
    }

    private boolean ReadAppSettings(Document doc, boolean basicInfoOnly) {
        NodeList appStuff = doc.getElementsByTagName("Application");
        if (appStuff.getLength() < 1) {
            LOGGER.severe("No <Application> section of config file");
            return false;
        }
        if (appStuff.getLength() > 1) {
            LOGGER.warning("More than one <Application> section found, only using first.");
        }
        boolean networkSettingsRead = false;
        FrameworkNode baseNode = new FrameworkNode(appStuff.item(0));
        FetchDimenstions(baseNode);

        boolean currErrorBool = Configuration.getConfig().DoNotReportAliasErrors();
        Configuration.getConfig().SetDoNotReportAliasErrors(basicInfoOnly);

        AliasMgr.getAliasMgr().addMarvinInfo();
        AliasMgr.ReadAliasFromRootDocument(doc);
        ReadAppAttributes(baseNode);

        Configuration.getConfig().SetDoNotReportAliasErrors(currErrorBool);


        ArrayList<String> declaredTabList = new ArrayList<>();

        for (FrameworkNode node : baseNode.getChildNodes()) {
            if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#Comment".equalsIgnoreCase(node.getNodeName())) {
                continue;
            } else if ("Title".equalsIgnoreCase(node.getNodeName())) {
                configuration.setAppTitle(node.getTextContent());
                LOGGER.config("Application Title = " + configuration.getAppTitle());
            } else if ("Heartbeat".equalsIgnoreCase(node.getNodeName())) {
                if (node.hasAttribute("rate")) {
                    configuration.setHeartbeatInterval(
                            node.getIntegerAttribute("rate", configuration.getHeartbeatInterval()));
                }
            } else if ("StyleSheet".equalsIgnoreCase(node.getNodeName())) {
                configuration.setCSSFile(node.getTextContent());
                LOGGER.config("Setting application CSS to " + configuration.getCSSFile());
            } else if ("IgnoreWebCerts".equalsIgnoreCase(node.getNodeName())) {
                configuration.setIgnoreWebCerts(node.getBooleanValue());

                LOGGER.config("Ignoring Web Certifications");
            } else if ("MonitorNumber".equalsIgnoreCase(node.getNodeName())) {
                if (false == basicInfoOnly) {
                    continue;
                }
                int count = Screen.getScreens().size();
                if (count == 1) {
                    continue;
                }
                try {
                    int monitorNum = Integer.parseInt(node.getTextContent());
                    if (monitorNum < 1) {
                        LOGGER.warning("<MonitorNumber> set to " + node.getTextContent()
                                + " smallest valid value is 1.Ignoring");
                        continue;
                    }

                    if (monitorNum <= count) {
                        Screen primary = Screen.getScreens().get(monitorNum - 1);
                        configuration.setPrimaryScreen(primary);
                        if (false == basicInfoOnly) {
                            LOGGER.config("Setting Primary Screen to monitor #" + node.getTextContent());
                        }
                    } else if (false == basicInfoOnly) {
                        LOGGER.warning("<MonitorNumber> set to " + node.getTextContent() + " however there are only "
                                + Integer.toHexString(count) + " monitors. Ignoring");
                    }
                } catch (Exception Ex) {
                    LOGGER.severe("Invalid MonitorNumber specified.  Ignoring.");
                    // return false;
                }
            } else if ("CreationSize".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"Height", "Width"}, node);
                if (node.hasAttribute("Height") && node.hasAttribute("Width")) {
                    configuration.setCreationWidth(node.getIntegerAttribute("Width", 0));
                    configuration.setCreationHeight(node.getIntegerAttribute("Height", 0));
                } else if (false == basicInfoOnly) {
                    LOGGER.severe("Invalid CreationSize specified");
                }
            } else if ("Tasks".equalsIgnoreCase(node.getNodeName())) {
                if (basicInfoOnly) {
                    continue;
                }

                Utility.ValidateAttributes(new String[]{"Enabled"}, node);
                if (node.hasAttribute("Enabled")) {
                    String str = node.getAttribute("Enabled");
                    if ("True".equalsIgnoreCase(str)) {
                        configuration.setAllowTasks(true);
                        LOGGER.config("Tasks Enabled");
                    } else {
                        configuration.setAllowTasks(false);
                        LOGGER.config("Tasks Disabled");
                    }
                }
            } else if ("Network".equalsIgnoreCase(node.getNodeName())) {
                if (basicInfoOnly) {
                    continue;
                }
                Utility.ValidateAttributes(new String[]{"IP", "Port"}, node);
                boolean fPort = false;
                if (node.hasAttribute("IP")) {
                    String str = node.getAttribute("IP");
                    configuration.setAddress(str);
                } else {
                    LOGGER.config("No IP specified in <Network> settings. Will listen on all interfaces");
                    configuration.setAddress("0.0.0.0");
                }

                if (node.hasAttribute("Port")) {
                    String str = node.getAttribute("Port");
                    try {
                        configuration.setPort(Integer.parseInt(str));
                        fPort = true;
                    } catch (Exception ex) {
                        LOGGER.severe("Invalid Network port: " + str);
                    }
                } else {
                    LOGGER.info("Port not attribute for <Network> settings.");
                }
                if (fPort) {
                    networkSettingsRead = true;
                } else {
                    return false;
                }
                for (FrameworkNode oscarNode : node.getChildNodes()) {
                    if ("#Text".equalsIgnoreCase(oscarNode.getNodeName())
                            || "#Comment".equalsIgnoreCase(oscarNode.getNodeName())) {
                        continue;
                    } else if ("Oscar".equalsIgnoreCase(oscarNode.getNodeName())) {
                        if (false == ReadOscarConnection(oscarNode)) {
                            return false;
                        }
                    }
                }
            } else if ("Padding".equalsIgnoreCase(node.getNodeName())) {
                if (basicInfoOnly) {
                    continue;
                }

                Utility.ValidateAttributes(new String[]{"top", "bottom", "left", "right", "legacymode"}, node);
                String strTop = "-1";
                String strBottom = "-1";
                String strLeft = "-1";
                String strRight = "-1";
                if (node.hasAttribute("top")) {
                    strTop = node.getAttribute("top");
                }
                if (node.hasAttribute("bottom")) {
                    strBottom = node.getAttribute("bottom");
                }
                if (node.hasAttribute("left")) {
                    strLeft = node.getAttribute("left");
                }
                if (node.hasAttribute("right")) {
                    strRight = node.getAttribute("right");
                }
                try {
                    configuration.setInsetTop(Integer.parseInt(strTop));
                    configuration.setInsetBottom(Integer.parseInt(strBottom));
                    configuration.setInsetLeft(Integer.parseInt(strLeft));
                    configuration.setInsetRight(Integer.parseInt(strRight));
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Invalid Application <Inset> configuration.");

                    return false;
                }
                if (node.hasAttribute("legacymode")) {
                    configuration.setLegacyInsetMode(node.getBooleanAttribute("legacymode"));
                    if (configuration.getLegacyInsetMode()) {
                        LOGGER.config("Using LEGACY mode of padding.");
                    }
                }

            } else if ("MainMenu".equalsIgnoreCase(node.getNodeName())) {
                if (false == ReadAppMenu(node, basicInfoOnly)) {
                    return false;
                }
            } else if ("UnregisteredData".equalsIgnoreCase(node.getNodeName())) {
                if (basicInfoOnly) {
                    continue;
                }

                if (false == ReadUnregisteredDataInfo(node)) {
                    return false;
                }
            } else if ("Tabs".equalsIgnoreCase(node.getNodeName())) {
                Utility.ValidateAttributes(new String[]{"side"}, node);

                if (node.hasAttribute("Side")) // where the tabs should be located.
                {
                    String sideStr = node.getAttribute("Side");

                    if ("Top".equalsIgnoreCase(sideStr)) {
                        configuration.setSide(Side.TOP);
                    } else if ("Bottom".equalsIgnoreCase(sideStr)) {
                        configuration.setSide(Side.BOTTOM);
                    } else if ("Left".equalsIgnoreCase(sideStr)) {
                        configuration.setSide(Side.LEFT);
                    } else if ("Right".equalsIgnoreCase(sideStr)) {
                        configuration.setSide(Side.RIGHT);
                    } else {
                        LOGGER.warning("Invalid <Tabs Side=> attribute: " + sideStr + ". Ignoring");
                    }
                }
                if (basicInfoOnly) {
                    continue;
                }
                int tabCount = 0;
                for (FrameworkNode tabNode : node.getChildNodes(true)) {
                    if ("#Text".equalsIgnoreCase(tabNode.getNodeName())) {
                        continue;
                    }
                    if ("Tab".equalsIgnoreCase(tabNode.getNodeName())) {
                        if (tabNode.hasAttributes()) {
                            String ID = tabNode.getAttribute("ID");
                            if (ID != null && ID.length() > 0) {
                                if (tabNode.hasChild("OnDemand")) {
                                    DynamicItemInfoContainer dynaInfo = ReadOnDemandInfo(tabNode.getChild("OnDemand"));

                                    if (null != dynaInfo) {
                                        OnDemandTabBuilder objBuilder = new OnDemandTabBuilder(ID, tabCount, dynaInfo);
                                        DataManager.getDataManager().AddOnDemandWidgetCriterea(dynaInfo, objBuilder);
                                        onDemandIDList.add(ID.toUpperCase());
                                    }
                                } else {
                                    declaredTabList.add(ID);
                                }
                                tabCount++;
                            } else {
                                LOGGER.warning("Tab defined in <Tabs> without an ID");
                            }
                        }
                    }
                }
            } else if ("RefreshInterval".equalsIgnoreCase(node.getNodeName())) {
                if (basicInfoOnly) {
                    continue;
                }
                String strInterval = node.getTextContent();
                try {
                    configuration.setTimerInterval(Long.parseLong(strInterval));
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Invalid Application <RefreshInterval> configuration.");
                    return false;
                }
            } else {
                LOGGER.warning("Unexpected section in <Application>: " + node.getNodeName());
            }
        }

        if (basicInfoOnly) {
            return true;
        }

        if (declaredTabList.isEmpty() && !DataManager.getDataManager().DynamicTabRegistered()) {
            LOGGER.severe("No Tabs defined in <Application> section of configuration file.");
            return false;
        }
        configuration.setPrimaryScreenDetermined(true);

        if (VerifyTabList(declaredTabList)) {
            _tabs = ReadTabs(doc, declaredTabList);
            if (null == _tabs) {
                return false;
            }
        }
        if (DataManager.getDataManager().DynamicTabRegistered()) {
            ReadOnDemandTabs(doc);
        }

        if (false == networkSettingsRead) {
            LOGGER.severe("No <Network> section found in Application.xml");
        }
        return networkSettingsRead;
    }

    private Menu ReadMenu(String title, FrameworkNode menuNode) {
        Menu objMenu = new Menu(title);
        List<MenuItem> items = ReadMenuItems(menuNode);
        if (null == items) {
            LOGGER.severe("Invalid Menu with Title of " + title + " defined");
            return null;

        }
        objMenu.getItems().addAll(items);
        return objMenu;
    }

    public MenuItem ReadMenuItem(FrameworkNode menuNode, int itemIndex) {
        if ("MenuItem".equalsIgnoreCase(menuNode.getNodeName())) {
            Utility.ValidateAttributes(new String[]{"Text", "Task", "CreateDataPoint"}, menuNode);
            if (menuNode.hasAttribute("Text") && menuNode.hasAttribute("Task")) {
                MenuItem objItem = new MenuItem(menuNode.getAttribute("Text"));
                HandleMenuStyleOverride(objItem, menuNode);

                if (menuNode.hasChild("Image")) {
                    ImageView iv = ConfigurationReader.GetImage(menuNode.getChild("Image"));
                    if (null != iv) {
                        objItem.setGraphic(iv);
                    }
                }

                if (Configuration.getConfig().getAllowTasks()) {
                    List<DataPointGenerator> dataPoints = new ArrayList<>();

                    String strTask = menuNode.getAttribute("Task");
                    if (menuNode.hasAttribute("CreateDataPoint")) {
                        dataPoints.addAll(ReadDataPointsForTask(itemIndex, menuNode.getAttribute("CreateDataPoint"),
                                menuNode.getAttribute("Text"), menuNode.getAttribute("Task")));
                        if (dataPoints.isEmpty()) {
                            return null;
                        }
                    }
                    objItem.setOnAction((ActionEvent t) -> {
                        for (DataPointGenerator dpGen : dataPoints) {
                            dpGen.generate();
                        }
                        TASKMAN.PerformTask(strTask);
                    });
                }
                return objItem;
            }
        }
        return null;
    }

    public List<MenuItem> ReadMenuItems(FrameworkNode menuNode) {
        ArrayList<MenuItem> retList = new ArrayList<>();
        for (FrameworkNode node : menuNode.getChildNodes()) {
            if ("MenuItem".equalsIgnoreCase(node.getNodeName())) {
                MenuItem objItem = ReadMenuItem(node, retList.size());
                if (null != objItem) {
                    retList.add(objItem);
                } else {
                    return null;
                }
            }
        }

        return retList;
    }

    private void ReadOnDemandTabs(Document doc) {
        FrameworkNode appNode = new FrameworkNode(doc.getChildNodes().item(0));

        for (FrameworkNode node : appNode.getChildNodes("tab")) {
            if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#comment".equalsIgnoreCase(node.getNodeName())) {
                continue;
            }
            if ("Tab".equalsIgnoreCase(node.getNodeName())) {
                if (node.hasAttributes()) {
                    if (node.hasAttribute("ID")) {
                        String id = node.getAttribute("ID");
                        for (Pair<DynamicItemInfoContainer, OnDemandWidgetBuilder> item : DataManager.getDataManager()
                                .getOnDemandList()) {
                            if (item.getValue() instanceof OnDemandTabBuilder) {
                                OnDemandTabBuilder objBuilder = (OnDemandTabBuilder) item.getValue();
                                if (objBuilder.getTabID().equalsIgnoreCase(id)) {
                                    objBuilder.setSourceNode(node);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean ReadOscarConnection(FrameworkNode oscarNode) {
        Utility.ValidateAttributes(new String[]{"IP", "Port", "Key"}, oscarNode);
        String ip = null;
        int port = -1;
        String key = "Biff Rulz!"; // default key
        boolean retVal = false;

        if (oscarNode.hasAttribute("IP") && oscarNode.hasAttribute("Port")) {
            ip = oscarNode.getAttribute("IP");
            port = oscarNode.getIntegerAttribute("Port", -1);
            if (port != -1) {
                retVal = true;
            }
        }
        if (oscarNode.hasAttribute("Key")) {
            key = oscarNode.getAttribute("Key");
        }
        if (false == retVal) {
            LOGGER.severe("<Network><Oscar> requires IP and Port");
        } else {
            configuration.addOscarBullhornEntry(ip, port, key);
        }

        return retVal;
    }

    public Configuration ReadStartupInfo(String filename) {
        Document doc = OpenXMLFile(filename);
        if (null == doc) {
            return null;
        }
        if (null == configuration) {
            configuration = new Configuration();
        }
        if (false == ReadAppSettings(doc, true)) {
            return null;
        }

        AliasMgr.getAliasMgr().ClearAll(); // nuke anything already read, will start fresh
        return configuration;
    }

    private List<TabWidget> ReadTabs(Document doc, List<String> tabIDList) {
        FrameworkNode appNode = new FrameworkNode(doc.getChildNodes().item(0));

        ArrayList<TabWidget> tabList = new ArrayList<>();

        for (FrameworkNode node : appNode.getChildNodes(true)) {
            if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#comment".equalsIgnoreCase(node.getNodeName())) {
                continue;
            }
            if ("DemoFramework".equalsIgnoreCase(node.getNodeName())) {
                LOGGER.warning("DemoFramework XML tag is deprecated.  Use 'Marvin'");
                continue;
            }

            if ("Marvin".equalsIgnoreCase(node.getNodeName())) {
                continue;
            }

            if ("Application".equalsIgnoreCase(node.getNodeName())) {
                continue;
            }
            if ("Tab".equalsIgnoreCase(node.getNodeName())) {
                TabWidget tab = null;
                if (false == ReadConditionals(node)) {
                    return null;
                }

                if (node.hasAttributes()) {
                    if (node.hasAttribute("ID")) {
                        boolean found = false;
                        String id = node.getAttribute("ID");
                        if (TabAlreadyLoaded(tabList, id)) {
                            LOGGER.severe("Tab ID=" + id + " defined twice in <Tabs>.");
                            return null;
                        }

                        for (String string : tabIDList) {
                            if (0 == string.compareToIgnoreCase(id)) {
                                found = true;

                                tab = new TabWidget(id);

                                tab = ConfigurationReader.ReadTab(node, tab, id);
                                if (null == tab) {
                                    return null;
                                }
                                /*
                                 * if (false) // TODO - hmm, not sure about this.... why did I do this { if
                                 * (node.hasAttribute("File")) // can externally define widgets within {
                                 * WidgetBuilder.StartReadingExternalFile(node); tabNode =
                                 * OpenTabDefinitionFile(node.getAttribute("File")); if (null == tabNode) {
                                 * LOGGER.severe("Invalid tab definition file: " + node.getAttribute("File"));
                                 * return null; }
                                 *
                                 * AliasMgr.getAliasMgr().AddAliasFromAttibuteList(node, new String[] { "ID",
                                 * "File", "Align", "hgap", "vgap", "Task" });
                                 *
                                 * if (false == AliasMgr.getAliasMgr()
                                 * .ReadAliasFromExternalFile(node.getAttribute("File"))) { return null; } if
                                 * (!ConfigurationReader.ReadTasksFromExternalFile(node.getAttribute("File"))) {
                                 * return null; } } else { Utility.ValidateAttributes( new String[] { "ID",
                                 * "File", "Align", "hgap", "vgap", "Task" }, node); tabNode = node; } if (null
                                 * == tabNode) { return null; } if (false == tab.LoadConfiguration(tabNode)) {
                                 * return null; } if (node.hasAttribute("File")) {
                                 * WidgetBuilder.DoneReadingExternalFile(); }
                                 *
                                 * break; }
                                 */
                            }
                        }
                        if (found) {
                            AliasMgr.getAliasMgr().PopAliasList();
                        } else {
                            if (!onDemandIDList.contains(id.toUpperCase())) {
                                LOGGER.warning("<Tab ID=" + id + "> found, but not used in <Tabs>.");
                            }
                            continue;
                        }
                    } else {
                        LOGGER.warning("<Tab> with no ID found.");
                        return null;
                    }
                    if (node.hasAttribute("Align")) {
                        String str = node.getAttribute("Align");
                        tab.setAlignment(str);
                    }
                    if (node.hasAttribute("task")) {
                        //tab.setOnActivateTask(node.getAttribute("task"));
                    }

                    if (node.hasAttribute("hgap")) {
                        try {
                            if (!tab.parsehGapValue(node)) {
                                LOGGER.config("Setting hGap for Tab ID=" + tab.getMinionID() + " to "
                                        + node.getAttribute("hgap"));
                            }
                            // tab.sethGap(Integer.parseInt(node.getAttribute("hgap")));
                        } catch (Exception ex) {
                            LOGGER.warning("Tab ID=" + tab.getMinionID() + " has invalid hgap.  Ignoring");
                        }
                    }
                    if (node.hasAttribute("vgap")) {
                        try {
                            if (!tab.parsevGapValue(node)) {
                                LOGGER.config("Setting vGap for Tab ID=" + tab.getMinionID() + " to "
                                        + node.getAttribute("vgap"));
                            }
//                            tab.setvGap(Integer.parseInt(node.getAttribute("vgap")));
                            // LOGGER.config("Setting vGap for Tab ID=" + tab.getMinionID() + " to " +
                            // node.getAttribute("vgap"));
                        } catch (Exception ex) {
                            LOGGER.warning("Tab ID=" + tab.getMinionID() + " has invalid vgap.  Ignoring");
                        }
                    }
                }
                if (null == tab) {
                    LOGGER.severe("Malformed <Tab> found in configuration file");
                    return null;
                }
                tabList.add(tab);
            } else if ("TaskList".equalsIgnoreCase(node.getNodeName())) {
                ConfigurationReader.ReadTaskList(node);
            } else if ("GenerateDataPoint".equalsIgnoreCase(node.getNodeName())) {
                if (!ConfigurationReader.ReadGenerateDataPoints(node)) {
                    return null;
                }
            } else if ("Prompt".equalsIgnoreCase(node.getNodeName())) {
                ConfigurationReader.ReadPrompt(node);
            } else if ("AliasList".equalsIgnoreCase(node.getNodeName())) {

            } else if ("Conditional".equalsIgnoreCase(node.getNodeName())) {
                // taken care of elsewhere
            } else {
                LOGGER.warning("Unexpected Tag Type in configuration file: " + node.getNodeName());
            }
        }
        if (false == VerifyDesiredTabsPresent(tabList, tabIDList)) {
            return null;
        }

        return SortTabs(tabList, tabIDList);
    }

    private boolean ReadUnregisteredDataInfo(FrameworkNode unregisteredDataNode) {
        Utility.ValidateAttributes(new String[]{"Enabled", "Width", "Title"}, unregisteredDataNode);

        if (unregisteredDataNode.hasAttributes()) {
            if (unregisteredDataNode.hasAttribute("Enabled")) {
                if (unregisteredDataNode.getBooleanAttribute("Enabled")) {
                    DynamicTabWidget.setEnabled(true);
                } else {
                    return true; // if it's not enabled, no reason to read the rest
                }
            }
            if (unregisteredDataNode.hasAttribute("Title")) {
                DynamicTabWidget.setTitleStr(unregisteredDataNode.getAttribute("Title"));
            }
            DynamicTabWidget
                    .setMaxWidth(unregisteredDataNode.getIntegerAttribute("Width", DynamicTabWidget.getMaxWidth()));
        } else {
            return true; // if no attributes, then it's not enabled, so we are outta here
        }

        for (FrameworkNode node : unregisteredDataNode.getChildNodes()) {
            if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#Comment".equalsIgnoreCase(node.getNodeName())) {
                continue;
            }

            if ("TitleStyle".equalsIgnoreCase(node.getNodeName())) {
                DynamicTabWidget.setTitleStyle(node.getTextContent());
            } else if ("EvenStyle".equalsIgnoreCase(node.getNodeName())) {
                for (FrameworkNode evenNode : node.getChildNodes()) {
                    if ("#Text".equalsIgnoreCase(evenNode.getNodeName())
                            || "#Comment".equalsIgnoreCase(evenNode.getNodeName())) {
                        continue;
                    }
                    if ("Background".equalsIgnoreCase(evenNode.getNodeName())) {
                        DynamicTabWidget.setEven_Background(evenNode.getTextContent());
                    } else if ("Id".equalsIgnoreCase(evenNode.getNodeName())) {
                        DynamicTabWidget.setEven_ID(evenNode.getTextContent());
                    } else if ("Value".equalsIgnoreCase(evenNode.getNodeName())) {
                        DynamicTabWidget.setEven_Value(evenNode.getTextContent());
                    } else {
                        LOGGER.warning("Unknown tag: " + evenNode.getNodeName() + " in <UnregisteredData><EvenStyle> ");
                    }
                }
            } else if ("OddStyle".equalsIgnoreCase(node.getNodeName())) {
                for (FrameworkNode oddNode : node.getChildNodes()) {
                    if ("#Text".equalsIgnoreCase(oddNode.getNodeName())
                            || "#Comment".equalsIgnoreCase(oddNode.getNodeName())) {
                        continue;
                    }

                    if ("Background".equalsIgnoreCase(oddNode.getNodeName())) {
                        DynamicTabWidget.setOdd_Background(oddNode.getTextContent());
                    } else if ("Id".equalsIgnoreCase(oddNode.getNodeName())) {
                        DynamicTabWidget.setOdd_ID(oddNode.getTextContent());
                    } else if ("Value".equalsIgnoreCase(oddNode.getNodeName())) {
                        DynamicTabWidget.setOdd_Value(oddNode.getTextContent());
                    } else {
                        LOGGER.warning("Unknown tag: " + oddNode.getNodeName() + " in <UnregisteredData><OddStyle> ");
                    }
                }
            } else {
                LOGGER.warning("Unknown tag: " + node.getNodeName() + " in <UnregisteredData>");
            }
        }
        return true;
    }

    /**
     * Makes sure the visual tabs matches the order they are listed in the <tabs>
     * section as opposed to the order the <tab> sections are listed in the file
     *
     * @param listTabs
     * @param TabID_List
     * @return sorted list of tabs
     */
    private List<TabWidget> SortTabs(List<TabWidget> listTabs, List<String> TabID_List) {
        List<TabWidget> sortedTabs = new ArrayList<>(listTabs.size());

        for (String id : TabID_List) {
            for (TabWidget tab : listTabs) {
                if (0 == id.compareToIgnoreCase(tab.getMinionID())) {
                    sortedTabs.add(tab);
                    break;
                }
            }
        }

        return sortedTabs;
    }

    private boolean TabAlreadyLoaded(ArrayList<TabWidget> tabs, String checkID) {
        for (TabWidget tab : tabs) {
            if (tab.getMinionID().equalsIgnoreCase(checkID)) {
                return true;
            }
        }
        return false;
    }

    private boolean VerifyDesiredTabsPresent(List<TabWidget> listTabs, List<String> TabID_List) {
        boolean RetVal = true;

        for (String id : TabID_List) {
            boolean found = false;

            for (TabWidget tab : listTabs) {
                if (0 == id.compareToIgnoreCase(tab.getMinionID())) {
                    found = true;
                    break;
                }
            }
            if (false == found) {
                LOGGER.severe("Tab with ID=" + id + " defined in <Tabs>, however not in <Tab> definitions.");
                RetVal = false;
            }
        }
        return RetVal;
    }

    /**
     * Verifies no duplicate Tab ID's in <Tabs> section
     *
     * @param ListTabID
     * @return true if all good, otherwise false
     */
    boolean VerifyTabList(ArrayList<String> listTabID) {
        boolean RetVal = true;
        for (int iIndex = 0; iIndex < listTabID.size(); iIndex++) {
            for (int index = iIndex + 1; index < listTabID.size(); index++) {
                if (0 == listTabID.get(iIndex).compareToIgnoreCase(listTabID.get(index))) {
                    RetVal = false;
                    LOGGER.severe("Duplicate Tab ID's found in <Application> settings: ID=" + listTabID.get(iIndex));
                }
            }
        }
        return RetVal;
    }
}
