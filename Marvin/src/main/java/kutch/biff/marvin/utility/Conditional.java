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

import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;

import javafx.beans.value.ObservableValue;
import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.widget.widgetbuilder.WidgetBuilder;

/**
 * @author Patrick Kutch
 */
public class Conditional {

    public enum Type {
        EQ, NE, GT, GE, LT, LE, CASE, Invalid
    }

    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    public static Conditional BuildConditional(Type type, FrameworkNode condNode, boolean usesThen) {
        Conditional objCond = null;
        if (type == Type.CASE) {
            objCond = ConditionalCase.BuildConditionalCase(condNode);
        } else if (type != Type.Invalid) {
            ArrayList<FrameworkNode> orChildren = condNode.getChildNodes("OR");
            ArrayList<FrameworkNode> andChildren = condNode.getChildNodes("And");

            if (orChildren.isEmpty() && andChildren.isEmpty()) {

                objCond = new Conditional(type, usesThen);
            } else {
                objCond = new CompoundConditional(type);
            }
            if (!objCond.readCondition(condNode)) {
                objCond = null;
            }
        }

        return objCond;
    }

    public static boolean EvaluateConditional(String strValue1, String strValue2, Type conditional,
                                              boolean isCaseSensitive) {
        boolean result;

        try {
            result = PerformValue(Double.parseDouble(strValue1), Double.parseDouble(strValue2), conditional);
        } catch (NumberFormatException ex) {
            result = PerformString(strValue1, strValue2, conditional, isCaseSensitive);
        }
        return result;
    }

    ;

    public static Type GetType(String strType) {
        if (null == strType) {
            return Type.Invalid;
        }

        if ("IF_EQ".equalsIgnoreCase(strType)) {
            return Type.EQ;
        }
        if ("IF_NE".equalsIgnoreCase(strType)) {
            return Type.NE;
        }
        if ("IF_GE".equalsIgnoreCase(strType)) {
            return Type.GE;
        }
        if ("IF_GT".equalsIgnoreCase(strType)) {
            return Type.GT;
        }
        if ("IF_LE".equalsIgnoreCase(strType)) {
            return Type.LE;
        }
        if ("IF_LT".equalsIgnoreCase(strType)) {
            return Type.LT;
        }
        if ("IF_EQ".equalsIgnoreCase(strType)) {
            return Type.EQ;
        }
        if ("CASE".equalsIgnoreCase(strType)) {
            return Type.CASE;
        }
        return Type.Invalid;
    }

    protected static boolean PerformString(String val1, String val2, Type testType, boolean isCaseSensitive) {
        if (!isCaseSensitive) {
            val1 = val1.toLowerCase();
            val2 = val2.toLowerCase();
        }

        val1 = val1.trim();
        val2 = val2.trim();
        switch (testType) {
            case EQ:
                return val1.equals(val2);
            case NE:
                return !val1.equals(val2);

            case GT:
                return val1.compareTo(val2) > 0;

            case GE:
                return val1.compareTo(val2) >= 0;

            case LT:
                return val1.compareTo(val2) < 0;

            case LE:
                return val1.compareTo(val2) <= 0;
            case CASE:
                break;
            case Invalid:
                break;
            default:
                break;
        }
        return false;
    }

    protected static boolean PerformValue(double Val1, double Val2, Type testType) {
        switch (testType) {
            case EQ:
                return Val1 == Val2;

            case NE:
                return Val1 != Val2;

            case GT:
                return Val1 > Val2;

            case GE:
                return Val1 >= Val2;

            case LT:
                return Val1 < Val2;

            case LE:
                return Val1 <= Val2;
            default:
                break;
        }
        return false;
    }

    private final TaskManager taskman = TaskManager.getTaskManager();
    private String value1ID;
    private String _Value1_Namespace;
    private String _Value2_ID;
    private String _Value2_Namespace;
    private String _Value2;
    private final Type _type;
    private String _If_Task;
    private String _Else_Task;

    private boolean _CaseSensitive;

    protected boolean _UsesThen;

    @SuppressWarnings("unused")
    private ValueRange dataIndexRange;
    @SuppressWarnings("unused")
    private String dataIndexToken;

