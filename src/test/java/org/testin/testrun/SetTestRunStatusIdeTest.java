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

package org.testin.testrun;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.UiInterceptors;
import com.intellij.ui.components.JBList;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.NodesOnDisk;
import org.testin.indexer.TestRuns;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItems;
import org.testin.services.Services;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class SetTestRunStatusIdeTest extends AbstractTempRootIdeTest {
    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private void finish(final @NotNull TestRunStatus finalTestRunStatus) {
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode testProject = made.testProject(root.resolve("NAFATH"));
        final @NotNull TestSetNode login = made.testSet(testProject.getTestCasesFolder(), "Login");
        final @NotNull UUID passed = made.testCase(login).getUuid();
        final @NotNull UUID stillPending = made.testCase(login).getUuid();
        final @NotNull UUID alsoPending = made.testCase(login).getUuid();

        final @NotNull TestRunNode testRun = made.testRun(testProject.getTestRunsFolder(), "Cycle-1");
        indexedTestRuns().putRunItems(testRun.getPath(), new RunItems().setAll(List.of(
                new RunItem().setUuid(passed).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build())),
                new RunItem().setUuid(stillPending),
                new RunItem().setUuid(alsoPending))));

        new TestRunStatusChange(getProject()).apply(testRun, finalTestRunStatus);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertTrue("a " + finalTestRunStatus + " test run refuses run item statuses, though only a Committed one may", testRun.whySignedOff().isEmpty());
        assertFalse("a " + finalTestRunStatus + " test run can still be edited", testRun.isOpen());
        assertEquals("a run item status recorded before the test run was " + finalTestRunStatus + " changed", RunItemStatus.PASSED, runItemStatusOf(testRun, passed));
        assertEquals("a pending test case is still pending once the test run is " + finalTestRunStatus, RunItemStatus.UNTESTED, runItemStatusOf(testRun, stillPending));
        assertEquals("a pending test case is still pending once the test run is " + finalTestRunStatus, RunItemStatus.UNTESTED, runItemStatusOf(testRun, alsoPending));
    }

    private @NotNull RunItemStatus runItemStatusOf(final @NotNull TestRunNode testRun, final @NotNull UUID testCaseUuid) {
        return indexedTestRuns().getRunItems(testRun.getPath()).runItemOf(testCaseUuid).map(RunItem::getStatus).orElse(RunItemStatus.REMOVED);
    }

    // Rule-TREE-PANEL-067, Rule-TREE-PANEL-009
    public void testCompletingATestRunMarksEveryPendingTestCaseUntested() {
        finish(TestRunStatus.COMPLETED);
    }

    // Rule-TREE-PANEL-067
    public void testClosingATestRunMarksEveryPendingTestCaseUntested() {
        finish(TestRunStatus.CLOSED);
    }

    private @NotNull SimpleTree aTreeShowing(final @NotNull TestRunNode testRun) {
        final @NotNull SimpleTree tree = new SimpleTree(new DefaultTreeModel(new DefaultMutableTreeNode(testRun)));
        tree.setSelectionRow(0);
        return tree;
    }

    private @NotNull TestRunNode aTestRun() {
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        return made.testRun(made.testProject(root.resolve("NAFATH")).getTestRunsFolder(), "Cycle-1");
    }

    private @NotNull AnActionEvent pressed(final @NotNull SetTestRunStatusAction action) {
        return TestActionEvent.createTestEvent(action, SimpleDataContext.getProjectContext(getProject()));
    }

    // UC-TREE-PANEL-020, Rule-TREE-PANEL-067
    public void testSetStatusIsOfferedOnAnOpenTestRunAndGrayOnOneSignedOff() {
        final @NotNull TestRunNode testRun = aTestRun();
        final @NotNull SetTestRunStatusAction setStatus = new SetTestRunStatusAction(getProject(), aTreeShowing(testRun));
        final @NotNull AnActionEvent whileOpen = pressed(setStatus);
        ActionUtil.updateAction(setStatus, whileOpen);
        assertTrue("Set Status is gray on an open test run", whileOpen.getPresentation().isEnabled());

        testRun.getMarker().changeStatus(TestRunStatus.CLOSED);
        final @NotNull AnActionEvent signedOff = pressed(setStatus);
        ActionUtil.updateAction(setStatus, signedOff);

        assertFalse("Set Status is offered on a Closed test run", signedOff.getPresentation().isEnabled());
        assertEquals("the gray Set Status does not say why", Bundle.message("test.run.not.open.description"), signedOff.getPresentation().getDescription());
    }

    // UC-TREE-PANEL-020, Rule-TREE-PANEL-068, Rule-TREE-PANEL-092
    public void testTheMenuOffersOnlyTheStatusesThatCanFollowAndEnterSetsTheFirst() {
        final @NotNull TestRunNode testRun = aTestRun();
        final @NotNull List<JBPopup> shown = new ArrayList<>();
        UiInterceptors.register(new UiInterceptors.UiInterceptor<>(JBPopup.class) {
            @Override
            protected void doIntercept(final @NotNull JBPopup popup) {
                shown.add(popup);
            }
        });
        final @NotNull SetTestRunStatusAction setStatus = new SetTestRunStatusAction(getProject(), aTreeShowing(testRun));

        ActionUtil.performAction(setStatus, pressed(setStatus));

        final @NotNull List<TestRunStatus> canFollow = Arrays.stream(TestRunStatus.values()).filter(testRunStatus -> testRunStatus.canBeSetFrom(TestRunStatus.CREATED)).toList();
        final @NotNull JBList<?> offered = Drawn.first(shown.getFirst().getContent(), JBList.class);
        assertEquals("the menu does not offer exactly the statuses that can follow Created", canFollow, IntStream.range(0, offered.getModel().getSize()).mapToObj(row -> offered.getModel().getElementAt(row)).toList());
        for (int row = 0; row < canFollow.size(); row++) {
            final @NotNull String drawn = ((SimpleColoredComponent) Drawn.row(offered, row)).getCharSequence(false).toString();
            assertTrue("a status's row does not read its name and its key: " + drawn, drawn.contains(canFollow.get(row).getName()) && drawn.contains(canFollow.get(row).getShortcutText()));
        }

        ShownDialog.press(shown.getFirst().getContent(), Shortcuts.Enter.getKey());
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertEquals("Enter did not set the status chosen", canFollow.getFirst(), testRun.getMarker().getStatus());
    }
}
