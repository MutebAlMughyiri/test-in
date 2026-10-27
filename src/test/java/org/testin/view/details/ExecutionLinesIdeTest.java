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

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCase;
import org.testin.model.node.TestRunNode;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.RunItemExecution;
import org.testin.services.Services;
import org.testin.testrun.RunItemRecorder;
import org.testin.util.Bundle;
import org.testin.view.AbstractViewPanelIdeTest;
import org.testin.view.Drawn;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class ExecutionLinesIdeTest extends AbstractViewPanelIdeTest {

    private TestCase testCase;

    private static @NotNull RunItemExecution execution(final @NotNull RunItemStatus runItemExecutionStatus, final int invocation, final int retry, final @NotNull String actual) {
        return RunItemExecution.builder().status(runItemExecutionStatus).invocation(invocation).retry(retry).actualResult(actual).build();
    }

    private @NotNull RunItem invokedThreeTimes() {
        return RunItem.builder().uuid(testCase.getUuid()).executions(List.of(
                execution(RunItemStatus.PASSED, 1, 0, ""),
                execution(RunItemStatus.FAILED, 2, 0, "The session ended early"),
                execution(RunItemStatus.PASSED, 2, 1, ""),
                execution(RunItemStatus.PASSED, 3, 0, "Stayed signed in"))).defaultNumber(4).build();
    }

    private @NotNull TestRuns theTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull RunItemRecorder runItemStatuses() {
        return Services.getInstance(getProject(), RunItemRecorder.class);
    }

    private @NotNull JBPanel<?> detailsOf(final @NotNull TestRunNode testRun, final @NotNull RunItem runItem, final @NotNull List<OptionalInt> shown) {
        final @NotNull JBPanel<?> tab = new JBPanel<>();
        new DetailsTab().load(getProject(), tab, Optional.of(testCase), Optional.of(runItem), testRun.treePath(), OptionalInt.empty(), shown::add);
        return tab;
    }

    private static @NotNull List<JComponent> named(final @NotNull JComponent tab, final @NotNull String name) {
        return Drawn.components(tab).stream().filter(JBLabel.class::isInstance).map(JComponent.class::cast)
                .filter(part -> name.equals(part.getAccessibleContext().getAccessibleName())).toList();
    }

    private static void clicked(final @NotNull Component part) {
        final @NotNull MouseEvent click = new MouseEvent(part, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 1, 1, 1, false, MouseEvent.BUTTON1);
        for (final MouseListener listener : part.getMouseListeners()) listener.mouseClicked(click);
    }

    private static void entered(final @NotNull Component part) {
        final @NotNull MouseEvent over = new MouseEvent(part, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 1, 1, 0, false, MouseEvent.NOBUTTON);
        for (final MouseListener listener : part.getMouseListeners()) listener.mouseEntered(over);
    }

    private @NotNull RunItem heldIn(final @NotNull TestRunNode testRun) {
        return theTestRuns().getRunItems(testRun.getPath()).runItemOf(testCase.getUuid()).orElseThrow();
    }

    @Override
    protected void setUp() {
        super.setUp();
        testCase = aTestCase(aTestSet("Login"), "Keep the session for 5 minutes in the background", "a");
    }

    // Rule-VIEW-PANEL-123
    public void testARunItemExecutedOnceReadsAsBefore() {
        final @NotNull TestRunNode testRun = aTestRun(List.of(RunItem.builder().uuid(testCase.getUuid()).build()));
        final @NotNull RunItem once = RunItem.builder().uuid(testCase.getUuid()).executions(List.of(execution(RunItemStatus.PASSED, 0, 0, ""))).build();

        final @NotNull List<String> words = Drawn.words(Drawn.detailsTab(getProject(), testCase, Optional.of(once), testRun.treePath()));

        assertTrue("the run item status is not shown: " + words, Drawn.holds(words, RunItemStatus.PASSED.getLabel()));
        assertFalse("a run item executed once says what its execution was: " + words, Drawn.holds(words, Bundle.message("execution.invocation", "1")));
    }

    // Rule-VIEW-PANEL-086, Rule-CODEGEN-112
    public void testEachExecutionHasALineThatSaysWhatItWas() {
        final @NotNull TestRunNode testRun = aTestRun(List.of(RunItem.builder().uuid(testCase.getUuid()).build()));

        final @NotNull List<String> words = Drawn.words(Drawn.detailsTab(getProject(), testCase, Optional.of(invokedThreeTimes()), testRun.treePath()));

        assertTrue(words.toString(), Drawn.holds(words, Bundle.message("execution.invocation", "1")));
        assertTrue(words.toString(), Drawn.holds(words, Bundle.message("execution.invocation", "2") + " " + Bundle.message("execution.retried")));
        assertTrue(words.toString(), Drawn.holds(words, Bundle.message("execution.retry", "1")));
        assertTrue(words.toString(), Drawn.holds(words, Bundle.message("execution.invocation", "3")));
    }

    // Rule-VIEW-PANEL-122
    public void testTheRowsBelowShowTheExecutionClickedOrElseTheDefault() {
        final @NotNull TestRunNode testRun = aTestRun(List.of(RunItem.builder().uuid(testCase.getUuid()).build()));

        final @NotNull List<String> byDefault = Drawn.words(Drawn.detailsTab(getProject(), testCase, Optional.of(invokedThreeTimes()), testRun.treePath()));
        final @NotNull List<String> clicked = Drawn.words(Drawn.detailsTab(getProject(), testCase, Optional.of(invokedThreeTimes()), testRun.treePath(), OptionalInt.of(1)));

        assertTrue("the default's actual result is not shown: " + byDefault, Drawn.holds(byDefault, "Stayed signed in"));
        assertTrue("the clicked execution's actual result is not shown: " + clicked, Drawn.holds(clicked, "The session ended early"));
        assertFalse("the default's actual result is still shown: " + clicked, Drawn.holds(clicked, "Stayed signed in"));
    }

    // Rule-VIEW-PANEL-120, Rule-VIEW-PANEL-121
    public void testTheStarMovesTheDefaultAndTheCrossFallsBackToTheLast() {
        final @NotNull TestRunNode testRun = aTestRun(List.of(invokedThreeTimes()));
        final @NotNull RunItem held = theTestRuns().getRunItems(testRun.getPath()).runItemOf(testCase.getUuid()).orElseThrow();

        assertTrue(runItemStatuses().chooseExecution(testRun.getPath(), testCase.getUuid(), 1));
        assertEquals("the chosen execution does not decide", RunItemStatus.FAILED, held.getStatus());

        assertTrue(runItemStatuses().removeExecution(testRun.getPath(), testCase.getUuid(), 1));
        assertEquals("with the chosen one gone, the last does not decide", RunItemStatus.PASSED, held.getStatus());
        assertEquals(3, held.getExecutions().size());
    }

    // Rule-VIEW-PANEL-121, Rule-TREE-PANEL-009
    public void testATestRunThatTakesNoMoreRunItemStatusesRefusesBoth() {
        final @NotNull TestRunNode testRun = aTestRun(List.of(invokedThreeTimes()));
        theTestRuns().changeTestRunMarker(testRun.getPath(), marker -> marker.setStatus(TestRunStatus.COMMITTED));

        assertFalse("a committed test run changed its default", runItemStatuses().chooseExecution(testRun.getPath(), testCase.getUuid(), 1));
        assertFalse("a committed test run lost an execution", runItemStatuses().removeExecution(testRun.getPath(), testCase.getUuid(), 0));
        assertEquals(4, theTestRuns().getRunItems(testRun.getPath()).runItemOf(testCase.getUuid()).orElseThrow().getExecutions().size());
    }

    // Rule-VIEW-PANEL-120
    public void testTheStarOnAnExecutionsLineMakesItTheDefault() {
        final @NotNull TestRunNode testRun = aTestRun(List.of(invokedThreeTimes()));
        final @NotNull JBPanel<?> details = detailsOf(testRun, heldIn(testRun), new ArrayList<>());

        final @NotNull List<JComponent> stars = named(details, Bundle.message("execution.set.default"));
        assertEquals("every execution but the default does not carry a star", 3, stars.size());
        clicked(stars.get(1));

        assertEquals("the star did not make the failed execution the default", RunItemStatus.FAILED, heldIn(testRun).getStatus());
    }

    // Rule-VIEW-PANEL-121
    public void testTheCrossOnAnExecutionsLineRemovesItAndShowsTheDefaultAgain() {
        final @NotNull TestRunNode testRun = aTestRun(List.of(invokedThreeTimes()));
        final @NotNull List<OptionalInt> shown = new ArrayList<>();
        final @NotNull JBPanel<?> details = detailsOf(testRun, heldIn(testRun), shown);

        clicked(named(details, Bundle.message("execution.remove")).getFirst());

        assertEquals("the cross did not remove the execution", 3, heldIn(testRun).getExecutions().size());
        assertEquals("the rows below did not go back to the default", List.of(OptionalInt.empty()), shown);
    }

    // Rule-VIEW-PANEL-120, Rule-VIEW-PANEL-122
    public void testHoveringALineShowsItsActionsAndClickingItShowsThatExecution() {
        final @NotNull TestRunNode testRun = aTestRun(List.of(invokedThreeTimes()));
        final @NotNull List<OptionalInt> shown = new ArrayList<>();
        final @NotNull JBPanel<?> details = detailsOf(testRun, heldIn(testRun), shown);
        final @NotNull Component third = Drawn.components(details).stream().filter(part -> Drawn.text(part).startsWith(Bundle.message("execution.invocation", "3"))).findFirst()
                .orElseThrow(() -> new AssertionError("no line names the third invocation: " + Drawn.words(details)));
        final @NotNull JComponent itsCross = named(details, Bundle.message("execution.remove")).getLast();
        assertFalse("a line's actions show before the pointer is on it", itsCross.getParent().isVisible());

        entered(third);
        assertTrue("hovering a line did not show its actions", itsCross.getParent().isVisible());

        clicked(third);
        assertEquals("clicking a line did not show that execution", List.of(OptionalInt.of(3)), shown);
    }
}
