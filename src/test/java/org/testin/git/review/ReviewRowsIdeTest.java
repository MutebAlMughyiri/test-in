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

package org.testin.git.review;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.git.AbstractGitRemoteIdeTest;
import org.testin.git.ShareGestures;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.ui.framework.ShownDialog;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;
import org.testin.util.JsonCodec;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ReviewRowsIdeTest extends AbstractGitRemoteIdeTest {
    private TestSetNode login;

    private TestCase first;

    private TestCase second;

    private static int rowNamed(final @NotNull JBTable table, final @NotNull String name) {
        for (int row = 0; row < table.getRowCount(); row++) {
            if (name.equals(table.getValueAt(row, 2))) return row;
        }
        throw new AssertionError("no row is named " + name);
    }

    private static void revert(final @NotNull JPopupMenu menu) {
        Arrays.stream(menu.getComponents()).filter(JMenuItem.class::isInstance).map(JMenuItem.class::cast)
                .filter(item -> item.getText().equals(Bundle.message("dialog.pending.revert.row"))).findFirst()
                .orElseThrow(() -> new AssertionError("the menu has no revert")).doClick();
        menu.setVisible(false);
    }

    @Override
    protected void setUp() {
        super.setUp();
        final @NotNull TestProjectNode demo = new NodesOnDisk(getProject()).testProject(work);
        login = new NodesOnDisk(getProject()).testSet(demo.getTestCasesFolder(), "Login");
        first = new NodesOnDisk(getProject()).testCase(login);
        second = TestCase.builder().uuid(UUID.randomUUID()).description("A wrong password is refused").order("n").build();
        assertTrue(indexedTestCases().putTestCaseVerbatim(login.getPath(), second));
        commitAll(work, "the Login test set");

        typed(first, "typed over the first");
        typed(second, "typed over the second");
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), PendingChangesDialog.class);
        super.tearDown();
    }

    private @NotNull TestCases indexedTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private void typed(final @NotNull TestCase testCase, final @NotNull String description) {
        assertTrue("the edit was not saved", indexedTestCases().putTestCase(login.getPath(), indexedTestCases().findTestCase(testCase.getUuid()).orElseThrow().edit().description(description).build()));
    }

    private @NotNull String onDisk(final @NotNull TestCase testCase) {
        return Services.getInstance(getProject(), JsonCodec.class).readValue(read(work, work.relativize(login.getPath()).resolve(testCase.getUuid() + ".testcase").toString()), TestCase.class).getDescription();
    }

    private @NotNull JBTable theReviewTable() {
        return ShareGestures.table(ShareGestures.theReviewOf(getProject(), work));
    }

    private @NotNull JPopupMenu rightClick(final @NotNull JBTable table, final int row) {
        final @NotNull JWindow onScreen = new JWindow();
        onScreen.setBounds(-4000, -4000, 10, 10);
        onScreen.setVisible(true);
        Disposer.register(getTestRootDisposable(), onScreen::dispose);

        final @NotNull Rectangle cell = table.getCellRect(row, 2, true);
        table.dispatchEvent(new MouseEvent(onScreen, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), InputEvent.BUTTON3_DOWN_MASK, cell.x + cell.width / 2, cell.y + cell.height / 2, 1, true, MouseEvent.BUTTON3));

        return Arrays.stream(Window.getWindows())
                .flatMap(window -> UIUtil.uiTraverser(window).filter(JPopupMenu.class).toList().stream())
                .filter(Component::isShowing).findFirst()
                .orElseThrow(() -> new AssertionError("right-clicking the row showed no menu"));
    }

    private void revertFromTheMenuOn(final @NotNull JBTable table, final int row) {
        revert(rightClick(table, row));
    }

    // UC-SHARE-010, Rule-SHARE-049
    public void testEveryRowArrivesSelected() {
        final @NotNull JBTable table = theReviewTable();

        assertTrue("the review has fewer rows than the two typed test cases", table.getRowCount() >= 2);
        assertEquals(table.getRowCount(), table.getSelectedRowCount());
    }

    // UC-SHARE-011, Rule-SHARE-118
    public void testRightClickingASelectedRowKeepsTheSelectionAndActsOnThatRow() {
        final @NotNull JBTable table = theReviewTable();
        final @NotNull List<Integer> selectedBefore = Arrays.stream(table.getSelectedRows()).boxed().toList();

        final @NotNull JPopupMenu menu = rightClick(table, rowNamed(table, "typed over the second"));
        assertEquals("the right-click changed what Commit will send", selectedBefore, Arrays.stream(table.getSelectedRows()).boxed().toList());
        revert(menu);

        assertEquals("the second test case was not the one reverted", "A wrong password is refused", onDisk(second));
        assertEquals("the first test case was reverted too", "typed over the first", onDisk(first));
    }

    // UC-SHARE-011, Rule-SHARE-053
    public void testARevertIsWrittenToDiskAtOnce() {
        final @NotNull String head = head(work, "HEAD");
        final @NotNull JBTable table = theReviewTable();

        revertFromTheMenuOn(table, rowNamed(table, "typed over the first"));

        assertEquals("the revert waited for a commit", "Log in with a valid user", onDisk(first));
        assertEquals("the revert made a commit", head, head(work, "HEAD"));
    }

    // UC-SHARE-011, Rule-INTERNAL-035, Rule-EDITOR-PANEL-069
    public void testARemovedTestCasePutBackBelongsToItsTestSet() {
        assertTrue("the test case was not removed", indexedTestCases().removeTestCase(login.getPath(), second.getUuid()));
        final @NotNull JBTable table = theReviewTable();

        revertFromTheMenuOn(table, rowNamed(table, "A wrong password is refused"));

        assertEquals("the removed test case did not come back", "A wrong password is refused", onDisk(second));
        assertEquals("the test case put back belongs to no test set, so its test method has nowhere to go", login.getPath(), indexedTestCases().findTestCase(second.getUuid()).orElseThrow().getParent().getPath());
    }

    // UC-SHARE-011, Rule-SHARE-119
    public void testARevertCanBeTakenBackInTheTestSetsHistory() {
        final @NotNull JBTable table = theReviewTable();
        revertFromTheMenuOn(table, rowNamed(table, "typed over the first"));
        assertEquals("Log in with a valid user", onDisk(first));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertTrue("the test set's history holds no revert", Services.getInstance(getProject(), UndoHistories.class).undo(UndoScope.of(login.getPath())));

        Await.until("undo did not put back what the revert threw away", () -> onDisk(first).equals("typed over the first"));
    }
}
