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
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.CheckboxTreeListener;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Bundle;
import org.testin.util.Fonts;

import javax.swing.JComponent;
import javax.swing.tree.DefaultTreeModel;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class SelectionTree implements DialogComponent {
    private final @NotNull CheckboxTree tree;

    private final @NotNull CheckedTreeNode full;

    private final @NotNull Set<Object> leaves = new LinkedHashSet<>();

    private final @NotNull Set<Object> chosen = new LinkedHashSet<>();

    private final @NotNull List<Runnable> changed = new ArrayList<>();

    private final @NotNull JComponent panel;

    private final @NotNull AbstractIconButton tickAll = AbstractIconButton.of(Bundle.message("selection.tick.all"), AllIcons.Actions.Selectall, this::flipTicks);

    public SelectionTree(final @NotNull String caption, final @NotNull CheckedTreeNode root, final @NotNull CheckboxTree.CheckboxTreeCellRenderer renderer, final @NotNull Optional<JComponent> trailing) {
        this.full = root;
        collectLeaves(root);

        tree = new CheckboxTree(renderer, copyShown(root, _ -> true), new CheckboxTreeBase.CheckPolicy(true, true, true, true));
        // Rule-INTERNAL-095
        DialogStyle.asRow(tree);
        // Rule-INTERNAL-102
        tree.setVisibleRowCount(DialogSize.VISIBLE_ROWS);
        TreeUtil.expandAll(tree);

        tree.addCheckboxTreeListener(new CheckboxTreeListener() {
            @Override
            public void nodeStateChanged(final @NotNull CheckedTreeNode treeNode) {
                keepTicks();
                tree.repaint();
                showTickAll();
                changed.forEach(Runnable::run);
            }
        });

        final @NotNull JBLabel title = Caption.of(caption, Fonts.caption());
        // Rule-INTERNAL-122
        title.setLabelFor(tree);

        final @NotNull JBPanel<?> buttons = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 0, 0)).andTransparent();
        buttons.add(tickAll);
        trailing.ifPresent(buttons::add);
        showTickAll();

        // Rule-INTERNAL-087, Rule-INTERNAL-099
        panel = DialogStyle.section(Caption.header(title, Optional.of(buttons)), ScrollPaneFactory.createScrollPane(tree, true));
    }

    private static boolean allChecked(final @NotNull CheckedTreeNode treeNode) {
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            if (!((CheckedTreeNode) treeNode.getChildAt(i)).isChecked()) return false;
        }

        return true;
    }

    // Rule-TREE-PANEL-130, Rule-TREE-PANEL-131
    public void show(final @NotNull Predicate<Object> leafShown) {
        tree.setModel(new DefaultTreeModel(copyShown(full, leafShown)));
        TreeUtil.expandAll(tree);
        showTickAll();
        changed.forEach(Runnable::run);
    }

    // Rule-TREE-PANEL-140
    private void flipTicks() {
        tree.setNodeState(shownRoot(), !everyShownTicked());
    }

    // Rule-TREE-PANEL-140, Rule-INTERNAL-137
    private void showTickAll() {
        if (everyShownTicked()) tickAll.show(Bundle.message("selection.untick.all"), AllIcons.Actions.Unselectall);
        else tickAll.show(Bundle.message("selection.tick.all"), AllIcons.Actions.Selectall);
    }

    // Rule-TREE-PANEL-140, Rule-TREE-PANEL-138
    private boolean everyShownTicked() {
        final @NotNull List<CheckedTreeNode> tickable = leafNodesOf(shownRoot()).stream().filter(CheckedTreeNode::isEnabled).toList();
        return !tickable.isEmpty() && tickable.stream().allMatch(leaf -> chosen.contains(leaf.getUserObject()));
    }

    // Rule-TREE-PANEL-130
    public void forEachChecked(final @NotNull Consumer<Object> visitor) {
        leavesOf(full).stream().filter(chosen::contains).forEach(visitor);
    }

    public void forEachLeaf(final @NotNull Consumer<Object> visitor) {
        leavesOf(full).forEach(visitor);
    }

    public int checkedLeaves() {
        return chosen.size();
    }

    // Rule-TREE-PANEL-130
    public int hiddenChecked() {
        return chosen.size() - (int) leavesOf(shownRoot()).stream().filter(chosen::contains).count();
    }

    public int branchesHoldingChecked() {
        return countBranches(full);
    }

    public boolean hasChecked() {
        return !chosen.isEmpty();
    }

    public void onCheckChanged(final @NotNull Runnable listener) {
        changed.add(listener);
    }

    private @NotNull CheckedTreeNode shownRoot() {
        return (CheckedTreeNode) tree.getModel().getRoot();
    }

    // Rule-TREE-PANEL-131, Rule-TREE-PANEL-138, Rule-TREE-PANEL-139
    private void keepTicks() {
        for (final CheckedTreeNode leaf : leafNodesOf(shownRoot())) {
            if (!leaf.isEnabled()) leaf.setChecked(chosen.contains(leaf.getUserObject()));
            else if (leaf.isChecked()) chosen.add(leaf.getUserObject());
            else chosen.remove(leaf.getUserObject());
        }
    }

    private @NotNull CheckedTreeNode copyShown(final @NotNull CheckedTreeNode treeNode, final @NotNull Predicate<Object> leafShown) {
        final @NotNull CheckedTreeNode copy = new CheckedTreeNode(treeNode.getUserObject());
        copy.setEnabled(treeNode.isEnabled());

        if (isLeaf(treeNode)) {
            copy.setChecked(chosen.contains(treeNode.getUserObject()));
            return copy;
        }

        for (int i = 0; i < treeNode.getChildCount(); i++) {
            final @NotNull CheckedTreeNode child = (CheckedTreeNode) treeNode.getChildAt(i);
            if (holdsShown(child, leafShown)) copy.add(copyShown(child, leafShown));
        }

        copy.setChecked(copy.getChildCount() > 0 && allChecked(copy));
        return copy;
    }

    private boolean holdsShown(final @NotNull CheckedTreeNode treeNode, final @NotNull Predicate<Object> leafShown) {
        if (isLeaf(treeNode)) return leafShown.test(treeNode.getUserObject());

        for (int i = 0; i < treeNode.getChildCount(); i++) {
            if (holdsShown((CheckedTreeNode) treeNode.getChildAt(i), leafShown)) return true;
        }

        return false;
    }

    private boolean isLeaf(final @NotNull CheckedTreeNode treeNode) {
        return leaves.contains(treeNode.getUserObject());
    }

    private int countBranches(final @NotNull CheckedTreeNode treeNode) {
        if (isLeaf(treeNode)) return 0;

        int branches = hasChosenLeaf(treeNode) && holdsALeaf(treeNode) ? 1 : 0;
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            branches += countBranches((CheckedTreeNode) treeNode.getChildAt(i));
        }

        return branches;
    }

    private boolean holdsALeaf(final @NotNull CheckedTreeNode treeNode) {
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            if (isLeaf((CheckedTreeNode) treeNode.getChildAt(i))) return true;
        }

        return false;
    }

    private boolean hasChosenLeaf(final @NotNull CheckedTreeNode treeNode) {
        if (isLeaf(treeNode)) return chosen.contains(treeNode.getUserObject());

        for (int i = 0; i < treeNode.getChildCount(); i++) {
            if (hasChosenLeaf((CheckedTreeNode) treeNode.getChildAt(i))) return true;
        }

        return false;
    }

    private void collectLeaves(final @NotNull CheckedTreeNode treeNode) {
        if (treeNode.isLeaf()) {
            leaves.add(treeNode.getUserObject());
            if (treeNode.isChecked()) chosen.add(treeNode.getUserObject());
        }

        for (int i = 0; i < treeNode.getChildCount(); i++) {
            collectLeaves((CheckedTreeNode) treeNode.getChildAt(i));
        }
    }

    private @NotNull List<Object> leavesOf(final @NotNull CheckedTreeNode treeNode) {
        return leafNodesOf(treeNode).stream().map(CheckedTreeNode::getUserObject).toList();
    }

    private @NotNull List<CheckedTreeNode> leafNodesOf(final @NotNull CheckedTreeNode treeNode) {
        final @NotNull List<CheckedTreeNode> found = new ArrayList<>();
        if (isLeaf(treeNode)) found.add(treeNode);

        for (int i = 0; i < treeNode.getChildCount(); i++) {
            found.addAll(leafNodesOf((CheckedTreeNode) treeNode.getChildAt(i)));
        }

        return found;
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return tree;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }
}
