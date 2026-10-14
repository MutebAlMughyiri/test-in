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

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Gestures;
import org.testin.OnScreen;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCase;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testrun.failure.FailureDetailDialog;
import org.testin.ui.framework.AbstractIconButton;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.MultiLineField;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FailureDialogIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull Transferable aPicture(final @NotNull Color color) {
        final @NotNull BufferedImage image = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        final @NotNull Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 40, 30);
        g.dispose();
        return new Transferable() {
            @Override
            public DataFlavor @NotNull [] getTransferDataFlavors() {
                return new DataFlavor[]{DataFlavor.imageFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(final @NotNull DataFlavor flavor) {
                return DataFlavor.imageFlavor.equals(flavor);
            }

            @Override
            public @NotNull Object getTransferData(final @NotNull DataFlavor flavor) {
                return DataFlavor.imageFlavor.equals(flavor) ? image : "";
            }
        };
    }

    private static @NotNull AnAction pasteIn(final @NotNull JComponent dialog) {
        final @NotNull List<KeyStroke> paste = Arrays.stream(CommonShortcuts.getPaste().getShortcuts()).filter(KeyboardShortcut.class::isInstance).map(shortcut -> ((KeyboardShortcut) shortcut).getFirstKeyStroke()).toList();
        final @NotNull List<Component> everything = new ArrayList<>(Drawn.components(dialog));
        everything.add(dialog);
        return everything.stream().filter(JComponent.class::isInstance)
                .flatMap(component -> ActionUtil.getActions((JComponent) component).stream())
                .filter(action -> Arrays.stream(action.getShortcutSet().getShortcuts()).anyMatch(shortcut -> shortcut instanceof final KeyboardShortcut key && paste.contains(key.getFirstKeyStroke())))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Ctrl+V does nothing in the failure dialog"));
    }

    private static @NotNull JComponent screenshotsRow(final @NotNull JComponent dialog) {
        final @NotNull Component caption = Drawn.components(dialog).stream()
                .filter(component -> Drawn.text(component).equalsIgnoreCase(Bundle.message("dialog.failure.caption.screenshots")))
                .findFirst().orElseThrow(() -> new AssertionError("the dialog has no Screenshots row: " + Drawn.words(dialog)));
        return (JComponent) caption.getParent();
    }

    private static @NotNull List<AbstractIconButton> removeButtons(final @NotNull JComponent dialog) {
        return Drawn.components(dialog).stream().filter(AbstractIconButton.class::isInstance).map(AbstractIconButton.class::cast)
                .filter(button -> Bundle.message("dialog.failure.screenshot.remove").equals(button.getAccessibleContext().getAccessibleName()) || Bundle.message("dialog.failure.screenshot.remove").equals(button.getToolTipText()))
                .toList();
    }

    private static @NotNull EditorTextField actualResultBox(final @NotNull JComponent dialog) {
        return TestRunFixture.boxesOf(dialog).getFirst();
    }

    private static int firstPixelOf(final byte @NotNull [] png) {
        try {
            return Objects.requireNonNull(ImageIO.read(new ByteArrayInputStream(png))).getRGB(0, 0);
        } catch (final IOException ex) {
            throw new AssertionError("a kept screenshot is not a picture", ex);
        }
    }

    private static @NotNull AnAction actionFor(final @NotNull JComponent on, final @NotNull KeyStroke key) {
        return ActionUtil.getActions(on).stream()
                .filter(action -> Arrays.stream(action.getShortcutSet().getShortcuts()).anyMatch(shortcut -> shortcut instanceof final KeyboardShortcut keyboard && key.equals(keyboard.getFirstKeyStroke())))
                .findFirst()
                .orElseThrow(() -> new AssertionError(key + " is not bound to the " + on.getClass().getSimpleName()));
    }

    @Override
    protected void setUp() {
        super.setUp();
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), FailureDetailDialog.class);
        CopyPasteManager.getInstance().setContents(new StringSelection(""));
        super.tearDown();
    }

    private @NotNull TestRunEditor walking(final @NotNull TestRunFixture fixture) {
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        editor.onStartExecutionClicked();
        Await.until("the walk never started", () -> editor.getWalk().getCurrentlyExecutingIndex() == 0);
        return editor;
    }

    private void awaitWrites() {
        Services.getInstance(getProject(), TestRuns.class).awaitWrites();
    }

    private void editTheFailureOf(final @NotNull TestRunEditor editor) {
        Gestures.press(getProject(), ActionManager.getInstance().getAction("Testin.UpdateRunItem"), editor.getList());
    }

    // Rule-EDITOR-PANEL-219
    public void testAPictureOnTheClipboardIsAScreenshotKeptBesideTheTestRunInTheOrderPasted() {
        final @NotNull TestRunFixture fixture = TestRunFixture.pending(getProject(), root, 2);
        final @NotNull TestRunEditor editor = walking(fixture);
        try {
            fixture.press(editor, RunItemStatus.FAILED);
            final @NotNull JComponent dialog = fixture.openFailureDialog();
            assertFalse("the Screenshots row is there before any picture", screenshotsRow(dialog).isVisible());

            CopyPasteManager.getInstance().setContents(new StringSelection("plain words"));
            assertFalse("Ctrl+V took text as a screenshot", Gestures.updated(getProject(), pasteIn(dialog), dialog).isEnabled());

            CopyPasteManager.getInstance().setContents(aPicture(Color.RED));
            Gestures.press(getProject(), pasteIn(dialog), dialog);
            CopyPasteManager.getInstance().setContents(aPicture(Color.BLUE));
            Gestures.press(getProject(), pasteIn(dialog), dialog);

            assertTrue("the Screenshots row did not appear with a picture", screenshotsRow(dialog).isVisible());
            assertEquals("a picture was pasted as letters", "", actualResultBox(dialog).getText());
            assertEquals("each picture has no button of its own to take it out", 2, removeButtons(dialog).size());

            OnScreen.pressKey(dialog, Shortcuts.Enter.getKey());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            awaitWrites();

            final @NotNull RunItem failed = fixture.runItemOf(fixture.testCases().getFirst());
            assertEquals(RunItemStatus.FAILED, failed.getStatus());
            assertEquals("the screenshots were not kept", 2, failed.shownExecution().getScreenshots().size());
            final @NotNull List<byte[]> kept = Services.getInstance(getProject(), TestRuns.class).screenshots(fixture.testRun().getPath(), failed);
            assertEquals("the first picture pasted is not the first kept", Color.RED.getRGB(), firstPixelOf(kept.getFirst()));
            assertEquals("the second picture pasted is not the second kept", Color.BLUE.getRGB(), firstPixelOf(kept.get(1)));
            for (final String name : failed.shownExecution().getScreenshots()) {
                assertTrue("a screenshot is not a picture file beside the test run: " + name, Files.isRegularFile(TestRunNode.screenshotFile(fixture.testRun().getPath(), name)));
            }

            final @NotNull List<String> pastedNames = List.copyOf(failed.shownExecution().getScreenshots());
            editor.getList().setSelectedIndex(0);
            editTheFailureOf(editor);
            final @NotNull JComponent again = fixture.openFailureDialog();
            removeButtons(again).getFirst().doClick();
            assertEquals("the button on a picture did not take it out", 1, removeButtons(again).size());
            OnScreen.pressKey(again, Shortcuts.Enter.getKey());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            final @NotNull RunItem edited = fixture.runItemOf(fixture.testCases().getFirst());
            assertEquals("taking a picture out did not take it out of the test run", List.of(pastedNames.get(1)), edited.shownExecution().getScreenshots());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-225
    public void testFailedIsRecordedOnlyOnceWhatWasTypedIsWritten() {
        final @NotNull TestRunFixture fixture = TestRunFixture.pending(getProject(), root, 2);
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            fixture.press(editor, RunItemStatus.FAILED);
            final @NotNull JComponent dialog = fixture.openFailureDialog();
            actualResultBox(dialog).setText("The dashboard stayed blank");
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final @NotNull TestCase walked = fixture.testCases().getFirst();
            Services.getInstance(getProject(), TestRuns.class).changeRunItems(fixture.testRun().getPath(), testRun -> testRun.getAll().removeIf(runItem -> runItem.getUuid().equals(walked.getUuid())));

            OnScreen.pressKey(dialog, Shortcuts.Enter.getKey());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertTrue("Failed was recorded on a test run that no longer covers the test case",
                    Services.getInstance(getProject(), TestRuns.class).getRunItems(fixture.testRun().getPath()).runItemOf(walked.getUuid()).isEmpty());
            assertTrue("no message said why nothing was recorded: " + balloons, balloons.contains(Bundle.message("run.item.status.test.case.not.covered")));
            assertFalse("Failed was announced though nothing was recorded", balloons.contains(RunItemStatus.FAILED.getLabel()));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-225
    public void testNothingIsRecordedWhenTheTestRunIsNoLongerThere() {
        final @NotNull TestRunFixture fixture = TestRunFixture.pending(getProject(), root, 2);
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            fixture.press(editor, RunItemStatus.FAILED);
            final @NotNull JComponent dialog = fixture.openFailureDialog();
            actualResultBox(dialog).setText("The dashboard stayed blank");
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            awaitWrites();
            Services.getInstance(getProject(), Nodes.class).removeTestRun(fixture.testRun().getPath(), _ -> {
            });
            Await.until("the test run was never taken out of the index", () -> Services.getInstance(getProject(), TestRuns.class).findRunItems(fixture.testRun().getPath()).isEmpty());

            OnScreen.pressKey(dialog, Shortcuts.Enter.getKey());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertTrue("no message said the test run is gone: " + balloons, balloons.contains(Bundle.message("run.item.status.test.run.gone")));
            assertFalse("Failed was announced on a test run that is gone", balloons.contains(RunItemStatus.FAILED.getLabel()));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-246
    public void testWhatHappenedIsTypedIntoTheGrowingBoxAndEnterRecordsTheFailure() {
        final @NotNull TestRunFixture fixture = TestRunFixture.pending(getProject(), root, 2);
        final @NotNull TestRunEditor editor = walking(fixture);
        try {
            final @NotNull FailureDetailDialog built = new FailureDetailDialog(getProject(), fixture.testRun().getPath(), fixture.runItemOf(fixture.testCases().getFirst()), _ -> {
            });
            assertTrue("what happened is not typed into the box the test case form uses",
                    ShownDialog.componentsOf(built).stream().map(DialogPart::getComponent).anyMatch(MultiLineField.class::isInstance));

            fixture.press(editor, RunItemStatus.FAILED);
            final @NotNull JComponent dialog = fixture.openFailureDialog();
            OnScreen.shown(dialog, getTestRootDisposable());
            final @NotNull EditorTextField box = actualResultBox(dialog);
            Await.until("the box never drew its editor", () -> box.getEditor() != null);
            final int oneLine = box.getPreferredSize().height;

            box.setText("""
                    one
                    two
                    three""");
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertTrue("the box did not grow as lines were added", box.getPreferredSize().height > oneLine);

            box.setText("""
                    1
                    2
                    3
                    4
                    5
                    6
                    7
                    8
                    9
                    10
                    11
                    12""");
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final int sixLines = box.getFontMetrics(box.getFont()).getHeight() * 6;
            assertTrue("the box grew past six lines: " + box.getPreferredSize().height + " > " + sixLines, box.getPreferredSize().height <= sixLines + oneLine);

            box.setText("The dashboard stayed blank");
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final @NotNull JComponent typedIn = Objects.requireNonNull(box.getEditor()).getContentComponent();
            assertTrue("Ctrl+Enter is not bound in the box", Gestures.updated(getProject(), actionFor(typedIn, Shortcuts.InsertNewLine.getKey()), typedIn).isEnabled());
            Gestures.press(getProject(), actionFor(typedIn, Shortcuts.InsertNewLine.getKey()), typedIn);
            assertTrue("Ctrl+Enter did not add a line", box.getText().contains("\n"));
            assertEquals("Ctrl+Enter recorded the failure", RunItemStatus.PENDING, fixture.runItemStatusOf(fixture.testCases().getFirst()));

            Gestures.press(getProject(), actionFor(box, Shortcuts.Enter.getKey()), box);

            assertEquals("Enter in the box did not record the failure", RunItemStatus.FAILED, fixture.runItemStatusOf(fixture.testCases().getFirst()));
            assertTrue(fixture.runItemOf(fixture.testCases().getFirst()).shownExecution().getActualResult().startsWith("The dashboard stayed blank"));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-167
    public void testEditingTheDetailLeavesTheRunItemStatusAndWritesOnlyTheFourFields() {
        final @NotNull List<TestCase> testCases = TestRunFixture.testCasesIn(getProject(), root, 2);
        final @NotNull RunItem failed = EditorFixtures.pending(testCases.getFirst()).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).executedBy("Sara").actualResult("It froze").build()));
        final @NotNull TestRunFixture fixture = TestRunFixture.of(getProject(), root, List.of(failed, EditorFixtures.pending(testCases.get(1))), testCases);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        try {
            final @NotNull RunItem before = fixture.runItemOf(testCases.getFirst());
            final @NotNull String executedBy = before.shownExecution().getExecutedBy();
            editor.getList().setSelectedIndex(0);

            editTheFailureOf(editor);
            final @NotNull JComponent dialog = fixture.openFailureDialog();
            actualResultBox(dialog).setText("It froze on the second login");
            TestRunFixture.boxesOf(dialog).get(1).setText("java.lang.IllegalStateException");
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            OnScreen.pressKey(dialog, Shortcuts.Enter.getKey());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            final @NotNull RunItem after = fixture.runItemOf(testCases.getFirst());
            assertEquals("editing the detail changed the run item status", RunItemStatus.FAILED, after.getStatus());
            assertEquals("editing the detail changed who recorded it", executedBy, after.shownExecution().getExecutedBy());
            assertEquals("It froze on the second login", after.shownExecution().getActualResult());
            assertEquals("java.lang.IllegalStateException", after.shownExecution().getStacktrace());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-168
    public void testTheEntryWorksOnExactlyOneFailedTestCase() {
        final @NotNull List<TestCase> testCases = TestRunFixture.testCasesIn(getProject(), root, 3);
        final @NotNull TestRunFixture fixture = TestRunFixture.of(getProject(), root, List.of(
                EditorFixtures.pending(testCases.getFirst()).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build())),
                EditorFixtures.pending(testCases.get(1)).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build())),
                EditorFixtures.pending(testCases.get(2)).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build()))), testCases);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        final @NotNull AnAction entry = ActionManager.getInstance().getAction("Testin.UpdateRunItem");
        try {
            editor.getList().setSelectedIndex(0);
            assertTrue("the entry is gray on one failed test case", Gestures.updated(getProject(), entry, editor.getList()).isEnabled());

            editor.getList().setSelectedIndices(new int[]{0, 1});
            final @NotNull Presentation two = Gestures.updated(getProject(), entry, editor.getList());
            assertFalse("the entry works on two test cases", two.isEnabled());
            assertEquals(Bundle.message("run.item.details.disabled.description"), two.getDescription());

            editor.getList().setSelectedIndex(2);
            assertFalse("the entry works on a test case that passed", Gestures.updated(getProject(), entry, editor.getList()).isEnabled());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-169
    public void testTheMessageComesOnlyAfterTheTestRunIsWritten() {
        final @NotNull List<TestCase> testCases = TestRunFixture.testCasesIn(getProject(), root, 2);
        final @NotNull TestRunFixture fixture = TestRunFixture.of(getProject(), root, List.of(
                EditorFixtures.pending(testCases.getFirst()).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build())),
                EditorFixtures.pending(testCases.get(1)).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build()))), testCases);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        final @NotNull List<String> heldWhenSaid = new ArrayList<>();
        Services.getInstance(getProject(), Notifier.class).watchBalloons(getTestRootDisposable(), html -> {
            if (html.contains(Bundle.message("run.item.updated")))
                heldWhenSaid.add(fixture.runItemOf(testCases.getFirst()).shownExecution().getActualResult());
        });
        try {
            editor.getList().setSelectedIndex(0);
            editTheFailureOf(editor);
            final @NotNull JComponent dialog = fixture.openFailureDialog();
            actualResultBox(dialog).setText("It froze");
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            OnScreen.pressKey(dialog, Shortcuts.Enter.getKey());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertEquals("the message did not come after the test run held the edit", List.of("It froze"), heldWhenSaid);

            heldWhenSaid.clear();
            editor.getList().setSelectedIndex(1);
            editTheFailureOf(editor);
            final @NotNull JComponent dropped = fixture.openFailureDialog();
            actualResultBox(dropped).setText("Never kept");
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            Services.getInstance(getProject(), TestRuns.class).changeRunItems(fixture.testRun().getPath(), testRun -> testRun.getAll().removeIf(runItem -> runItem.getUuid().equals(testCases.get(1).getUuid())));
            OnScreen.pressKey(dropped, Shortcuts.Enter.getKey());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals("an edit that was dropped reported itself as saved", List.of(), heldWhenSaid);
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-170
    public void testEditingOpensTheSameDialogFFilledInWithWhatIsThere() {
        final @NotNull List<TestCase> testCases = TestRunFixture.testCasesIn(getProject(), root, 1);
        final @NotNull TestRunFixture fixture = TestRunFixture.of(getProject(), root, List.of(
                EditorFixtures.pending(testCases.getFirst()).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).actualResult("It froze").stacktrace("java.lang.IllegalStateException").build()))
                        .setBugSeverity(BugSeverity.BLOCKER).setBugPriority(BugPriority.HIGH)), testCases);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        try {
            editor.getList().setSelectedIndex(0);

            editTheFailureOf(editor);

            final @NotNull JComponent dialog = fixture.openFailureDialog();
            assertEquals("It froze", actualResultBox(dialog).getText());
            assertEquals("java.lang.IllegalStateException", TestRunFixture.boxesOf(dialog).get(1).getText());
            assertTrue("the severity recorded is not the one chosen", Drawn.components(dialog).stream()
                    .anyMatch(component -> component instanceof final JRadioButton radio && radio.isSelected() && radio.getText().equals(BugSeverity.BLOCKER.getLabel())));
        } finally {
            Disposer.dispose(editor);
        }
    }
}
