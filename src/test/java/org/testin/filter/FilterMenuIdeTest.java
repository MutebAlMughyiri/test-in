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

package org.testin.filter;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.editor.AbstractTestinEditor;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.indexer.TestCases;
import org.testin.model.Automated;
import org.testin.model.Groups;
import org.testin.model.Priority;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestCaseStatus;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.testcase.TestSetEditorFields;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class FilterMenuIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull FilterPopupBtn filterOf(final @NotNull AbstractTestinEditor<?, ?> editor) {
        return editor.getToolBar().getToolbarItem(FilterPopupBtn.class);
    }

    private static @NotNull String textOf(final @NotNull AnAction action) {
        return Objects.toString(action.getTemplatePresentation().getText(), "");
    }

    private static @NotNull List<AnAction> childrenOf(final @NotNull ActionGroup group) {
        return MenuChildren.of(group);
    }

    private static @NotNull List<String> entriesOf(final @NotNull FilterPopupBtn filter) {
        return childrenOf(filter.menu()).stream().skip(1).map(FilterMenuIdeTest::textOf).toList();
    }

    private static @NotNull AnAction entry(final @NotNull ActionGroup group, final @NotNull String text) {
        return childrenOf(group).stream().filter(child -> textOf(child).equals(text)).findFirst()
                .orElseThrow(() -> new AssertionError("no '" + text + "' in " + childrenOf(group).stream().map(FilterMenuIdeTest::textOf).toList()));
    }

    private static void tick(final @NotNull FilterPopupBtn filter, final @NotNull String submenu, final @NotNull String value) {
        final @NotNull AnAction toggle = entry((ActionGroup) entry(filter.menu(), submenu), value);
        ((ToggleAction) toggle).setSelected(TestActionEvent.createTestEvent(toggle), true);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private static void reset(final @NotNull FilterPopupBtn filter) {
        final @NotNull AnAction resetEntry = childrenOf(filter.menu()).getFirst();
        ActionUtil.performAction(resetEntry, TestActionEvent.createTestEvent(resetEntry));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private static boolean enabled(final @NotNull AnAction action) {
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action);
        ActionUtil.updateAction(action, e);
        return e.getPresentation().isEnabled();
    }

    private static void searchForNumberOne(final @NotNull AbstractTestinEditor<?, ?> editor) {
        editor.getToolBar().getSearchTxt().setText("number 1");
        Await.until("the search for 'number 1' never narrowed the list to 32", () -> editor.getCurrentTestCases().size() == 32);
    }

    private static void onThirdPage(final @NotNull AbstractTestinEditor<?, ?> editor) {
        editor.setCurrentPage(3);
        editor.refreshView();
        assertEquals("the editor did not move to page 3", 3, editor.getCurrentPage());
    }

    private @NotNull TestProjectNode aTestProject() {
        return EditorFixtures.testProject(getProject(), root);
    }

    private @NotNull TestSetNode aTestSet(final @NotNull TestProjectNode testProject) {
        return EditorFixtures.testSet(getProject(), testProject, "Checkout");
    }

    private @NotNull List<TestCaseDto> createdTestCases(final @NotNull TestSetNode testSet, final int count) {
        final @NotNull List<TestCaseDto> made = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final @NotNull TestCaseDto testCase = TestCaseDto.builder()
                    .id(UUID.randomUUID())
                    .description("Test case number " + (i + 1))
                    .order(String.format("m%04d", i))
                    .priority(i == 0 ? Priority.HIGH : Priority.LOW)
                    .groups(new ArrayList<>(i % 2 == 0 ? List.of("Smoke") : List.of()))
                    .module(i % 3 == 0 ? "Payments" : "")
                    .build();
            testCase.setParent(testSet);
            Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(testSet.getPath(), testCase);
            made.add(testCase);
        }
        return made;
    }

    private @NotNull TestSetEditor openedTestSet(final int count) {
        final @NotNull TestSetNode testSet = aTestSet(aTestProject());
        createdTestCases(testSet, count);
        return EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
    }

    private @NotNull TestRunEditor openedTestRun(final int count) {
        final @NotNull TestProjectNode testProject = aTestProject();
        final @NotNull List<TestCaseDto> testCases = createdTestCases(aTestSet(testProject), count);
        final @NotNull TestRunNode testRun = EditorFixtures.testRun(getProject(), testProject, testCases.stream().map(EditorFixtures::pending).toList());
        return EditorFixtures.openTestRunEditor(getProject(), testRun, getTestRootDisposable());
    }

    // Rule-EDITOR-PANEL-094
    public void testSevenThingsCanBeFilteredOnEachWhereItBelongs() {
        final @NotNull List<String> everywhere = List.of(TestSetEditorFields.PRIORITY.getName(), Bundle.message("filter.automation"), TestSetEditorFields.GROUP.getName(), TestSetEditorFields.MODULE.getName(), Bundle.message("filter.test.case.status"));

        final @NotNull FilterPopupBtn inATestSet = filterOf(openedTestSet(3));
        assertEquals("a test set editor does not filter on the priority, the automation, the group, the module and the status", everywhere, entriesOf(inATestSet).subList(0, everywhere.size()));
        assertFalse("the run item status works in a test set editor", enabled(entry(inATestSet.menu(), Bundle.message("filter.run.item.status"))));
        assertFalse("a test set editor offers to pick across test sets", enabled(entry(inATestSet.menu(), Bundle.message("filter.test.set"))));

        final @NotNull FilterPopupBtn inATestRun = filterOf(openedTestRun(3));
        assertEquals("a test run editor does not add the run item status to the five", Bundle.message("filter.run.item.status"), entriesOf(inATestRun).get(everywhere.size()));

        final @NotNull Path checkout = root.resolve("Checkout");
        final @NotNull FilterPopupBtn inCreateTestRun = new FilterPopupBtn(new FilterSource() {
            @Override
            public boolean holdsSeveralTestSets() {
                return true;
            }

            @Override
            public @NotNull Map<Path, String> getAvailableTestSets() {
                return Map.of(checkout, "Checkout");
            }

            @Override
            public void onToolBarFilterSelectionChanged() {
            }

            @Override
            public void onToolBarFilterResetButtonClicked() {
            }

            @Override
            public @NotNull Set<String> getAvailableModules() {
                return Set.of();
            }

            @Override
            public @NotNull Set<String> getAvailableGroups() {
                return Set.of();
            }
        });
        tick(inCreateTestRun, Bundle.message("filter.test.set"), "Checkout");
        assertEquals("the Create Test Run dialog cannot filter on the test set", Set.of(checkout), inCreateTestRun.getSelectedTestSet());
    }

    // Rule-EDITOR-PANEL-097, Rule-EDITOR-PANEL-098
    public void testFilteringGoesBackToTheFirstPageAndTheButtonCountsTheFiltersOn() {
        final @NotNull TestSetEditor editor = openedTestSet(120);
        final @NotNull FilterPopupBtn filter = filterOf(editor);
        onThirdPage(editor);

        tick(filter, TestSetEditorFields.PRIORITY.getName(), Priority.LOW.getLabel());

        assertEquals("filtering did not go back to the first page", 1, editor.getCurrentPage());
        assertEquals("the button does not say one filter is on", "(1)", filter.getText());

        tick(filter, Bundle.message("filter.test.case.status"), TestCaseStatus.PENDING.getLabel());
        assertEquals("the button does not say two filters are on", "(2)", filter.getText());
    }

    // Rule-EDITOR-PANEL-099, Rule-EDITOR-PANEL-100
    public void testResetClearsEveryFilterAndLeavesTheSearchText() {
        final @NotNull TestRunEditor editor = openedTestRun(120);
        final @NotNull FilterPopupBtn filter = filterOf(editor);
        searchForNumberOne(editor);

        tick(filter, TestSetEditorFields.PRIORITY.getName(), Priority.LOW.getLabel());
        tick(filter, Bundle.message("filter.automation"), Automated.NONE.getLabel());
        tick(filter, TestSetEditorFields.GROUP.getName(), Groups.NONE);
        tick(filter, TestSetEditorFields.MODULE.getName(), "Payments");
        tick(filter, Bundle.message("filter.run.item.status"), RunItemStatus.PENDING.getLabel());
        assertTrue("the filters did not narrow the list", editor.getCurrentTestCases().size() < 32);

        reset(filter);

        assertTrue("Reset Filters left the priority on", filter.getSelectedPriority().isEmpty());
        assertTrue("Reset Filters left the automation on", filter.getSelectedAutomation().isEmpty());
        assertTrue("Reset Filters left the group on", filter.getSelectedGroup().isEmpty());
        assertTrue("Reset Filters left the module on", filter.getSelectedModule().isEmpty());
        assertTrue("Reset Filters left the run item status on", filter.getSelectedStatus().isEmpty());
        assertFalse("the button still counts filters after Reset Filters", filter.hasActiveFilters());
        assertEquals("Reset Filters cleared the search text", "number 1", editor.getToolBar().getSearchTxt().getText());
        assertEquals("the search stopped narrowing the list after Reset Filters", 32, editor.getCurrentTestCases().size());
    }

    // Rule-EDITOR-PANEL-009
    public void testFilteringSaysNothingAndATestCaseItHidesIsReportedRatherThanShown() {
        final @NotNull TestSetEditor editor = openedTestSet(6);
        final @NotNull FilterPopupBtn filter = filterOf(editor);
        final @NotNull TestCaseDto hidden = editor.getAllTestCases().stream().filter(testCase -> testCase.getPriority() == Priority.LOW).findFirst().orElseThrow();
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());

        tick(filter, TestSetEditorFields.PRIORITY.getName(), Priority.HIGH.getLabel());
        assertEquals("the filter did not narrow the list to the one high priority test case", 1, editor.getCurrentTestCases().size());
        assertEquals("filtering said something", List.of(), balloons.shown());

        editor.selectTestCase(hidden);

        assertEquals("the test case the filter hides was not reported", List.of(Refused.HIDDEN_BY_THE_FILTER.about(hidden.getDescription())), balloons.shown());
        assertEquals("going to a hidden test case threw the filter away", Set.of(Priority.HIGH), filter.getSelectedPriority());
        assertFalse("a test case the filter hides was brought into view", editor.getCurrentTestCases().contains(hidden));
    }

    // Rule-EDITOR-PANEL-260
    public void testTheTestSetFilterNarrowsATestRunToTheChosenTestSet() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull List<TestCaseDto> login = EditorFixtures.testCases(getProject(), EditorFixtures.testSet(getProject(), testProject, "Login"), 2);
        final @NotNull List<TestCaseDto> checkout = EditorFixtures.testCases(getProject(), EditorFixtures.testSet(getProject(), testProject, "Checkout"), 1);
        final @NotNull List<TestCaseDto> both = new ArrayList<>(login);
        both.addAll(checkout);
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), EditorFixtures.testRun(getProject(), testProject, both.stream().map(EditorFixtures::pending).toList()), getTestRootDisposable());
        try {
            tick(filterOf(editor), Bundle.message("filter.test.set"), "Checkout");

            assertEquals("the Test Set filter did not narrow the test run to Checkout", checkout.stream().map(TestCaseDto::getId).toList(), editor.getCurrentTestCases().stream().map(TestCaseDto::getId).toList());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-260
    public void testATestCaseThatWasDeletedAddsNoTestSetToTheFilter() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull List<TestCaseDto> login = EditorFixtures.testCases(getProject(), EditorFixtures.testSet(getProject(), testProject, "Login"), 2);
        final @NotNull List<TestCaseDto> withADeletedOne = new ArrayList<>(login);
        withADeletedOne.add(TestCaseDto.builder().id(UUID.randomUUID()).description("Deleted since").build());
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), EditorFixtures.testRun(getProject(), testProject, withADeletedOne.stream().map(EditorFixtures::pending).toList()), getTestRootDisposable());
        try {
            assertEquals("a deleted test case brought a test set of its own into the filter", List.of("Login"),
                    childrenOf((ActionGroup) entry(filterOf(editor).menu(), Bundle.message("filter.test.set"))).stream().map(FilterMenuIdeTest::textOf).toList());
        } finally {
            Disposer.dispose(editor);
        }
    }
}
