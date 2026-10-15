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

package org.testin.testrun;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinDataKeys;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItem;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testrun.failure.FailureDetailDialog;
import org.testin.util.Bundle;
import org.testin.view.ViewToolWindowFactory;

import java.util.List;
import java.util.Optional;

// UC-EDITOR-PANEL-040
public class EditFailureDetailAction extends AbstractAnyProjectAction {
    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-167
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project project) {
        final @NotNull Optional<TestCase> selected = TestinDataKeys.singleSelectedTestCase(e);
        final @NotNull Optional<TestRunEditor> testRunEditor = TestinDataKeys.testRunEditor(e);
        if (selected.isEmpty() || testRunEditor.isEmpty()) return;

        edit(project, testRunEditor.orElseThrow(), selected.orElseThrow());
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-167, Rule-EDITOR-PANEL-169, Rule-EDITOR-PANEL-170
    private void edit(final @NotNull Project project, final @NotNull TestRunEditor testRunEditor, final @NotNull TestCase testCase) {
        final @NotNull Optional<RunItem> found = testRunEditor.runItem(testCase.getUuid());
        if (found.isEmpty()) return;

        final @NotNull RunItem runItem = found.orElseThrow();

        if (runItem.isRemoved()) {
            Services.getInstance(project, RunItemRecorder.class).refuseRemoved();
            return;
        }

        TestinLogger.trace("update test run item for: " + testCase.getDescription());

        new FailureDetailDialog(project, testRunEditor.getParent().getPath(), runItem, fields -> {
            if (!Services.getInstance(project, RunItemRecorder.class).reviseFailure(testRunEditor.getParent().getPath(), testCase.getUuid(), fields))
                return;

            ApplicationManager.getApplication().invokeLater(() -> {
                testRunEditor.refreshView();

                ViewToolWindowFactory.refreshIfShowing(project, List.of(testCase));
            });

            Services.getInstance(project, Notifier.class).softShow(project, Bundle.message("run.item.updated"));
        }).show();
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-168, Rule-TREE-PANEL-009
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project project) {
        final @NotNull Optional<String> signedOff = TestinDataKeys.testRunEditor(e).flatMap(testRunEditor -> testRunEditor.getParent().whySignedOff());

        GrayWithReason.unless(this, e, signedOff.isEmpty() && TestinDataKeys.testRunEditor(e)
                        .flatMap(testRunEditor -> TestinDataKeys.singleSelectedTestCase(e).flatMap(testCase -> testRunEditor.runItem(testCase.getUuid())))
                        .filter(RunItem::isFailed)
                        .isPresent(),
                signedOff.orElse(Bundle.message("run.item.details.disabled.description")));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
