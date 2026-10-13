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

import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.util.Bundle;
import org.testin.view.AbstractViewPanelIdeTest;
import org.testin.view.BrowserOpened;
import org.testin.view.Drawn;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Optional;

public class BugOnTheSummaryLineIdeTest extends AbstractViewPanelIdeTest {

    private static final @NotNull String ISSUE = "https://github.com/mtb550/test-in/issues/412";

    private TestCaseDto testCase;
    private TestRunNode testRun;

    private static @NotNull RunItem recorded(final @NotNull TestCaseDto testCase, final @NotNull RunItemStatus status, final @NotNull String bugIssueUrl) {
        return RunItem.builder().id(testCase.getId()).bugSeverity(BugSeverity.MAJOR).bugPriority(BugPriority.HIGH).bugIssueUrl(bugIssueUrl).executions(List.of(RunItemExecution.builder().status(status).build())).build();
    }

    private static @NotNull Component rowOf(final @NotNull Component drawn) {
        Component row = drawn;
        while (!(row.getParent().getLayout() instanceof GridBagLayout)) row = row.getParent();
        return row;
    }

    @Override
    protected void setUp() {
        super.setUp();
        testCase = aTestCase(aTestSet("Login"), "Log in with a valid user", "a");
        testRun = aTestRun(List.of(RunItem.builder().id(testCase.getId()).build()));
    }

    private @NotNull JBPanel<?> drawn(final @NotNull RunItem runItem) {
        return Drawn.detailsTab(getProject(), testCase, Optional.of(runItem), testRun.getPath2());
    }

    // Rule-VIEW-PANEL-066
    public void testAFailedTestCaseCarriesItsBugAndReportBugOnTheSummaryLine() {
        final @NotNull JBPanel<?> tab = drawn(recorded(testCase, RunItemStatus.FAILED, ""));

        final @NotNull Component status = Drawn.reading(tab, RunItemStatus.FAILED.getLabel());
        final @NotNull Component report = Drawn.reading(tab, Bundle.message("bug.dialog.title"));
        assertTrue("Report Bug is not a link", report instanceof ActionLink);
        assertSame("Report Bug is not on the summary line", rowOf(status), rowOf(report));
        assertTrue("the bug's chip is not on the summary line", Drawn.words(tab).stream().anyMatch(word -> word.startsWith(BugSeverity.MAJOR.getLabel())));
    }

    // Rule-VIEW-PANEL-066
    public void testTheBugStaysWhileTheTestCaseHasAnIssueWhateverItsRunItemStatus() {
        for (final RunItemStatus status : List.of(RunItemStatus.PASSED, RunItemStatus.BLOCKED, RunItemStatus.FAILED)) {
            final @NotNull JBPanel<?> tab = drawn(recorded(testCase, status, ISSUE));
            final @NotNull Component issue = Drawn.reading(tab, "#412");
            assertSame("the issue of a " + status.getLabel() + " test case is not on the summary line", rowOf(Drawn.reading(tab, status.getLabel())), rowOf(issue));
        }
        assertFalse("a passed test case with no issue still carries a bug", Drawn.words(drawn(recorded(testCase, RunItemStatus.PASSED, ""))).contains(Bundle.message("bug.dialog.title")));
    }

    // Rule-VIEW-PANEL-075
    public void testAReportedTestCaseShowsItsIssueAsANumberThatOpensIt() {
        final @NotNull BrowserOpened browser = BrowserOpened.recording(getTestRootDisposable());
        final @NotNull JBPanel<?> tab = drawn(recorded(testCase, RunItemStatus.FAILED, ISSUE));

        final @NotNull ActionLink issue = (ActionLink) Drawn.reading(tab, "#412");
        assertTrue("hovering the issue does not give its whole address: " + issue.getToolTipText(), String.valueOf(issue.getToolTipText()).contains(ISSUE));
        assertFalse("the repository is repeated on the row: " + Drawn.words(tab), Drawn.holds(Drawn.words(tab), "mtb550/test-in"));
        assertFalse("Report Bug is offered beside the issue it was reported as: " + Drawn.words(tab), Drawn.words(tab).contains(Bundle.message("bug.dialog.title")));

        issue.doClick();
        assertEquals("clicking the issue did not open it in the browser", List.of(ISSUE), browser.addresses());

        final @NotNull ActionLink listed = (ActionLink) Drawn.reading(BugIssueLink.of(getProject(), ISSUE), "#412");
        assertTrue("hovering the issue on a bug card does not give its whole address: " + listed.getToolTipText(), String.valueOf(listed.getToolTipText()).contains(ISSUE));

        listed.doClick();
        assertEquals("clicking the issue on a bug card did not open it in the browser", List.of(ISSUE, ISSUE), browser.addresses());
    }
}
