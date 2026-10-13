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
package org.testin.view.details;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.LoginTestSource;
import org.testin.Said;
import org.testin.actions.Declared;
import org.testin.config.TestinYml;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.model.Automated;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.Failure;
import org.testin.model.status.ExecutionStatus;
import org.testin.notifications.Done;
import org.testin.runner.ExecutionReported;
import org.testin.runner.ExecutionRecorder;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.view.Drawn;
import org.testin.view.KeyPress;
import org.testin.view.PopupsBuilt;
import org.testin.view.ViewOnScreen;
import org.testin.view.ViewTab;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PanelActionsIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String LOGIN_TEST = "nafath.LoginTest";

    private ViewOnScreen view;

    private static @NotNull String nameOf(final @NotNull JBLabel button) {
        return Objects.requireNonNullElse(button.getAccessibleContext().getAccessibleName(), "");
    }

    private static @NotNull String whyGray(final @NotNull JBLabel button) {
        return Objects.requireNonNullElse(button.getAccessibleContext().getAccessibleDescription(), "");
    }

    private static int widthOf(final @NotNull JBLabel button) {
        return Optional.ofNullable(button.getIcon()).map(Icon::getIconWidth).orElse(0);
    }

    private static void pointerOver(final @NotNull JBLabel button) {
        final @NotNull MouseEvent entered = new MouseEvent(button, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 2, 2, 0, false);
        for (final MouseListener listener : button.getMouseListeners()) listener.mouseEntered(entered);
    }

    private static void click(final @NotNull JBLabel button) {
        final @NotNull MouseEvent clicked = new MouseEvent(button, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 2, 2, 1, false, MouseEvent.BUTTON1);
        for (final MouseListener listener : button.getMouseListeners()) listener.mouseClicked(clicked);
    }

    private static @NotNull Set<String> automationWords() {
        return Arrays.stream(Automated.values()).map(Automated::getLabel).collect(Collectors.toSet());
    }

    private static @NotNull KeyStroke f5() {
        return ((KeyboardShortcut) Declared.shortcutSet(TestCaseQuickAction.RUN_TEST_METHOD.getActionId()).getShortcuts()[0]).getFirstKeyStroke();
    }

    private static <T extends PsiElement> void clickTheMark(final @NotNull LineMarkerInfo<T> mark, final @NotNull MouseEvent click) {
        Optional.ofNullable(mark.getNavigationHandler()).orElseThrow(() -> new AssertionError("the mark does nothing when clicked")).navigate(click, mark.getElement());
    }

    @Override
    protected void setUp() {
        super.setUp();
        view = ViewOnScreen.closed(getProject(), getTestRootDisposable());
    }

    private @NotNull TestCase aTestCase() {
        return indexedTestCase(createdTestSet("Login"), "Log in with a valid user", "b");
    }

    private @NotNull List<JBLabel> buttonsOn(final @NotNull TestCase testCase) {
        final @NotNull JBPanel<?> tab = Drawn.detailsTab(getProject(), testCase, Optional.empty(), List.of());
        final @NotNull Container actions = Drawn.components(tab).stream()
                .filter(JBLabel.class::isInstance)
                .map(JBLabel.class::cast)
                .filter(label -> nameOf(label).equals(TestCaseQuickAction.NAVIGATE_TO_TEST_CASE.getTooltip()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the panel drew no button that opens the test case: " + Drawn.words(tab)))
                .getParent();
        final @NotNull Set<String> actionNames = Arrays.stream(TestCaseQuickAction.values()).map(TestCaseQuickAction::getTooltip).collect(Collectors.toSet());
        return Arrays.stream(actions.getComponents()).filter(JBLabel.class::isInstance).map(JBLabel.class::cast)
                .filter(label -> actionNames.contains(nameOf(label)))
                .toList();
    }

    private void codeIsOff() {
        assertTrue(TestinYml.save(getProject(), TestinYml.lines("")));
    }

    private void assertGrayAndSaysWhy(final @NotNull JBLabel button, final @NotNull String what) {
        final int before = widthOf(button);
        pointerOver(button);

        assertEquals(what + " grew under the pointer while it cannot work", before, widthOf(button));
        assertEquals(what + " turned the pointer into a hand while it cannot work", Cursor.DEFAULT_CURSOR, button.getCursor().getType());
        assertFalse(what + " does not say what it is waiting for", whyGray(button).isEmpty());
    }

    // Rule-VIEW-PANEL-050
    public void testTheRunButtonIsDrawnGrayAndSaysWhyWhereItCannotWork() {
        final @NotNull TestCase testCase = aTestCase();
        codeIsOff();

        final @NotNull JBLabel runButton = buttonsOn(testCase).get(1);
        assertEquals("the run button was not drawn", TestCaseQuickAction.RUN_TEST_METHOD.getTooltip(), nameOf(runButton));
        assertGrayAndSaysWhy(runButton, "the run button");
        assertEquals("the run button does not say it waits for testin.yml", TestCaseQuickAction.RUN_TEST_METHOD.whyNotOffered(getProject()).orElseThrow(), whyGray(runButton));
    }

    // Rule-VIEW-PANEL-051, Rule-VIEW-PANEL-053
    public void testTheRunButtonAndTheStopButtonShareOnePlaceAndNeverShowTogether() {
        final @NotNull TestCase testCase = aTestCase();
        final @NotNull TestNGExecution execution = Services.getInstance(getProject(), TestNGExecution.class);

        final @NotNull List<String> idle = buttonsOn(testCase).stream().map(PanelActionsIdeTest::nameOf).toList();
        assertEquals("the run button is not second of the three", TestCaseQuickAction.RUN_TEST_METHOD.getTooltip(), idle.get(1));
        assertFalse("the stop button was drawn for a test case that is not running", idle.contains(TestCaseQuickAction.STOP_TEST_METHOD.getTooltip()));

        execution.starting(testCase);
        try {
            final @NotNull List<String> running = buttonsOn(testCase).stream().map(PanelActionsIdeTest::nameOf).toList();
            assertEquals("the run button's place does not hold the stop button while the test case runs", TestCaseQuickAction.STOP_TEST_METHOD.getTooltip(), running.get(1));
            assertFalse("the run button was drawn beside the stop button", running.contains(TestCaseQuickAction.RUN_TEST_METHOD.getTooltip()));
            assertEquals("the stop button was added rather than put in the run button's place", idle.size(), running.size());
        } finally {
            execution.stop(List.of(testCase));
        }
    }

    // Rule-VIEW-PANEL-052
    public void testTheRunButtonGrowsUnderThePointer() {
        final @NotNull JBLabel runButton = buttonsOn(aTestCase()).get(1);
        assertEquals("the run button cannot work here: " + whyGray(runButton), "", whyGray(runButton));
        final int before = widthOf(runButton);

        pointerOver(runButton);

        assertTrue("the run button did not grow under the pointer", widthOf(runButton) > before);
        assertEquals("the pointer is not a hand over the run button", Cursor.HAND_CURSOR, runButton.getCursor().getType());
    }

    // Rule-VIEW-PANEL-056
    public void testTheGoToCodeButtonIsDrawnGrayAndSaysWhyWhereItCannotWork() {
        final @NotNull TestCase testCase = aTestCase();
        codeIsOff();

        final @NotNull JBLabel code = buttonsOn(testCase).getFirst();
        assertTrue("the go to code button was not drawn: " + nameOf(code), automationWords().contains(nameOf(code)));
        assertGrayAndSaysWhy(code, "the go to code button");
        assertEquals("the go to code button does not say it waits for testin.yml", TestCaseQuickAction.NAVIGATE_TO_TEST_METHOD.whyNotOffered(getProject()).orElseThrow(), whyGray(code));
    }

    // Rule-VIEW-PANEL-057
    public void testGoToCodeIsTheFirstOfTheThreeButtons() {
        final @NotNull List<String> names = buttonsOn(aTestCase()).stream().map(PanelActionsIdeTest::nameOf).toList();

        assertEquals("the line does not hold three buttons: " + names, 3, names.size());
        assertTrue("the first button is not go to code: " + names, automationWords().contains(names.getFirst()));
        assertEquals("the run button is not second: " + names, TestCaseQuickAction.RUN_TEST_METHOD.getTooltip(), names.get(1));
        assertEquals("the button that opens the test case is not last: " + names, TestCaseQuickAction.NAVIGATE_TO_TEST_CASE.getTooltip(), names.get(2));
    }

    // Rule-VIEW-PANEL-063
    public void testGoingToTheTestCaseIsAnIconOfItsOwnThatRefusesWithNoTestSet() {
        final @NotNull JBLabel withASet = buttonsOn(aTestCase()).get(2);
        assertEquals("the test case opens from the panel but its icon is gray: " + whyGray(withASet), "", whyGray(withASet));
        assertEquals(Cursor.HAND_CURSOR, withASet.getCursor().getType());

        final @NotNull TestCase removedFromItsSet = TestCase.builder().description("Log in with a valid user").build();
        final @NotNull JBLabel gray = buttonsOn(removedFromItsSet).get(2);
        assertEquals("the icon does not say there is no test set to open", Bundle.message("navigate.test.case.nowhere"), whyGray(gray));
        assertGrayAndSaysWhy(gray, "the icon that opens the test case");

        final @NotNull List<String> said = Said.during(getProject(), () -> click(gray));
        assertTrue("clicking the gray icon did not say why: " + said, said.stream().anyMatch(words -> words.contains(Bundle.message("navigate.test.case.nowhere"))));
    }

    // Rule-VIEW-PANEL-084, Rule-VIEW-PANEL-055
    public void testF5StopsTheTestCaseOnDisplayAndItIsRecordedAsNotRun() {
        final @NotNull TestCase testCase = aTestCase();
        final @NotNull TestNGExecution execution = Services.getInstance(getProject(), TestNGExecution.class);
        ExecutionRecorder.startRecording(getProject());
        view.getPanel().show(List.of(testCase), testCase.getParent().treePath());
        execution.starting(testCase);

        final @NotNull List<String> said = Said.during(getProject(), () -> assertTrue("F5 did nothing on a running test case", KeyPress.press(getProject(), view.keyboardTarget(ViewTab.DETAILS), f5())));

        assertFalse("F5 did not stop the running test case", execution.isRunning(testCase.getId()));
        assertTrue("stopping said nothing: " + said, said.stream().anyMatch(words -> words.contains(Done.STOPPED.getOutcome())));

        ExecutionReported.broadcast(getProject(), testCase.getId().toString(), ExecutionStatus.FAILED, Duration.ZERO, Failure.NONE);
        settled();
        view.getPanel().refreshCurrentView();
        assertEquals("the test case the tester stopped was recorded as failed", ExecutionStatus.IDLE, execution.executionStatusOf(testCase));
        assertFalse("the panel shows the stopped test case as failed: " + view.words(ViewTab.DETAILS), view.words(ViewTab.DETAILS).contains(ExecutionStatus.FAILED.getBadge().label()));
    }

    // Rule-VIEW-PANEL-049
    public void testSavingAChangeRewritesTheAutomationCodeForThatField() {
        final @NotNull TestSetNode testSet = createdTestSet("Login");
        final @NotNull TestCase testCase = createdTestCase(testSet, "Log in with a valid user", "b");
        settled();
        assertEquals("\"Log in with a valid user\"", attributeOf(writtenMethodOf(LOGIN_TEST, testCase), "description"));
        final @NotNull PopupsBuilt popups = PopupsBuilt.recording(getTestRootDisposable());
        view.getPanel().show(List.of(testCase), testSet.treePath());

        assertTrue(FieldChange.pressF2(getProject(), view));
        FieldChange.chooseTheDescription(popups.last());
        FieldChange.typeAndSave(getProject(), "Sign in with a valid user");

        Await.until("saving the description did not rewrite it in the test method", () -> {
            settled();
            return methodOf(LOGIN_TEST, testCase).map(testMethod -> attributeOf(testMethod, "description")).filter("\"Sign in with a valid user\""::equals).isPresent();
        });
    }

    // Rule-VIEW-PANEL-010, Rule-VIEW-PANEL-011, Rule-VIEW-PANEL-022
    public void testClickingTheMarkBesideATestMethodOpensThePanelOnThatOneTestCase() {
        final @NotNull TestCase testCase = aTestCase();
        final @NotNull String text = LoginTestSource.withTestNg(testCase.getId().toString());
        final @NotNull PsiFile file = myFixture.addFileToProject("nafath/LoginMarkTest.java", text);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
        myFixture.getEditor().getCaretModel().moveToOffset(text.indexOf(testCase.getId().toString()));
        final @NotNull GutterMark mark = myFixture.findGuttersAtCaret().stream()
                .filter(found -> Bundle.message("gutter.view.details").equals(found.getTooltipText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the test method has no mark"));

        clickTheMark(((LineMarkerInfo.LineMarkerGutterIconRenderer<?>) mark).getLineMarkerInfo(), new MouseEvent(myFixture.getEditor().getComponent(), MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 1, 1, 1, false, MouseEvent.BUTTON1));

        Await.until("clicking the mark did not open the panel", view::isOpen);
        assertEquals("clicking the mark did not move the keyboard into the panel", 1, view.timesTheDetailsTabTookTheKeyboard());
        assertTrue("the panel does not show the test case behind the mark: " + view.words(ViewTab.DETAILS), Drawn.holds(view.words(ViewTab.DETAILS), "Log in with a valid user"));
        assertFalse("the mark handed over more than one test case", view.getPanel().getPage().hasNext());
    }
}
