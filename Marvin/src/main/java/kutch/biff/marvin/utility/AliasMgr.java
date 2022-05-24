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
package kutch.biff.marvin.utility;

import static kutch.biff.marvin.configuration.ConfigurationReader.OpenXMLFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javafx.geometry.Rectangle2D;
import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.logger.MarvinLogger;

/**
 * This class handles aliases.
 *
 * @author Patrick Kutch
 */
public final class AliasMgr {

    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static final AliasMgr _Mgr = new AliasMgr();
    private static final String strCurrentRowIsOddAlias = "CurrentRowIsOddAlias";
    private static final String strCurrentColumnIsOddAlias = "CurrentColumnIsOddAlias";
    private static final String strCurrentRowAlias = "CurrentRowAlias";
    private static final String strNextRowAlias = "NextRowAlias";
    private static final String strCurrentColumnAlias = "CurrentColumnAlias";
    private static final String strNextColumnAlias = "NextColumnAlias";
    private static final String strPrevColumnAlias = "PrevColumnAlias";
    private static final String strPrevRowAlias = "PrevRowAlias";
    private String currentFileName = "not set";

    public static AliasMgr getAliasMgr() {
        return _Mgr;
    }

    /**
     * Looks for <AliasList> aliases and processes them
     *
     * @param aliasNode <AliasList> node
     * @return true if successful, else false
     */
    public static boolean HandleAliasNode(FrameworkNode aliasNode) {
        if (aliasNode.hasAttribute("File")) {
            String filename = aliasNode.getAttribute("File");
            getAliasMgr().ReadExternalAliasFile(filename);
        }
        for (FrameworkNode nodeAlias : aliasNode.getChildNodes()) {
            if ("Alias".equalsIgnoreCase(nodeAlias.getNodeName())) {
                String strReplace = null;
                NamedNodeMap map = nodeAlias.GetNode().getAttributes();
                for (int iLoop = 0; iLoop < map.getLength(); iLoop++) {
                    FrameworkNode node = new FrameworkNode(map.item(iLoop));
                    if ("REPLACE".equals(node.getNodeName())) {
                        strReplace = node.getTextContent();
                    }
                }

                for (int iLoop = 0; iLoop < map.getLength(); iLoop++) {
                    FrameworkNode node = new FrameworkNode(map.item(iLoop));

                    String strAlias = node.getNodeName();
                    if ("REPLACE".equals(strAlias)) {
                        continue; // ignore
                    }
                    if (strAlias.contains("REPLACE")) {
                        if (null != strReplace) {
                            strAlias = strAlias.replace("REPLACE", strReplace);
                        } else {
                            LOGGER.warning("Attempted to replace 'REPLACE' in alias " + strAlias
                                    + " however REPLACE not defined.  Ignoring");
                        }
                    }

                    String strValue = node.getTextContent();
                    AliasMgr._Mgr.AddAlias(strAlias, strValue);
                }
            } // Default Alias, only alias if not already aliased
            else if ("DefaultAlias".equalsIgnoreCase(nodeAlias.getNodeName())) {
                NamedNodeMap map = nodeAlias.GetNode().getAttributes();
                String strReplace = null;
                for (int iLoop = 0; iLoop < map.getLength(); iLoop++) {
                    FrameworkNode node = new FrameworkNode(map.item(iLoop));
                    if ("REPLACE".equals(node.getNodeName())) {
                        strReplace = node.getTextContent();
                    }
                }

                for (int iLoop = 0; iLoop < map.getLength(); iLoop++) {
                    FrameworkNode node = new FrameworkNode(map.item(iLoop));

                    String strAlias = node.getNodeName();
                    String strValue = node.getTextContent();

                    if ("REPLACE".equals(strAlias)) {
                        continue; // ignore
                    }
                    if (strAlias.contains("REPLACE")) {
                        if (null != strReplace) {
                            strAlias = strAlias.replace("REPLACE", strReplace);
                        } else {
                            LOGGER.warning("Attempted to replace 'REPLACE' in alias " + strAlias
                                    + " however REPLACE not defined.  Ignoring");
                        }
                    }

                    if (false == getAliasMgr().IsAliased(strAlias)) {
                        AliasMgr._Mgr.AddAlias(strAlias, strValue);
                    }
                }
            } // Allow reading alias's defined in other files
            else if ("Import".equalsIgnoreCase(nodeAlias.getNodeName())) {
                String strImportFile = nodeAlias.getTextContent();
                if (false == ReadAliasFromExternalFile(strImportFile)) {
                    return false;
                }
            } else // unknown
            {
                LOGGER.severe("Unknown <AliasList> entry: " + nodeAlias.getNodeName());
                return false;
            }
        }
        return true;
    }

