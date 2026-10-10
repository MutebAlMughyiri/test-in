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
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinData;
import org.testin.util.Bundle;
import org.testin.util.ClipboardContents;

import java.awt.datatransfer.DataFlavor;
import java.util.Optional;

public class PasteTestCaseAction extends AbstractAnyProjectAction {
    private @NotNull Optional<Answered> answered = Optional.empty();

    private static @NotNull Optional<PasteTestCaseWork> work(final @NotNull AnActionEvent e, final @NotNull Project p) {
        return TestinData.editor(e).map(editor -> new PasteTestCaseWork(p, editor));
    }

    // UC-EDITOR-PANEL-017
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project p) {
        work(e, p).ifPresent(PasteTestCaseWork::paste);
    }

    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project p) {
        // Rule-EDITOR-PANEL-214
        if (TestinData.editor(e).filter(editor -> !editor.getParent().isTestCaseContainer()).isPresent()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setDescription(Bundle.message("paste.test.case.disabled.description"));
            return;
        }

        GrayWithReason.unless(this, e, work(e, p).map(this::clipboardHoldsTestCases).orElse(false), Bundle.message("paste.test.case.nothing.description"));
    }

    private boolean clipboardHoldsTestCases(final @NotNull PasteTestCaseWork work) {
        return ClipboardContents.withFlavor(DataFlavor.stringFlavor)
                .map(contents -> answered.filter(last -> last.contents().equals(contents)).orElseGet(() -> {
                    final @NotNull Answered now = new Answered(contents, work.holdsTestCases(contents));
                    answered = Optional.of(now);
                    return now;
                }).holdsTestCases())
                .orElse(false);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
