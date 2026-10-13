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
import org.testin.model.NodeType;
import org.testin.notifications.Refused;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextFieldWithSelections;
import org.testin.util.Bundle;

import java.util.List;
import java.util.function.BiConsumer;

public final class CreateTestRunDialog extends AbstractFrameworkDialog {
    private final @NotNull TextFieldWithSelections<NodeType> nameAndType;

    private final @NotNull BiConsumer<@NotNull String, @NotNull NodeType> onCreate;

    // UC-TREE-PANEL-009, UC-TREE-PANEL-010, Rule-TREE-PANEL-032
    public CreateTestRunDialog(final @NotNull Project project, final @NotNull BiConsumer<@NotNull String, @NotNull NodeType> onCreate) {
        super(project);
        this.onCreate = onCreate;

        title = Bundle.message("dialog.create.test.run.title");

        final @NotNull DialogPart<TextFieldWithSelections<NodeType>> built = DialogPart.<NodeType>textFieldWithSelections()
                .icon(NodeType.TR.getIcon())
                .placeholder(Bundle.message("dialog.create.test.run.placeholder"))
                .selection(NodeType.TR.getIcon(), NodeType.TR.getDescription(), Bundle.message("dialog.create.test.run.hint.tr"), NodeType.TR)
                .selection(NodeType.TRP.getIcon(), NodeType.TRP.getDescription(), Bundle.message("dialog.create.test.run.hint.trp"), NodeType.TRP)
                .build();
        nameAndType = built.getComponent();
        components = List.of(built);

        shortcuts = List.of(
                StatusBarShortcut.confirm(this::submit),
                StatusBarShortcut.select(),
                StatusBarShortcut.cancel(this::closeCancel));
    }

    // UC-TREE-PANEL-009, UC-TREE-PANEL-010, Rule-TREE-PANEL-005, Rule-TREE-PANEL-095
    @Override
    protected void submit() {
        final @NotNull NodeType type = nameAndType.getSelectedValue();

        final @NotNull String name = accepted(nameAndType, value -> Refused.ofName(type, value));
        if (name.isEmpty()) return;

        onCreate.accept(name, type);
        closeOk();
    }
}
