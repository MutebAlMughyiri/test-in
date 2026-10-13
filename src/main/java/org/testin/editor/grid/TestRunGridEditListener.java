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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItem;
import org.testin.services.Services;
import org.testin.testrun.RunItemRecorder;
import org.testin.testrun.TestRunEditorAttributes;

import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TestRunGridEditListener extends AbstractGridEditListener {
    private final @NotNull TestRunEditor editor;

    private final @NotNull Runnable onEdited;

    private final @NotNull RunItemRecorder runItemStatusService;

    private final @NotNull TestRuns testRuns;

    public TestRunGridEditListener(final @NotNull Project p, final @NotNull TestRunEditor editor, final @NotNull List<TestCaseDto> pageTestCases, final @NotNull Runnable onEdited) {
        super(p, pageTestCases);
        this.editor = editor;
        this.onEdited = onEdited;
        this.runItemStatusService = Services.getInstance(p, RunItemRecorder.class);
        this.testRuns = Services.getInstance(p, TestRuns.class);
    }

    @Override
    protected int columnCount() {
        return TestRunEditorAttributes.COLUMNS.size();
    }

    // UC-EDITOR-PANEL-041, Rule-EDITOR-PANEL-174
    @Override
    protected @NotNull GridEdit apply(final @NotNull DefaultTableModel model, final @NotNull TestCaseDto onThisRow, final int row, final int col) {
        final @NotNull TestRunEditorAttributes attr = TestRunEditorAttributes.atColumn(col);

        if (!attr.isEdited()) return GridEdit.UNCHANGED;

        final @NotNull Optional<RunItem> found = editor.runItem(onThisRow.getId());
        if (found.isEmpty()) return GridEdit.UNCHANGED;
        final @NotNull RunItem runItem = found.orElseThrow();

        final @NotNull String before = attr.getRunItemValueExtractor().apply(runItem);

        if (runItem.isRemoved()) {
            model.setValueAt(before, row, col);
            runItemStatusService.refuseRemoved();
            return GridEdit.REFUSED;
        }

        // Rule-EDITOR-PANEL-174
        if (runItemStatusService.heldTestRun(editor.getParent().getPath()).isEmpty()) {
            model.setValueAt(before, row, col);
            return GridEdit.REFUSED;
        }

        final @NotNull String typed = String.valueOf(model.getValueAt(row, col));
        testRuns.changeRunItem(editor.getParent().getPath(), onThisRow.getId(), result -> attr.getRunItemValueSetter().execute(result, typed));
        final @NotNull String after = attr.getRunItemValueExtractor().apply(runItem);

        model.setValueAt(after, row, col);

        if (Objects.equals(before, after)) return GridEdit.UNCHANGED;

        onEdited.run();

        return GridEdit.WROTE;
    }
}
