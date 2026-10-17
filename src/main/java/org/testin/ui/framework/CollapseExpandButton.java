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

package org.testin.ui.framework;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.List;

public final class CollapseExpandButton extends AbstractIconButton {
    private final @NotNull JTree tree;

    private @NotNull List<TreePath> folders = List.of();

    // Rule-TREE-PANEL-145
    public CollapseExpandButton(final @NotNull JTree tree) {
        super(Bundle.message("toolbar.collapse.all"), AllIcons.Actions.Collapseall);
        this.tree = tree;
        addActionListener(_ -> flip());

        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(final @NotNull TreeExpansionEvent event) {
                updateState();
            }

            @Override
            public void treeCollapsed(final @NotNull TreeExpansionEvent event) {
                updateState();
            }
        });
        tree.addPropertyChangeListener(JTree.TREE_MODEL_PROPERTY, _ -> listFolders());
        listFolders();
    }

    // Rule-TREE-PANEL-145
    private void flip() {
        if (everyFolderOpen()) TreeUtil.collapseAll(tree, true, 0);
        else TreeUtil.expandAll(tree);
    }

    // Rule-TREE-PANEL-145, Rule-INTERNAL-137
    private void updateState() {
        if (everyFolderOpen()) show(Bundle.message("toolbar.collapse.all"), AllIcons.Actions.Collapseall);
        else show(Bundle.message("toolbar.expand.all"), AllIcons.Actions.Expandall);
    }

    // Rule-TREE-PANEL-145
    private boolean everyFolderOpen() {
        return folders.stream().allMatch(tree::isExpanded);
    }

    private void listFolders() {
        folders = TreeUtil.treeNodeTraverser((TreeNode) tree.getModel().getRoot()).preOrderDfsTraversal().filter(node -> !node.isLeaf()).map(TreeUtil::getPathFromRoot).toList();
        updateState();
    }
}
