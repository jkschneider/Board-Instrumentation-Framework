/*
 * ##############################################################################
 * #  Copyright (c) 2018 Intel Corporation
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javafx.util.Pair;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.TabWidget;

/**
 * @author Patrick.Kutch@gmail.com
 */
public class DynamicItemInfoContainer {
    public enum SortMethod {
        NAMESPACE, ID, VALUE, NONE
    }

    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final Pair<ArrayList<String>, ArrayList<String>> __namespaceCriterea;
    private final Pair<ArrayList<String>, ArrayList<String>> __idCriterea;
    private FrameworkNode __node;
    private final HashMap<String, Boolean> previouslyChecked;
    private int numberOfMatchesUsingThisPattern;
    private String tokenizerToken;
    private String matchedSortString;

    private Map<String, String> aliasListSnapshot;
    ;
    private SortMethod __SortMethod;
    private List<String> styleOverrideEven;
    private List<String> styleOverrideOdd;
    private String styleOverrideFileEven;
    private String styleOverrideFileOdd;
    private String styleOverrideIDEven;
    private String styleOverrideIDOdd;

    public DynamicItemInfoContainer(Pair<ArrayList<String>, ArrayList<String>> namespaceCriterea,
                                    Pair<ArrayList<String>, ArrayList<String>> idCriterea) {
        previouslyChecked = new HashMap<>();
        __namespaceCriterea = namespaceCriterea;
        __idCriterea = idCriterea;
        __node = null;
        tokenizerToken = null;
        numberOfMatchesUsingThisPattern = 0;
        matchedSortString = "";
        __SortMethod = SortMethod.NONE;
        styleOverrideEven = new ArrayList<>();
        styleOverrideOdd = new ArrayList<>();
        styleOverrideFileEven = styleOverrideFileOdd = null;
        styleOverrideIDEven = styleOverrideIDOdd = null;
    }

    public void ApplyOddEvenStyle(BaseWidget objWidget, int number) {
        if (number % 2 == 0) // even style
        {
            if (null != styleOverrideFileEven) {
                objWidget.setBaseCSSFilename(styleOverrideFileEven);
            }
            if (null != styleOverrideIDEven) {
                objWidget.setStyleID(styleOverrideIDEven);
            }
            objWidget.addOnDemandStyle(getStyleOverrideEven());
        } else {
            if (null != styleOverrideFileOdd) {
                objWidget.setBaseCSSFilename(styleOverrideFileOdd);
            }
            if (null != styleOverrideIDOdd) {
                objWidget.setStyleID(styleOverrideIDOdd);
            }
            objWidget.addOnDemandStyle(getStyleOverrideOdd());
        }
    }

    public void ApplyOddEvenStyle(TabWidget objWidget, int number, String title) {
        if (number % 2 == 0) // even style
        {
            // LOGGER.severe("Applying Even Style to Tab " + Title);
            if (null != styleOverrideFileEven) {
                objWidget.setBaseCSSFilename(styleOverrideFileEven);
            }
            if (null != styleOverrideIDEven) {
                objWidget.setStyleID(styleOverrideIDEven);
            }
            objWidget.addOnDemandStyle(getStyleOverrideEven());
        } else {
            // LOGGER.severe("Applying Odd Style to Tab " + Title);
            if (null != styleOverrideFileOdd) {
                objWidget.setBaseCSSFilename(styleOverrideFileOdd);
            }
            if (null != styleOverrideIDOdd) {
                objWidget.setStyleID(styleOverrideIDOdd);
            }
            objWidget.addOnDemandStyle(getStyleOverrideOdd());
        }
    }

    public String getLastMatchedSortStr() {
        return matchedSortString;
    }

    public int getMatchedCount() {
        return numberOfMatchesUsingThisPattern;
    }

    public FrameworkNode getNode() {
        return __node;
    }

    public SortMethod getSortByMethod() {
        return __SortMethod;
    }

    public List<String> getStyleOverrideEven() {
        return styleOverrideEven;
    }

    public List<String> getStyleOverrideOdd() {
        return styleOverrideOdd;
    }

    public String getToken() {
        return tokenizerToken;
    }

