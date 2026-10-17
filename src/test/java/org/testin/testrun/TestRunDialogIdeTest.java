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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractOpenEditorsIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.creator.CreateTestRun;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestProjectIndexer;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.model.status.PackageStatus;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestCaseStatus;
import org.testin.model.status.TestSetStatus;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItems;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.form.TestRunConfigurationDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class TestRunDialogIdeTest extends AbstractOpenEditorsIdeTest {

    private TestProjectNode testProject;

    private TestSetNode login;

    private TestCase first;

    private TestCase second;

    private TestSetNode empty;

    private TestSetNode old;

    private TestCase inOld;

    private TestSetPackageNode archive;

    private TestSetNode inside;

    private TestCase inInside;

    private static @NotNull Map<TestRunConfiguration, String> everyQuestionAnswered() {
        final @NotNull Map<TestRunConfiguration, String> answers = new EnumMap<>(TestRunConfiguration.class);
        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            if (field.isChoice()) answers.put(field, field.getOptions().getFirst());
        }
        return answers;
    }

    private static boolean answers(final @NotNull Shortcut shortcut, final @NotNull KeyStroke key) {
        return shortcut instanceof final KeyboardShortcut keyboard && key.equals(keyboard.getFirstKeyStroke()) && keyboard.getSecondKeyStroke() == null;
    }

    private static @NotNull AnAction actionFor(final @NotNull JComponent component, final @NotNull KeyStroke key) {
        return ActionUtil.getActions(component).stream()
                .filter(action -> Arrays.stream(action.getShortcutSet().getShortcuts()).anyMatch(shortcut -> answers(shortcut, key)))
                .findFirst()
                .orElseThrow(() -> new AssertionError(component.getClass().getSimpleName() + " does not answer " + key));
    }

    @Override
    public void setUp() {
        super.setUp();
        Services.getInstance(getProject(), TestProjectIndexer.class).resetForReindex();
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        testProject = made.testProject(root.resolve("NAFATH"));
        login = made.testSet(testProject.getTestCasesFolder(), "Login");
        first = made.testCase(login);
        second = made.testCase(login);
        empty = made.testSet(testProject.getTestCasesFolder(), "Empty");
        old = made.testSet(testProject.getTestCasesFolder(), "Old");
        inOld = made.testCase(old);
        archive = made.testSetPackage(testProject.getTestCasesFolder(), "Archive");
        inside = made.testSet(archive, "Inside");
        inInside = made.testCase(inside);
        assertTrue(nodes().mark(old, TestSetStatus.DEPRECATED, "Mohammed AlZamil"));
        assertTrue(nodes().mark(archive, PackageStatus.ARCHIVED, "Mohammed AlZamil"));
        bound().choose("NAFATH");
    }

    @Override
    public void tearDown() {
        ShownDialog.close(getProject(), TestRunConfigurationDialog.class);
        bound().choose("");
        super.tearDown();
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull BoundTestProject bound() {
        return Services.getInstance(getProject(), BoundTestProject.class);
    }

    private @NotNull TestRunNode cycle1In(final @NotNull AbstractNode parent, final @NotNull List<RunItem> runItems) {
        final @NotNull TestRunNode testRun = new NodesOnDisk(getProject()).testRun(parent, "Cycle-1");
        indexedTestRuns().putRunItems(testRun.getPath(), new RunItems().setAll(new ArrayList<>(runItems)));
        return testRun;
    }

    private void theCreateDialogOpened() {
        new CreateTestRun(getProject()).configureTestRun(testProject.getTestCasesFolder(), "Cycle-1", testProject.getTestRunsFolder(), Set.of(), everyQuestionAnswered());
        Await.until("the test run dialog never opened", () -> ShownDialog.isOpen(getProject(), TestRunConfigurationDialog.class));
        ShownDialog.sized(getProject(), TestRunConfigurationDialog.class);
    }

    private void theEditDialogOpenedOn(final @NotNull TestRunNode testRun) {
        new EditTestRunWork(getProject()).editAt(new TreePath(new DefaultMutableTreeNode(testRun)));
        Await.until("the edit dialog never opened", () -> ShownDialog.isOpen(getProject(), TestRunConfigurationDialog.class));
        ShownDialog.sized(getProject(), TestRunConfigurationDialog.class);
    }

    private @NotNull JComponent content() {
        return ShownDialog.content(getProject(), TestRunConfigurationDialog.class);
    }

    private @NotNull CheckboxTree theTestCaseTree() {
        return Optional.ofNullable(UIUtil.uiTraverser(content()).filter(CheckboxTree.class).first()).orElseThrow(() -> new AssertionError("the dialog shows no test case tree"));
    }

    private @NotNull List<CheckedTreeNode> rows() {
        final @NotNull List<CheckedTreeNode> found = new ArrayList<>();
        final @NotNull CheckedTreeNode top = (CheckedTreeNode) theTestCaseTree().getModel().getRoot();
        for (final Object each : Collections.list(top.depthFirstEnumeration())) found.add((CheckedTreeNode) each);
        return found;
    }

    private @NotNull List<Object> offered() {
        return rows().stream().map(CheckedTreeNode::getUserObject).toList();
    }

    private @NotNull CheckedTreeNode rowOf(final @NotNull Object held) {
        return rows().stream().filter(row -> row.getUserObject().equals(held)).findFirst().orElseThrow(() -> new AssertionError(held + " is not offered: " + offered()));
    }

    private @NotNull JButton theButton(final @NotNull String text) {
        return Drawn.components(content()).stream()
                .filter(JButton.class::isInstance)
                .map(JButton.class::cast)
                .filter(button -> text.equals(button.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the dialog has no " + text + " button: " + Drawn.words(content())));
    }

    private @NotNull List<AbstractNode> theTestRunsUnder(final @NotNull AbstractNode parent) {
        return nodes().getChildren(parent.getPath());
    }

    // Rule-TREE-PANEL-030
    public void testARetiredOrEmptyTestSetIsNotOffered() {
        theCreateDialogOpened();
        final @NotNull List<Object> offered = offered();

        assertTrue("a test set that holds test cases is not offered: " + offered, offered.containsAll(List.of(login, first, second)));
        for (final Object left : List.of(empty, old, inOld, archive, inside, inInside)) {
            assertFalse(left + " is offered for a new test run: " + offered, offered.contains(left));
        }
    }

    // Rule-TREE-PANEL-029
    public void testATestRunCannotBeCreatedEmpty() {
        theCreateDialogOpened();
        final @NotNull CheckboxTree tree = theTestCaseTree();
        final @NotNull JButton create = theButton(Bundle.message("test.run.create.button"));

        tree.setNodeState((CheckedTreeNode) tree.getModel().getRoot(), false);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertFalse("Create works with no test case checked", create.isEnabled());
        assertEquals("hovering over the gray Create does not say why", Bundle.message("test.run.form.no.test.case"), Drawn.hovering(create));
        create.doClick();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertTrue("an empty test run was created", theTestRunsUnder(testProject.getTestRunsFolder()).isEmpty());

        tree.setNodeState(rowOf(first), true);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertTrue("Create is gray with a test case checked", create.isEnabled());
    }

    // Rule-TREE-PANEL-138
    public void testADisabledTestCaseOpensUntickedAndTheNewTestRunLeavesItOut() {
        second.setStatus(TestCaseStatus.DISABLED);
        assertTrue("the Disabled status was not stored", Services.getInstance(getProject(), TestCases.class).putTestCase(login.getPath(), second));

        theCreateDialogOpened();

        assertFalse("a Disabled test case opens ticked", rowOf(second).isChecked());
        assertFalse("a Disabled test case can be ticked", rowOf(second).isEnabled());
        assertTrue("a test case that can run opens unticked", rowOf(first).isChecked());

        theButton(Bundle.message("test.run.create.button")).doClick();

        Await.until("the test run was not created", () -> theTestRunsUnder(testProject.getTestRunsFolder()).size() == 1);
        final @NotNull AbstractNode made = theTestRunsUnder(testProject.getTestRunsFolder()).getFirst();
        Await.until("the new test run holds no test case", () -> !indexedTestRuns().getRunItems(made.getPath()).coveredUuids().isEmpty());
        assertEquals("the new test run covers a Disabled test case", Set.of(first.getUuid()), indexedTestRuns().getRunItems(made.getPath()).coveredUuids());
    }

    // Rule-TREE-PANEL-122
    public void testEnterDoesNothingInTheChangeLogOrTheName() {
        theCreateDialogOpened();
        final @NotNull EditorTextField changeLog = Optional.ofNullable(UIUtil.uiTraverser(content()).filter(EditorTextField.class).first()).orElseThrow(() -> new AssertionError("the dialog has no change log"));
        final @NotNull JTextField name = Optional.ofNullable(UIUtil.uiTraverser(content()).filter(JTextField.class).filter(JTextField::isEditable).first()).orElseThrow(() -> new AssertionError("the dialog has no name field"));

        final @NotNull AnAction enter = actionFor(changeLog, Shortcuts.Enter.getKey());
        ActionUtil.performAction(enter, TestActionEvent.createTestEvent(enter));
        name.postActionEvent();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertEquals("Enter changed the change log", "", changeLog.getText());
        assertTrue("Enter closed the dialog", ShownDialog.isOpen(getProject(), TestRunConfigurationDialog.class));
        assertTrue("Enter created the test run", theTestRunsUnder(testProject.getTestRunsFolder()).isEmpty());
    }

    // Rule-TREE-PANEL-093
    public void testTheEditDialogTicksWhatTheTestRunCoversAndAFolderOnlyWhenAllUnderItIs() {
        final @NotNull TestSetNode card = new NodesOnDisk(getProject()).testSet(testProject.getTestCasesFolder(), "Card");
        final @NotNull TestCase paid = new NodesOnDisk(getProject()).testCase(card);
        final @NotNull TestRunNode testRun = cycle1In(testProject.getTestRunsFolder(), List.of(new RunItem().setUuid(first.getUuid()), new RunItem().setUuid(paid.getUuid())));

        theEditDialogOpenedOn(testRun);

        assertTrue("a test case the test run covers is not ticked", rowOf(first).isChecked());
        assertFalse("a test case the test run does not cover is ticked", rowOf(second).isChecked());
        assertFalse("a folder is ticked over a test case the test run does not cover", rowOf(login).isChecked());
        assertTrue("a folder whose every test case is covered is not ticked", rowOf(card).isChecked());
        assertTrue("a covered test case under a fully covered folder is not ticked", rowOf(paid).isChecked());
    }

    private @NotNull CheckedTreeNode theDeletedRow(final @NotNull UUID deleted) {
        return rows().stream().filter(row -> row.getUserObject() instanceof final TestCase testCase && testCase.getUuid().equals(deleted)).findFirst()
                .orElseThrow(() -> new AssertionError("a test case deleted since is not offered: " + offered()));
    }

    // Rule-TREE-PANEL-076, Rule-TREE-PANEL-134
    public void testSavingTheEditKeepsWhatTheDialogDoesNotShowAndTheDeletedTestCasesLeftTicked() {
        final @NotNull UUID deleted = UUID.randomUUID();
        final @NotNull TestRunNode testRun = cycle1In(testProject.getTestRunsFolder(), List.of(new RunItem().setUuid(first.getUuid()), new RunItem().setUuid(inOld.getUuid()).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build())), new RunItem().setUuid(deleted).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build()))));

        indexedTestRuns().changeTestRunMarker(testRun.getPath(), marker -> marker.configure(everyQuestionAnswered()));
        theEditDialogOpenedOn(testRun);
        assertFalse("a test case of a deprecated test set is offered", offered().contains(inOld));
        final @NotNull CheckedTreeNode deletedRow = theDeletedRow(deleted);
        assertTrue("a test case deleted since is not ticked", deletedRow.isChecked());
        assertEquals("the deleted test case is not under its own folder", Bundle.message("test.run.deleted.test.cases"), ((CheckedTreeNode) deletedRow.getParent()).getUserObject());

        theButton(StatusBarShortcut.SAVE).doClick();

        Await.until("saving the edit did not close the dialog", () -> !ShownDialog.isOpen(getProject(), TestRunConfigurationDialog.class));
        final @NotNull RunItems saved = indexedTestRuns().getRunItems(testRun.getPath());
        assertEquals("saving removed what the test run recorded for a test case the dialog did not show", Set.of(first.getUuid(), inOld.getUuid(), deleted), saved.coveredUuids());
        assertEquals(RunItemStatus.FAILED, saved.runItemOf(inOld.getUuid()).map(RunItem::getStatus).orElseThrow());
        assertEquals(RunItemStatus.PASSED, saved.runItemOf(deleted).map(RunItem::shownExecution).map(RunItemExecution::getStatus).orElseThrow());
    }

    // Rule-TREE-PANEL-134
    public void testUntickingADeletedTestCaseAndSavingRemovesItFromTheTestRun() {
        final @NotNull UUID deleted = UUID.randomUUID();
        final @NotNull TestRunNode testRun = cycle1In(testProject.getTestRunsFolder(), List.of(new RunItem().setUuid(first.getUuid()), new RunItem().setUuid(deleted).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build()))));
        indexedTestRuns().changeTestRunMarker(testRun.getPath(), marker -> marker.configure(everyQuestionAnswered()));
        theEditDialogOpenedOn(testRun);

        theTestCaseTree().setNodeState(theDeletedRow(deleted), false);
        theButton(StatusBarShortcut.SAVE).doClick();

        Await.until("saving the edit did not close the dialog", () -> !ShownDialog.isOpen(getProject(), TestRunConfigurationDialog.class));
        assertEquals("the unticked deleted test case was kept", Set.of(first.getUuid()), indexedTestRuns().getRunItems(testRun.getPath()).coveredUuids());
    }

    // Rule-TREE-PANEL-070, Rule-TREE-PANEL-072
    public void testReCreatingCarriesOnlyTheTestCasesAndTheConfigurationIntoTheSameFolder() {
        final @NotNull TestRunPackageNode sprint = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunPackageNode made = Services.getInstance(getProject(), NodeMapper.class).getTestRunPackageNode(testProject.getTestRunsFolder().getPath().resolve("Sprint"), testProject.getTestRunsFolder());
            nodes().addTestRunPackage(made);
            return made;
        });
        final @NotNull TestRunNode source = cycle1In(sprint, List.of(new RunItem().setUuid(first.getUuid()).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).duration(Duration.ofSeconds(5)).actualResult("The dashboard never opened").stacktrace("java.lang.AssertionError").build()))));
        final @NotNull Map<TestRunConfiguration, String> configuration = everyQuestionAnswered();
        configuration.put(TestRunConfiguration.TEST_TYPE, TestRunConfiguration.TEST_TYPE.getOptions().get(1));
        indexedTestRuns().changeTestRunMarker(source.getPath(), marker -> marker.configure(configuration));

        new ReCreateTestRunWork(getProject()).reCreateAt(new TreePath(new Object[]{new DefaultMutableTreeNode(sprint), new DefaultMutableTreeNode(source)}));
        Await.until("the re-create dialog never opened", () -> ShownDialog.isOpen(getProject(), TestRunConfigurationDialog.class));
        ShownDialog.sized(getProject(), TestRunConfigurationDialog.class);
        theButton(Bundle.message("test.run.create.button")).doClick();

        Await.until("the new test run was not created in the folder of the one it was made from", () -> theTestRunsUnder(sprint).size() == 2);
        final @NotNull AbstractNode made = theTestRunsUnder(sprint).stream().filter(node -> !node.getPath().equals(source.getPath())).findFirst().orElseThrow();
        Await.until("the new test run holds no test case", () -> !indexedTestRuns().getRunItems(made.getPath()).coveredUuids().isEmpty());
        final @NotNull RunItem carried = indexedTestRuns().getRunItems(made.getPath()).runItemOf(first.getUuid()).orElseThrow(() -> new AssertionError("the test case was not carried over"));

        assertEquals("the run item status was carried over", RunItemStatus.PENDING, carried.getStatus());
        assertEquals("the duration was carried over", Duration.ZERO, carried.shownExecution().getDuration());
        assertEquals("the failure was carried over", "", carried.shownExecution().getActualResult());
        assertEquals("the failure was carried over", "", carried.shownExecution().getStacktrace());
        assertEquals("the configuration was not carried over", TestRunConfiguration.TEST_TYPE.getOptions().get(1), TestRunConfiguration.TEST_TYPE.valueIn(((TestRunNode) made).getMarker()));
    }
}
