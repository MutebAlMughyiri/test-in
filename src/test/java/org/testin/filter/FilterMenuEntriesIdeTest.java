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
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.Gestures;
import org.testin.codegen.AutomationState;
import org.testin.editor.AbstractTestinEditor;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.indexer.TestCases;
import org.testin.model.Automated;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.TestCaseStatus;
import org.testin.services.Services;
import org.testin.testrun.form.TestRunFormFilter;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.tree.TreeModel;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FilterMenuEntriesIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String GENERATED_CLASS = "nafath.CheckoutTest";

    private @NotNull TestSetNode testSet = new TestSetNode();

    private @NotNull List<TestCaseDto> testCases = List.of();

    private static @NotNull List<AnAction> childrenOf(final @NotNull ActionGroup group) {
        return MenuChildren.of(group);
    }

    private static @NotNull String nameOf(final @NotNull AnAction action) {
        return Objects.requireNonNullElse(action.getTemplatePresentation().getText(), "");
    }

    private static @NotNull FilterPopupButton filterOf(final @NotNull AbstractTestinEditor<?, ?> editor) {
        return editor.getToolBar().getToolbarItem(FilterPopupButton.class);
    }

    private static @NotNull AnAction entry(final @NotNull FilterPopupButton filter, final @NotNull String name) {
        return childrenOf(filter.menu()).stream().filter(action -> nameOf(action).equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("the Filter menu has no " + name + ": " + childrenOf(filter.menu()).stream().map(FilterMenuEntriesIdeTest::nameOf).toList()));
    }

    private static @NotNull List<String> namesUnder(final @NotNull FilterPopupButton filter, final @NotNull String name) {
        return childrenOf((DefaultActionGroup) entry(filter, name)).stream().map(FilterMenuEntriesIdeTest::nameOf).toList();
    }

    private static @NotNull List<String> shown(final @NotNull AbstractTestinEditor<?, ?> editor) {
        return editor.getCurrentTestCases().stream().map(TestCaseDto::getDescription).toList();
    }

    private void threeStatesOfAutomation() {
        testSet = createdTestSet("Checkout");
        final @NotNull TestCaseDto written = createdTestCase(testSet, "Log in with a valid user", "m0001");
        final @NotNull TestCaseDto empty = createdTestCase(testSet, "Log in with a wrong password", "m0002");
        final @NotNull TestCaseDto missing = indexedTestCase(testSet, "Log in with a locked account", "m0003");
        settled();
        final @NotNull PsiMethod method = writtenMethodOf(GENERATED_CLASS, written);
        writtenByTheTester(method, "System.out.println(1);");
        settled();
        testCases = List.of(written, empty, missing);
    }

    private @NotNull TestRunEditor aTestRunEditorOver(final @NotNull List<TestCaseDto> covered) {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestRunNode testRun = EditorFixtures.testRun(getProject(), testProject, covered.stream().map(EditorFixtures::pending).toList());
        return EditorFixtures.openTestRunEditor(getProject(), testRun, getTestRootDisposable());
    }

    private void choose(final @NotNull AbstractTestinEditor<?, ?> editor, final @NotNull String menu, final @NotNull String option) {
        final @NotNull AnAction chosen = childrenOf((DefaultActionGroup) entry(filterOf(editor), menu)).stream().filter(action -> nameOf(action).equals(option)).findFirst().orElseThrow();
        Gestures.press(getProject(), chosen, editor.getList());
    }

    private void awaitTheAnswers(final @NotNull AbstractTestinEditor<?, ?> editor) {
        final @NotNull AutomationState state = Services.getInstance(getProject(), AutomationState.class);
        Await.until("the automation state never arrived", () -> {
            editor.refreshView();
            return testCases.stream().noneMatch(testCase -> state.of(testCase.getId()) == Automated.UNKNOWN);
        });
    }

    // Rule-EDITOR-PANEL-198
    public void testTheAutomationFilterOffersTheThreeStatesInBothEditors() {
        threeStatesOfAutomation();
        final @NotNull TestSetEditor testSetEditor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull TestRunEditor testRunEditor = aTestRunEditorOver(testCases);
        try {
            final @NotNull List<String> threeStates = Automated.FILTERABLE.stream().map(Automated::getLabel).toList();
            for (final AbstractTestinEditor<?, ?> editor : List.<AbstractTestinEditor<?, ?>>of(testSetEditor, testRunEditor)) {
                awaitTheAnswers(editor);
                assertEquals("the Automation filter does not offer the three states", threeStates, namesUnder(filterOf(editor), Bundle.message("filter.automation")));
                assertEquals("choosing no state did not show everything", 3, shown(editor).size());

                choose(editor, Bundle.message("filter.automation"), Automated.WRITTEN.getLabel());
                assertEquals("the Automation filter does not behave like the filters beside it", List.of(testCases.getFirst().getDescription()), shown(editor));

                choose(editor, Bundle.message("filter.automation"), Automated.MISSING.getLabel());
                assertEquals(List.of(testCases.getFirst().getDescription(), testCases.get(2).getDescription()), shown(editor));

                filterOf(editor).resetToolBarFilter();
                assertEquals("clearing the Automation filter did not show everything", 3, shown(editor).size());
            }
        } finally {
            Disposer.dispose(testSetEditor);
            Disposer.dispose(testRunEditor);
        }
    }

    // Rule-EDITOR-PANEL-260
    public void testTestSetIsGrayWithItsReasonInATestSetEditorAndListsTheRunsTestSets() {
        threeStatesOfAutomation();
        final @NotNull TestSetEditor testSetEditor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull TestRunEditor testRunEditor = aTestRunEditorOver(testCases);
        try {
            final @NotNull Presentation inATestSet = Gestures.updated(getProject(), entry(filterOf(testSetEditor), Bundle.message("filter.test.set")), testSetEditor.getList());
            assertFalse("Test Set works in a test set editor", inATestSet.isEnabled());
            assertEquals("Test Set does not say why it is gray", Bundle.message("filter.test.set.one"), inATestSet.getDescription());
            assertEquals("the label is not plain", Bundle.message("filter.test.set"), inATestSet.getText());

            final @NotNull AnAction inATestRun = entry(filterOf(testRunEditor), Bundle.message("filter.test.set"));
            assertTrue("Test Set does not work in a test run editor", Gestures.updated(getProject(), inATestRun, testRunEditor.getList()).isEnabled());
            assertEquals("Test Set does not list the run's test set", List.of(testSet.getName()),
                    childrenOf((ActionGroup) inATestRun).stream().map(child -> child.getTemplatePresentation().getText()).toList());
        } finally {
            Disposer.dispose(testSetEditor);
            Disposer.dispose(testRunEditor);
        }
    }

    // Rule-EDITOR-PANEL-261
    public void testStatusFiltersOnTheTestCasesOwnStatusAndRunItemStatusIsGrayInATestSetEditor() {
        threeStatesOfAutomation();
        final @NotNull TestCaseDto reviewed = testCases.get(1).edit().status(TestCaseStatus.REVIEWED).build();
        reviewed.setParent(testSet);
        Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(testSet.getPath(), reviewed);
        final @NotNull TestSetEditor testSetEditor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull TestRunEditor testRunEditor = aTestRunEditorOver(testCases);
        try {
            final @NotNull List<String> fourStatuses = Arrays.stream(TestCaseStatus.values()).map(TestCaseStatus::getLabel).toList();
            for (final AbstractTestinEditor<?, ?> editor : List.<AbstractTestinEditor<?, ?>>of(testSetEditor, testRunEditor)) {
                assertEquals("Status does not offer the test case's own four", fourStatuses, namesUnder(filterOf(editor), Bundle.message("filter.test.case.status")));
                choose(editor, Bundle.message("filter.test.case.status"), TestCaseStatus.REVIEWED.getLabel());
                assertEquals("Status did not filter on the test case's own status in " + editor.getClass().getSimpleName(), List.of(reviewed.getDescription()), shown(editor));
                filterOf(editor).resetToolBarFilter();
            }

            final @NotNull Presentation inATestSet = Gestures.updated(getProject(), entry(filterOf(testSetEditor), Bundle.message("filter.run.item.status")), testSetEditor.getList());
            assertFalse("Run Item Status works in a test set editor", inATestSet.isEnabled());
            assertEquals("Run Item Status does not say why it is gray", Bundle.message("filter.run.only"), inATestSet.getDescription());
            assertTrue("Run Item Status does not work in a test run editor", Gestures.updated(getProject(), entry(filterOf(testRunEditor), Bundle.message("filter.run.item.status")), testRunEditor.getList()).isEnabled());

            final @NotNull CheckedTreeNode tree = new CheckedTreeNode("all");
            final @NotNull CheckedTreeNode set = new CheckedTreeNode(testSet);
            testCases.forEach(testCase -> set.add(new CheckedTreeNode(testCase.getId().equals(reviewed.getId()) ? reviewed : testCase)));
            tree.add(set);
            final @NotNull TestRunFormFilter form = new TestRunFormFilter(getProject(), theTestCasesDirectory().getPath(), tree);
            final @NotNull FilterPopupButton formFilter = (FilterPopupButton) Drawn.components(form.getSelection().getPanel()).stream().filter(FilterPopupButton.class::isInstance).findFirst().orElseThrow();
            assertEquals("Status is not in the Create Test Run dialog", fourStatuses, namesUnder(formFilter, Bundle.message("filter.test.case.status")));
            final @NotNull AnAction reviewedOnly = childrenOf((DefaultActionGroup) entry(formFilter, Bundle.message("filter.test.case.status"))).stream().filter(action -> nameOf(action).equals(TestCaseStatus.REVIEWED.getLabel())).findFirst().orElseThrow();
            Gestures.press(getProject(), reviewedOnly, form.getSelection().getPanel());
            final @NotNull TreeModel offered = Drawn.components(form.getSelection().getPanel()).stream().filter(Tree.class::isInstance).map(Tree.class::cast).findFirst().orElseThrow().getModel();
            assertEquals("Status did not filter the Create Test Run dialog", 1, offered.getChildCount(offered.getChild(offered.getRoot(), 0)));
        } finally {
            Disposer.dispose(testSetEditor);
            Disposer.dispose(testRunEditor);
        }
    }
}
