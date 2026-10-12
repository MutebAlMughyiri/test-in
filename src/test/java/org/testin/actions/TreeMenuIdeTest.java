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

package org.testin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.testin.creator.CreateTreeNodeAction;
import org.testin.explorer.tree.UpdateStatusAction;
import org.testin.model.NodeType;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestCasesFolderNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.model.node.TestRunsFolderNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.model.status.TestRunStatus;
import org.testin.model.status.TestSetStatus;
import org.testin.open.OpenAction;
import org.testin.order.OrderNodeAction;
import org.testin.remove.RemoveAction;
import org.testin.rename.RenameAction;
import org.testin.testrun.EditTestRunAction;
import org.testin.testrun.ReCreateTestRunAction;

import java.util.List;
import java.util.Objects;

public class TreeMenuIdeTest extends BasePlatformTestCase {

    private static @NotNull List<AbstractNode> everyKind() {
        return List.of(new TestProjectNode(), new TestCasesFolderNode(), new TestRunsFolderNode(),
                new TestSetPackageNode(), new TestRunPackageNode(), new TestSetNode(), aTestRunIn(TestRunStatus.CREATED));
    }

    private static @NotNull TestRunNode aTestRunIn(final @NotNull TestRunStatus status) {
        final @NotNull TestRunNode testRun = new TestRunNode();
        testRun.getMarker().changeStatus(status);
        return testRun;
    }

    private static @NotNull String kindOf(final @NotNull AbstractNode node) {
        return node.getType().getDescription();
    }

    private static @NotNull String said(final @NotNull Presentation shown) {
        return Objects.requireNonNullElse(shown.getDescription(), "");
    }

    private @NotNull Presentation updated(final @NotNull AnAction action, final @NotNull List<AbstractNode> selected) {
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action, SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, getProject())
                .add(TestinDataKeys.SELECTED_NODES, selected)
                .build());
        ActionUtil.updateAction(action, e);
        return e.getPresentation();
    }

    private @NotNull Presentation updated(final @NotNull AnAction action, final @NotNull AbstractNode selected) {
        return updated(action, List.of(selected));
    }

    private void assertGrayWithAReason(final @NotNull String entry, final @NotNull Presentation shown) {
        assertFalse(entry + " is not gray", shown.isEnabled());
        assertFalse(entry + " is gray and does not say why", said(shown).isBlank());
    }

    // Rule-TREE-PANEL-058
    public void testTheTestProjectAndTheTwoContainersCannotBeOrdered() {
        for (final AbstractNode node : everyKind()) {
            final boolean fixed = node instanceof TestProjectNode || node instanceof TestCasesFolderNode || node instanceof TestRunsFolderNode;

            assertEquals("Order on " + kindOf(node), !fixed, updated(new OrderNodeAction(), node).isEnabled());
        }
    }

    // Rule-TREE-PANEL-069
    public void testReCreateWorksOnATestRunInEveryStatus() {
        for (final TestRunStatus status : TestRunStatus.values()) {
            assertTrue("Re-create is gray on a " + status + " test run", updated(new ReCreateTestRunAction(), aTestRunIn(status)).isEnabled());
        }
    }

    // Rule-TREE-PANEL-073
    public void testASignedOffTestRunCannotBeEdited() {
        for (final TestRunStatus status : TestRunStatus.values()) {
            final @NotNull Presentation shown = updated(new EditTestRunAction(), aTestRunIn(status));

            if (status.isTerminal()) assertGrayWithAReason("Edit on a " + status + " test run", shown);
            else assertTrue("Edit is gray on a " + status + " test run", shown.isEnabled());
        }
    }

    // Rule-TREE-PANEL-096, Rule-TREE-PANEL-025
    public void testCreateIsGrayOnANodeThatCannotHoldOneAndNamesTheKindsThatCan() {
        for (final AbstractNode node : everyKind()) {
            final @NotNull Presentation shown = updated(new CreateTreeNodeAction(), node);
            final boolean holdsNewNodes = node instanceof TestCasesFolderNode || node instanceof TestRunsFolderNode
                    || node instanceof TestSetPackageNode || node instanceof TestRunPackageNode;

            assertEquals("Create Testin Node on " + kindOf(node), holdsNewNodes, shown.isEnabled());
            if (holdsNewNodes) continue;

            assertTrue("Create Testin Node on " + kindOf(node) + " does not name " + NodeType.TSP.getDescription() + ": " + said(shown),
                    said(shown).contains(NodeType.TSP.getDescription()));
            assertTrue("Create Testin Node on " + kindOf(node) + " does not name " + NodeType.TRP.getDescription() + ": " + said(shown),
                    said(shown).contains(NodeType.TRP.getDescription()));
        }
    }

    // Rule-TREE-PANEL-022
    public void testOnlyATestSetAndATestRunOffersToOpen() {
        for (final AbstractNode node : everyKind()) {
            final boolean opens = node instanceof TestSetNode || node instanceof TestRunNode;

            assertEquals("Open on " + kindOf(node), opens, updated(new OpenAction(), node).isEnabled());
        }
    }

    // Rule-TREE-PANEL-042
    public void testTheTwoContainersAreNeverOfferedForRemoval() {
        for (final AbstractNode container : List.of(new TestCasesFolderNode(), new TestRunsFolderNode())) {
            assertGrayWithAReason("Remove on " + kindOf(container), updated(new RemoveAction(), container));
        }

        assertGrayWithAReason("Remove on both containers", updated(new RemoveAction(), List.of(new TestCasesFolderNode(), new TestRunsFolderNode())));
    }

    // Rule-TREE-PANEL-065
    public void testAStatusIsSetOnOneNodeAndTheCurrentOneIsGray() {
        final @NotNull TestSetNode testSet = new TestSetNode();

        assertGrayWithAReason("Active on an active test set", updated(new UpdateStatusAction(TestSetStatus.ACTIVE), testSet));
        assertTrue("the status a test set has is not shown", updated(new UpdateStatusAction(TestSetStatus.ACTIVE), testSet).isVisible());
        assertTrue("Deprecated is gray on an active test set", updated(new UpdateStatusAction(TestSetStatus.DEPRECATED), testSet).isEnabled());

        assertFalse("a status was offered for two nodes at once",
                updated(new UpdateStatusAction(TestSetStatus.DEPRECATED), List.of(testSet, new TestSetNode())).isEnabled());
    }

    // Rule-TREE-PANEL-104
    public void testAnEntryThatCannotWorkIsGrayAndSaysWhy() {
        final @NotNull TestRunNode signedOff = aTestRunIn(TestRunStatus.CLOSED);

        assertGrayWithAReason("Order on Test Cases", updated(new OrderNodeAction(), new TestCasesFolderNode()));
        assertGrayWithAReason("Rename on Test Runs", updated(new RenameAction(), new TestRunsFolderNode()));
        assertGrayWithAReason("Rename on a closed test run", updated(new RenameAction(), signedOff));
        assertGrayWithAReason("Remove on Test Cases", updated(new RemoveAction(), new TestCasesFolderNode()));
        assertGrayWithAReason("Edit on a closed test run", updated(new EditTestRunAction(), signedOff));
        assertGrayWithAReason("Re-create on a test set", updated(new ReCreateTestRunAction(), new TestSetNode()));
        assertGrayWithAReason("Create Testin Node on a test set", updated(new CreateTreeNodeAction(), new TestSetNode()));
    }
}
