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

package org.testin.editor.grid;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractGridEditListener implements TableModelListener {
    protected final @NotNull Project project;

    protected final @NotNull Notifier notifier;

    private final @NotNull List<TestCaseDto> pageTestCases;

    private final @NotNull Set<UUID> writtenThisGesture = new HashSet<>();

    private boolean updating = false;

    protected AbstractGridEditListener(final @NotNull Project project, final @NotNull List<TestCaseDto> pageTestCases) {
        this.project = project;
        this.notifier = Services.getInstance(project, Notifier.class);
        this.pageTestCases = pageTestCases;
    }

    private static @NotNull String shortened(final @NotNull String value) {
        final @NotNull String oneLine = value.replace('\n', ' ').trim();

        return oneLine.length() <= 60 ? oneLine : oneLine.substring(0, 59) + "…";
    }

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-052
    @Override
    public final void tableChanged(final TableModelEvent e) {
        if (updating) return;
        if (e.getType() != TableModelEvent.UPDATE) return;

        final int row = e.getFirstRow();
        final int col = e.getColumn();
        if (row < 0 || col < 0) return;
        if (!(e.getSource() instanceof DefaultTableModel model)
                || row >= model.getRowCount()
                || row >= pageTestCases.size()
                || col >= model.getColumnCount()
                || col >= columnCount()) return;

        final @NotNull TestCaseDto edited = pageTestCases.get(row);

        updating = true;
        try {
            final @NotNull String typed = String.valueOf(model.getValueAt(row, col));

            final @NotNull GridEdit outcome = apply(model, edited, row, col);

            if (!outcome.isSaid()) sayIfRewritten(typed, String.valueOf(model.getValueAt(row, col)));

            if (!outcome.isWritten()) return;

            confirmEdit(edited);

            ViewToolWindowFactory.refreshIfShowing(project, List.of(edited));
        } finally {
            updating = false;
        }
    }

    protected abstract int columnCount();

    protected abstract @NotNull GridEdit apply(final @NotNull DefaultTableModel model, final @NotNull TestCaseDto onThisRow, final int row, final int col);

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-008
    private void confirmEdit(final @NotNull TestCaseDto edited) {
        final boolean first = writtenThisGesture.isEmpty();
        writtenThisGesture.add(edited.getId());
        if (!first) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            final int written = writtenThisGesture.size();
            writtenThisGesture.clear();

            notifier.softShowCounted(project, Done.UPDATED, written);
        });
    }

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-005
    private void sayIfRewritten(final @NotNull String typed, final @NotNull String stored) {
        if (typed.equals(stored)) return;

        notifier.softShow(project, Bundle.message("grid.adjusted.title"),
                Bundle.message("grid.adjusted.message", shortened(stored), shortened(typed)));
    }
}