    public static boolean ReadAliasFromExternalFile(String fileName) {
        Document doc = OpenXMLFile(fileName);
        AliasMgr.getAliasMgr().SetCurrentConfigFile(fileName);
        AliasMgr.getAliasMgr().AddRandomValueAlias();
        return ReadAliasFromRootDocument(doc);
    }

    public static boolean ReadAliasFromRootDocument(Document doc) {
        if (null != doc) {
            NodeList rootNodes = doc.getElementsByTagName("Marvin");
            if (0 == rootNodes.getLength()) {
                rootNodes = doc.getElementsByTagName("MarvinExternalFile");
            }
            if (0 == rootNodes.getLength()) {
                rootNodes = doc.getElementsByTagName("Widget");
            }
            if (0 == rootNodes.getLength()) {
                LOGGER.severe(
                        "Requested to read <AliasList> from invalid xml file. Root node must be Marvin, MarvinExternalFile or Widget");
                return false;
            }

            FrameworkNode rootNode = new FrameworkNode(rootNodes.item(0));

            for (FrameworkNode child : rootNode.getChildNodes()) {
                if ("AliasList".equalsIgnoreCase(child.getNodeName())) {
                    if (false == AliasMgr.HandleAliasNode(child)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings("rawtypes")
    private final List<Map> aliasList;

    private int randomVal;

    private String externalAliasFile;

    private AliasMgr() {
        aliasList = new ArrayList<>();
        externalAliasFile = null;
        ClearAll();
    }

    /**
     * @param Alias
     * @param Value
     */
    @SuppressWarnings({"unchecked"})
    public void AddAlias(String alias, String value) {
        if (null == alias) {
            LOGGER.severe("Attempted to set an ALIAS ID to NULL");
            return;
        }
        Map<String, String> map = aliasList.get(0);
        if (map.containsKey(alias.toUpperCase())) {
            LOGGER.config("Duplicate Alias detected for : " + alias + ". Ignoring.");
            return;
        }
        if (null == value) {
            String strError = "Alias [" + alias + "] has NULL value!";
            LOGGER.severe(strError);
            map.put(alias.toUpperCase(), strError);
        } else {
            map.put(alias.toUpperCase(), value);
        }
    }

    public void AddAliasFromAttibuteList(FrameworkNode node, String[] knownAttributes) {
        if (node.hasAttributes()) {
            NamedNodeMap attrs = node.GetNode().getAttributes();

            for (int oLoop = 0; oLoop < attrs.getLength(); oLoop++) {
                boolean found = false;
                Attr attribute = (Attr) attrs.item(oLoop);
                String strAlias = attribute.getName();
                for (int iLoop = 0; iLoop < knownAttributes.length; iLoop++) // compare to list of valid
                {
                    if (0 == knownAttributes[iLoop].compareToIgnoreCase(strAlias)) // 1st check case independent just
                    // for fun
                    {
                        found = true;
                        break;
                    }
                }
                if (false == found) {
                    String strValue = node.getAttribute(strAlias);
                    AddAlias(strAlias, strValue);
                    LOGGER.config(
                            "Adding Alias for external file from attribute list : " + strAlias + "-->" + strValue);
                }
            }
        }
    }

    private void AddEnvironmentVars() {
        Map<String, String> env = System.getenv();
        for (String envName : env.keySet()) {
            AddAlias(envName, env.get(envName));
        }
    }

    public void addMarvinInfo() {
        try {
            String current = new java.io.File(".").getCanonicalPath();
            AddRootAlias("WORKING_DIR", current);

            String path = new File(".").toURI().toString();
            AddRootAlias("WORKING_DIR_URI", path);
        } catch (IOException ex) {

        }

        Configuration CONFIG = Configuration.getConfig();
        Rectangle2D visualBounds = CONFIG.getPrimaryScreen().getVisualBounds();
        double width = CONFIG.getWidth();
        double height = CONFIG.getHeight();
        // use configured dimensions, otherwise use screen;
        if (width == 0.0) {
            width = visualBounds.getWidth();
        }
        if (height == 0.0) {
            height = visualBounds.getHeight();
        }
        // Width = Width - CONFIG.getAppBorderWidth() * 2;
        // Height = Height - CONFIG.getBottomOffset() - CONFIG.getTopOffset();
        double h2WRatio = width / height;
        double w2HRatio = height / width;

        h2WRatio = visualBounds.getWidth() / visualBounds.getHeight();
        w2HRatio = visualBounds.getHeight() / visualBounds.getWidth();
        AddRootAlias("CANVAS_WIDTH", Integer.toString(Configuration.getConfig().getCanvasWidth()));
        AddRootAlias("CANVAS_HEIGHT", Integer.toString(Configuration.getConfig().getCanvasHeight()));
        AddRootAlias("SCREEN_H2W_RATIO", Double.toString(h2WRatio));
        AddRootAlias("SCREEN_W2H_RATIO", Double.toString(w2HRatio));
    }

    public void AddRandomValueAlias() {
        randomVal++;
        AddAlias("RandomVal", Integer.toString(randomVal));
    }

    @SuppressWarnings("unchecked")
    public void AddRootAlias(String Alias, String Value) {
        if (null == Alias) {
            LOGGER.severe("Attempted to set a Root ALIAS ID to NULL");
            return;
        }
        Map<String, String> map = aliasList.get(aliasList.size() - 1);
        if (map.containsKey(Alias.toUpperCase())) {
            return;
        }
        if (null == Value) {
            String strError = "Root Alias [" + Alias + "] has NULL value!";
            LOGGER.severe(strError);
            map.put(Alias.toUpperCase(), strError);
        } else {
            map.put(Alias.toUpperCase(), Value);
        }
    }

    @SuppressWarnings("unchecked")
    public void AddUpdateAlias(String Alias, String Value) {
        if (null == Alias) {
            LOGGER.severe("Attempted to set an ALIAS ID to NULL");
            return;
        }
        Map<String, String> map = aliasList.get(0);
        if (map.containsKey(Alias.toUpperCase())) {
            UpdateAlias(Alias, Value);
        }
        if (null == Value) {
            String strError = "Alias [" + Alias + "] has NULL value!";
            LOGGER.severe(strError);
            map.put(Alias.toUpperCase(), strError);
        } else {
            map.put(Alias.toUpperCase(), Value);
        }
    }

    public final void ClearAll() {
        aliasList.clear();
        PushAliasList(true);
        randomVal = 1;
        AddAlias("RandomVal", Integer.toString(randomVal));
        AddAlias("DEBUG_STYLE", "-fx-border-color:blue;-fx-border-style: dashed");
        PushAliasList(true);
        AddEnvironmentVars();
        if (null != externalAliasFile) {
            ReadExternalAliasFile(externalAliasFile);
        }

    }

    /**
     * Simple debug routine to dump top alias list
     */
    @SuppressWarnings("rawtypes")
    public void DumpTop() {
        Map map = aliasList.get(0);
        String aliasStr = "Global Alias List:\n";

        if (map.isEmpty()) {
            return;
        }

        for (Object objKey : map.keySet()) {
            String key = (String) objKey;
            aliasStr += key + "-->" + map.get(objKey) + "\n";
        }
        LOGGER.info(aliasStr);
    }

    /**
     * Fetches the string associated with the alias if exists, else null
     *
     * @param strAliasRequested
     * @return
     */
    @SuppressWarnings("rawtypes")
    public String GetAlias(String strAliasRequested) {
        String strAlias = strAliasRequested.toUpperCase();

        for (Map map : aliasList) {
            if (map.containsKey(strAlias)) {
                String strRetVal = (String) map.get(strAlias);
                if (strAlias.equalsIgnoreCase(strNextRowAlias)) {
                    int currVal = Integer.parseInt(GetAlias(strCurrentRowAlias));
                    strRetVal = Integer.toString(currVal + 1);
                } else if (strAlias.equalsIgnoreCase(strPrevRowAlias)) {
                    int currVal = Integer.parseInt(GetAlias(strCurrentRowAlias));
                    currVal -= 1;
                    if (currVal < 0) {
                        currVal = 0;
                    }
                    strRetVal = Integer.toString(currVal);
                } else if (strAlias.equalsIgnoreCase(strNextColumnAlias)) {
                    int currVal = Integer.parseInt(GetAlias(strCurrentColumnAlias));
                    strRetVal = Integer.toString(currVal + 1);
                } else if (strAlias.equalsIgnoreCase(strPrevColumnAlias)) {
                    int currVal = Integer.parseInt(GetAlias(strCurrentColumnAlias));
                    currVal -= 1;
                    if (currVal < 0) {
                        currVal = 0;
                    }
                    strRetVal = Integer.toString(currVal);
                }
                return strRetVal;
            }
        }

        String strError = strAliasRequested;

        if (!Configuration.getConfig().DoNotReportAliasErrors()) {

            String strfName = currentFileName;
            strError = "Tried to use Alias [" + strAliasRequested + "] that has not been set. File: " + strfName;
            LOGGER.severe(strError);
        }
        return strError;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getSnapshot() {
        Map<String, String> retMap = new HashMap<>();
        for (int i = aliasList.size() - 1; i >= 0; i--) {
            Map<String, String> aliasMap = aliasList.get(i);
            for (String key : aliasMap.keySet()) {
                retMap.put(key, aliasMap.get(key));
            }
        }
        return retMap;
    }

    /**
     * Just checks to see if the given string is an Alias
     *
     * @param strAlias
     * @return
     */
    @SuppressWarnings("rawtypes")
    public boolean IsAliased(String strAlias) {
        strAlias = strAlias.toUpperCase();
        for (Map map : aliasList) {
            if (map.containsKey(strAlias)) {
                return true;
            }
        }
        return false;
    }

    public int LoadAliasFile(String encodedFilename) {
        StringTokenizer tokens = new StringTokenizer(encodedFilename, "=");
        tokens.nextElement(); // eat up the -alaisfile=
        externalAliasFile = (String) tokens.nextElement(); // to get to the real filename!
        return ReadExternalAliasFile(externalAliasFile);
    }

    /**
     *
     */
    public void PopAliasList() {
        aliasList.remove(0);
        GridMacroMgr.getGridMacroMgr().PopGridMacroList();
    }

    /**
     * Implemented as a kind of stack for scope reasons meaning that if an alias
     * is used in a file, it is valid for all nested files, but not outside of
     * that scope
     */
    public final void PushAliasList(boolean addRowColAliases) {
        aliasList.add(0, new HashMap<>()); // put in position 0

        if (addRowColAliases) {
            AddAlias(strCurrentColumnIsOddAlias, "FALSE");
            AddAlias(strCurrentRowIsOddAlias, "FALSE");
            AddAlias(strCurrentRowAlias, "0");
            AddAlias(strNextRowAlias, "1");
            AddAlias(strCurrentColumnAlias, "0");
            AddAlias(strNextColumnAlias, "1");
            AddAlias(strPrevColumnAlias, "0");
            AddAlias(strPrevRowAlias, "0");
        }
        GridMacroMgr.getGridMacroMgr().PushGridMacroList();
    }

    private int ReadExternalAliasFile(String filename) {
        LOGGER.info("Reading external Alias File: " + filename);

        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AliasMgr.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
        String line;
        SetCurrentConfigFile(filename);
        try {
            while ((line = br.readLine()) != null) {
                if (line.trim() != null) {
                    StringTokenizer st = new StringTokenizer(line, "=");
                    String strAlias;
                    String Value;
                    if (st.hasMoreElements()) {
                        strAlias = ((String) st.nextElement()).trim();
                    } else {
                        continue; // no more
                    }
                    if (st.hasMoreElements()) {
                        Value = ((String) st.nextElement()).trim();
                        if (Value.charAt(0) == '"' && Value.charAt(Value.length() - 1) == '"') {
                            Value = Value.substring(1, Value.length() - 1);
                        }
                    } else {
                        if (line.charAt(0) != '#') {
                            LOGGER.severe("Bad Alias in Alias File: " + line); // only be here if line is something like
                            // alias=
                        }
                        continue;
                    }
                    AddAlias(strAlias, Value);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(AliasMgr.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(AliasMgr.class.getName()).log(Level.SEVERE, null, ex);
        }

        // process the line.
        return 0;
    }

    public void SetCurrentConfigFile(String strFname) {
        currentFileName = strFname;
        AddAlias("CurrentConfigFilename", strFname);
    }

    @SuppressWarnings("unchecked")
    public void SilentAddAlias(String Alias, String Value) {
        if (null == Alias) {
            return;
        }
        Map<String, String> map = aliasList.get(0);
        if (map.containsKey(Alias.toUpperCase())) {
            return;
        }
        if (null == Value) {
            String strError = "Alias [" + Alias + "] has NULL value!";
            map.put(Alias.toUpperCase(), strError);
        } else {
            map.put(Alias.toUpperCase(), Value);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean UpdateAlias(String Alias, String newValue) {
        String strCheck = Alias.toUpperCase();

        for (Map map : aliasList) {
            if (map.containsKey(strCheck)) {
                map.replace(strCheck, newValue);
                return true;
            }
        }
        LOGGER.severe("Asked to updated alias: " + Alias + ". However it did not exist.");
        return false;
    }

    public void UpdateCurrentColumn(int newValue) {
        UpdateAlias(strNextColumnAlias, Integer.toString(newValue + 1));
        UpdateAlias(strCurrentColumnAlias, Integer.toString(newValue));
        UpdateAlias(strCurrentColumnIsOddAlias, (newValue % 2) == 0 ? "FALSE" : "TRUE");
    }

    public void UpdateCurrentRow(int newValue) {
        UpdateAlias(strNextRowAlias, Integer.toString(newValue + 1));
        UpdateAlias(strCurrentRowAlias, Integer.toString(newValue));
        UpdateAlias(strCurrentRowIsOddAlias, (newValue % 2) == 0 ? "FALSE" : "TRUE");
    }
}
