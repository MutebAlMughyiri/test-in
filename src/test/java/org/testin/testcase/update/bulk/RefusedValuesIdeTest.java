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

package org.testin.testcase.update.bulk;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.toolbar.GridViewButton;
import org.testin.indexer.TestCases;
import org.testin.model.Priority;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Done;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.testcase.TestSetEditorFields;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RefusedValuesIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestSetNode testSet = new TestSetNode();

    private static @NotNull JBTable theGridOf(final @NotNull TestSetEditor editor) {
        editor.getToolBar().getToolbarItem(GridViewButton.class).doClick();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        return (JBTable) editor.getPreferredFocusedComponent();
    }

    private static void typed(final @NotNull JBTable grid, final int row, final @NotNull TestSetEditorFields field, final @NotNull String value) {
        grid.getModel().setValueAt(value, row, field.column());
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private static @NotNull String unreadable(final @NotNull String what) {
        return Refused.UNREADABLE.about(what);
    }

    @Override
    protected void tearDown() {
        for (final Editor open : EditorFactory.getInstance().getAllEditors()) {
            if (!open.isDisposed()) EditorFactory.getInstance().releaseEditor(open);
        }
        super.tearDown();
    }

    private @NotNull List<TestCase> aTestSetHolding(final @NotNull String... descriptions) {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        testSet = EditorFixtures.testSet(getProject(), testProject, "Checkout");
        final @NotNull List<TestCase> made = new ArrayList<>();
        for (int i = 0; i < descriptions.length; i++) {
            final @NotNull TestCase testCase = TestCase.builder().uuid(UUID.randomUUID()).description(descriptions[i]).order(String.format("m%04d", i))
                    .priority(Priority.MEDIUM).groups(new ArrayList<>(List.of("Smoke"))).build();
            testCase.setParent(testSet);
            Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(testSet.getPath(), testCase);
            made.add(testCase);
        }
        return made;
    }

    private @NotNull TestCase stored(final @NotNull TestCase testCase) {
        return Services.getInstance(getProject(), TestCases.class).findTestCase(testCase.getUuid()).orElseThrow();
    }

    // Rule-EDITOR-PANEL-206
    public void testAValueTestinCannotReadIsRefusedAndTheTesterToldOnceForACell() {
        final @NotNull List<TestCase> testCases = aTestSetHolding("Log in", "Log out");
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            final @NotNull JBTable grid = theGridOf(editor);

            typed(grid, 0, TestSetEditorFields.PRIORITY, "Urgent");

            assertEquals("the cell kept a value Testin could not read", Priority.MEDIUM.getLabel(), grid.getModel().getValueAt(0, TestSetEditorFields.PRIORITY.column()));
            assertEquals("what the test case had did not stay", Priority.MEDIUM, stored(testCases.getFirst()).getPriority());
            assertEquals("the tester was not told once", List.of(unreadable(Bundle.message("grid.refused.as", "Urgent", TestSetEditorFields.PRIORITY.getName()))), balloons);
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-206
    public void testABulkEditRefusesWhatItCannotReadSaysSoOnceWithACountAndDoesNotCountIt() {
        final @NotNull List<TestCase> testCases = aTestSetHolding("Log in", "Log out", "Pay by card");
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        final @NotNull PriorityBulkSectionDialog dialog = new PriorityBulkSectionDialog(getProject(), testCases, _ -> {
        });

        final @NotNull List<TestCase> written = dialog.applyValues(testCases, List.of(EditedValue.of("Urgent"), EditedValue.of("Soon"), EditedValue.of(Priority.HIGH.getLabel())));

        assertEquals("a refused test case was counted among the ones the change touched", List.of(testCases.get(2).getUuid()), written.stream().map(TestCase::getUuid).toList());
        assertEquals(Priority.HIGH, written.getFirst().getPriority());
        assertEquals("the bulk edit did not say once, with a count", List.of(unreadable(Bundle.message("field.value.many", "2"))), balloons);
    }

    // Rule-EDITOR-PANEL-206
    public void testBlankIsNotUnreadableItClearsTheGroupsAndABulkEditGivesTheDefaultPriority() {
        final @NotNull List<TestCase> testCases = aTestSetHolding("Log in", "Log out");
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            final @NotNull JBTable grid = theGridOf(editor);

            typed(grid, 0, TestSetEditorFields.PRIORITY, "");
            typed(grid, 1, TestSetEditorFields.GROUP, "");

            Await.until("a blank group was not written", () -> stored(testCases.get(1)).getGroups().isEmpty());
            assertEquals("a blank priority in a cell changed the priority", Priority.MEDIUM, stored(testCases.getFirst()).getPriority());
            assertTrue("a blank value was called unreadable: " + balloons, balloons.stream().noneMatch(said -> said.startsWith(unreadable("").substring(0, 14))));

            balloons.clear();
            final @NotNull List<TestCase> written = new PriorityBulkSectionDialog(getProject(), testCases, _ -> {
            }).applyValues(testCases, List.of(EditedValue.of(""), EditedValue.UNCHANGED));
            assertEquals("a blank priority in a bulk edit did not set the default priority", List.of(Priority.LOW), written.stream().map(TestCase::getPriority).toList());
            assertTrue("a blank priority in a bulk edit was called unreadable: " + balloons, balloons.stream().noneMatch(said -> said.startsWith(unreadable("").substring(0, 14))));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-224
    public void testADescriptionThatCannotNameAMethodOrNamesATakenOneIsRefusedInTheUpdateDialogsWords() {
        final @NotNull List<TestCase> testCases = aTestSetHolding("Log in", "Log out", "Pay by card", "Check out");
        final @NotNull List<TestCase> inTheDialog = testCases.subList(0, 3);
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        final @NotNull DescriptionBulkSectionDialog dialog = new DescriptionBulkSectionDialog(getProject(), inTheDialog, _ -> {
        });

        final @NotNull List<TestCase> written = dialog.applyValues(inTheDialog, List.of(EditedValue.of("123 go"), EditedValue.of("Check out!"), EditedValue.of("Pay by card again")));

        assertEquals("a refused description was written", List.of(testCases.get(2).getUuid()), written.stream().map(TestCase::getUuid).toList());
        assertTrue("the message does not say the description cannot name a method: " + balloons, balloons.stream().anyMatch(said -> said.startsWith(Bundle.message("description.not.a.method.title"))));
        assertTrue("the message does not say another test case names that method: " + balloons, balloons.stream().anyMatch(said -> said.startsWith(Bundle.message("description.taken.title"))));
    }

    // Rule-EDITOR-PANEL-224
    public void testTwoRowsGivenOneDescriptionClashButTwoThatSwapDoNot() {
        final @NotNull List<TestCase> testCases = aTestSetHolding("Log in", "Log out");
        final @NotNull DescriptionBulkSectionDialog dialog = new DescriptionBulkSectionDialog(getProject(), testCases, _ -> {
        });

        final @NotNull List<TestCase> swapped = dialog.applyValues(testCases, List.of(EditedValue.of("Log out"), EditedValue.of("Log in")));
        assertEquals("two rows that swap their descriptions were refused", 2, swapped.size());

        final @NotNull List<TestCase> same = dialog.applyValues(testCases, List.of(EditedValue.of("Sign in"), EditedValue.of("Sign in")));
        assertEquals("two rows given one description did not clash", 1, same.size());
    }

    // Rule-EDITOR-PANEL-206
    public void testARefusedTestCaseIsNotCountedInTheUpdatedMessage() {
        final @NotNull List<TestCase> testCases = aTestSetHolding("Log in", "Log out");
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            final @NotNull JBTable grid = theGridOf(editor);
            typed(grid, 0, TestSetEditorFields.PRIORITY, "Urgent");
            assertFalse("a refused cell said it updated something: " + balloons, balloons.contains(Done.UPDATED.getOutcome()));
            assertEquals(Priority.MEDIUM, stored(testCases.getFirst()).getPriority());
        } finally {
            Disposer.dispose(editor);
        }
    }
}
