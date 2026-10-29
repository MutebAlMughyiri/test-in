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

package org.testin.codegen;

import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.FilesUnder;
import org.testin.config.TestinYml;
import org.testin.editor.EditorFixtures;
import org.testin.editor.statusbar.EditorStatusBar;
import org.testin.editor.testset.TestSetEditor;
import org.testin.filter.FilterPopupButton;
import org.testin.filter.Filtering;
import org.testin.model.Automated;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.form.TestRunFormFilter;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.JComponent;
import java.awt.Container;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutomatedStateIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String GENERATED_CLASS = "nafath.CheckoutTest";

    private @NotNull TestSetNode testSet = new TestSetNode();

    private static @NotNull JComponent automatedCount(final @NotNull EditorStatusBar bar) {
        return (JComponent) ((Container) bar.getComponent(2)).getComponent(3);
    }

    private @NotNull AutomationState state() {
        return Services.getInstance(getProject(), AutomationState.class);
    }

    private @NotNull List<TestCase> threeStatesOfAutomation() {
        testSet = createdTestSet("Checkout");
        final @NotNull TestCase written = createdTestCase(testSet, "Log in with a valid user", "m0001");
        final @NotNull TestCase empty = createdTestCase(testSet, "Log in with a wrong password", "m0002");
        final @NotNull TestCase missing = indexedTestCase(testSet, "Log in with a locked account", "m0003");
        settled();
        writtenByTheTester(writtenMethodOf(GENERATED_CLASS, written), "System.out.println(1);");
        settled();
        return List.of(written, empty, missing);
    }

    private void readAndAwait(final @NotNull List<TestCase> testCases) {
        state().read(getProject(), testCases, () -> {
        });
        Await.until("the automation state never arrived", () -> testCases.stream().noneMatch(testCase -> state().of(testCase.getUuid()) == Automated.UNKNOWN));
    }

    // Rule-EDITOR-PANEL-195
    public void testATestCaseIsWrittenMissingOrNotAutomatedAndAnEmptyGeneratedMethodIsNotAutomation() {
        final @NotNull List<TestCase> testCases = threeStatesOfAutomation();
        final @NotNull TestCase nameless = indexedTestCase(testSet, "!!!", "m0004");

        readAndAwait(List.of(testCases.getFirst(), testCases.get(1), testCases.get(2), nameless));

        assertEquals("a method with something in it is not automation", Automated.WRITTEN, state().of(testCases.getFirst().getUuid()));
        assertEquals("a generated method nobody filled in counts as automation", Automated.NONE, state().of(testCases.get(1).getUuid()));
        assertEquals("a test case naming a method it does not have is not reported missing", Automated.MISSING, state().of(testCases.get(2).getUuid()));
        assertEquals("a test case that names no method is not reported not automated", Automated.NONE, state().of(nameless.getUuid()));
    }

    // Rule-EDITOR-PANEL-195, Rule-CODEGEN-003
    public void testABodyIsTheTestersAsSoonAsItIsAnythingButTheTodo() {
        testSet = createdTestSet("Checkout");
        final @NotNull TestCase commented = createdTestCase(testSet, "Log in with a valid user", "m0001");
        final @NotNull TestCase emptied = createdTestCase(testSet, "Log in with a wrong password", "m0002");
        final @NotNull TestCase untouched = createdTestCase(testSet, "Log in with a locked account", "m0003");
        settled();
        writtenByTheTester(writtenMethodOf(GENERATED_CLASS, commented), "\n// open the login page first\n");
        writtenByTheTester(writtenMethodOf(GENERATED_CLASS, emptied), "");
        settled();

        readAndAwait(List.of(commented, emptied, untouched));

        assertEquals("a body holding only the tester's comment is not the tester's", Automated.WRITTEN, state().of(commented.getUuid()));
        assertEquals("a body whose TODO the tester deleted is not the tester's", Automated.WRITTEN, state().of(emptied.getUuid()));
        assertEquals("the one TODO line Testin wrote counts as automation", Automated.NONE, state().of(untouched.getUuid()));
    }

    // Rule-EDITOR-PANEL-195, Rule-CODEGEN-003
    public void testABodyThatKeepsTheTodoAndAddsAStatementIsTheTesters() {
        testSet = createdTestSet("Checkout");
        final @NotNull TestCase kept = createdTestCase(testSet, "Log in with a valid user", "m0001");
        settled();
        final @NotNull PsiMethod testMethod = writtenMethodOf(GENERATED_CLASS, kept);
        final @NotNull String body = Optional.ofNullable(testMethod.getBody()).map(PsiElement::getText).orElseThrow(() -> new AssertionError("the generated method has no body"));
        writtenByTheTester(testMethod, body.substring(1, body.length() - 1).strip() + "\nSystem.out.println(1);");
        settled();

        readAndAwait(List.of(kept));

        assertEquals("a body that kept the TODO and added a statement is not the tester's", Automated.WRITTEN, state().of(kept.getUuid()));
    }

    // Rule-TREE-PANEL-129
    public void testTheTreeOfANewTestRunPicksTheAutomatedTestCasesAsAnEditorDoes() {
        final @NotNull List<TestCase> testCases = threeStatesOfAutomation();
        final @NotNull CheckedTreeNode root = new CheckedTreeNode(testSet);
        testCases.forEach(testCase -> root.add(new CheckedTreeNode(testCase)));
        final @NotNull TestRunFormFilter form = new TestRunFormFilter(getProject(), theTestCasesDirectory().getPath(), root);
        Await.until("the automation state never arrived", () -> testCases.stream().noneMatch(testCase -> state().of(testCase.getUuid()) == Automated.UNKNOWN));
        settled();

        Filtering.choose(Drawn.first(form.getSelection().getPanel(), FilterPopupButton.class), Bundle.message("filter.automation"), Automated.WRITTEN.getLabel());

        final @NotNull CheckboxTree tree = Drawn.first(form.getSelection().getPanel(), CheckboxTree.class);
        final @NotNull List<Object> shown = Collections.list(((CheckedTreeNode) tree.getModel().getRoot()).depthFirstEnumeration()).stream()
                .map(row -> ((CheckedTreeNode) row).getUserObject())
                .filter(TestCase.class::isInstance)
                .toList();
        assertEquals("the automation filter of a new test run's tree did not pick the automated test case alone", List.of(testCases.getFirst()), shown);
    }

    // Rule-EDITOR-PANEL-196
    public void testTheStateIsReadAwayFromTheScreenAndNothingUnreadIsCalledNotAutomated() {
        final @NotNull List<TestCase> testCases = threeStatesOfAutomation();

        state().read(getProject(), testCases, () -> {
        });

        for (final TestCase testCase : testCases) {
            assertEquals("the state was read on the screen's thread, or reported before it was read", Automated.UNKNOWN, state().of(testCase.getUuid()));
        }
        Await.until("the answers never arrived", () -> testCases.stream().noneMatch(testCase -> state().of(testCase.getUuid()) == Automated.UNKNOWN));
    }

    // Rule-EDITOR-PANEL-197
    public void testNothingIsSavedAndAChangedMethodShowsTheNextTimeTheTesterLooks() {
        final @NotNull List<TestCase> testCases = threeStatesOfAutomation();
        final @NotNull Map<String, String> before = FilesUnder.snapshot(root);

        readAndAwait(testCases);
        assertEquals(Automated.NONE, state().of(testCases.get(1).getUuid()));
        assertEquals("reading the automation state wrote something", before, FilesUnder.snapshot(root));

        writtenByTheTester(writtenMethodOf(GENERATED_CLASS, testCases.get(1)), "System.out.println(2);");
        settled();
        state().read(getProject(), testCases, () -> {
        });

        Await.until("a method written since the last look did not show", () -> state().of(testCases.get(1).getUuid()) == Automated.WRITTEN);
    }

    // Rule-CODEGEN-082
    public void testTurningTheCodeOffForgetsWhatWasRead() {
        final @NotNull List<TestCase> testCases = threeStatesOfAutomation();
        readAndAwait(testCases);
        final @NotNull BoundTestProject bound = Services.getInstance(getProject(), BoundTestProject.class);
        final @NotNull String chosen = bound.name();
        final @NotNull AtomicBoolean answered = new AtomicBoolean();
        try {
            bound.choose("Payments");

            state().read(getProject(), testCases, () -> answered.set(true));

            Await.until("turning the code off was never answered", answered::get);
            assertTrue("the automation read while the code was on is still shown with it off", testCases.stream().allMatch(testCase -> state().of(testCase.getUuid()) == Automated.UNKNOWN));
        } finally {
            bound.choose(chosen);
        }
    }

    // Rule-EDITOR-PANEL-210
    public void testTheStatusBarSaysHowManyHaveAGeneratedMethodBehindThem() {
        threeStatesOfAutomation();
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        try {
            final @NotNull String counted = Bundle.message("statusbar.automated.count", Automated.WRITTEN.getLabel(), "1", "3");
            Await.until("the status bar never counted the automated test cases: " + Drawn.text(automatedCount(editor.getStatusBar())), () -> {
                editor.refreshView();
                return automatedCount(editor.getStatusBar()).isVisible() && Drawn.text(automatedCount(editor.getStatusBar())).equals(counted);
            });
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-211
    public void testTheCountSaysNothingUntilAnAnswerIsInAndNothingWhereNoneCanBeRead() {
        final @NotNull EditorStatusBar bar = new EditorStatusBar();
        bar.showAutomated(0, 0);
        assertFalse("nobody has looked yet, and the count says none are automated", automatedCount(bar).isVisible());
        bar.showAutomated(0, 2);
        assertTrue("an answer saying none are automated was not shown", automatedCount(bar).isVisible());
        assertEquals("the count was not written from how many the answer covers", Bundle.message("statusbar.automated.count", Automated.WRITTEN.getLabel(), "0", "2"), Drawn.text(automatedCount(bar)));

        threeStatesOfAutomation();
        assertTrue(TestinYml.save(getProject(), TestinYml.lines("")));
        assertFalse("testin.yml still names the open test project, so the IDE can read the code", CodeOn.isOn(getProject(), CodeChangeRegistry.need(getProject())));
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        try {
            editor.refreshView();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            editor.refreshView();
            assertFalse("the count said something in an IDE that cannot read the code", automatedCount(editor.getStatusBar()).isVisible());
        } finally {
            Disposer.dispose(editor);
        }
    }
}
