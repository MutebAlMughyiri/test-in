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

package org.testin.git.conflict;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogButton;
import org.testin.ui.framework.DialogSize;
import org.testin.ui.framework.RadioSelection;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class ResolveConflictDialog extends AbstractFrameworkDialog {
    private static final int SHOWN = 70;

    private static final @NotNull String LINE = "\n";

    private final @NotNull List<Question> questions;

    private final @NotNull List<RadioSelection<Boolean>> answers = new ArrayList<>();

    private final @NotNull Consumer<Set<String>> onResolved;

    private final @NotNull Runnable onSkipped;

    // Rule-SHARE-082
    public ResolveConflictDialog(final @NotNull Project project, final @NotNull String testCase, final @NotNull List<Question> questions, final @NotNull List<String> settled, final @NotNull Consumer<Set<String>> onResolved, final @NotNull Runnable onSkipped) {
        super(project);
        this.questions = questions;
        this.onResolved = onResolved;
        this.onSkipped = onSkipped;

        title = Bundle.message("dialog.conflict.title", testCase);

        final @NotNull List<DialogPart<?>> rows = new ArrayList<>();

        if (!settled.isEmpty()) rows.add(DialogPart.message(MergeOutcome.settledSentence(settled, LINE)));

        for (final Question question : questions) {
            final @NotNull DialogPart<RadioSelection<Boolean>> row = DialogPart.<Boolean>radios(FieldName.of(question.field()))
                    .option(Bundle.message("dialog.conflict.option.mine", shortened(question.mine())), Boolean.FALSE)
                    .option(Bundle.message("dialog.conflict.option.remote", shortened(question.theirs())), Boolean.TRUE)
                    .select(Boolean.FALSE)
                    .build();

            rows.add(row);
            answers.add(row.getComponent());
        }

        final @NotNull DialogPart<DialogButton> keep = DialogPart.button(Bundle.message("dialog.conflict.button.keep"));
        rows.add(keep);

        components = List.copyOf(rows);

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, Bundle.message("dialog.conflict.button.keep"), this::submit),
                StatusBarShortcut.build(Shortcuts.Escape, Bundle.message("dialog.conflict.shortcut.skip"), this::skip));

        size = DialogSize.HALF;
    }

    // UC-SHARE-018, Rule-SHARE-083
    private static @NotNull String shortened(final @NotNull String value) {
        final @NotNull String oneLine = value.replace('\n', ' ').trim();
        if (oneLine.isEmpty()) return Bundle.message("dialog.conflict.empty");

        return oneLine.length() <= SHOWN ? oneLine : oneLine.substring(0, SHOWN - 1) + "…";
    }

    // UC-SHARE-018, Rule-SHARE-084
    private void skip() {
        closeCancel();
        onSkipped.run();
    }

    // UC-SHARE-018
    @Override
    protected void submit() {
        final @NotNull Set<String> takeTheirs = new LinkedHashSet<>();

        for (int i = 0; i < questions.size(); i++) {
            if (answers.get(i).getSelected()) takeTheirs.add(questions.get(i).field());
        }

        onResolved.accept(takeTheirs);
        closeOk();
    }
}
