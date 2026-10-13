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

package org.testin.clipboard;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinDataKeys;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.JsonCodec;

import java.awt.datatransfer.StringSelection;
import java.util.List;

public class CopyTestCaseAction extends AbstractAnyProjectAction {
    // UC-EDITOR-PANEL-015
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project project) {
        final @NotNull List<TestCaseDto> testCases = TestinDataKeys.selectedTestCases(e);

        if (!testCases.isEmpty()) {
            try {
                final @NotNull String json = Services.getInstance(project, JsonCodec.class).writeValueAsString(testCases);
                CopyPasteManager.getInstance().setContents(new StringSelection(json));

                Services.getInstance(project, Notifier.class).softShowCounted(project, Done.COPIED, testCases.size());

            } catch (final Exception ex) {
                TestinLogger.error("Copy Node failed: " + FailureText.of(ex));
                Services.getInstance(project, Notifier.class).softRefuse(project, Bundle.message("clipboard.copy.failed.title"), FailureText.of(ex));
            }
        }
    }

    // Rule-EDITOR-PANEL-230
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project project) {
        // Rule-EDITOR-PANEL-214
        if (TestinDataKeys.editor(e).filter(editor -> !editor.getParent().isTestCaseContainer()).isPresent()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setDescription(Bundle.message("copy.test.case.disabled.description"));
            return;
        }

        GrayWithReason.unlessTestCaseSelected(this, e);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
