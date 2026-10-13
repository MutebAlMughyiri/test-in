/*
 * Copyright 2026 Muteb AlMughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.editor.grid;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.TestCase;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class GridSelectionListener implements ListSelectionListener {
    private final @NotNull TestinEditor editor;
    private final @NotNull JBTable table;
    private final @NotNull JBList<TestCase> list;
    private final @NotNull List<TestCase> pageTestCases;

    // UC-EDITOR-PANEL-024, Rule-EDITOR-PANEL-111
    @Override
    public void valueChanged(final ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;

        final int @NotNull [] rows = Arrays.stream(table.getSelectedRows())
                .filter(row -> row >= 0 && row < pageTestCases.size())
                .toArray();

        if (rows.length == 0) return;

        final boolean gridHadFocus = table.isFocusOwner();

        editor.selectTestCase(pageTestCases.get(rows[0]));
        list.setSelectedIndices(rows);

        if (gridHadFocus) {
            ApplicationManager.getApplication().invokeLater(table::requestFocusInWindow);
        }
    }
}
