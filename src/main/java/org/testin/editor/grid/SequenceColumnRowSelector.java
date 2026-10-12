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

import com.intellij.ui.table.JBTable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class SequenceColumnRowSelector extends MouseAdapter {
    private final @NotNull JBTable table;

    // UC-EDITOR-PANEL-024, Rule-EDITOR-PANEL-108
    @Override
    public void mousePressed(final MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e) || e.isPopupTrigger()) return;

        final int viewRow = table.rowAtPoint(e.getPoint());
        final int viewCol = table.columnAtPoint(e.getPoint());
        if (viewRow < 0 || viewCol < 0) return;
        if (!GridTables.isSequenceColumn(table, viewCol)) return;

        final @NotNull ListSelectionModel rows = table.getSelectionModel();

        if (e.isShiftDown()) {
            final int anchor = rows.getAnchorSelectionIndex();
            rows.setSelectionInterval(anchor < 0 ? viewRow : anchor, viewRow);
        } else if (e.isControlDown() || e.isMetaDown()) {
            if (rows.isSelectedIndex(viewRow)) {
                rows.removeSelectionInterval(viewRow, viewRow);
            } else {
                rows.addSelectionInterval(viewRow, viewRow);
            }
        } else {
            rows.setSelectionInterval(viewRow, viewRow);
        }

        table.setColumnSelectionInterval(0, table.getColumnCount() - 1);

        table.getColumnModel().getSelectionModel().setAnchorSelectionIndex(viewCol);

        table.requestFocusInWindow();

        e.consume();
    }
}
