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

package org.testin.testcase;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.clipboard.CutState;
import org.testin.codegen.CodeChangeType;
import org.testin.codegen.WaitForIndexing;
import org.testin.editor.TestinEditor;
import org.testin.editor.open.TestinEditors;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

record RemoveTestCaseWork(@NotNull Project project, @NotNull TestinEditor editor, @NotNull AbstractNode node, @NotNull List<TestCase> selected, @NotNull CutState cutState, @NotNull TestCases testCases, @NotNull TestinEditors editors, @NotNull Notifier notifier) {
    RemoveTestCaseWork(final @NotNull Project project, final @NotNull TestinEditor editor, final @NotNull AbstractNode node, final @NotNull List<TestCase> selected) {
        this(project, editor, node, selected, Services.getInstance(project, CutState.class), Services.getInstance(project, TestCases.class), Services.getInstance(project, TestinEditors.class), Services.getInstance(project, Notifier.class));
    }

    // UC-EDITOR-PANEL-011, Rule-EDITOR-PANEL-062
    void remove() {
        final @NotNull List<TestCase> selectedTestCases = selected;
        if (selectedTestCases.isEmpty()) return;

        final @NotNull Runnable delete = () -> {
            if (WaitForIndexing.refuses(project, Bundle.message("remove.confirm.button"))) return;

            ApplicationManager.getApplication().runWriteAction(() -> performDeletion(selectedTestCases));
        };

        final @NotNull String message = selectedTestCases.size() == 1
                ? Bundle.message("remove.test.case.confirm.one", selectedTestCases.getFirst().getDescription())
                : Bundle.message("remove.test.case.confirm.many", String.valueOf(selectedTestCases.size()));

        new ConfirmDialog(project, Bundle.message("remove.confirm.title"), message, node.getPath().toString(), "", Bundle.message("remove.confirm.button"), delete).show();
    }

    // UC-EDITOR-PANEL-011, Rule-EDITOR-PANEL-064
    private void performDeletion(final @NotNull List<TestCase> selectedTestCases) {
        editor.getAllTestCases().removeAll(selectedTestCases);

        cutState.clear();

        ApplicationManager.getApplication().executeOnPooledThread(() -> removeFromDisk(selectedTestCases));
    }

    // UC-EDITOR-PANEL-011, Rule-EDITOR-PANEL-064
    private void removeFromDisk(final @NotNull List<TestCase> selectedTestCases) {
        final @NotNull List<UUID> testCaseIds = TestCaseSnapshot.idsOf(selectedTestCases);
        final @NotNull TestCaseSnapshot before = TestCaseSnapshot.of(project, node.getPath(), testCaseIds);

        final @NotNull List<TestCase> removed = new ArrayList<>();
        for (final TestCase testCase : selectedTestCases) {
            if (testCases.removeTestCase(node.getPath(), testCase.getId())) removed.add(testCase);
        }

        TestCaseSnapshot.record(project, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.remove"), removed), before, TestCaseSnapshot.of(project, node.getPath(), testCaseIds));

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!removed.isEmpty()) CodeChangeType.REMOVE_TEST_CASE.executeAll(project, removed);

            if (removed.size() == selectedTestCases.size()) editor.refreshView();
            else editors.reloadOpen(node.getPath());

            if (!removed.isEmpty())
                notifier.softShowCounted(project, Done.REMOVED, removed.size());
        });
    }
}
