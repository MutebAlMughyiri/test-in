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

package org.testin.importexport.shared;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.ui.framework.DialogComponent;

import javax.swing.JComponent;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public final class SheetPreview implements DialogComponent {
    private static final @NotNull DefaultTableModel NO_MODEL = new DefaultTableModel();

    private final @NotNull Project p;
    private final @NotNull List<TestSetEditorAttributes> attributes;

    private final @NotNull JBTabbedPane tabs = new JBTabbedPane();
    private final @NotNull Map<String, DefaultTableModel> models = new LinkedHashMap<>();

    private @NotNull Map<String, List<TestCaseDto>> sheets = new LinkedHashMap<>();

    // UC-SHARE-007, Rule-SHARE-036
    public void show(final @NotNull Map<String, List<TestCaseDto>> newSheets) {
        TestinLogger.debug("Import preview: showing " + newSheets.values().stream().mapToInt(List::size).sum()
                + " test cases in " + newSheets.size() + " sheet(s), replacing " + sheets.size() + " sheet(s)");
        sheets = newSheets;

        models.clear();
        while (tabs.getTabCount() > 0) {
            tabs.removeTabAt(0);
        }

        for (final Map.Entry<String, List<TestCaseDto>> entry : sheets.entrySet()) {
            final @NotNull List<TestCaseDto> testCases = entry.getValue();

            final @NotNull DefaultTableModel model = new TablePanelBuilder().createModel(attributes, testCases);
            model.addTableModelListener(new CellEditListener(attributes, p, testCases));

            models.put(entry.getKey(), model);
            tabs.addTab(entry.getKey(), new JBScrollPane(new TablePanelBuilder().buildTable(model, p)));
        }
    }

    public boolean isEmpty() {
        return sheets.isEmpty();
    }

    // UC-SHARE-001, Rule-SHARE-008
    public @NotNull Map<String, List<TestCaseDto>> selected() {
        final @NotNull Map<String, List<TestCaseDto>> selectedBySheet = new LinkedHashMap<>();

        for (final Map.Entry<String, List<TestCaseDto>> entry : sheets.entrySet()) {
            final @NotNull DefaultTableModel model = models.getOrDefault(entry.getKey(), NO_MODEL);

            final @NotNull List<TestCaseDto> testCasesInSheet = entry.getValue();
            final @NotNull List<TestCaseDto> selected = new ArrayList<>();

            for (int row = 0; row < model.getRowCount(); row++) {
                if (Boolean.TRUE.equals(model.getValueAt(row, 0))) selected.add(testCasesInSheet.get(row));
            }

            TestinLogger.info("Import preview: sheet '" + entry.getKey() + "' holds " + testCasesInSheet.size()
                    + " test cases, table has " + model.getRowCount() + " rows, " + selected.size() + " ticked");

            if (!selected.isEmpty()) selectedBySheet.put(entry.getKey(), selected);
        }

        return selectedBySheet;
    }

    @Override
    public @NotNull JComponent getPanel() {
        return tabs;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return tabs;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    @Override
    public boolean wantsFocus() {
        return false;
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }
}
