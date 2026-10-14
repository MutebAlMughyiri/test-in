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

package org.testin.editor;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionWrapper;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.Gestures;
import org.testin.Said;
import org.testin.codegen.AutomationState;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.editor.open.TestinEditors;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.model.Automated;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItem;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.view.details.TestCaseHoverButtons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class NavigateToTestCaseIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String TO_THE_TEST_CASE = Bundle.message("action.Testin.NavigateToTestCase.text");

    private static final @NotNull String TO_THE_METHOD = Bundle.message("action.Testin.NavigateToTestMethod.text");

    private static final @NotNull String RUN_TEST_METHOD_TEXT = Bundle.message("action.Testin.RunTestMethod.text");

    private @NotNull TestSetNode testSet = new TestSetNode();

    private static @NotNull AnAction navigateToTestCase() {
        return ActionManager.getInstance().getAction("Testin.NavigateToTestCase");
    }

    private static @NotNull List<String> entriesOf(final @NotNull AbstractEditorContextMenu menu) {
        final @NotNull List<String> names = new ArrayList<>();
        for (final AnAction entry : menu.getChildren(ActionManager.getInstance())) {
            if (entry instanceof Separator) continue;
            final @NotNull AnAction own = entry instanceof final AnActionWrapper wrapper ? wrapper.getDelegate() : entry;
            names.add(Objects.requireNonNullElse(ActionManager.getInstance().getId(own), Objects.requireNonNullElse(own.getTemplatePresentation().getText(), "")));
        }
        return names;
    }

    private @NotNull List<TestCase> automatedTestCases() {
        testSet = createdTestSet("Checkout");
        final @NotNull List<TestCase> made = List.of(createdTestCase(testSet, "Log in with a valid user", "m0001"), createdTestCase(testSet, "Log in with a wrong password", "m0002"));
        settled();
        return made;
    }

    private @NotNull TestRunEditor aTestRunEditorOver(final @NotNull List<RunItem> runItems) {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestRunNode testRun = EditorFixtures.testRun(getProject(), testProject, runItems);
        return EditorFixtures.openTestRunEditor(getProject(), testRun, getTestRootDisposable());
    }

    // Rule-EDITOR-PANEL-233
    public void testItOpensTheTestCasesOwnTestSetAndSelectsItThere() {
        final @NotNull List<TestCase> testCases = automatedTestCases();
        final @NotNull TestinEditors editors = Services.getInstance(getProject(), TestinEditors.class);
        final @NotNull TestRunEditor testRun = aTestRunEditorOver(testCases.stream().map(EditorFixtures::pending).toList());
        try {
            testRun.getList().setSelectedIndex(1);

            Gestures.press(getProject(), navigateToTestCase(), testRun.getList());

            Await.until("the test case's own test set did not open in the test set editor", () -> editors.editorFor(testSet).filter(TestSetEditor.class::isInstance).isPresent());
            final @NotNull TestSetEditor opened = (TestSetEditor) editors.editorFor(testSet).orElseThrow();
            assertEquals("a test set other than the test case's own opened", List.of(testSet.getPath().toAbsolutePath()), editors.openNodePaths());
            Await.until("the test case was not selected in its test set", () -> Optional.ofNullable(opened.getList().getSelectedValue()).map(TestCase::getUuid).filter(testCases.get(1).getUuid()::equals).isPresent());
        } finally {
            Disposer.dispose(testRun);
            editors.closeAll();
        }
    }

    // Rule-EDITOR-PANEL-234
    public void testItIsOfferedOnATestRunsCardsAndMenuAndNotInATestSet() {
        final @NotNull List<TestCase> testCases = automatedTestCases();
        final @NotNull TestRunEditor testRun = aTestRunEditorOver(testCases.stream().map(EditorFixtures::pending).toList());
        final @NotNull TestSetEditor testSetEditor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        try {
            assertTrue("a test run's card does not offer it", TestCaseQuickAction.onCard(getProject(), testRun.getParent(), testCases.getFirst()).stream().anyMatch(offered -> offered.action() == TestCaseQuickAction.NAVIGATE_TO_TEST_CASE));
            assertTrue("the test run editor's menu does not offer it", entriesOf(testRun.contextMenu).contains("Testin.NavigateToTestCase"));
            assertTrue("a test set's card offers it", TestCaseQuickAction.onCard(getProject(), testSet, testCases.getFirst()).stream().noneMatch(offered -> offered.action() == TestCaseQuickAction.NAVIGATE_TO_TEST_CASE));
            assertFalse("the test set editor's menu offers it", entriesOf(testSetEditor.contextMenu).contains("Testin.NavigateToTestCase"));
        } finally {
            Disposer.dispose(testRun);
            Disposer.dispose(testSetEditor);
        }
    }

    // Rule-EDITOR-PANEL-236
    public void testATestCaseInNoTestSetIsRefusedAndNothingOpens() {
        final @NotNull TestRunEditor testRun = aTestRunEditorOver(List.of(new RunItem().setUuid(UUID.randomUUID())));
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            testRun.getList().setSelectedIndex(0);
            final @NotNull Presentation shown = Gestures.updated(getProject(), navigateToTestCase(), testRun.getList());
            assertFalse("Navigate to Test Case works on a test case in no test set", shown.isEnabled());
            assertEquals(Bundle.message("navigate.test.case.nowhere"), shown.getDescription());

            TestCaseQuickAction.NAVIGATE_TO_TEST_CASE.execute(getProject(), testRun.getList().getSelectedValue());

            assertTrue("no message said why: " + balloons, balloons.contains(Bundle.message("navigate.test.case.nowhere")));
            assertEquals("something opened for a test case in no test set", List.of(), Services.getInstance(getProject(), TestinEditors.class).openNodePaths());
        } finally {
            Disposer.dispose(testRun);
        }
    }

    // Rule-EDITOR-PANEL-237
    public void testOneGestureHasOneNameWhereverItIsOffered() {
        final @NotNull List<TestCase> testCases = automatedTestCases();
        final @NotNull AutomationState state = Services.getInstance(getProject(), AutomationState.class);
        state.read(getProject(), testCases, () -> {
        });
        Await.until("the automation state never arrived", () -> state.of(testCases.getFirst().getUuid()) != Automated.UNKNOWN);

        assertEquals(TO_THE_TEST_CASE, navigateToTestCase().getTemplatePresentation().getText());
        assertEquals(TO_THE_METHOD, ActionManager.getInstance().getAction("Testin.NavigateToTestMethod").getTemplatePresentation().getText());
        assertEquals(RUN_TEST_METHOD_TEXT, ActionManager.getInstance().getAction("Testin.RunTestMethod").getTemplatePresentation().getText());

        assertEquals("the card's buttons are named otherwise", List.of(TO_THE_METHOD, RUN_TEST_METHOD_TEXT, TO_THE_TEST_CASE),
                Arrays.asList(TestCaseQuickAction.NAVIGATE_TO_TEST_METHOD.getTooltip(), TestCaseQuickAction.RUN_TEST_METHOD.getTooltip(), TestCaseQuickAction.NAVIGATE_TO_TEST_CASE.getTooltip()));

        final @NotNull List<String> named = TestCaseHoverButtons.of(getProject(), testCases.getFirst(), Optional.empty()).stream()
                .map(button -> Objects.requireNonNullElse(button.getAccessibleContext().getAccessibleName(), ""))
                .filter(name -> !name.isEmpty())
                .toList();
        assertEquals("the view panel names its buttons otherwise", List.of(TO_THE_METHOD, RUN_TEST_METHOD_TEXT, TO_THE_TEST_CASE), named);
    }
}
