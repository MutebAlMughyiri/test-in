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

package org.testin.testcase;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Gestures;
import org.testin.clipboard.CutState;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.undo.TestinUndoAction;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class RemoveTestCasesIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull AnAction undoOn(final @NotNull TestSetEditor editor) {
        final @NotNull KeyStroke controlZ = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Shortcuts.menuMask());
        return ActionUtil.getActions(editor.getList()).stream()
                .filter(TestinUndoAction.class::isInstance)
                .filter(action -> Stream.of(action.getShortcutSet().getShortcuts()).anyMatch(shortcut -> shortcut instanceof final KeyboardShortcut key && controlZ.equals(key.getFirstKeyStroke())))
                .findFirst().orElseThrow(() -> new AssertionError("nothing on the cards takes a change back on Ctrl+Z"));
    }

    private static @NotNull String stamp(final @NotNull Path testSet, final @NotNull TestCase testCase) {
        try (final Stream<Path> files = Files.list(testSet)) {
            final @NotNull Path file = files.filter(candidate -> candidate.getFileName().toString().startsWith(testCase.getId().toString())).findFirst().orElseThrow();
            return Files.readString(file) + "@" + file.toFile().lastModified();
        } catch (final IOException ex) {
            throw new AssertionError("the test set could not be read", ex);
        }
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), ConfirmDialog.class);
        Services.getInstance(getProject(), CutState.class).clear();
        super.tearDown();
    }

    private @NotNull TestCases theTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull TestSetEditor aTestSetOf(final @NotNull String... descriptions) {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        for (int i = 0; i < descriptions.length; i++)
            EditorFixtures.testCase(getProject(), testSet, descriptions[i], String.format("m%04d", i));
        return EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
    }

    private @NotNull AnActionEvent eventOn(final @NotNull AnAction action, final @NotNull TestSetEditor editor) {
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action, Gestures.dataOf(getProject(), editor.getList()));
        ActionUtil.updateAction(action, e);
        return e;
    }

    private @NotNull List<String> askedToRemove(final @NotNull TestSetEditor editor) {
        final @NotNull AnAction remove = ActionManager.getInstance().getAction("Testin.RemoveTestCase");
        final @NotNull AnActionEvent e = eventOn(remove, editor);
        assertTrue("Remove is gray on the selected test cases", e.getPresentation().isEnabled());

        ShownDialog.open(getProject(), ConfirmDialog.class, () -> ActionUtil.performAction(remove, e));
        return Drawn.words(ShownDialog.content(getProject(), ConfirmDialog.class)).stream().map(word -> StringUtil.unescapeXmlEntities(StringUtil.removeHtmlTags(word)).replace("&#39;", "'")).toList();
    }

    private void confirm() {
        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter.getKey());
    }

    private void awaitGone(final @NotNull TestCase testCase) {
        Await.until("'" + testCase.getDescription() + "' was never removed", () -> theTestCases().findTestCase(testCase.getId()).isEmpty());
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    // Rule-EDITOR-PANEL-062
    public void testTheConfirmationNamesTheTestCaseOrCountsThemAndSaysWhichTestSet() {
        final @NotNull TestSetEditor editor = aTestSetOf("Log in", "Log out", "Reset the password");
        editor.getList().setSelectedIndex(1);

        final @NotNull List<String> one = askedToRemove(editor);
        assertTrue("the confirmation does not name the test case: " + one, Drawn.holds(one, Bundle.message("remove.test.case.confirm.one", "Log out")));
        assertTrue("the confirmation does not say which test set: " + one, Drawn.holds(one, "Checkout"));
        ShownDialog.close(getProject(), ConfirmDialog.class);

        editor.getList().setSelectionInterval(0, 2);
        final @NotNull List<String> three = askedToRemove(editor);
        assertTrue("the confirmation does not count the test cases: " + three, Drawn.holds(three, Bundle.message("remove.test.case.confirm.many", "3")));
        assertTrue("the confirmation does not say which test set: " + three, Drawn.holds(three, "Checkout"));
    }

    // Rule-EDITOR-PANEL-063
    public void testARemovalIsTakenBackWithControlZ() {
        final @NotNull TestSetEditor editor = aTestSetOf("Log in", "Log out");
        editor.getList().setSelectedIndex(0);
        final @NotNull TestCase removed = editor.getList().getSelectedValue();

        askedToRemove(editor);
        confirm();
        awaitGone(removed);

        final @NotNull AnAction undo = undoOn(editor);
        Await.until("the removal never reached the undo history", () -> eventOn(undo, editor).getPresentation().isEnabled());
        ActionUtil.performAction(undo, eventOn(undo, editor));

        Await.until("Ctrl+Z did not bring the removed test case back", () -> theTestCases().findTestCase(removed.getId()).isPresent());
    }

    // Rule-EDITOR-PANEL-064
    public void testNothingIsRenumberedAndTheNumbersOnScreenCloseUp() {
        final @NotNull TestSetEditor editor = aTestSetOf("Log in", "Log out", "Reset the password");
        final @NotNull Path testSet = editor.getParent().getPath();
        final @NotNull TestCase first = editor.getList().getModel().getElementAt(0);
        final @NotNull TestCase last = editor.getList().getModel().getElementAt(2);
        final @NotNull String firstBefore = stamp(testSet, first);
        final @NotNull String lastBefore = stamp(testSet, last);
        assertEquals(3, editor.positionOf(last));

        editor.getList().setSelectedIndex(1);
        final @NotNull TestCase removed = editor.getList().getSelectedValue();
        askedToRemove(editor);
        confirm();
        awaitGone(removed);

        assertEquals("a test case before the gap was rewritten", firstBefore, stamp(testSet, first));
        assertEquals("a test case after the gap was renumbered", lastBefore, stamp(testSet, last));
        assertEquals("the number on screen did not close up", 2, editor.positionOf(last));
    }

    // Rule-EDITOR-PANEL-065
    public void testATestCaseWaitingToBePastedIsAskedAboutAndItsRemovalCallsOffTheCut() {
        final @NotNull TestSetEditor editor = aTestSetOf("Log in", "Log out");
        editor.getList().setSelectedIndex(0);
        final @NotNull TestCase waiting = editor.getList().getSelectedValue();
        final @NotNull CutState cutState = Services.getInstance(getProject(), CutState.class);
        cutState.cut(editor, List.of(waiting));

        final @NotNull List<String> asked = askedToRemove(editor);
        assertTrue("a test case waiting to be pasted was not asked about like any other: " + asked, Drawn.holds(asked, Bundle.message("remove.test.case.confirm.one", "Log in")));
        confirm();
        awaitGone(waiting);

        assertFalse("removing the cut test case did not call the cut off", cutState.isCutting());
    }
}
