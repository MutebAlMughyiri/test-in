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

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.AbstractEditorContextMenu;
import org.testin.model.TestCase;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class GridContextMenuListener extends MouseAdapter {
    private final @NotNull JBTable table;

    private final @NotNull JBList<TestCase> list;

    private final @NotNull AbstractEditorContextMenu contextMenu;

    private final @NotNull List<TestCase> pageTestCases;

    private boolean popupShownOnPress;

    public GridContextMenuListener(final @NotNull JBTable table, final @NotNull JBList<TestCase> list, final @NotNull AbstractEditorContextMenu contextMenu, final @NotNull List<TestCase> pageTestCases) {
        this.table = table;
        this.list = list;
        this.contextMenu = contextMenu;
        this.pageTestCases = pageTestCases;
    }

    // UC-EDITOR-PANEL-024, Rule-EDITOR-PANEL-110
    @Override
    public void mousePressed(final MouseEvent e) {
        popupShownOnPress = e.isPopupTrigger();
        showPopupIfRequested(e);
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        if (e.isPopupTrigger() && !popupShownOnPress) {
            showPopupIfRequested(e);
        }
        popupShownOnPress = false;
    }

    private void showPopupIfRequested(final @NotNull MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        final int row = table.rowAtPoint(e.getPoint());
        final int column = table.columnAtPoint(e.getPoint());
        if (row < 0 || row >= pageTestCases.size() || column < 0) return;

        if (!table.isRowSelected(row)) {
            table.changeSelection(row, column, false, false);
            list.setSelectedIndex(row);
        }

        ActionManager.getInstance()
                .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, contextMenu)
                .getComponent().show(e.getComponent(), e.getX(), e.getY());
    }
}
