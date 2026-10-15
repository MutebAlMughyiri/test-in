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
import com.intellij.ui.CheckedTreeNode;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SelectionTreeIdeTest extends AbstractIdeTest {

    private static final @NotNull String LOGIN_VALID = "Log in with a valid user";

    private static final @NotNull String LOGIN_LOCKED = "Log in with a locked user";

    private static final @NotNull String REFUND = "Refund a paid order";

    private static final @NotNull Set<String> IN_LOGIN = Set.of(LOGIN_VALID, LOGIN_LOCKED);

    private static @NotNull CheckedTreeNode aTestCase(final @NotNull String name, final boolean ticked) {
        final @NotNull CheckedTreeNode leaf = new CheckedTreeNode(name);
        leaf.setChecked(ticked);
        return leaf;
    }

    private static @NotNull CheckedTreeNode aTestSet(final @NotNull String name, final @NotNull CheckedTreeNode... testCases) {
        final @NotNull CheckedTreeNode branch = new CheckedTreeNode(name);
        for (final CheckedTreeNode testCase : testCases) branch.add(testCase);
        return branch;
    }

    private static @NotNull SelectionTree selection(final boolean refundTicked) {
        final @NotNull CheckedTreeNode root = new CheckedTreeNode("Test Cases");
        root.add(aTestSet("Login", aTestCase(LOGIN_VALID, false), aTestCase(LOGIN_LOCKED, false)));
        root.add(aTestSet("Payments", aTestCase(REFUND, refundTicked)));

        return new SelectionTree("Test cases", root, new CheckboxTree.CheckboxTreeCellRenderer() {
        }, Optional.empty());
    }

    private static @NotNull List<Object> ticked(final @NotNull SelectionTree selection) {
        final @NotNull List<Object> ticked = new ArrayList<>();
        selection.forEachChecked(ticked::add);
        return ticked;
    }

    private static boolean inLogin(final @NotNull Object leaf) {
        return leaf instanceof final String name && IN_LOGIN.contains(name);
    }

    private static @NotNull CheckboxTree drawnTree(final @NotNull SelectionTree selection) {
        return Drawn.components(selection.getPanel()).stream()
                .filter(CheckboxTree.class::isInstance)
                .map(CheckboxTree.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the selection draws no tree"));
    }

    // Rule-TREE-PANEL-130
    public void testAFilterHidesATickedTestCaseWithoutUnticking() {
        final @NotNull SelectionTree selection = selection(true);

        selection.show(SelectionTreeIdeTest::inLogin);

        assertEquals("a ticked test case the filter hides left the test run", List.of(REFUND), ticked(selection));
        assertEquals("the hidden ticked test case is not counted as hidden", 1, selection.hiddenChecked());
    }

    private static @NotNull CheckedTreeNode disabled(final @NotNull CheckedTreeNode leaf) {
        leaf.setEnabled(false);
        return leaf;
    }

    private static @NotNull SelectionTree withADisabledTestCase(final boolean ticked) {
        final @NotNull CheckedTreeNode root = new CheckedTreeNode("Test Cases");
        root.add(aTestSet("Login", aTestCase(LOGIN_VALID, false), disabled(aTestCase(LOGIN_LOCKED, ticked))));

        return new SelectionTree("Test cases", root, new CheckboxTree.CheckboxTreeCellRenderer() {
        }, Optional.empty());
    }

    private static @NotNull AbstractIconButton tickAll(final @NotNull SelectionTree selection) {
        return Drawn.components(selection.getPanel()).stream()
                .filter(AbstractIconButton.class::isInstance)
                .map(AbstractIconButton.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the selection offers no select all"));
    }

    // Rule-TREE-PANEL-140, Rule-TREE-PANEL-131
    public void testSelectAllTicksOnlyWhatTheFilterShowsAndThenUnticksIt() {
        final @NotNull SelectionTree selection = selection(true);
        selection.show(SelectionTreeIdeTest::inLogin);

        tickAll(selection).doClick();
        assertEquals("select all ticked what the filter hides, or missed what it shows", List.of(LOGIN_VALID, LOGIN_LOCKED, REFUND), ticked(selection));

        tickAll(selection).doClick();
        assertEquals("unselect all touched a test case the filter hides", List.of(REFUND), ticked(selection));
    }

    // Rule-TREE-PANEL-140, Rule-TREE-PANEL-138
    public void testSelectAllLeavesADisabledTestCaseAsItWas() {
        final @NotNull SelectionTree selection = withADisabledTestCase(false);

        tickAll(selection).doClick();
        assertEquals("select all ticked a disabled test case", List.of(LOGIN_VALID), ticked(selection));
        assertEquals("with every test case it can tick ticked, the button does not offer to untick", Bundle.message("selection.untick.all"), tickAll(selection).getAccessibleContext().getAccessibleName());
    }

    // Rule-TREE-PANEL-138
    public void testTickingATestSetLeavesItsDisabledTestCaseOut() {
        final @NotNull SelectionTree selection = withADisabledTestCase(false);

        final @NotNull CheckboxTree tree = drawnTree(selection);
        tree.setNodeState((CheckedTreeNode) tree.getModel().getRoot(), true);

        assertEquals("a disabled test case was ticked with its test set", List.of(LOGIN_VALID), ticked(selection));
    }

    // Rule-TREE-PANEL-139
    public void testADisabledTestCaseTheTestRunCoversStaysTicked() {
        final @NotNull SelectionTree selection = withADisabledTestCase(true);

        final @NotNull CheckboxTree tree = drawnTree(selection);
        tree.setNodeState((CheckedTreeNode) tree.getModel().getRoot(), false);

        assertEquals("unticking the test set removed a disabled test case the test run covers", List.of(LOGIN_LOCKED), ticked(selection));
    }

    // Rule-TREE-PANEL-131
    public void testTickingARowUnderAFilterTicksOnlyTheTestCasesItShows() {
        final @NotNull SelectionTree selection = selection(false);
        selection.show(SelectionTreeIdeTest::inLogin);

        final @NotNull CheckboxTree tree = drawnTree(selection);
        tree.setNodeState((CheckedTreeNode) tree.getModel().getRoot(), true);

        assertEquals("ticking a row under a filter ticked a test case the filter hides", List.of(LOGIN_VALID, LOGIN_LOCKED), ticked(selection));
    }
}
