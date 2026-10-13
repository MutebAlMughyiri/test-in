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

import com.intellij.codeInspection.ex.InspectionProfileWrapper;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.ui.Animator;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.OnScreen;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.filter.SortDirection;
import org.testin.filter.SortField;
import org.testin.filter.SortPopupButton;
import org.testin.filter.Sorting;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;
import org.testin.testcase.TestSetEditorFields;
import org.testin.testrun.TestRunFixture;
import org.testin.testrun.failure.FailureDetailDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.Icons;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

public class LightModeIdeTest extends AbstractTempRootIdeTest {

    private static final @NonNls
    @NotNull String WITH_TYPOS = "The dashbord stayd blank";

    private static final @NotNull KeyStroke DETAILS = Shortcuts.ToggleDetails.getKey();
    private static final @NotNull KeyStroke DETAILS_RELEASED = KeyStroke.getKeyStroke(DETAILS.getKeyCode(), DETAILS.getModifiers(), true);
    private static final int STEPS = 10;
    private final @NotNull List<LightModeScreen> opened = new ArrayList<>();

    private static void press(final @NotNull JFrame frame, final @NotNull KeyStroke key) {
        assertTrue("light mode does not answer " + key, OnScreen.pressKey(frame.getRootPane(), key));
    }

    private static @NotNull List<Component> everythingIn(final @NotNull JFrame frame) {
        return Drawn.components(frame.getRootPane());
    }

    private static <T> @NotNull T theOne(final @NotNull JFrame frame, final @NotNull Class<T> kind) {
        return everythingIn(frame).stream().filter(kind::isInstance).map(kind::cast).findFirst().orElseThrow(() -> new AssertionError("light mode holds no " + kind.getSimpleName()));
    }

    private static @NotNull JFrame frameOf(final @NotNull LightModeScreen window) {
        return (JFrame) Objects.requireNonNull(SwingUtilities.getWindowAncestor(window.frame().rootPane()));
    }

    private static @NotNull Animator theHeightMotion(final @NotNull LightModeScreen window) {
        return window.frame().heightMotion().orElseThrow(() -> new AssertionError("the window changed height without moving"));
    }

    private static @NotNull List<Integer> heightsDriven(final @NotNull Animator motion, final @NotNull JFrame frame, final int to) {
        final @NotNull List<Integer> seen = new ArrayList<>();
        for (int step = 0; step < to; step++) {
            motion.paintNow(step, STEPS, 0);
            seen.add(frame.getHeight());
        }
        return seen;
    }

    private static int durationOf(final @NotNull Animator motion) {
        try {
            final @NotNull Field duration = Animator.class.getDeclaredField("cycleDuration");
            duration.setAccessible(true);
            return duration.getInt(motion);
        } catch (final ReflectiveOperationException ex) {
            throw new LinkageError("could not read how long the movement lasts", ex);
        }
    }