    /*
     * public Conditional(Conditional.Type type) { _type = type; _Value1_ID = null;
     * _Value1_Namespace = null; _Value2_ID = null; _Value2_Namespace = null;
     * _Value2 = null; _If_Task = null; _Else_Task = null; _UsesThen = true; }
     */
    public Conditional(Conditional.Type type, boolean usesThen) {
        _type = type;
        value1ID = null;
        _Value1_Namespace = null;
        _Value2_ID = null;
        _Value2_Namespace = null;
        _Value2 = null;
        _If_Task = null;
        _Else_Task = null;
        _UsesThen = usesThen;
        dataIndexRange = ValueRange.of(-1, -1);
        dataIndexToken = ",";

    }

    public void Enable() {
        DataManager.getDataManager().AddListener(value1ID, _Value1_Namespace, (ObservableValue<?> o, Object oldVal, Object newVal) -> {
            Perform(newVal.toString());
        });
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Conditional other = (Conditional) obj;
        if (this._CaseSensitive != other._CaseSensitive) {
            return false;
        }
        if (!Objects.equals(this.value1ID, other.value1ID)) {
            return false;
        }
        if (!Objects.equals(this._Value1_Namespace, other._Value1_Namespace)) {
            return false;
        }
        if (!Objects.equals(this._Value2_ID, other._Value2_ID)) {
            return false;
        }
        if (!Objects.equals(this._Value2_Namespace, other._Value2_Namespace)) {
            return false;
        }
        if (!Objects.equals(this._Value2, other._Value2)) {
            return false;
        }
        if (!Objects.equals(this._If_Task, other._If_Task)) {
            return false;
        }
        if (!Objects.equals(this._Else_Task, other._Else_Task)) {
            return false;
        }
        return this._type == other._type;
    }

    public String getElse_Task() {
        return _Else_Task;
    }

    protected String getElseTask() {
        return _Else_Task;
    }

    public String getIf_Task() {
        return _If_Task;
    }

    protected String getThenTask() {
        return _If_Task;
    }

    protected Type getType() {
        return _type;
    }

    // used for compound conditionals only
    protected String GetValue1() {
        return DataManager.getDataManager().GetValue(value1ID, _Value1_Namespace);
    }

    public String getValue1_Namespace() {
        return _Value1_Namespace;
    }

    public String getValue2() {
        return _Value2;
    }

    protected String GetValue2() {
        if (_Value2_ID != null && _Value2_Namespace != null) {
            return DataManager.getDataManager().GetValue(_Value2_ID, _Value2_Namespace);
        }
        return _Value2;
    }

    public String getValue2_ID() {
        return _Value2_ID;
    }

