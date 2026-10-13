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

import org.jetbrains.annotations.NotNull;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestRunNode;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.services.Services;
import org.testin.testrun.RunItemRecorder;
import org.testin.util.Bundle;
import org.testin.view.AbstractViewPanelIdeTest;
import org.testin.view.Drawn;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class ExecutionLinesIdeTest extends AbstractViewPanelIdeTest {

    private TestCaseDto testCase;

    private static @NotNull RunItemExecution execution(final @NotNull RunItemStatus status, final int invocation, final int retry, final boolean retried, final @NotNull String actual) {
        return RunItemExecution.builder().status(status).invocation(invocation).retry(retry).retried(retried).actualResult(actual).build();
    }

    private @NotNull RunItem invokedThreeTimes() {
        return RunItem.builder().id(testCase.getId()).executions(List.of(
                execution(RunItemStatus.PASSED, 1, 0, false, ""),
                execution(RunItemStatus.FAILED, 2, 0, true, "The session ended early"),
                execution(RunItemStatus.PASSED, 2, 1, false, ""),
                execution(RunItemStatus.PASSED, 3, 0, false, "Stayed signed in"))).build();
    }

    private @NotNull TestRuns theTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull RunItemRecorder runItemStatuses() {
        return Services.getInstance(getProject(), RunItemRecorder.class);
    }

    @Override
    protected void setUp() {
        super.setUp();
        testCase = aTestCase(aTestSet("Login"), "Keep the session for 5 minutes in the background", "a");
    }

    // Rule-VIEW-PANEL-123
    public void testARunItemExecutedOnceReadsAsBefore() {
        final @NotNull TestRunNode testRun = aTestRun(List.of(RunItem.builder().id(testCase.getId()).build()));
        final @NotNull RunItem once = RunItem.builder().id(testCase.getId()).executions(List.of(execution(RunItemStatus.PASSED, 0, 0, false, ""))).build();

        final @NotNull List<String> words = Drawn.words(Drawn.detailsTab(getProject(), testCase, Optional.of(once), testRun.treePath()));

        assertTrue("the run item status is not shown: " + words, Drawn.holds(words, RunItemStatus.PASSED.getLabel()));
        assertFalse("a run item executed once says what its execution was: " + words, Drawn.holds(words, Bundle.message("execution.invocation", "1")));
    }

    // Rule-VIEW-PANEL-086, Rule-CODEGEN-112
    public void testEachExecutionHasALineThatSaysWhatItWas() {
        final @NotNull TestRunNode testRun = aTestRun(List.of(RunItem.builder().id(testCase.getId()).build()));

        final @NotNull List<String> words = Drawn.words(Drawn.detailsTab(getProject(), testCase, Optional.of(invokedThreeTimes()), testRun.treePath()));

        assertTrue(words.toString(), Drawn.holds(words, Bundle.message("execution.invocation", "1")));
        assertTrue(words.toString(), Drawn.holds(words, Bundle.message("execution.invocation", "2") + " " + Bundle.message("execution.retried")));
        assertTrue(words.toString(), Drawn.holds(words, Bundle.message("execution.retry", "1")));
        assertTrue(words.toString(), Drawn.holds(words, Bundle.message("execution.invocation", "3")));
    }

    // Rule-VIEW-PANEL-122
    public void testTheRowsBelowShowTheExecutionClickedOrElseTheDefault() {
        final @NotNull TestRunNode testRun = aTestRun(List.of(RunItem.builder().id(testCase.getId()).build()));

        final @NotNull List<String> byDefault = Drawn.words(Drawn.detailsTab(getProject(), testCase, Optional.of(invokedThreeTimes()), testRun.treePath()));
        final @NotNull List<String> clicked = Drawn.words(Drawn.detailsTab(getProject(), testCase, Optional.of(invokedThreeTimes()), testRun.treePath(), OptionalInt.of(1)));

        assertTrue("the default's actual result is not shown: " + byDefault, Drawn.holds(byDefault, "Stayed signed in"));
        assertTrue("the clicked execution's actual result is not shown: " + clicked, Drawn.holds(clicked, "The session ended early"));
        assertFalse("the default's actual result is still shown: " + clicked, Drawn.holds(clicked, "Stayed signed in"));
    }

    // Rule-VIEW-PANEL-120, Rule-VIEW-PANEL-121
    public void testTheStarMovesTheDefaultAndTheCrossFallsBackToTheLast() {
        final @NotNull TestRunNode testRun = aTestRun(List.of(invokedThreeTimes()));
        final @NotNull RunItem held = theTestRuns().getRunItems(testRun.getPath()).runItemOf(testCase.getId()).orElseThrow();

        assertTrue(runItemStatuses().chooseExecution(testRun.getPath(), testCase.getId(), 1));
        assertEquals("the chosen execution does not decide", RunItemStatus.FAILED, held.getStatus());

        assertTrue(runItemStatuses().removeExecution(testRun.getPath(), testCase.getId(), 1));
        assertEquals("with the chosen one gone, the last does not decide", RunItemStatus.PASSED, held.getStatus());
        assertEquals(3, held.getExecutions().size());
    }

    // Rule-VIEW-PANEL-121, Rule-TREE-PANEL-009
    public void testATestRunThatTakesNoMoreStatusesRefusesBoth() {
        final @NotNull TestRunNode testRun = aTestRun(List.of(invokedThreeTimes()));
        theTestRuns().changeTestRunMarker(testRun.getPath(), marker -> marker.setStatus(TestRunStatus.COMMITTED));

        assertFalse("a committed test run changed its default", runItemStatuses().chooseExecution(testRun.getPath(), testCase.getId(), 1));
        assertFalse("a committed test run lost an execution", runItemStatuses().removeExecution(testRun.getPath(), testCase.getId(), 0));
        assertEquals(4, theTestRuns().getRunItems(testRun.getPath()).runItemOf(testCase.getId()).orElseThrow().getExecutions().size());
    }
}
