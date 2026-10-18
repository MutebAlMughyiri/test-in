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
import org.testin.model.NodeType;
import org.testin.notifications.Refused;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogButton;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogSize;
import org.testin.ui.framework.SelectionTree;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;

public final class TestRunConfigurationDialog extends AbstractFrameworkDialog {
    private final @NotNull TestRunFormPurpose action;

    private final @NotNull TestRunConfigurationForm form;

    private final @NotNull SelectionTree selection;

    public TestRunConfigurationDialog(final @NotNull Project project, final @NotNull TestRunConfigurationForm form, final @NotNull TestRunFormFilter filter, final @NotNull TestRunFormPurpose action) {
        super(project);
        this.action = action;
        this.form = form;
        this.selection = filter.getSelection();

        title = action.title();

        final @NotNull DialogPart<DialogButton> confirm = DialogPart.button(action.button());
        components = List.of(
                DialogPart.of(form),
                DialogPart.of(selection),
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

    // UC-TREE-PANEL-009, UC-TREE-PANEL-021, UC-TREE-PANEL-022, Rule-TREE-PANEL-029, Rule-TREE-PANEL-121
    @Override
    protected void submit() {
        if (whyNot().isPresent() || refusesTheName()) return;

        if (action.submit().saved(form, selection)) closeOk();
    }

    // UC-TREE-PANEL-009, UC-TREE-PANEL-021, UC-TREE-PANEL-022, Rule-TREE-PANEL-095
    private boolean refusesTheName() {
        final @NotNull String name = form.getTestRunName();
        if (name.isEmpty()) {
            notifier.softRefuse(project, Bundle.message("test.run.needs.a.name"));
            return true;
        }

        final @NotNull Optional<Refused> notAName = Refused.ofName(NodeType.TEST_RUN, name);
        notAName.ifPresent(refusal -> notifier.softRefuse(project, refusal, name));
        return notAName.isPresent();
    }
}
