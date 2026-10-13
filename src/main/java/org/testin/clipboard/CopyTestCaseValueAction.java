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
import org.testin.model.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.ShortcutMenuPopup;
import org.testin.util.Bundle;

import java.awt.datatransfer.StringSelection;
import java.util.List;

public class CopyTestCaseValueAction extends AbstractAnyProjectAction {
    private static void copy(final @NotNull Project project, final @NotNull CopyChoice choice, final @NotNull List<TestCaseDto> selected) {
        CopyPasteManager.getInstance().setContents(new StringSelection(choice.from(selected)));

        Services.getInstance(project, Notifier.class).softShow(project, choice.copiedMessage(selected.size()));
    }

    // UC-EDITOR-PANEL-014, Rule-EDITOR-PANEL-207
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project project) {
        final @NotNull List<TestCaseDto> selected = TestinDataKeys.selectedTestCases(e);
        if (selected.isEmpty()) return;

        new ShortcutMenuPopup<>(project, Bundle.message("copy.menu.title"), CopyChoice.values(), choice -> copy(project, choice, selected)).show();
    }

    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project project) {
        GrayWithReason.unlessTestCaseSelected(this, e);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