    public String getValue2_Namespace() {
        return _Value2_Namespace;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.value1ID);
        hash = 59 * hash + Objects.hashCode(this._Value1_Namespace);
        hash = 59 * hash + Objects.hashCode(this._Value2_ID);
        hash = 59 * hash + Objects.hashCode(this._Value2_Namespace);
        hash = 59 * hash + Objects.hashCode(this._Value2);
        hash = 59 * hash + Objects.hashCode(this._type);
        hash = 59 * hash + Objects.hashCode(this._Else_Task);
        hash = 59 * hash + (this._CaseSensitive ? 1 : 0);
        return hash;
    }

    public boolean isCaseSensitive() {
        return _CaseSensitive;
    }

    protected void Perform(String Val1) {
        String Val2 = GetValue2();
        if (null == Val1 || null == Val2) {
            LOGGER.warning("Tried to perform Conditional, but data not yet available");
            return;
        }

        if (Conditional.EvaluateConditional(Val1, Val2, _type, isCaseSensitive())) {
            taskman.AddDeferredTask(_If_Task);
        } else if (_Else_Task != null) {
            taskman.AddDeferredTask(_Else_Task);
        }
    }

    protected boolean readCondition(FrameworkNode condNode) {
        boolean retVal = true;

        if (ReadMinionSrc(condNode)) {
            for (FrameworkNode node : condNode.getChildNodes()) {
                if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#Comment".equalsIgnoreCase(node.getNodeName())) {
                    continue;
                } else if ("MinionSrc".equalsIgnoreCase(node.getNodeName())) {
                    continue;
                }
                if ("Value".equalsIgnoreCase(node.getNodeName())) {
                    if (node.hasChild("MinionSrc")) {
                        FrameworkNode valNode = node.getChild("MinionSrc");
                        if (valNode.hasAttribute("ID")) {
                            _Value2_ID = valNode.getAttribute("ID");
                        } else {
                            LOGGER.severe("Conditional <Value><MinionSrc> defined with invalid MinionSrc, no ID");
                            retVal = false;
                        }
                        if (valNode.hasAttribute("Namespace")) {
                            _Value2_Namespace = valNode.getAttribute("Namespace");
                        } else {
                            LOGGER.severe(
                                    "Conditional  <Value><MinionSrc> defined with invalid MinionSrc, no Namespace");
                            retVal = false;
                        }
                    } else {
                        _Value2 = node.getTextContent();
                        if (_Value2.length() < 1) {
                            LOGGER.severe("Conditional <Value> is empty");
                            retVal = false;
                        }
                    }
                }

                if (_UsesThen) {
                    if ("Then".equalsIgnoreCase(node.getNodeName())) {
                        _If_Task = node.getTextContent();
                        if (_If_Task.length() < 1) {
                            LOGGER.severe("Conditional <Then> is empty");
                            retVal = false;
                        }
                    }

                    if ("Else".equalsIgnoreCase(node.getNodeName())) {
                        _Else_Task = node.getTextContent();
                        if (_Else_Task.length() < 1) {
                            LOGGER.severe("Conditional <Else> is empty");
                            retVal = false;
                        }
                    }
                }
            }
            if (null == _If_Task && _UsesThen) {
                LOGGER.severe("Conditional defined with no <Then>");
                retVal = false;
            }

            if (null == value1ID && _Value2 == null) {
                LOGGER.severe("Conditional defined with no <Value>");
                retVal = false;
            }
        } else {
            retVal = false;
        }
        return retVal;
    }

    protected boolean ReadMinionSrc(FrameworkNode condNode) {
        String ID = null;
        String Namespace = null;

        for (FrameworkNode node : condNode.getChildNodes()) {
            if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#Comment".equalsIgnoreCase(node.getNodeName())) {
                continue;
            }
            if ("MinionSrc".equalsIgnoreCase(node.getNodeName())) {
                if (node.hasAttribute("ID")) {
                    ID = node.getAttribute("ID");
                } else {
                    LOGGER.severe("Conditional defined with invalid MinionSrc, no ID");
                    return false;
                }
                if (node.hasAttribute("Namespace")) {
                    Namespace = node.getAttribute("Namespace");
                } else {
                    LOGGER.severe("Conditional defined with invalid MinionSrc, no Namespace");
                    return false;
                }
                Pair<ValueRange, String> indexInfo = WidgetBuilder.ReadMinionSrcIndexInfo(node);
                dataIndexRange = indexInfo.getKey();
                dataIndexToken = indexInfo.getValue();
            }
        }
        SetNamespaceAndID(Namespace, ID);
        return true;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this._CaseSensitive = caseSensitive;
    }

    public void setElse_Task(String elseTask) {
        this._Else_Task = elseTask;
    }

    public void setIf_Task(String ifTask) {
        this._If_Task = ifTask;
    }

    public void SetNamespaceAndID(String namespace, String id) {
        value1ID = id;
        _Value1_Namespace = namespace;
    }

    public void setValue1_Namespace(String value1Namespace) {
        this._Value1_Namespace = value1Namespace;
    }

    public void setValue2(String value2) {
        this._Value2 = value2;
    }

    public void setValue2_ID(String value2ID) {
        this._Value2_ID = value2ID;
    }

    public void setValue2_Namespace(String value2Namespace) {
        this._Value2_Namespace = value2Namespace;
    }

    @Override
    public String toString() {
        return "Conditional{" + "_Value1_ID=" + value1ID + ", _Value1_Namespace=" + _Value1_Namespace
                + ", _Value2_ID=" + _Value2_ID + ", _Value2_Namespace=" + _Value2_Namespace + ", _Value2=" + _Value2
                + ", _type=" + _type + ", _If_Task=" + _If_Task + ", _Else_Task=" + _Else_Task + '}';
    }
}
