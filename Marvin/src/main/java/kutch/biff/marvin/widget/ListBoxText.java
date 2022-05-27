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
package kutch.biff.marvin.widget;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;

/**
 * @author Patrick.Kutch@gmail.com
 */
public class ListBoxText extends BaseWidget {
    private ListView<String> listView;
    ObservableList<String> _list;

    public ListBoxText() {
        setDefaultIsSquare(false);

        _list = FXCollections.observableArrayList();
        listView = new ListView<>(_list);
    }

    public void addEntry(String strNewEntry) {
        _list.add(strNewEntry);
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        ConfigureDimentions();
        ConfigureAlignment();
        SetupPeekaboo(dataMgr);

        pane.add(listView, getColumn(), getRow(), getColumnSpan(), getRowSpan());
        dataMgr.AddListener(getMinionID(), getNamespace(), (ObservableValue<?> o, Object oldVal, Object newVal) -> {
            if (IsPaused()) {
                return;
            }

            String textString = newVal.toString();
            addEntry(textString);
        });

        SetupTaskAction();
        return ApplyCSS();

    }

    @Override
    public Node getStylableObject() {
        return listView;
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return listView.getStylesheets();
    }

    @Override
    public void SetInitialValue(String value) {
        addEntry(value);
    }

    @Override
    public void UpdateTitle(String newTitle) {

    }

}
