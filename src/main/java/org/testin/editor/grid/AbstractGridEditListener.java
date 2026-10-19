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
import org.testin.model.TestCase;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractGridEditListener implements TableModelListener {
    protected final @NotNull Project project;

    protected final @NotNull Notifier notifier;

    private final @NotNull List<TestCase> pageTestCases;

    private final @NotNull Set<UUID> writtenThisGesture = new HashSet<>();

    private final @NotNull List<String> refusedThisGesture = new ArrayList<>();

    private final @NotNull List<String> adjustedThisGesture = new ArrayList<>();

    private boolean gestureOpen = false;

    private boolean updating = false;

    protected AbstractGridEditListener(final @NotNull Project project, final @NotNull List<TestCase> pageTestCases) {
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
        final int column = e.getColumn();
        if (row < 0 || column < 0) return;
        if (!(e.getSource() instanceof DefaultTableModel model)
                || row >= model.getRowCount()
                || row >= pageTestCases.size()
                || column >= model.getColumnCount()
                || column >= columnCount()) return;

        final @NotNull TestCase edited = pageTestCases.get(row);

        updating = true;
        try {
            final @NotNull String typed = String.valueOf(model.getValueAt(row, column));

            final @NotNull GridEdit outcome = apply(model, edited, row, column);

            if (!outcome.isSaid()) sayIfRewritten(typed, String.valueOf(model.getValueAt(row, column)));

            if (!outcome.isWritten()) return;

            if (outcome.isConfirmedNow()) {
                writtenThisGesture.add(edited.getUuid());
                sayAtTheEndOfTheGesture();
            }

            ViewToolWindowFactory.refreshIfShowing(project, List.of(edited));
        } finally {
            updating = false;
        }
    }

    protected abstract int columnCount();

    protected abstract @NotNull GridEdit apply(final @NotNull DefaultTableModel model, final @NotNull TestCase onThisRow, final int row, final int column);

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-008, Rule-EDITOR-PANEL-226
    protected final void confirmWritten(final int written) {
        notifier.softShowCounted(project, Done.UPDATED, written);
    }

    // Rule-EDITOR-PANEL-226
    protected final void refuse(final @NotNull String sentence) {
        refusedThisGesture.add(sentence);
        sayAtTheEndOfTheGesture();
    }

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-005, Rule-EDITOR-PANEL-051
    private void sayIfRewritten(final @NotNull String typed, final @NotNull String stored) {
        if (typed.equals(stored)) return;

        adjustedThisGesture.add(Bundle.message("grid.adjusted.message", shortened(stored), shortened(typed)));
        sayAtTheEndOfTheGesture();
    }

    private void sayAtTheEndOfTheGesture() {
        if (gestureOpen) return;

        gestureOpen = true;
        ApplicationManager.getApplication().invokeLater(this::sayTheGesture);
    }

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-008, Rule-EDITOR-PANEL-226
    private void sayTheGesture() {
        gestureOpen = false;

        confirmWritten(writtenThisGesture.size());
        writtenThisGesture.clear();

        if (refusedThisGesture.size() == 1) notifier.softRefuse(project, refusedThisGesture.getFirst());
        if (refusedThisGesture.size() > 1)
            notifier.softRefuse(project, Done.counted(Bundle.message("grid.refused.title"), refusedThisGesture.size()), refusedThisGesture.getFirst());
        refusedThisGesture.clear();

        if (!adjustedThisGesture.isEmpty())
            notifier.softShow(project, Done.counted(Bundle.message("grid.adjusted.title"), adjustedThisGesture.size()), adjustedThisGesture.getFirst());
        adjustedThisGesture.clear();
    }
}
