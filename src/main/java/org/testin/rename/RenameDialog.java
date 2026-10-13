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

package org.testin.rename;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeType;
import org.testin.model.node.AbstractNode;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

final class RenameDialog extends AbstractFrameworkDialog {
    private final @NotNull TextInput nameInput;

    private final @NotNull Consumer<@NotNull String> onSubmit;

    private final @NotNull NodeType type;

    // UC-TREE-PANEL-011, Rule-INTERNAL-099, Rule-INTERNAL-105, Rule-INTERNAL-108, Rule-TREE-PANEL-123
    RenameDialog(final @NotNull Project project, final @NotNull AbstractNode dir, final @NotNull Consumer<@NotNull String> onSubmit) {
        super(project);
        this.onSubmit = onSubmit;
        this.type = dir.getType();

        title = Bundle.message("dialog.rename.title");

        final @NotNull DialogPart<TextInput> built = DialogPart.textInput()
                .icon(AllIcons.Actions.Edit)
                .placeholder(Bundle.message("dialog.rename.placeholder"))
                .value(dir.getName())
                .build();
        nameInput = built.getComponent();

        final @NotNull List<String> place = Services.getInstance(project, TestinRoot.class).place(Objects.toString(dir.getPath().getParent(), ""));
        components = List.of(
                DialogPart.of(new RenameCard(dir, place, nameInput)),
                DialogPart.button(Bundle.message("dialog.rename.confirm")));

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, Bundle.message("dialog.rename.confirm"), this::submit),
                StatusBarShortcut.cancel(this::closeCancel));
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-005, Rule-TREE-PANEL-095
    @Override
    protected void submit() {
        final @NotNull String value = accepted(nameInput, typed -> Refused.ofName(type, typed));
        if (value.isEmpty()) return;

        onSubmit.accept(value);
        closeOk();
    }
}
