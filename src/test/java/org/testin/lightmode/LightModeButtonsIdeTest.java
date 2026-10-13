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

package org.testin.lightmode;

import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.Gestures;
import org.testin.OnScreen;
import org.testin.actions.Declared;
import org.testin.editor.EditorFixtures;
import org.testin.editor.card.ShownTestCaseAction;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.ExecutionStatus;
import org.testin.model.status.RunItemStatus;
import org.testin.runner.CapturedRunner;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class LightModeButtonsIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String TO_THE_METHOD = Bundle.message("action.Testin.NavigateToTestMethod.text");
    private static final @NotNull String RUN = Bundle.message("action.Testin.RunTestMethod.text");
    private static final @NotNull String TO_THE_TEST_CASE = Bundle.message("action.Testin.NavigateToTestCase.text");

    private final @NotNull List<LightModeScreen> opened = new ArrayList<>();

    private @NotNull CapturedRunner runner = new CapturedRunner();

    private static @NotNull List<JComponent> buttons(final @NotNull JFrame frame) {
        final @NotNull List<String> names = List.of(TO_THE_METHOD, RUN, Bundle.message("card.stop.test.method"), TO_THE_TEST_CASE);
        return Drawn.components(frame.getRootPane()).stream().filter(JBLabel.class::isInstance)
                .map(JComponent.class::cast)
                .filter(label -> names.contains(Objects.requireNonNullElse(label.getAccessibleContext().getAccessibleName(), "")) && Drawn.text(label).isEmpty())
                .toList();
    }

    private static @NotNull JComponent button(final @NotNull JFrame frame, final @NotNull String name) {
        return buttons(frame).stream().filter(button -> name.equals(button.getAccessibleContext().getAccessibleName())).findFirst()
                .orElseThrow(() -> new AssertionError("light mode has no " + name + " button"));
    }

    private static @NotNull String shortcutOn(final @NotNull JComponent button) {
        final @NotNull HelpTooltip tooltip = Objects.requireNonNull(HelpTooltip.getTooltipFor(button), "the button has no tooltip");
        try {
            final @NotNull Field shortcut = HelpTooltip.class.getDeclaredField("shortcut");
            shortcut.setAccessible(true);
            return Objects.requireNonNullElse((String) shortcut.get(tooltip), "");
        } catch (final ReflectiveOperationException ex) {
            throw new LinkageError("could not read the tooltip's key", ex);
        }
    }

    private static @NotNull AnAction runTestMethodKey(final @NotNull JFrame frame) {
        final @NotNull List<KeyStroke> keys = keysOf("Testin.RunTestMethod");
        return ActionUtil.getActions(frame.getRootPane()).stream()
                .filter(ShownTestCaseAction.class::isInstance)
                .filter(action -> Arrays.stream(action.getShortcutSet().getShortcuts()).anyMatch(shortcut -> shortcut instanceof final KeyboardShortcut key && keys.contains(key.getFirstKeyStroke())))
                .findFirst()
                .orElseThrow(() -> new AssertionError("light mode does not answer the key of Testin.RunTestMethod"));
    }

    private static @NotNull List<KeyStroke> keysOf(final @NotNull String actionId) {
        return Arrays.stream(Declared.shortcutSet(actionId).getShortcuts()).filter(KeyboardShortcut.class::isInstance).map(shortcut -> ((KeyboardShortcut) shortcut).getFirstKeyStroke()).toList();
    }

    private static void click(final @NotNull JComponent button) {
        for (final MouseListener listener : button.getMouseListeners()) {
            listener.mouseClicked(new MouseEvent(button, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 2, 2, 1, false, MouseEvent.BUTTON1));
        }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    @Override
    protected void setUp() {
        super.setUp();
        runner = CapturedRunner.installed(getTestRootDisposable());
    }

    @Override
    protected void tearDown() {
        opened.forEach(LightModeScreen::closeQuietly);
        super.tearDown();
    }

    private @NotNull List<TestCase> automatedTestCases() {
        final @NotNull TestSetNode testSet = createdTestSet("Checkout");
        final @NotNull List<TestCase> made = List.of(createdTestCase(testSet, "Log in with a valid user", "m0001"), createdTestCase(testSet, "Log in with a wrong password", "m0002"));
        settled();
        return made;
    }

    private @NotNull TestRunEditor walkingThrough(final @NotNull List<TestCase> testCases) {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestRunNode testRun = EditorFixtures.testRun(getProject(), testProject, testCases.stream().map(EditorFixtures::pending).toList());
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), testRun, getTestRootDisposable());
        editor.onStartExecutionClicked();
        Await.until("the walk never started", () -> editor.getWalk().getCurrentlyExecutingIndex() == 0);
        return editor;
    }

    private @NotNull JFrame lightModeOn(final @NotNull TestRunEditor editor) {
        final @NotNull LightModeScreen window = new LightModeScreen(editor, () -> {
        });
        opened.add(window);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        return (JFrame) Objects.requireNonNull(SwingUtilities.getWindowAncestor(window.frame().rootPane()));
    }

    // Rule-EDITOR-PANEL-243
    public void testLightModeOffersTheCardsThreeButtonsAtTheRightEndOfTheTestSetLine() {
        final @NotNull List<TestCase> testCases = automatedTestCases();
        final @NotNull TestRunEditor editor = walkingThrough(testCases);
        final @NotNull JFrame frame = lightModeOn(editor);
        try {
            assertEquals("light mode does not offer the test run card's buttons", List.of(TO_THE_METHOD, RUN, TO_THE_TEST_CASE),
                    buttons(frame).stream().map(button -> button.getAccessibleContext().getAccessibleName()).toList());
            final @NotNull Component name = Drawn.reading(frame.getRootPane(), "Checkout");
            final @NotNull JComponent setLine = (JComponent) name.getParent();
            for (final JComponent button : buttons(frame)) {
                assertTrue(button.getAccessibleContext().getAccessibleName() + " is not always visible", button.isShowing());
                assertTrue(button.getAccessibleContext().getAccessibleName() + " is not on the test set line", SwingUtilities.isDescendingFrom(button, setLine));
                assertTrue(button.getAccessibleContext().getAccessibleName() + " works on the card but not here", Objects.requireNonNullElse(button.getAccessibleContext().getAccessibleDescription(), "").isEmpty());
            }
            final @NotNull JComponent last = buttons(frame).getLast();
            final @NotNull Point end = SwingUtilities.convertPoint(last, last.getWidth(), 0, setLine);
            assertTrue("the buttons are not at the right end of the test set line: " + end.x + " of " + setLine.getWidth(), setLine.getWidth() - setLine.getInsets().right - end.x <= 2);

            click(button(frame, RUN));

            assertEquals("Run did not run the test case on screen", List.of(List.of(testCases.getFirst())), runner.runs());
            CapturedRunner.reports(getProject(), testCases.getFirst(), ExecutionStatus.PASSED, Duration.ofMillis(84));
            Await.until("Run did not claim the test case for this test run first", () -> editor.runItem(testCases.getFirst().getId()).filter(item -> item.getStatus() == RunItemStatus.PASSED).isPresent());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-245
    public void testLightModeAnswersTheKeymapsKeysAndNamesThem() {
        final @NotNull List<TestCase> testCases = automatedTestCases();
        final @NotNull TestRunEditor editor = walkingThrough(testCases);
        final @NotNull JFrame frame = lightModeOn(editor);
        try {
            assertTrue("Run Test Method is not on F5", keysOf("Testin.RunTestMethod").contains(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)));
            assertTrue("Navigate to Test Method is not on Shift+F5", keysOf("Testin.NavigateToTestMethod").contains(KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK)));
            assertEquals("the Run tooltip does not name its key", Declared.shortcutText("Testin.RunTestMethod"), shortcutOn(button(frame, RUN)));
            assertEquals("the Navigate to Test Method tooltip does not name its key", Declared.shortcutText("Testin.NavigateToTestMethod"), shortcutOn(button(frame, TO_THE_METHOD)));

            final @NotNull List<String> bar = Drawn.words(frame.getRootPane());
            final int lastRunItemStatus = Arrays.stream(RunItemStatus.values()).filter(RunItemStatus::isRunItemStatus).mapToInt(status -> bar.lastIndexOf(status.getLabel())).max().orElse(-1);
            assertTrue("the status bar does not list Run Test Method after the run item status keys: " + bar, bar.lastIndexOf(RUN) > lastRunItemStatus);
            assertTrue("the status bar does not list Navigate to Test Method after the run item status keys: " + bar, bar.lastIndexOf(TO_THE_METHOD) > lastRunItemStatus);

            final @NotNull AnAction runKey = runTestMethodKey(frame);
            Gestures.press(getProject(), runKey, frame.getRootPane());
            assertEquals("F5 did not run the test case on screen", List.of(List.of(testCases.getFirst())), runner.runs());
            assertTrue(Services.getInstance(getProject(), TestNGExecution.class).isRunning(testCases.getFirst().getId()));
            Gestures.press(getProject(), runKey, frame.getRootPane());
            assertFalse("F5 did not stop the test case while its automation ran", Services.getInstance(getProject(), TestNGExecution.class).isRunning(testCases.getFirst().getId()));

            OnScreen.pressKey(frame.getRootPane(), RunItemStatus.FAILED.getMenuEntry().shortcut());
            assertEquals("the buttons stayed while the failure form was open", List.of(), buttons(frame));
            assertFalse("the keys stayed while the failure form was open", Gestures.updated(getProject(), runKey, frame.getRootPane()).isEnabled());
        } finally {
            Disposer.dispose(editor);
        }
    }
}
