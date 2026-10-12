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
package org.testin.view;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.Gestures;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.Failure;
import org.testin.services.Services;
import org.testin.testrun.TestRunFixture;
import org.testin.util.Bundle;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.testin.view.Drawn.holds;

public class PanelShowsOneTestCaseIdeTest extends AbstractViewPanelIdeTest {

    private static final @NotNull String RUN_BAND = Bundle.message("details.band.run").toUpperCase(Locale.ROOT);

    private static @NotNull TestCaseDto handedCopy(final @NotNull TestCaseDto tc, final @NotNull String description) {
        final @NotNull TestCaseDto copy = TestCaseDto.builder().id(tc.getId()).description(description).build();
        copy.setParent(tc.getParent());
        return copy;
    }

    private @NotNull List<TestCaseDto> threeTestCases() {
        final @NotNull TestSetNode ts = aTestSet("Login");
        return List.of(aTestCase(ts, "Log in with a valid user", "a"), aTestCase(ts, "Log in with a locked user", "b"), aTestCase(ts, "Log in with no password", "c"));
    }

    private @NotNull List<String> pathOf(final @NotNull TestRunNode tr) {
        return tr.getPath2();
    }

    // Rule-VIEW-PANEL-002
    public void testThePanelDrawsTheFirstOfTheTestCasesItWasHandedAndNoOther() {
        final @NotNull List<TestCaseDto> handed = threeTestCases();

        view.getPanel().show(handed, handed.getFirst().getParent().getPath2());

        final @NotNull List<String> words = details();
        assertTrue("the first test case was not drawn: " + words, holds(words, "Log in with a valid user"));
        assertFalse("a second test case was drawn beside the first: " + words, holds(words, "Log in with a locked user"));
        assertFalse("a third test case was drawn beside the first: " + words, holds(words, "Log in with no password"));
    }

    // Rule-VIEW-PANEL-003
    public void testNothingButAskingOpensThePanel() {
        final @NotNull List<TestCaseDto> handed = threeTestCases();
        final @NotNull List<String> path = handed.getFirst().getParent().getPath2();

        view.getPanel().showIfOpen(handed, path);
        ViewToolWindowFactory.refreshIfShowing(getProject(), handed);
        view.getPanel().refreshCurrentView();
        settled();
        assertFalse("the panel opened without the tester asking for a test case's details", view.isOpen());

        view.getPanel().show(handed, path);
        assertTrue("asking for a test case's details did not open the panel", view.isOpen());
    }

    // Rule-VIEW-PANEL-004
    public void testAnOpenPanelFollowsAndAClosedOneStaysClosedUntilAskedAgain() {
        final @NotNull List<TestCaseDto> handed = threeTestCases();
        final @NotNull List<String> path = handed.getFirst().getParent().getPath2();

        view.getPanel().show(List.of(handed.getFirst()), path);
        view.getPanel().showIfOpen(List.of(handed.get(1)), path);
        assertTrue("the open panel did not follow the tester to the next test case: " + details(), holds(details(), "Log in with a locked user"));

        view.closedByTheTester();
        view.getPanel().showIfOpen(List.of(handed.get(2)), path);
        assertFalse("the closed panel came back when the tester moved on", view.isOpen());

        view.getPanel().show(List.of(handed.get(2)), path);
        assertTrue("the tester asked again and the panel stayed closed", view.isOpen());
    }

    // Rule-VIEW-PANEL-005
    public void testThePanelDrawsWhatTestinHoldsNotTheCopyItWasHanded() {
        final @NotNull TestCaseDto tc = threeTestCases().getFirst();

        view.getPanel().show(List.of(handedCopy(tc, "The copy the editor handed over")), tc.getParent().getPath2());
        assertTrue("the panel drew the copy it was handed rather than what Testin holds: " + details(), holds(details(), "Log in with a valid user"));

        Services.getInstance(getProject(), TestCases.class).putTestCase(tc.getParent().getPath(), handedCopy(tc, "Sign in with a valid user"));
        ViewToolWindowFactory.refreshIfShowing(getProject(), List.of(tc));

        assertTrue("a value changed somewhere else did not reach the panel: " + details(), holds(details(), "Sign in with a valid user"));
    }