    private static void pumpForATenthOfASecond() {
        final long until = System.currentTimeMillis() + 100;
        while (System.currentTimeMillis() < until) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            TimeoutUtil.sleep(5);
        }
    }

    private static @NotNull String describedIn(final @NotNull JFrame frame) {
        return everythingIn(frame).stream().filter(JTextArea.class::isInstance).map(JTextArea.class::cast)
                .map(JTextArea::getText).filter(text -> text.startsWith("Test case number")).findFirst().orElse("");
    }

    private static @NotNull Color frameColorOf(final @NotNull JBLabel name) {
        final @NotNull RoundedLineBorder rounded = (RoundedLineBorder) ((CompoundBorder) name.getBorder()).getOutsideBorder();
        return rounded.getLineColor();
    }

    private static double contrast(final @NotNull Color first, final @NotNull Color second) {
        final double one = luminance(first) + 0.05;
        final double two = luminance(second) + 0.05;
        return Math.max(one, two) / Math.min(one, two);
    }

    private static double luminance(final @NotNull Color color) {
        return 0.2126 * channel(color.getRed()) + 0.7152 * channel(color.getGreen()) + 0.0722 * channel(color.getBlue());
    }

    private static double channel(final int value) {
        final double scaled = value / 255.0;
        return scaled <= 0.03928 ? scaled / 12.92 : Math.pow((scaled + 0.055) / 1.055, 2.4);
    }

    private static int indexIgnoringCase(final @NotNull List<String> words, final @NotNull String wanted) {
        for (int i = 0; i < words.size(); i++) {
            if (words.get(i).equalsIgnoreCase(wanted)) return i;
        }
        return -1;
    }

    private static boolean spellCheckingIsOn(final @NotNull PsiFile typedIn) {
        final @NotNull Object customization = Objects.requireNonNull(InspectionProfileWrapper.getCustomInspectionProfileWrapper(typedIn), "the box was given no inspection customization");
        try {
            final @NotNull Field on = customization.getClass().getDeclaredField("myUseSpellCheck");
            on.setAccessible(true);
            return on.getBoolean(customization);
        } catch (final ReflectiveOperationException ex) {
            throw new LinkageError("the box was customized by something other than spell checking: " + customization.getClass().getName(), ex);
        }
    }

    private static @NotNull List<JComponent> buttons(final @NotNull JFrame frame) {
        final @NotNull List<String> names = List.of(Bundle.message("action.Testin.NavigateToTestMethod.text"), Bundle.message("action.Testin.RunTestMethod.text"), Bundle.message("card.stop.test.method"), Bundle.message("action.Testin.NavigateToTestCase.text"));
        return everythingIn(frame).stream().filter(JBLabel.class::isInstance).map(JBLabel.class::cast).filter(label -> Objects.requireNonNullElse(label.getText(), "").isEmpty()).map(JComponent.class::cast)
                .filter(label -> names.contains(Objects.requireNonNullElse(label.getAccessibleContext().getAccessibleName(), "")))
                .toList();
    }

    private static @NotNull List<String> buttonNames(final @NotNull JFrame frame) {
        return buttons(frame).stream().map(button -> button.getAccessibleContext().getAccessibleName()).toList();
    }

    private @NotNull List<TestCaseDto> aTestSetWithSteps(final int count, final int steps) {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), testProject, "Checkout");
        final @NotNull List<TestCaseDto> made = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final @NotNull TestCaseDto testCase = TestCaseDto.builder().id(UUID.randomUUID()).description("Test case number " + (i + 1)).expectedResult("It works " + (i + 1))
                    .steps(new ArrayList<>(IntStream.rangeClosed(1, steps).mapToObj(step -> "Do step " + step + " of the test case and look at what happens").toList()))
                    .order(String.format("m%04d", i)).build();
            testCase.setParent(testSet);
            Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(testSet.getPath(), testCase);
            made.add(testCase);
        }
        return made;
    }

    private @NotNull TestRunFixture aTestRunOf(final @NotNull List<TestCaseDto> testCases) {
        return TestRunFixture.of(getProject(), root, testCases.stream().map(EditorFixtures::pending).toList(), testCases);
    }

    @Override
    protected void setUp() {
        super.setUp();
        animateWindows(true);
    }

    private void animateWindows(final boolean on) {
        Registry.get("ide.animate.toolwindows").setValue(on, getTestRootDisposable());
    }

    private @NotNull TestRunEditor walking(final @NotNull TestRunFixture fixture) {
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        editor.onStartExecutionClicked();
        Await.until("the walk never started", () -> editor.getWalk().getCurrentlyExecutingIndex() == 0);
        return editor;
    }

    private @NotNull LightMode lightMode() {
        return Services.getInstance(getProject(), LightMode.class);
    }

    private @NotNull JFrame lightModeOn(final @NotNull TestRunEditor editor) {
        final @NotNull List<Frame> before = List.of(Frame.getFrames());
        lightMode().toggle(editor, () -> {
        });
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        return Arrays.stream(Frame.getFrames())
                .filter(Frame::isShowing)
                .filter(frame -> !before.contains(frame))
                .filter(JFrame.class::isInstance).map(JFrame.class::cast)
                .filter(frame -> frame.isUndecorated() && frame.isAlwaysOnTop())
                .reduce((_, second) -> second)
                .orElseThrow(() -> new AssertionError("light mode opened no window"));
    }

    @Override
    protected void tearDown() {
        opened.forEach(LightModeScreen::closeQuietly);
        super.tearDown();
    }

    private @NotNull LightModeScreen openedOn(final @NotNull TestRunEditor editor) {
        final @NotNull LightModeScreen window = new LightModeScreen(editor, () -> {
        });
        opened.add(window);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        return window;
    }

    private void passAndShowTheNext(final @NotNull LightModeScreen window) {
        press(frameOf(window), RunItemStatus.PASSED.getMenuEntry().shortcut());
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        window.refresh();
    }

    // Rule-EDITOR-PANEL-128
    public void testTheEditorAndLightModeRecordOnTheSameTestRun() {
        final @NotNull TestRunFixture fixture = aTestRunOf(aTestSetWithSteps(3, 1));
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull JFrame frame = lightModeOn(editor);
        try {
            press(frame, RunItemStatus.PASSED.getMenuEntry().shortcut());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals("light mode recorded somewhere the editor does not see", RunItemStatus.PASSED, editor.runItem(fixture.testCases().getFirst().getId()).map(RunItem::getStatus).orElseThrow());
            assertEquals(RunItemStatus.PASSED, fixture.statusOf(fixture.testCases().getFirst()));
            Await.until("the editor's walk did not move with light mode", () -> editor.getWalk().getCurrentlyExecutingIndex() == 1);

            editor.getList().setSelectedIndex(1);
            fixture.press(editor, RunItemStatus.BLOCKED);
            final @NotNull String third = TestSetEditorFields.DESCRIPTION.displayValue(fixture.testCases().get(2));
            Await.until("light mode did not follow the run item status the editor recorded: " + describedIn(frame), () -> describedIn(frame).equals(third));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-201
    public void testTheFirstTestCaseArrivesInPlaceAndTheNextComesInMoving() {
        final @NotNull TestRunFixture fixture = aTestRunOf(aTestSetWithSteps(3, 1));
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull LightModeScreen window = openedOn(editor);
        try {
            final @NotNull SlidingPanel testCaseView = theOne(frameOf(window), SlidingPanel.class);
            assertFalse("the first test case slid in, though none left to make room for it", testCaseView.isSliding());
            assertTrue("the first test case moved in", window.slideMotion().isEmpty());

            passAndShowTheNext(window);

            final @NotNull Animator slide = window.slideMotion().orElseThrow(() -> new AssertionError("the next test case arrived without moving"));
            slide.paintNow(STEPS / 2, STEPS, 0);
            assertTrue("the next test case did not come in moving", testCaseView.isSliding());
            slide.paintNow(STEPS - 1, STEPS, 0);
            assertFalse("the test case was still moving at the end", testCaseView.isSliding());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-201
    public void testTheNewTestCaseIsDrawnAboveWhileTheOldOneGoesDown() {
        final @NotNull SlidingPanel panel = new SlidingPanel(new BorderLayout());
        final @NotNull JComponent shown = new JPanel();
        shown.setOpaque(true);
        shown.setBackground(Color.BLUE);
        panel.add(shown);
        panel.setSize(100, 100);
        panel.doLayout();

        panel.captureLeaving();
        shown.setBackground(Color.GREEN);
        panel.setTravelled(0.5);

        final @NotNull BufferedImage drawn = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        final @NotNull Graphics2D g = drawn.createGraphics();
        panel.paint(g);
        g.dispose();

        assertEquals("the arriving test case is not drawn coming in from above", Color.GREEN.getRGB(), drawn.getRGB(50, 25));
        assertEquals("the leaving test case is not drawn going down and out", Color.BLUE.getRGB(), drawn.getRGB(50, 75));
    }

    // Rule-EDITOR-PANEL-202
    public void testTheWindowEasesToItsNewHeightRatherThanJumping() {
        final @NotNull TestRunFixture fixture = aTestRunOf(aTestSetWithSteps(2, 4));
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull LightModeScreen window = openedOn(editor);
        final @NotNull JFrame frame = frameOf(window);
        try {
            final int closed = frame.getHeight();

            press(frame, DETAILS);
            press(frame, DETAILS_RELEASED);
            assertEquals("the window jumped when the details opened", closed, frame.getHeight());
            final @NotNull List<Integer> opening = heightsDriven(theHeightMotion(window), frame, STEPS);
            final int open = opening.getLast();
            assertTrue("opening the details did not grow the window", open > closed);
            assertTrue("the window jumped to its new height: " + opening, opening.stream().filter(height -> height > closed && height < open).count() >= 3);

            press(frame, RunItemStatus.FAILED.getMenuEntry().shortcut());
            assertEquals("the window jumped when the failure form took the details' place", open, frame.getHeight());
            final @NotNull List<Integer> failing = heightsDriven(theHeightMotion(window), frame, STEPS);
            final int withForm = failing.getLast();
            assertTrue("the window did not change height for the failure form", withForm != open);
            assertTrue("the window jumped to the failure form's height: " + failing, failing.stream().anyMatch(height -> height > Math.min(open, withForm) && height < Math.max(open, withForm)));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-203
    public void testEverythingMovesForTheSame200Milliseconds() {
        final @NotNull TestRunFixture fixture = aTestRunOf(aTestSetWithSteps(3, 4));
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull LightModeScreen window = openedOn(editor);
        try {
            passAndShowTheNext(window);
            final @NotNull Animator slide = window.slideMotion().orElseThrow(() -> new AssertionError("the next test case arrived without moving"));

            press(frameOf(window), DETAILS);
            final @NotNull Animator height = theHeightMotion(window);

            assertEquals("the slide does not last " + Motion.DURATION_MS + " ms", 200, durationOf(slide));
            assertEquals("the change of height does not last as long as the slide", durationOf(slide), durationOf(height));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-204
    public void testAKeyPressedWhileTheWindowMovesIsTakenAtOnceFromWhereItIs() {
        final @NotNull TestRunFixture fixture = aTestRunOf(aTestSetWithSteps(2, 6));
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull LightModeScreen window = openedOn(editor);
        final @NotNull JFrame frame = frameOf(window);
        try {
            final int closed = frame.getHeight();
            final @NotNull TestCaseDetails details = theOne(frame, TestCaseDetails.class);

            press(frame, DETAILS);
            press(frame, DETAILS_RELEASED);
            final @NotNull Animator opening = theHeightMotion(window);
            final int midway = heightsDriven(opening, frame, STEPS / 2).getLast();
            assertTrue("the window never started to move", midway > closed);

            press(frame, DETAILS);
            assertFalse("the key pressed while the window was moving waited for the movement", details.isVisible());
            assertTrue("the movement in progress was not dropped", opening.isDisposed());
            assertEquals("the next movement did not start from where the first was dropped", midway, frame.getHeight());

            final @NotNull List<Integer> closing = heightsDriven(theHeightMotion(window), frame, STEPS);
            assertEquals("the window did not end at the height the last key asked for", closed, (int) closing.getLast());
            assertTrue("the window went on toward the dropped height: " + closing, closing.stream().allMatch(height -> height <= midway && height >= closed));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-216
    public void testWithAnimateWindowsOffNothingMoves() {
        animateWindows(false);
        final @NotNull TestRunFixture fixture = aTestRunOf(aTestSetWithSteps(3, 4));
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull LightModeScreen window = openedOn(editor);
        final @NotNull JFrame frame = frameOf(window);
        try {
            final int closed = frame.getHeight();

            press(frame, DETAILS);

            assertTrue("the window moved with Animate windows off", window.frame().heightMotion().isEmpty());
            assertTrue("the details did not open at their full height at once", frame.getHeight() > closed);

            passAndShowTheNext(window);
            assertTrue("the next test case moved in with Animate windows off", window.slideMotion().isEmpty());
            assertFalse("the next test case slid in with Animate windows off", theOne(frame, SlidingPanel.class).isSliding());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-217
    public void testLightModeNeverGrowsPastTheDisplayAndSaysItIsCutOff() {
        animateWindows(false);
        final @NotNull TestRunFixture fixture = aTestRunOf(aTestSetWithSteps(2, 300));
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull LightModeScreen window = openedOn(editor);
        final @NotNull JFrame frame = frameOf(window);
        try {
            press(frame, DETAILS);
            pumpForATenthOfASecond();

            final @NotNull GraphicsConfiguration screen = frame.getGraphicsConfiguration();
            final int usable = screen.getBounds().height - Toolkit.getDefaultToolkit().getScreenInsets(screen).top - Toolkit.getDefaultToolkit().getScreenInsets(screen).bottom;
            assertTrue("light mode grew past the display: " + frame.getHeight() + " > " + usable, frame.getHeight() <= usable);
            assertTrue("a test case longer than the screen does not say it goes on", Drawn.holds(Drawn.words(theOne(frame, TestCaseDetails.class)), Bundle.message("light.cut.off")));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-232
    public void testTheTestSetNameSitsInAQuietRoundedFrameThatGoesWithIt() {
        final @NotNull TestRunFixture fixture = aTestRunOf(aTestSetWithSteps(2, 1));
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull JFrame frame = lightModeOn(editor);
        try {
            final @NotNull JBLabel name = everythingIn(frame).stream().filter(JBLabel.class::isInstance).map(JBLabel.class::cast)
                    .filter(label -> "Checkout".equals(label.getText())).findFirst().orElseThrow(() -> new AssertionError("light mode does not show the test set's name"));
            final @NotNull JTextArea description = everythingIn(frame).stream().filter(JTextArea.class::isInstance).map(JTextArea.class::cast)
                    .filter(area -> area.getText().equals(TestSetEditorFields.DESCRIPTION.displayValue(fixture.testCases().getFirst()))).findFirst().orElseThrow();

            assertNull("the test set's name carries an icon", name.getIcon());
            assertTrue("the test set's name is not in a frame of its own", name.getBorder() instanceof CompoundBorder);
            assertTrue("the frame is not rounded", ((CompoundBorder) name.getBorder()).getOutsideBorder() instanceof RoundedLineBorder);
            assertEquals("the name is not in the gray of the letter frames", Icons.GRAY, name.getForeground());
            assertEquals("the frame is not in the gray of the letter frames", Icons.GRAY, frameColorOf(name));
            assertTrue("the frame is as loud as the description", contrast(Icons.GRAY, frame.getContentPane().getBackground()) < contrast(description.getForeground(), frame.getContentPane().getBackground()));

            theOne(frame, ViewMenuButton.class).getSelectedDetails().remove(LightModePart.SET_NAME);
            lightMode().refresh(editor.getParent());
            assertFalse("the name stayed after it was turned off", name.isShowing());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-225
    public void testTheFailureFormStaysOpenWithWhatWasTypedWhenNothingCouldBeWritten() {
        final @NotNull TestRunFixture fixture = aTestRunOf(aTestSetWithSteps(2, 1));
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull JFrame frame = lightModeOn(editor);
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            press(frame, RunItemStatus.FAILED.getMenuEntry().shortcut());
            final @NotNull FailureForm form = theOne(frame, FailureForm.class);
            final @NotNull EditorTextField typed = Drawn.components(form).stream().filter(EditorTextField.class::isInstance).map(EditorTextField.class::cast).findFirst().orElseThrow();
            typed.setText("The dashboard stayed blank");
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final @NotNull TestCaseDto walked = fixture.testCases().getFirst();
            Services.getInstance(getProject(), TestRuns.class).changeRunItems(fixture.testRun().getPath(), testRun -> testRun.getAll().removeIf(runItem -> runItem.getId().equals(walked.getId())));

            press(frame, Shortcuts.Enter.getKey());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertTrue("the failure form closed though nothing was written", everythingIn(frame).contains(form));
            assertEquals("what was typed was lost", "The dashboard stayed blank", typed.getText());
            assertTrue("no message said why nothing was recorded: " + balloons, balloons.contains(Bundle.message("run.item.status.test.case.not.covered")));
            assertFalse("Failed was announced though nothing was recorded", balloons.contains(RunItemStatus.FAILED.getLabel()));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-148
    public void testTheFormAndTheDialogHoldTheSameFiveFieldsEachInASectionOfItsOwn() {
        final @NotNull TestRunFixture fixture = aTestRunOf(aTestSetWithSteps(2, 1));
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull JFrame frame = lightModeOn(editor);
        try {
            press(frame, RunItemStatus.FAILED.getMenuEntry().shortcut());
            final @NotNull List<String> inTheForm = Drawn.words(theOne(frame, FailureForm.class));

            final @NotNull TestCaseDto walked = fixture.testCases().getFirst();
            final @NotNull FailureDetailDialog dialog = new FailureDetailDialog(getProject(), fixture.testRun().getPath(), fixture.runItemOf(walked), _ -> {
            });
            final @NotNull List<String> inTheDialog = ShownDialog.wordsOf(dialog).stream()
                    .filter(word -> !word.equals(walked.getDescription()) && !word.equals(walked.getExpectedResult())).toList();

            assertEquals("the dialog and the form do not hold the same fields", inTheDialog, inTheForm.stream().map(word -> word.replaceAll("<[^>]+>", "").trim()).toList());
            assertEquals("the dialog does not hold the five fields after the test case, one component each", 6, ShownDialog.componentsOf(dialog).size());
            final @NotNull List<String> captions = List.of("Actual Result", "Bug Severity", "Bug Priority", "Stacktrace", Bundle.message("dialog.failure.caption.screenshots"));
            int at = -1;
            for (final String caption : captions) {
                final int found = indexIgnoringCase(inTheForm, caption);
                assertTrue("the form has no " + caption + " field after the one before it: " + inTheForm, found > at);
                at = found;
            }
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-221
    public void testWhatHappenedIsSpellCheckedInTheFormAndInTheDialog() {
        final @NotNull TestRunFixture fixture = aTestRunOf(aTestSetWithSteps(2, 1));
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull JFrame frame = lightModeOn(editor);
        try {
            press(frame, RunItemStatus.FAILED.getMenuEntry().shortcut());
            final @NotNull EditorTextField inTheForm = Drawn.components(theOne(frame, FailureForm.class)).stream().filter(EditorTextField.class::isInstance).map(EditorTextField.class::cast).findFirst().orElseThrow();
            assertSpellChecked("light mode's form", inTheForm);
            assertTrue("light mode does not offer the corrections key while the form is open", Drawn.holds(Drawn.words(frame.getRootPane()), Bundle.message("dialog.key.corrections")));

            final @NotNull FailureDetailDialog dialog = new FailureDetailDialog(getProject(), fixture.testRun().getPath(), fixture.runItemOf(fixture.testCases().getFirst()), _ -> {
            });
            final @NotNull JComponent fields = new JPanel();
            ShownDialog.componentsOf(dialog).forEach(component -> fields.add(component.getComponent().getPanel()));
            OnScreen.shown(fields, getTestRootDisposable());
            final @NotNull EditorTextField inTheDialog = Drawn.components(fields).stream().filter(EditorTextField.class::isInstance).map(EditorTextField.class::cast).findFirst().orElseThrow();
            assertSpellChecked("the failure dialog", inTheDialog);
        } finally {
            Disposer.dispose(editor);
        }
    }

    private void assertSpellChecked(final @NotNull String where, final @NotNull EditorTextField box) {
        Await.until(where + ": the box never drew its editor", () -> box.getEditor() != null);
        box.setText(WITH_TYPOS);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        final @NotNull PsiFile typedIn = Optional.ofNullable(ReadAction.computeBlocking(() -> PsiDocumentManager.getInstance(getProject()).getPsiFile(box.getDocument()))).orElseThrow(() -> new AssertionError(where + ": the box has no file to check"));
        assertTrue(where + ": spell checking is not switched on for the box", spellCheckingIsOn(typedIn));

        assertEquals(where + ": the text was changed without the tester picking a correction", WITH_TYPOS, box.getText());
    }

    // Rule-EDITOR-PANEL-243
    public void testAButtonThatCannotWorkIsGrayWithItsReasonAndNeverHidden() {
        final @NotNull TestRunFixture fixture = aTestRunOf(aTestSetWithSteps(2, 1));
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull JFrame frame = lightModeOn(editor);
        try {
            final @NotNull List<String> named = buttonNames(frame);
            assertEquals("light mode does not offer the card's three buttons", List.of(
                    Bundle.message("action.Testin.NavigateToTestMethod.text"),
                    Bundle.message("action.Testin.RunTestMethod.text"),
                    Bundle.message("action.Testin.NavigateToTestCase.text")), named);
            for (final JComponent button : buttons(frame)) {
                assertTrue(button.getAccessibleContext().getAccessibleName() + " is hidden", button.isShowing());
            }
            final @NotNull JComponent method = buttons(frame).getFirst();
            assertFalse("a button that cannot work does not say why", Objects.requireNonNullElse(method.getAccessibleContext().getAccessibleDescription(), "").isBlank());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-244
    public void testEachButtonIsAViewMenuEntryNamedAsItIsAndOnUntilTurnedOff() {
        final @NotNull TestRunFixture fixture = aTestRunOf(aTestSetWithSteps(2, 1));
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull JFrame frame = lightModeOn(editor);
        try {
            final @NotNull ViewMenuButton view = theOne(frame, ViewMenuButton.class);
            final @NotNull List<String> entries = Arrays.stream(LightModePart.values()).map(LightModePart::getName).toList();
            for (final String button : buttonNames(frame))
                assertTrue("the View menu has no entry named " + button + ": " + entries, entries.contains(button));
            assertTrue("the three buttons are not on to start with", view.getSelectedDetails().containsAll(List.of(LightModePart.TEST_METHOD_BUTTON, LightModePart.RUN_BUTTON, LightModePart.TEST_CASE_BUTTON)));
            assertTrue("Run and Stop are not one entry", LightModePart.RUN_BUTTON.governs(TestCaseQuickAction.RUN_TEST_METHOD) && LightModePart.RUN_BUTTON.governs(TestCaseQuickAction.STOP_TEST_METHOD));

            view.getSelectedDetails().remove(LightModePart.TEST_CASE_BUTTON);
            lightMode().refresh(editor.getParent());
            assertFalse("turning Navigate to Test Case off left its button", buttonNames(frame).contains(Bundle.message("action.Testin.NavigateToTestCase.text")));
            assertEquals("turning one button off took another with it", 2, buttonNames(frame).size());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-283
    public void testLightModeFollowsTheTestCasesAsShownTheSortIncluded() {
        final @NotNull TestRunFixture fixture = aTestRunOf(aTestSetWithSteps(3, 1));
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        try {
            final @NotNull SortPopupButton sort = editor.getToolBar().getToolbarItem(SortPopupButton.class);
            Sorting.choose(sort, SortField.DESCRIPTION.getLabel());
            Sorting.choose(sort, SortDirection.DESCENDING.getLabel());
            editor.onStartExecutionClicked();
            Await.until("the walk never started", () -> editor.getWalk().getCurrentlyExecutingIndex() == 0);

            final @NotNull JFrame frame = lightModeOn(editor);

            Await.until("light mode did not start on the first test case as sorted: " + describedIn(frame), () -> describedIn(frame).startsWith("Test case number 3"));
        } finally {
            Disposer.dispose(editor);
        }
    }
}
