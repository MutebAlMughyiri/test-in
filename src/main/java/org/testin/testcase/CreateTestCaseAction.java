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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.TestinDataKeys;
import org.testin.codegen.CodeChangeType;
import org.testin.editor.TestinEditor;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.services.TestCaseCompletions;
import org.testin.testcase.create.CreateTestCaseDialog;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// UC-EDITOR-PANEL-005
public class CreateTestCaseAction extends AbstractAnyProjectAction {
    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-008, Rule-EDITOR-PANEL-030
    public static void openCreateDialog(final @NotNull Project project, final @NotNull TestinEditor editor, final @NotNull TestSetNode testSet) {
        new CreateTestCaseDialog(project, testSet, testCase -> {
            final @NotNull List<TestCaseDto> affectedNodes = List.of(testCase);

            final @NotNull List<UUID> testCaseIds = TestCaseSnapshot.idsOf(affectedNodes);
            final @NotNull TestCaseSnapshot before = TestCaseSnapshot.of(project, testSet.getPath(), testCaseIds);

            editor.appendNewTestCase(testCase, () -> {
                TestCaseSnapshot.record(project, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.create"), affectedNodes), before, TestCaseSnapshot.of(project, testSet.getPath(), testCaseIds));

                ApplicationManager.getApplication().invokeLater(() -> {
                    Services.getInstance(project, Notifier.class).softShow(project, Done.CREATED);
                    CodeChangeType.CREATE_TEST_CASE.execute(project, testCase);
                });
            });
            Services.getInstance(project, TestCaseCompletions.class).addNewItems(affectedNodes);
        }).show();
    }

    // UC-EDITOR-PANEL-005
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project project) {
        TestinDataKeys.editor(e)
                .filter(editor -> editor.getParent().isTestCaseContainer())
                .ifPresent(editor -> openCreateDialog(project, editor, (TestSetNode) editor.getParent()));
    }

    // UC-EDITOR-PANEL-005, UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-214
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project project) {
        final @NotNull Optional<TestinEditor> editor = TestinDataKeys.editor(e);
        final boolean holdsTestCases = editor.filter(open -> open.getParent().isTestCaseContainer()).isPresent();

        final boolean loading = editor.filter(TestinEditor::isLoading).isPresent();
        final boolean enabled = holdsTestCases && !loading;

        e.getPresentation().setEnabled(enabled);

        if (!enabled && editor.isPresent()) {
            e.getPresentation().setDescription(loading
                    ? Bundle.message("create.test.case.still.loading")
                    : Bundle.message("create.test.case.disabled.description"));
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
