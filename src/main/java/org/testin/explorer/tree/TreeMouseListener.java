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

package org.testin.explorer.tree;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.project.Project;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.open.OpenAction;

import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.Optional;

@AllArgsConstructor
public class TreeMouseListener extends PopupHandler {
    private final @NotNull Project project;
    private final @NotNull SimpleTree tree;
    private final @NotNull TreeContextMenu treeContextMenu;

    @Override
    public void invokePopup(final @NotNull Component comp, final int x, final int y) {
        nodeAt(x, y).ifPresent(selPath -> {
            if (!tree.getSelectionModel().isPathSelected(selPath)) {
                tree.setSelectionPath(selPath);
            }

            final @NotNull ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, treeContextMenu);
            popupMenu.getComponent().show(comp, x, y);
        });
    }

    // UC-TREE-PANEL-005, UC-TREE-PANEL-006
    @Override
    public void mouseClicked(final @NotNull MouseEvent e) {
        if (nodeAt(e.getX(), e.getY()).isEmpty()) return;

        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
            OpenAction.execute(project, TreeValues.selectedNodes(tree.getSelectionPaths()));
            e.consume();
        }
    }

    private @NotNull Optional<TreePath> nodeAt(final int x, final int y) {
        final int row = tree.getClosestRowForLocation(x, y);
        if (row < 0) return Optional.empty();

        return Optional.ofNullable(tree.getRowBounds(row))
                .filter(bounds -> y >= bounds.y && y < bounds.y + bounds.height)
                .map(_ -> tree.getPathForRow(row))
                .filter(path -> TreeValues.directoryAt(path).isPresent());
    }
}