    // Rule-VIEW-PANEL-007
    public void testOpeningPagingAndClosingSayNothing() {
        final @NotNull List<TestCaseDto> handed = threeTestCases();
        final @NotNull List<String> path = handed.getFirst().getParent().getPath2();

        final @NotNull List<String> said = Said.during(getProject(), () -> {
            view.getPanel().show(handed, path);
            view.getPanel().getPage().goNext();
            settled();
            view.getPanel().getPage().goPrevious();
            settled();
            view.getPanel().hide();
            view.getPanel().show(handed, path);
            view.getPanel().hide(path);
        });

        assertEquals("opening, paging or closing the panel said something", List.of(), said);
    }

    // Rule-VIEW-PANEL-008
    public void testEveryRefreshDrawsBothTabs() {
        final @NotNull TestCaseDto tc = threeTestCases().getFirst();

        view.getPanel().show(List.of(tc), tc.getParent().getPath2());
        assertTrue("the Details tab was not drawn: " + details(), holds(details(), "Log in with a valid user"));
        Await.until("the History tab was not drawn: " + view.words(ViewTab.HISTORY), () -> view.words(ViewTab.HISTORY).equals(List.of(Bundle.message("view.history.not.under.git"))));

        view.getPanel().reset();
        assertEquals("the Details tab was not drawn again", List.of(Bundle.message("details.placeholder")), details());
        assertEquals("the History tab still shows a test case the panel no longer shows", List.of(), view.words(ViewTab.HISTORY));
    }

    // Rule-VIEW-PANEL-012
    public void testThePanelOpensOnDetailsWhicheverTabWasInFront() {
        final @NotNull TestCaseDto tc = threeTestCases().getFirst();
        view.bringToFront(ViewTab.HISTORY);
        assertEquals(ViewTab.HISTORY.getDisplayName(), view.tabInFront());

        view.getPanel().show(List.of(tc), tc.getParent().getPath2());

        assertEquals("the panel opened on the tab that was in front last time", ViewTab.DETAILS.getDisplayName(), view.tabInFront());
    }

