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

package org.testin.bug;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.Notification;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.git.history.BugCard;
import org.testin.git.history.BugHistory;
import org.testin.git.history.History;
import org.testin.help.Hint;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.indexer.TestCaseFile;
import org.testin.indexer.TestRuns;
import org.testin.model.testrun.RunItems;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.view.Drawn;
import org.testin.view.details.BugIssueLink;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class BugIssueStatesIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull UUID TEST_CASE_ID = UUID.fromString("66666666-6666-4666-8666-666666666693");
    private static final @NotNull String FIXED = "https://github.com/mtb550/test-03/issues/9";
    private static final @NotNull String OPEN = "https://github.com/mtb550/test-03/issues/14";
    private static final @NotNull String ANSWER = """
            {"data":{"repository":{"i9":{"state":"CLOSED","stateReason":"COMPLETED"},"i14":{"state":"OPEN","stateReason":null}}}}""";
    private static final @NotNull String NO_PROJECT_SCOPE = """
            {"errors":[{"type":"INSUFFICIENT_SCOPES","message":"The 'title' field requires one of the following scopes: ['read:project']"}]}""";

    private final @NotNull List<List<String>> asked = new ArrayList<>();

    private @NotNull BugIssueStates states() {
        return Services.getInstance(getProject(), BugIssueStates.class);
    }

    private void twoBugsFiled() {
        filed("Cycle 3", FIXED);
        filed("Cycle 4", OPEN);
    }

    private void filed(final @NotNull String testRun, final @NotNull String bugIssueUrl) {
        final @NotNull RunItem failed = RunItem.builder().id(TEST_CASE_ID).status(RunItemStatus.FAILED).bugIssueUrl(bugIssueUrl).build();
        Services.getInstance(getProject(), TestRuns.class).putRunItems(root.resolve("NAFATH").resolve("Test Runs").resolve(testRun), RunItems.builder().all(new ArrayList<>(List.of(failed))).build());
    }

    private void ghAnswers(final @NotNull String withBoards) {
        states().answerWith(new GitHubCli((arguments, _) -> {
            asked.add(arguments);
            return Optional.of(new ProcessOutput(arguments.contains("field=Status") ? withBoards : ANSWER, "", 0, false, false));
        }), getTestRootDisposable());
    }

    private @NotNull List<List<String>> askedAboutTest03() {
        return asked.stream().filter(arguments -> arguments.contains("name=test-03")).toList();
    }

    private int readAndWait() {
        final @NotNull AtomicInteger redrawn = new AtomicInteger();
        states().readAll(redrawn::incrementAndGet);
        Await.until("the bug states were never read", () -> !states().isReading());
        return redrawn.get();
    }

    // Rule-VIEW-PANEL-092
    public void testAnAnswerThatDidNotChangeRedrawsNothing() {
        nothingWaits();
        twoBugsFiled();
        ghAnswers(ANSWER);

        assertEquals("the first answer was not drawn", 1, readAndWait());
        assertEquals("an answer that changed nothing redrew the panel", 0, readAndWait());
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-091, Rule-VIEW-PANEL-092, Rule-VIEW-PANEL-095
    public void testEveryFiledBugGetsItsStateFromOneRequestAndAFixedOneStaysListed() {
        twoBugsFiled();
        ghAnswers(ANSWER);

        readAndWait();

        assertEquals("one repository is one request", 1, askedAboutTest03().size());
        assertEquals(Bundle.message("bug.state.fixed"), states().of(FIXED).label());
        assertEquals(Bundle.message("bug.state.open"), states().of(OPEN).label());

        final @NotNull List<BugCard> bugs = BugHistory.addTo(History.NOT_UNDER_GIT, getProject(), new TestCaseFile(root.resolve("NAFATH"), Path.of("Test Cases", "Login", TEST_CASE_ID + ".tc")), TEST_CASE_ID, BugHistory.runItemsNow(getProject(), TEST_CASE_ID)).bugs();
        assertEquals("the fixed bug left the history", 2, bugs.size());

        final @NotNull List<String> fixed = Drawn.words(BugIssueLink.of(getProject(), FIXED));
        assertTrue("the fixed bug's pill is missing: " + fixed, Drawn.holds(fixed, "#9") && Drawn.holds(fixed, Bundle.message("bug.state.fixed")));
        final @NotNull List<String> open = Drawn.words(BugIssueLink.of(getProject(), OPEN));
        assertTrue("the open bug shows no pill: " + open, Drawn.holds(open, "#14") && Drawn.holds(open, Bundle.message("bug.state.open")));
    }

    private @NotNull List<String> hintsFor(final @NotNull SetupStep step) {
        return Services.getInstance(getProject(), Hints.class).waiting().stream().filter(hint -> hint.step() == step).map(Hint::text).toList();
    }

    private void nothingWaits() {
        final @NotNull Hints hints = Services.getInstance(getProject(), Hints.class);
        Arrays.stream(SetupStep.values()).forEach(hints::clear);
    }

    // Rule-VIEW-PANEL-094, Rule-INTERNAL-127
    public void testABoardTheTokenCannotReadFallsBackToTheIssueStateAndWaitsAsOneHintUntilRead() {
        nothingWaits();
        twoBugsFiled();
        ghAnswers(NO_PROJECT_SCOPE);
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        readAndWait();
        readAndWait();

        assertEquals(Bundle.message("bug.state.fixed"), states().of(FIXED).label());
        assertEquals("a message was raised for the board", List.of(), said.stream().map(Notification::getTitle).toList());
        assertEquals("the board did not wait as one hint", 1, hintsFor(SetupStep.BOARD_COLUMNS).size());
        assertEquals("Refresh did not ask for the board again", 2, askedAboutTest03().stream().filter(arguments -> arguments.contains("field=Status")).count());

        ghAnswers(ANSWER);
        readAndWait();
        assertEquals("a board read left its hint waiting", List.of(), hintsFor(SetupStep.BOARD_COLUMNS));
    }

    // Rule-VIEW-PANEL-093, Rule-INTERNAL-127
    public void testAGhThatCannotBeStartedLeavesTheLinksAloneAndWaitsAsOneHint() {
        nothingWaits();
        twoBugsFiled();
        states().answerWith(new GitHubCli((_, _) -> Optional.empty()), getTestRootDisposable());
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        readAndWait();

        assertEquals(BugIssueState.NOT_READ, states().of(OPEN));
        assertEquals("a message was raised for the bug states", List.of(), said.stream().map(Notification::getTitle).toList());
        assertEquals(List.of(Bundle.message("bug.reason.no.gh")), hintsFor(SetupStep.BUG_STATES));
        nothingWaits();
    }

    // Rule-VIEW-PANEL-093, Rule-INTERNAL-127
    public void testARepositoryThatCannotBeReadKeepsItsHintWhileAnotherIsRead() {
        nothingWaits();
        twoBugsFiled();
        filed("Cycle 5", "https://github.com/mtb550/elsewhere/issues/3");
        states().answerWith(new GitHubCli((arguments, _) -> Optional.of(arguments.contains("name=elsewhere")
                ? new ProcessOutput("", "Could not resolve to a Repository with the name 'mtb550/elsewhere'.", 1, false, false)
                : new ProcessOutput(ANSWER, "", 0, false, false))), getTestRootDisposable());

        readAndWait();

        assertEquals(Bundle.message("bug.state.fixed"), states().of(FIXED).label());
        assertEquals("a repository read fine cleared the hint another could not be read for", 1, hintsFor(SetupStep.BUG_STATES).size());
        nothingWaits();
    }

    // Rule-VIEW-PANEL-093, Rule-INTERNAL-127
    public void testNoBugLinkLeftClearsTheHint() {
        nothingWaits();
        twoBugsFiled();
        states().answerWith(new GitHubCli((_, _) -> Optional.empty()), getTestRootDisposable());
        readAndWait();
        assertEquals(1, hintsFor(SetupStep.BUG_STATES).size());

        Services.getInstance(getProject(), TestRuns.class).getAllRunItems().keySet().forEach(testRun -> Services.getInstance(getProject(), TestRuns.class).putRunItems(testRun, RunItems.builder().all(new ArrayList<>()).build()));
        readAndWait();

        assertEquals("the hint waited on bug links nobody has any more", List.of(), hintsFor(SetupStep.BUG_STATES));
    }
}
