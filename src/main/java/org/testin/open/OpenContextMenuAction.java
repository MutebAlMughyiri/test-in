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

package org.testin.open;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.testin.explorer.tree.TreeValues;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Optional;
import java.util.function.Supplier;

// Rule-EDITOR-PANEL-123
public class OpenContextMenuAction extends DumbAwareAction {
    private final @NotNull JComponent owner;

    private final @NotNull DefaultActionGroup contextMenu;

    private final @NotNull Supplier<Optional<Point>> anchor;

    public OpenContextMenuAction(final @NotNull SimpleTree tree, final @NotNull DefaultActionGroup contextMenu) {
        this(tree, contextMenu, () -> selectedRow(tree));
    }

    public OpenContextMenuAction(final @NotNull JBList<?> list, final @NotNull DefaultActionGroup contextMenu) {
        this(list, contextMenu, () -> selectedCell(list));
    }

    public OpenContextMenuAction(final @NotNull JBTable table, final @NotNull DefaultActionGroup contextMenu) {
        this(table, contextMenu, () -> selectedCell(table));
    }

    private OpenContextMenuAction(final @NotNull JComponent owner, final @NotNull DefaultActionGroup contextMenu, final @NotNull Supplier<Optional<Point>> anchor) {
        super(Bundle.message("menu.show.context"));
        this.owner = owner;
        this.contextMenu = contextMenu;
        this.anchor = anchor;
        this.registerCustomShortcutSet(Shortcuts.ContextMenu.getCustomShortcut(), owner);
    }

    private static @NotNull Optional<Point> selectedRow(final @NotNull SimpleTree tree) {
        final int[] rows = TreeValues.selectedRows(tree);
        if (rows.length == 0) return Optional.empty();

        return Optional.ofNullable(tree.getRowBounds(rows[0]))
                .map(bounds -> new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2));
    }

    private static @NotNull Optional<Point> selectedCell(final @NotNull JBTable table) {
        final int row = table.getSelectedRow();
        final int column = table.getSelectedColumn();
        if (row < 0 || column < 0) return Optional.empty();

        final @NotNull Rectangle cell = table.getCellRect(row, column, true);
        return Optional.of(new Point(cell.x + cell.width / 4, cell.y + cell.height / 2));
    }

    private static @NotNull Optional<Point> selectedCell(final @NotNull JBList<?> list) {
        final int index = list.getSelectedIndex();
        if (index == -1) return Optional.empty();

        return Optional.ofNullable(list.getCellBounds(index, index))
                .map(bounds -> new Point(bounds.x + bounds.width / 4, bounds.y + bounds.height / 2));
    }

    @TestOnly
    @NotNull Optional<Point> whereItOpens() {
        return anchor.get();
    }

    // Rule-EDITOR-PANEL-124
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        anchor.get().ifPresent(at -> ActionManager.getInstance()
                .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, contextMenu)
                .getComponent()
                .show(owner, at.x, at.y));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
