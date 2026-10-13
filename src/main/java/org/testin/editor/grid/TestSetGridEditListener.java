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
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.indexer.TestCases;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.testcase.Can;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.testcase.TestCaseSnapshot;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;

import javax.swing.table.DefaultTableModel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class TestSetGridEditListener extends AbstractGridEditListener {
    private final @NotNull Runnable onEdited;

    private final @NotNull Path testSetPath;

    private final @NotNull TestCases testCases;

    private final @NotNull Map<UUID, Changed> changedThisGesture = new LinkedHashMap<>();

    public TestSetGridEditListener(final @NotNull Project project, final @NotNull List<TestCaseDto> pageTestCases, final @NotNull Runnable onEdited, final @NotNull Path testSetPath) {
        super(project, pageTestCases);
        this.onEdited = onEdited;
        this.testSetPath = testSetPath;
        this.testCases = Services.getInstance(project, TestCases.class);
    }

    private static @NotNull String quoted(final @NotNull String typed, final @NotNull TestSetEditorAttributes attribute) {
        return Bundle.message("grid.refused.as", typed.trim(), attribute.getName());
    }

    @Override
    protected int columnCount() {
        return TestSetEditorAttributes.COLUMNS.size();
    }

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-050
    @Override
    protected @NotNull GridEdit apply(final @NotNull DefaultTableModel model, final @NotNull TestCaseDto testCase, final int row, final int col) {
        final @NotNull TestSetEditorAttributes attribute = TestSetEditorAttributes.atColumn(col);

        if (!attribute.can(Can.EDIT)) return GridEdit.UNCHANGED;

        final @NotNull Optional<Changed> pending = Optional.ofNullable(changedThisGesture.get(testCase.getId()));
        final @NotNull TestCaseSnapshot undoFrom = pending.map(Changed::before)
                .orElseGet(() -> TestCaseSnapshot.of(project, testSetPath, List.of(testCase.getId())));
        final @NotNull TestCaseDto current = pending.map(Changed::testCase).orElse(testCase);

        final @NotNull String typed = String.valueOf(model.getValueAt(row, col));

        final @NotNull Object before = attribute.gridValue(current);
        final @NotNull Optional<TestCaseDto> took = attribute.getImportSetter().execute(project, current, typed);
        final @NotNull Object after = attribute.gridValue(took.orElse(current));

        model.setValueAt(after, row, col);

        // Rule-EDITOR-PANEL-206
        if (took.isEmpty()) {
            notifier.softRefuse(project, Refused.UNREADABLE, quoted(typed, attribute));
            return GridEdit.REFUSED;
        }

        if (Objects.equals(before, after)) return GridEdit.UNCHANGED;

        persistAndGenerate(took.orElseThrow(), attribute, undoFrom);

        return GridEdit.WROTE;
    }

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-053
    private void persistAndGenerate(final @NotNull TestCaseDto edited, final @NotNull TestSetEditorAttributes attribute, final @NotNull TestCaseSnapshot undoFrom) {
        if (testSetPath.toString().isEmpty()) {
            TestinLogger.warn("[grid] edit not persisted - the editor has no test set path");
            return;
        }

        final boolean first = changedThisGesture.isEmpty();
        final @NotNull Set<GenType<TestCaseDto>> generators = Optional.ofNullable(changedThisGesture.get(edited.getId()))
                .map(Changed::generators)
                .orElseGet(LinkedHashSet::new);
        generators.add(attribute.getGenType());
        changedThisGesture.put(edited.getId(), new Changed(edited, undoFrom, generators));

        if (first) ApplicationManager.getApplication().invokeLater(this::saveTheGesture);
    }

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-053, Rule-EDITOR-PANEL-038
    private void saveTheGesture() {
        final @NotNull List<Changed> gesture = List.copyOf(changedThisGesture.values());
        changedThisGesture.clear();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull List<TestCaseDto> written = new ArrayList<>();
            final @NotNull List<TestCaseSnapshot> before = new ArrayList<>();

            for (final Changed changed : gesture) {
                if (!testCases.putTestCase(testSetPath, changed.testCase())) continue;

                changed.generators().forEach(generator -> generator.execute(project, changed.testCase()));
                written.add(changed.testCase());
                before.add(changed.before());
            }

            ApplicationManager.getApplication().invokeLater(onEdited);

            if (written.isEmpty()) return;

            TestCaseSnapshot.record(project, UndoScope.of(testSetPath), TestCaseSnapshot.describe(Bundle.message("snapshot.verb.edit"), written),
                    before, List.of(TestCaseSnapshot.of(project, testSetPath, TestCaseSnapshot.idsOf(written))));
        });
    }
}
