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

package org.testin.clipboard;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Gestures;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.toolbar.GridViewButton;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testcase.TestCaseOrder;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.Assert.assertNotEquals;

public class ClipboardGesturesIdeTest extends AbstractTempRootIdeTest {
    private static final @NotNull String COPY = "Testin.CopyTestCaseNode";

    private static final @NotNull String CUT = "Testin.CutTestCaseNode";

    private static final @NotNull String PASTE = "Testin.PasteTestCaseNode";

    private @NotNull TestProjectNode testProject = new TestProjectNode();

    private static @NotNull TestCase at(final @NotNull TestSetEditor editor, final int index) {
        return editor.getList().getModel().getElementAt(index);
    }

    private static @NotNull List<String> descriptionsOf(final @NotNull List<TestCase> testCases) {
        return testCases.stream().map(TestCase::getDescription).toList();
    }

    private static int opacityOfCard(final @NotNull TestSetEditor editor, final int index) {
        final @NotNull JBList<TestCase> list = editor.getList();
        list.setSize(900, 2000);
        final @NotNull Component card = list.getCellRenderer().getListCellRendererComponent(list, at(editor, index), index, false, false);
        card.setSize(900, Math.max(40, card.getPreferredSize().height));
        card.doLayout();

        final @NotNull BufferedImage image = new BufferedImage(card.getWidth(), card.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final @NotNull Graphics2D g = image.createGraphics();
        try {
            card.paint(g);
        } finally {
            g.dispose();
        }
        return image.getRGB(card.getWidth() - 3, card.getHeight() / 2) >>> 24;
    }

    @Override
    protected void setUp() {
        super.setUp();
        CutState.startClipboardWatch(getProject());
        testProject = EditorFixtures.testProject(getProject(), root);
    }

    @Override
    protected void tearDown() {
        Services.getInstance(getProject(), CutState.class).clear();
        CopyPasteManager.getInstance().setContents(new StringSelection(""));
        super.tearDown();
    }

    private @NotNull TestCases theTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull CutState cutState() {
        return Services.getInstance(getProject(), CutState.class);
    }

    private @NotNull TestSetEditor aTestSetOf(final @NotNull String name, final @NotNull String... descriptions) {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), testProject, name);
        for (int i = 0; i < descriptions.length; i++) {
            final @NotNull TestCase testCase = TestCase.builder().uuid(UUID.randomUUID()).description(descriptions[i]).order(String.format("m%04d", i)).createdBy("Ann").build();
            testCase.setParent(testSet);
            theTestCases().putTestCaseVerbatim(testSet.getPath(), testCase);
        }
        return EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
    }

    private @NotNull AnActionEvent eventOn(final @NotNull AnAction action, final @NotNull JComponent component) {
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action, Gestures.dataOf(getProject(), component));
        ActionUtil.updateAction(action, e);
        return e;
    }

    private boolean offered(final @NotNull String actionId, final @NotNull JComponent on) {
        return eventOn(ActionManager.getInstance().getAction(actionId), on).getPresentation().isEnabled();
    }

    private void choose(final @NotNull String actionId, final @NotNull JComponent on) {
        final @NotNull AnAction action = ActionManager.getInstance().getAction(actionId);
        final @NotNull AnActionEvent e = eventOn(action, on);
        assertTrue(actionId + " is gray on " + on.getClass().getName() + ": " + e.getPresentation().getDescription(), e.getPresentation().isEnabled());
        ActionUtil.performAction(action, e);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private @NotNull List<TestCase> orderedIn(final @NotNull TestSetEditor editor) {
        return TestCaseOrder.ordered(theTestCases().getTestCasesForTestSet(editor.getParent().getPath()));
    }

    private @NotNull List<String> everyFile() {
        try (final Stream<Path> files = Files.walk(testProject.getPath())) {
            return files.filter(Files::isRegularFile).filter(file -> file.toString().endsWith(".testcase")).sorted().map(file -> file + "@" + file.toFile().lastModified()).toList();
        } catch (final IOException ex) {
            throw new AssertionError("the test project could not be read", ex);
        }
    }

    // Rule-EDITOR-PANEL-075
    public void testCopyingPutsTheTestCasesThemselvesOnTheClipboard() {
        final @NotNull TestSetEditor login = aTestSetOf("Login", "Log in", "Log out", "Reset the password");
        login.getList().setSelectedIndices(new int[]{0, 2});

        choose(COPY, login.getList());

        final @NotNull List<TestCase> onTheClipboard = CopiedTestCases.onTheClipboard(getProject());
        assertEquals("the clipboard does not hold the test cases copied", List.of(at(login, 0).getUuid(), at(login, 2).getUuid()), onTheClipboard.stream().map(TestCase::getUuid).toList());
        assertEquals("the clipboard holds less than the whole test case", "Ann", onTheClipboard.getFirst().getCreatedBy());
    }

    // Rule-EDITOR-PANEL-076, Rule-EDITOR-PANEL-078
    public void testACutCardIsFadedAndCopyingCallsTheCutOffAndBringsItBack() {
        final @NotNull TestSetEditor login = aTestSetOf("Login", "Log in", "Log out");
        final int opaque = opacityOfCard(login, 0);
        login.getList().setSelectedIndex(0);

        choose(CUT, login.getList());

        assertTrue("the cut test case is not waiting to move", cutState().isPending(at(login, 0).getUuid()));
        assertTrue("a cut card is not drawn faded: " + opacityOfCard(login, 0) + " against " + opaque, opacityOfCard(login, 0) < opaque / 2);
        assertEquals("a card that was not cut is drawn faded", opaque, opacityOfCard(login, 1));

        login.getList().setSelectedIndex(1);
        choose(COPY, login.getList());

        assertFalse("copying did not call off the waiting cut", cutState().isCutting());
        assertEquals("the faded card did not come back", opaque, opacityOfCard(login, 0));
    }

    // Rule-EDITOR-PANEL-077
    public void testCopyTestCaseWorksOnACardAndInTheGrid() {
        final @NotNull TestSetEditor login = aTestSetOf("Login", "Log in", "Log out");
        login.getList().setSelectedIndex(1);
        assertTrue("Copy Test Case is gray on a selected card", offered(COPY, login.getList()));

        login.getToolBar().getToolbarItem(GridViewButton.class).doClick();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        final @NotNull JBTable grid = Drawn.components(login.getComponent()).stream().filter(JBTable.class::isInstance).map(JBTable.class::cast).findFirst().orElseThrow();
        grid.changeSelection(0, 1, false, false);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        choose(COPY, grid);

        assertEquals("Copy Test Case in the grid did not copy the selected row", List.of(at(login, 0).getUuid()), CopiedTestCases.onTheClipboard(getProject()).stream().map(TestCase::getUuid).toList());
    }

    // Rule-EDITOR-PANEL-079
    public void testACutOnItsOwnChangesNothing() {
        final @NotNull TestSetEditor login = aTestSetOf("Login", "Log in", "Log out");
        final @NotNull List<String> before = everyFile();
        login.getList().setSelectedIndex(0);

        choose(CUT, login.getList());

        assertEquals("a cut on its own wrote or removed a file", before, everyFile());
        assertEquals("a cut on its own took a test case out of the test set", List.of("Log in", "Log out"), descriptionsOf(orderedIn(login)));
        assertEquals("a cut on its own took a card out of the editor", 2, login.getList().getModel().getSize());
    }

    // Rule-EDITOR-PANEL-080
    public void testACutIsCalledOffByEscapeAndByAnythingWrittenToTheClipboard() {
        final @NotNull TestSetEditor login = aTestSetOf("Login", "Log in", "Log out");
        login.getList().setSelectedIndex(0);

        choose(CUT, login.getList());
        assertTrue(cutState().isCutting());
        CopyPasteManager.getInstance().setContents(new StringSelection("copied in another application"));
        assertFalse("text written to the clipboard did not call the cut off", cutState().isCutting());

        choose(CUT, login.getList());
        assertTrue(cutState().isCutting());
        final @NotNull AnAction escape = ActionUtil.getActions(login.getList()).stream()
                .filter(action -> Stream.of(action.getShortcutSet().getShortcuts()).anyMatch(shortcut -> shortcut instanceof final KeyboardShortcut key && Shortcuts.Escape.getKey().equals(key.getFirstKeyStroke())))
                .findFirst().orElseThrow(() -> new AssertionError("nothing on the cards answers Escape"));
        ActionUtil.performAction(escape, eventOn(escape, login.getList()));
        assertFalse("Escape did not call the cut off", cutState().isCutting());
    }

    // Rule-EDITOR-PANEL-081, Rule-EDITOR-PANEL-083
    public void testAPastedCopyIsANewTestCaseLandingUnderTheSelectedOne() {
        final @NotNull TestSetEditor login = aTestSetOf("Login", "Log in", "Log out");
        final @NotNull TestSetEditor checkout = aTestSetOf("Checkout", "Open the cart", "Pay", "Get a receipt");
        login.getList().setSelectionInterval(0, 1);
        choose(COPY, login.getList());

        checkout.getList().setSelectedIndex(1);
        choose(PASTE, checkout.getList());

        Await.until("the copies were never pasted", () -> orderedIn(checkout).size() == 5);
        assertEquals("the copies did not land under the selected test case in the order they were pasted", List.of("Open the cart", "Pay", "Log in (Copy)", "Log out (Copy)", "Get a receipt"), descriptionsOf(orderedIn(checkout)));
        final @NotNull TestCase copy = orderedIn(checkout).get(2);
        assertNotEquals("a pasted copy kept the original's identity", at(login, 0).getUuid(), copy.getUuid());
        assertEquals("the original was touched by a copy", 2, orderedIn(login).size());
    }

    // Rule-EDITOR-PANEL-083
    public void testWithNothingSelectedPastedTestCasesLandAtTheEnd() {
        final @NotNull TestSetEditor login = aTestSetOf("Login", "Log in");
        final @NotNull TestSetEditor checkout = aTestSetOf("Checkout", "Open the cart", "Pay");
        login.getList().setSelectedIndex(0);
        choose(COPY, login.getList());

        checkout.getList().clearSelection();
        choose(PASTE, checkout.getList());

        Await.until("the copy was never pasted", () -> orderedIn(checkout).size() == 3);
        assertEquals(List.of("Open the cart", "Pay", "Log in (Copy)"), descriptionsOf(orderedIn(checkout)));
    }

    // Rule-EDITOR-PANEL-082, Rule-EDITOR-PANEL-084
    public void testAPastedCutKeepsItsIdentityAndIsOneEntryOnTheUndoHistory() {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);
        final @NotNull String tester = settings.testerName;
        settings.testerName = "Sara";
        try {
            final @NotNull TestSetEditor login = aTestSetOf("Login", "Log in", "Log out");
            final @NotNull TestSetEditor checkout = aTestSetOf("Checkout", "Pay");
            final @NotNull UUID moved = at(login, 0).getUuid();
            final @NotNull UndoScope scope = UndoScope.of(checkout.getParent().getPath());
            login.getList().setSelectedIndex(0);
            choose(CUT, login.getList());

            choose(PASTE, checkout.getList());

            Await.until("the cut was never pasted", () -> orderedIn(checkout).size() == 2 && orderedIn(login).size() == 1);
            final @NotNull TestCase arrived = theTestCases().findTestCase(moved).orElseThrow();
            assertEquals("the pasted cut lost its identity", moved, arrived.getUuid());
            assertEquals("the pasted cut lost who created it", "Ann", arrived.getCreatedBy());
            assertEquals("the tester who pasted is not recorded as the one who changed it", "Sara", arrived.getUpdatedBy());

            Await.until("the cut and paste never reached the undo history", () -> Services.getInstance(getProject(), UndoHistories.class).canUndo(scope));
            assertTrue("the cut and paste is not named as a move", Services.getInstance(getProject(), UndoHistories.class).undoDescription(scope).startsWith(Bundle.message("snapshot.verb.move")));
            assertTrue(Services.getInstance(getProject(), UndoHistories.class).undo(scope));

            assertEquals("one undo did not put the cut test case back where it came from", List.of("Log in", "Log out"), descriptionsOf(orderedIn(login)));
            assertEquals(List.of("Pay"), descriptionsOf(orderedIn(checkout)));
            assertFalse("the cut and its paste made more than one undo entry", Services.getInstance(getProject(), UndoHistories.class).canUndo(scope));
        } finally {
            settings.testerName = tester;
        }
    }

    // Rule-EDITOR-PANEL-085
    public void testAnythingOnTheClipboardButTestCasesIsTurnedAway() {
        final @NotNull TestSetEditor checkout = aTestSetOf("Checkout", "Pay");
        final @NotNull List<String> before = everyFile();

        for (final String text : List.of("Log in with a valid user", "[1, 2, 3]", "{\"description\": \"Log in\"}")) {
            CopyPasteManager.getInstance().setContents(new StringSelection(text));

            assertFalse("Paste Test Case is offered over '" + text + "'", offered(PASTE, checkout.getList()));
            new PasteTestCaseWork(getProject(), checkout).paste();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        }

        assertEquals("text that is not test cases was pasted", before, everyFile());
        assertEquals(1, orderedIn(checkout).size());
    }
}
