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

package org.testin.creator.dialogs;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.git.GitRefs;
import org.testin.model.NodeType;
import org.testin.notifications.Refused;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;

import java.util.List;
import java.util.function.Consumer;

public final class CreateProjectDialog extends AbstractFrameworkDialog {
    private final @NotNull TextInput nameInput;

    private final @NotNull Consumer<@NotNull String> onCreate;

    // UC-TREE-PANEL-002, UC-TREE-PANEL-003
    public CreateProjectDialog(final @NotNull Project p, final @NotNull Consumer<@NotNull String> onCreate) {
        super(p);
        this.onCreate = onCreate;

        title = Bundle.message("dialog.create.project.title");

        final @NotNull ComponentDialogBase<TextInput> built = ComponentDialogBase.textField()
                .icon(NodeType.TP.getIcon())
                .placeholder(Bundle.message("dialog.create.project.placeholder"))
                .build();
        nameInput = built.getComponent();
        components = List.of(built);

        shortcuts = List.of(
                StatusBarShortcut.confirm(this::submit),
                StatusBarShortcut.cancel(this::closeCancel));
    }

    // UC-TREE-PANEL-002, UC-TREE-PANEL-003, Rule-TREE-PANEL-095, Rule-TREE-PANEL-107
    private static boolean isNameOrUrl(final @NotNull String typed) {
        return GitRefs.isRepositoryUrl(typed) || NodeType.TP.canTakeName(typed);
    }

    // UC-TREE-PANEL-002, Rule-TREE-PANEL-005, Rule-TREE-PANEL-095
    @Override
    protected void submit() {
        final @NotNull String name = accepted(nameInput, CreateProjectDialog::isNameOrUrl, Refused.NOT_A_JAVA_NAME);
        if (name.isEmpty()) return;

        onCreate.accept(name);
        closeOk();
    }
}