    // Rule-VIEW-PANEL-014
    public void testTheFolderThePanelWasOpenedFromDecidesWhetherTheTestRunIsDrawn() {
        final @NotNull TestCaseDto tc = threeTestCases().getFirst();
        final @NotNull TestRunNode tr = aTestRun(List.of(RunItem.builder().id(tc.getId()).executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build())).build()));

        view.getPanel().show(List.of(tc), pathOf(tr));
        assertTrue("opened from the test run, the panel drew no test run band: " + details(), details().contains(RUN_BAND));

        view.getPanel().show(List.of(tc), tc.getParent().getPath2());
        assertFalse("opened from the test set, the panel drew the test run band: " + details(), details().contains(RUN_BAND));
    }

    // Rule-VIEW-PANEL-018
    public void testEveryFillStartsAgainAtTheFirstTestCase() {
        final @NotNull List<TestCaseDto> first = threeTestCases();
        final @NotNull TestSetNode other = aTestSet("Logout");
        final @NotNull List<TestCaseDto> second = List.of(aTestCase(other, "Log out from the menu", "a"), aTestCase(other, "Log out by closing the tab", "b"), aTestCase(other, "Log out after a timeout", "c"));

        view.getPanel().show(first, first.getFirst().getParent().getPath2());
        view.getPanel().getPage().goNext();
        view.getPanel().getPage().goNext();
        settled();
        assertTrue("paging did not reach the third test case: " + details(), holds(details(), "Log in with no password"));

        view.getPanel().showIfOpen(second, other.getPath2());

        assertTrue("the new fill did not start at its first test case: " + details(), holds(details(), "Log out from the menu"));
        assertFalse("the paging position was carried over into the new fill", view.getPanel().getPage().hasPrevious());
    }

    // Rule-VIEW-PANEL-029
    public void testTheTestRunBandIsDrawnOnlyWhenTheTestRunHoldsTheTestCase() {
        final @NotNull List<TestCaseDto> handed = threeTestCases();
        final @NotNull TestRunNode tr = aTestRun(List.of(RunItem.builder().id(handed.getFirst().getId()).executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build())).build()));

        view.getPanel().show(List.of(handed.get(1)), pathOf(tr));
        assertFalse("the test run band was drawn for a test case the test run does not hold: " + details(), details().contains(RUN_BAND));
        assertTrue("the test case the test run does not hold was not drawn on its own: " + details(), holds(details(), "Log in with a locked user"));

        view.getPanel().show(List.of(handed.getFirst()), pathOf(tr));
        assertTrue("the test run band was not drawn for a test case the test run holds: " + details(), details().contains(RUN_BAND));
    }

    // Rule-VIEW-PANEL-030
    public void testTheTestRunBandIsReadFromTheTestRunItself() {
        final @NotNull TestCaseDto tc = threeTestCases().getFirst();
        final @NotNull TestRunNode tr = aTestRun(List.of(EditorFixtures.pending(tc)));
        view.getPanel().show(List.of(tc), pathOf(tr));
        assertTrue(details().contains(RunItemStatus.PENDING.getLabel()));

        final @NotNull TestRuns testRuns = Services.getInstance(getProject(), TestRuns.class);
        testRuns.changeRunItem(tr.getPath(), tc.getId(), result -> {
            result.setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build()));
            result.recordActualResult("The session was dropped");
        });
        view.getPanel().refreshIfShowing(List.of(tc));

        final @NotNull RunItem recorded = testRuns.findRunItems(tr.getPath()).flatMap(run -> run.runItemOf(tc.getId())).orElseThrow();
        assertTrue("the band does not show the run item status the test run holds: " + details(), details().contains(recorded.shownStatus().getLabel()));
        assertTrue("the band does not show the actual result the test run holds: " + details(), holds(details(), recorded.getActualResult()));
        assertFalse("the band still shows what it drew before the test run changed: " + details(), details().contains(RunItemStatus.PENDING.getLabel()));
    }

    // Rule-VIEW-PANEL-005
    public void testAStatusSetInTheEditorShowsInThePanelAtOnce() {
        final @NotNull TestCaseDto tc = threeTestCases().getFirst();
        final @NotNull TestRunNode tr = aTestRun(List.of(EditorFixtures.pending(tc)));
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), tr, getTestRootDisposable());
        try {
            editor.getList().setSelectedIndex(0);
            view.getPanel().show(List.of(tc), pathOf(tr));
            assertTrue(details().contains(RunItemStatus.PENDING.getLabel()));

            Gestures.press(getProject(), TestRunFixture.keyFor(editor, RunItemStatus.PASSED), editor.getList());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertTrue("the status just set did not reach the panel: " + details(), holds(details(), RunItemStatus.PASSED.getLabel()));
            assertFalse("the panel still shows the status from before: " + details(), details().contains(RunItemStatus.PENDING.getLabel()));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-VIEW-PANEL-083
    public void testOpenedFromATestRunThePanelShowsTheTestCaseAsTheRowShowsIt() {
        final @NotNull TestCaseDto tc = threeTestCases().getFirst();
        final @NotNull RunItem row = EditorFixtures.pending(tc);
        row.recordRunItemStatus(RunItemStatus.FAILED, "muteb", Duration.ZERO, Failure.NONE);
        final @NotNull TestRunNode tr = aTestRun(List.of(row));

        view.getPanel().show(List.of(handedCopy(tc, "The copy the editor handed over")), pathOf(tr));
        assertTrue("the panel did not show the test case as the test run's run item shows it: " + details(), holds(details(), tc.getDescription()));
        assertFalse("the panel showed the copy it was handed: " + details(), holds(details(), "The copy the editor handed over"));

        Services.getInstance(getProject(), TestRuns.class).changeRunItem(tr.getPath(), tc.getId(), result -> result.recordRunItemStatus(RunItemStatus.PASSED, "muteb", Duration.ZERO, Failure.NONE));
        view.getPanel().refreshCurrentView();

        assertTrue("the panel did not read the run item again when it refreshed: " + details(), holds(details(), RunItemStatus.PASSED.getLabel()));
    }

    // Rule-VIEW-PANEL-112, Rule-EDITOR-PANEL-239
    public void testFromACommittedTestRunTheDetailsShowTheCommitAndTheHistoryTheTestCaseNow() {
        final @NotNull TestCaseDto tc = threeTestCases().getFirst();
        final @NotNull TestRunNode tr = aTestRun(List.of(EditorFixtures.pending(tc)));
        final @NotNull TestRuns testRuns = Services.getInstance(getProject(), TestRuns.class);
        testRuns.changeTestRunMarker(tr.getPath(), marker -> marker.recordCommit("ea9a501"));
        testRuns.rememberRecorded("ea9a501", Map.of(tc.getId(), Optional.of(handedCopy(tc, "As the commit recorded it"))));

        view.getPanel().show(List.of(tc), pathOf(tr));

        assertEquals("the Details tab does not show the commit's text", "As the commit recorded it", view.getPanel().shownTestCase().orElseThrow().getDescription());
        assertEquals("the History tab would compare the commit's text with the last commit", tc.getDescription(), view.getPanel().liveTestCase().orElseThrow().getDescription());
    }
}