    private boolean Matches(String compare, Pair<ArrayList<String>, ArrayList<String>> patternPair) {
        ArrayList<String> patternList = patternPair.getKey();
        ArrayList<String> excludePatternList = patternPair.getValue();
        for (String matchPattern : patternList) {
            if (Glob.check(matchPattern, compare)) {
                boolean noGood = false;
                for (String excludePattern : excludePatternList) {
                    if (Glob.check(excludePattern, compare)) {
                        noGood = true;
                        break;
                    }
                }
                if (!noGood) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean Matches(String namespace, String id, String value) {
        matchedSortString = "";
        namespace = namespace.toUpperCase();
        id = id.toUpperCase();
        // if already checked, no need to do it again
        if (previouslyChecked.containsKey(namespace + id)) {
            return false;
        }
        if (__idCriterea.getKey().isEmpty() && previouslyChecked.containsKey(namespace)) {
            return false;
        }

        boolean matched = Matches(namespace, __namespaceCriterea);
        if (matched && !__idCriterea.getKey().isEmpty()) {
            matched = Matches(id, __idCriterea);

            previouslyChecked.put(namespace + id, matched);
        } else {
            previouslyChecked.put(namespace, matched);
        }
        if (matched) {
            numberOfMatchesUsingThisPattern++;
            if (getSortByMethod() == SortMethod.NAMESPACE) {
                matchedSortString = namespace;
            } else if (getSortByMethod() == SortMethod.ID) {
                matchedSortString = id;
            } else if (getSortByMethod() == SortMethod.VALUE) {
                matchedSortString = value;
            } else {
                matchedSortString = null;
            }

        }
        return matched;
    }

    public void putAliasListSnapshot() {
        if (null == aliasListSnapshot) {
            return;
        }
        AliasMgr aMgr = AliasMgr.getAliasMgr();
        for (String key : aliasListSnapshot.keySet()) {
            aMgr.SilentAddAlias(key, aliasListSnapshot.get(key));
        }
    }

    private List<String> readStyleItems(FrameworkNode styleNode) {
        ArrayList<String> retList = new ArrayList<>();
        for (FrameworkNode node : styleNode.getChildNodes()) {
            if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#comment".equalsIgnoreCase(node.getNodeName())) {
                continue;
            }
            if ("Item".equalsIgnoreCase(node.getNodeName())) {
                retList.add(node.getTextContent());
            } else {
                LOGGER.severe("Unknown Tag under Selected : " + node.getNodeName());
            }
        }
        return retList;
    }

    public void ReadStyles(FrameworkNode onDemandNode) {
        if (onDemandNode.hasChild("StyleOverride-Even")) {
            FrameworkNode evenNode = onDemandNode.getChild("StyleOverride-Even");
            styleOverrideEven = readStyleItems(evenNode);

            if (evenNode.hasAttribute("File")) {
                styleOverrideFileEven = evenNode.getAttribute("File");
            }
            if (evenNode.hasAttribute("ID")) {
                styleOverrideIDEven = evenNode.getAttribute("ID");

            }
        }

        if (onDemandNode.hasChild("StyleOverride-Odd")) {
            FrameworkNode oddNode = onDemandNode.getChild("StyleOverride-Odd");
            styleOverrideOdd = readStyleItems(oddNode);

            if (oddNode.hasAttribute("File")) {
                styleOverrideFileOdd = oddNode.getAttribute("File");
            }
            if (oddNode.hasAttribute("ID")) {
                styleOverrideIDOdd = oddNode.getAttribute("ID");
            }
        }
    }

    public void setNode(FrameworkNode node) {
        __node = node;
    }

    public void setSortByMethod(SortMethod sortMethod) {
        __SortMethod = sortMethod;
    }

    public void setToken(String strToken) {
        tokenizerToken = strToken;
    }

    public void TakeAliasSnapshot() {
        aliasListSnapshot = AliasMgr.getAliasMgr().getSnapshot();
    }

    public String[] tokenize(String ID) {
        if (".".equalsIgnoreCase(getToken())) {
            return ID.split("\\.");
        }
        return ID.split(getToken());
    }

    public boolean tokenizeAndCreateAlias(String ID) {
        if (null == getToken()) {
            return false;
        }
        String[] tokens = tokenize(ID);
        if (tokens.length <= 0) {
            return false;
        }
        int index = 1;
        for (String token : tokens) {
            String alias = "TriggeredIDPart." + Integer.toString(index++);
            AliasMgr.getAliasMgr().AddAlias(alias, token);
        }
        return true;
    }
}
