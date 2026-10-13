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

package org.testin.testproject;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.help.Guide;
import org.testin.help.Guides;
import org.testin.model.NodeType;
import org.testin.model.status.ProjectStatus;
import org.testin.notifications.Done;
import org.testin.services.Services;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.SelectionTable;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.List;
import java.util.Map;

// Rule-TREE-PANEL-106
public final class BindTestProjectDialog extends AbstractFrameworkDialog {
    private final @NotNull SelectionTable projects;
    private final @NotNull Runnable onBound;
    private final @NotNull BoundTestProject boundTestProject = Services.getInstance(project, BoundTestProject.class);

    public BindTestProjectDialog(final @NotNull Project project, final @NotNull Map<String, ProjectStatus> underRoot, final @NotNull Runnable onBound) {
        super(project);
        this.onBound = onBound;
        Services.getInstance(project, Guides.class).add(Guide.LINK_THIS_REPOSITORY);

        title = Bundle.message("dialog.bind.title");

        final @NotNull DialogPart<SelectionTable> table = DialogPart.table()
                .column(NodeType.TP.getDescription(), 260)
                .column(Bundle.message("caption.status"), 100)
                .build();

        components = List.of(table);

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, StatusBarShortcut.SELECT, this::submit),
                StatusBarShortcut.cancel(this::closeCancel));

        projects = table.getComponent();

        for (final Map.Entry<String, ProjectStatus> entry : underRoot.entrySet()) {
            projects.addRow(entry.getKey(), entry.getValue().getLabel());
        }

        selectCurrent();
    }

    private void selectCurrent() {
        final @NotNull String bound = boundTestProject.name();

        for (int row = 0; row < projects.getRowCount(); row++) {
            if (projects.getValueAt(row, 0).equals(bound)) {
                projects.selectRows(List.of(row));
                return;
            }
        }
    }

    // UC-TREE-PANEL-004, Rule-TREE-PANEL-106
    @Override
    protected void submit() {
        final @NotNull List<Integer> selected = projects.getSelectedRows();
        if (selected.isEmpty()) return;

        boundTestProject.choose(projects.getValueAt(selected.getFirst(), 0));
        closeOk();

        notifier.softShow(project, Done.BOUND);
        onBound.run();
    }
}
