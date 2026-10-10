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

import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.CheckboxTreeListener;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Fonts;

import javax.swing.JComponent;
import javax.swing.tree.DefaultTreeModel;
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
            public void nodeStateChanged(final @NotNull CheckedTreeNode node) {
                keepTicks();
                changed.forEach(Runnable::run);
            }
        });

        final @NotNull JBLabel title = Caption.of(caption, Fonts.caption());
        // Rule-INTERNAL-122
        title.setLabelFor(tree);

        // Rule-INTERNAL-087, Rule-INTERNAL-099
        panel = DialogStyle.section(Caption.header(title, trailing), ScrollPaneFactory.createScrollPane(tree, true));
    }

    private static boolean allChecked(final @NotNull CheckedTreeNode node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            if (!((CheckedTreeNode) node.getChildAt(i)).isChecked()) return false;
        }

        return true;
    }

    // Rule-TREE-PANEL-130, Rule-TREE-PANEL-131
    public void show(final @NotNull Predicate<Object> leafShown) {
        tree.setModel(new DefaultTreeModel(copyShown(full, leafShown)));
        TreeUtil.expandAll(tree);
        changed.forEach(Runnable::run);
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

    // Rule-TREE-PANEL-131
    private void keepTicks() {
        for (final CheckedTreeNode leaf : leafNodesOf(shownRoot())) {
            if (leaf.isChecked()) chosen.add(leaf.getUserObject());
            else chosen.remove(leaf.getUserObject());
        }
    }

    private @NotNull CheckedTreeNode copyShown(final @NotNull CheckedTreeNode node, final @NotNull Predicate<Object> leafShown) {
        final @NotNull CheckedTreeNode copy = new CheckedTreeNode(node.getUserObject());

        if (isLeaf(node)) {
            copy.setChecked(chosen.contains(node.getUserObject()));
            return copy;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            final @NotNull CheckedTreeNode child = (CheckedTreeNode) node.getChildAt(i);
            if (holdsShown(child, leafShown)) copy.add(copyShown(child, leafShown));
        }

        copy.setChecked(copy.getChildCount() > 0 && allChecked(copy));
        return copy;
    }

    private boolean holdsShown(final @NotNull CheckedTreeNode node, final @NotNull Predicate<Object> leafShown) {
        if (isLeaf(node)) return leafShown.test(node.getUserObject());

        for (int i = 0; i < node.getChildCount(); i++) {
            if (holdsShown((CheckedTreeNode) node.getChildAt(i), leafShown)) return true;
        }

        return false;
    }

    private boolean isLeaf(final @NotNull CheckedTreeNode node) {
        return leaves.contains(node.getUserObject());
    }

    private int countBranches(final @NotNull CheckedTreeNode node) {
        if (isLeaf(node)) return 0;

        int branches = hasChosenLeaf(node) && holdsALeaf(node) ? 1 : 0;
        for (int i = 0; i < node.getChildCount(); i++) {
            branches += countBranches((CheckedTreeNode) node.getChildAt(i));
        }

        return branches;
    }

    private boolean holdsALeaf(final @NotNull CheckedTreeNode node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            if (isLeaf((CheckedTreeNode) node.getChildAt(i))) return true;
        }

        return false;
    }

    private boolean hasChosenLeaf(final @NotNull CheckedTreeNode node) {
        if (isLeaf(node)) return chosen.contains(node.getUserObject());

        for (int i = 0; i < node.getChildCount(); i++) {
            if (hasChosenLeaf((CheckedTreeNode) node.getChildAt(i))) return true;
        }

        return false;
    }

    private void collectLeaves(final @NotNull CheckedTreeNode node) {
        if (node.isLeaf()) {
            leaves.add(node.getUserObject());
            if (node.isChecked()) chosen.add(node.getUserObject());
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectLeaves((CheckedTreeNode) node.getChildAt(i));
        }
    }

    private @NotNull List<Object> leavesOf(final @NotNull CheckedTreeNode node) {
        return leafNodesOf(node).stream().map(CheckedTreeNode::getUserObject).toList();
    }

    private @NotNull List<CheckedTreeNode> leafNodesOf(final @NotNull CheckedTreeNode node) {
        final @NotNull List<CheckedTreeNode> found = new ArrayList<>();
        if (isLeaf(node)) found.add(node);

        for (int i = 0; i < node.getChildCount(); i++) {
            found.addAll(leafNodesOf((CheckedTreeNode) node.getChildAt(i)));
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
