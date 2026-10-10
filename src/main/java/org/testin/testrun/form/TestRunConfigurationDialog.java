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

package org.testin.testrun.form;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.DialogButton;
import org.testin.ui.framework.DialogSize;
import org.testin.ui.framework.SelectionTree;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;

public final class TestRunConfigurationDialog extends AbstractFrameworkDialog {
    private final @NotNull TestRunFormAction action;
    private final @NotNull TestRunConfigurationForm form;
    private final @NotNull SelectionTree selection;

    public TestRunConfigurationDialog(final @NotNull Project p, final @NotNull TestRunConfigurationForm form, final @NotNull TestRunFormFilter filter, final @NotNull TestRunFormAction action) {
        super(p);
        this.action = action;
        this.form = form;
        this.selection = filter.getSelection();

        title = action.title();

        final @NotNull ComponentDialogBase<DialogButton> confirm = ComponentDialogBase.button(action.button());
        components = List.of(
                ComponentDialogBase.of(form),
                ComponentDialogBase.of(selection),
                confirm);

        shortcuts = List.of(
                StatusBarShortcut.navigate(),
                StatusBarShortcut.hint("Space", Bundle.message("shortcut.check")),
                StatusBarShortcut.cancel(this::closeCancel));

        size = DialogSize.HALF;

        final @NotNull DialogButton confirmButton = confirm.getComponent();
        final @NotNull Runnable refresh = () -> {
            confirmButton.tally(tally());
            confirmButton.enableUnless(whyNot());
        };

        refresh.run();
        selection.onCheckChanged(refresh);
        form.onAnswerChanged(refresh);
    }

    // Rule-TREE-PANEL-130
    private @NotNull String tally() {
        final int hidden = selection.hiddenChecked();
        if (hidden == 0)
            return Bundle.message("test.run.form.tally", selection.checkedLeaves(), selection.branchesHoldingChecked());

        return Bundle.message("test.run.form.tally.hidden", selection.checkedLeaves(), selection.branchesHoldingChecked(), hidden);
    }

    // UC-TREE-PANEL-009, Rule-TREE-PANEL-029, Rule-TREE-PANEL-121
    private @NotNull Optional<String> whyNot() {
        if (!selection.hasChecked()) return Optional.of(Bundle.message("test.run.form.no.test.case"));

        return form.unanswered();
    }

    // UC-TREE-PANEL-009, Rule-TREE-PANEL-029, Rule-TREE-PANEL-121
    @Override
    protected void submit() {
        if (whyNot().isPresent()) return;

        if (action.submit().saved(form, selection)) closeOk();
    }
}
