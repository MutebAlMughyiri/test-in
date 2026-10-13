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

package org.testin.model.testrun;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testng.annotations.Test;

import java.awt.Color;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestRunResultAnalysisSegmentsTest {

    private static @NotNull TestRunSummary testRunSummary(final long passed, final long failed, final long blocked, final long untested, final long removed) {
        return new TestRunSummary(passed + failed + blocked + untested + removed, passed, failed, blocked, untested, removed, "");
    }

    private static @NotNull List<Segment> of(final @NotNull TestRunSummary summary, final @NotNull TestRunStatus testRun) {
        return TestRunResultAnalysis.segments(summary, testRun);
    }

    private static @NotNull String words(final @NotNull TestRunSummary summary) {
        return of(summary, TestRunStatus.IN_PROGRESS).stream()
                .map(Segment::text)
                .collect(Collectors.joining(" · "));
    }

    @Test
    public void aTestRunNobodyHasTouchedSaysNothing() {
        assertTrue(of(TestRunSummary.EMPTY, TestRunStatus.IN_PROGRESS).isEmpty(),
                "nothing at all rather than a blank piece: the bar hides a test run with nothing recorded the way it hides a zero duration");
    }

    // Rule-EDITOR-PANEL-175
    @Test
    public void aRunItemStatusNoTestCaseCarriesIsLeftOut() {
        assertEquals(words(testRunSummary(12, 0, 0, 108, 0)), "Passed 12 · Pending 108");
    }

    @Test
    public void theRunItemStatusesReadInTheOrderTheEnumDeclaresThem() {
        assertEquals(words(testRunSummary(1, 2, 3, 4, 0)), "Passed 1 · Failed 2 · Blocked 3 · Pending 4");
    }

    @Test
    public void testCasesDeletedUnderTheTestRunAreCountedToo() {
        assertEquals(words(testRunSummary(5, 0, 0, 0, 2)), "Passed 5 · Removed 2");
    }

    @Test
    public void theNamesAreTheRunItemStatusesOwn() {
        assertEquals(words(testRunSummary(1, 0, 0, 0, 0)), RunItemStatus.PASSED.getLabel() + " 1");
    }

    @Test
    public void aRunItemStatusIsPaintedInItsOwnColor() {
        final @NotNull Color painted = of(testRunSummary(0, 3, 0, 0, 0), TestRunStatus.IN_PROGRESS).getFirst().color();

        assertEquals(painted, RunItemStatus.FAILED.getRowColor(), "failed should be painted in the Failed run item status's own color");
    }

    @Test
    public void everyRunItemStatusesColorFollowsTheThemeRatherThanBeingPickedOnce() {
        for (final Segment segment : of(testRunSummary(1, 1, 1, 1, 0), TestRunStatus.IN_PROGRESS)) {
            assertTrue(segment.color() instanceof JBColor,
                    segment.text() + " is drawn in a color that was resolved once and cannot follow a theme change");
        }
    }

    @Test
    public void aRemovedTestCaseIsCountedWithoutBeingPaintedAsARunItemStatus() {
        final @NotNull List<Segment> segments = of(testRunSummary(0, 0, 0, 0, 2), TestRunStatus.CLOSED);

        assertEquals(segments.size(), 1);
        assertEquals(segments.getFirst().text(), RunItemStatus.REMOVED.getLabel() + " 2");
        assertEquals(segments.getFirst().color(), UIUtil.getInactiveTextColor(),
                "removed is not a run item status, so it is drawn in the same color as the rest of the bar");
    }

    // Rule-EDITOR-PANEL-176
    @Test
    public void untouchedTestCasesArePendingUntilTheTestRunGivesUpOnThem() {
        assertEquals(words(testRunSummary(0, 0, 0, 7, 0)), RunItemStatus.PENDING.getLabel() + " 7");

        for (final TestRunStatus over : new TestRunStatus[]{TestRunStatus.COMPLETED, TestRunStatus.CLOSED}) {
            assertEquals(of(testRunSummary(0, 0, 0, 7, 0), over).getFirst().text(), RunItemStatus.UNTESTED.getLabel() + " 7",
                    "a test run that is " + over.getLabel() + " has stopped waiting for them");
        }
    }

    @Test
    public void theThreeRunItemStatusesKeepTheirNameWhicheverStateTheTestRunIsIn() {
        final @NotNull String closed = of(testRunSummary(1, 1, 1, 0, 0), TestRunStatus.CLOSED).stream()
                .map(Segment::text)
                .collect(Collectors.joining(" · "));

        assertEquals(words(testRunSummary(1, 1, 1, 0, 0)), closed);
    }

    @Test
    public void thePlainLabelIsTheFinishedName() {
        assertEquals(TestRunResultAnalysis.UNTESTED.getLabel(), RunItemStatus.UNTESTED.getLabel());
        assertEquals(TestRunResultAnalysis.UNTESTED.heading(testRunSummary(0, 0, 0, 7, 0)), RunItemStatus.UNTESTED.getLabel() + " (7)");
    }

    // Rule-EDITOR-PANEL-190
    @Test
    public void theAnalysisHasOneSectionForEachRunItemStatusEachCountingItsOwn() {
        final @NotNull TestRunSummary summary = testRunSummary(1, 2, 3, 4, 0);

        assertEquals(Stream.of(TestRunResultAnalysis.values()).map(TestRunResultAnalysis::getLabel).toList(),
                List.of(RunItemStatus.PASSED.getLabel(), RunItemStatus.FAILED.getLabel(), RunItemStatus.BLOCKED.getLabel(), RunItemStatus.UNTESTED.getLabel()));
        assertEquals(TestRunResultAnalysis.PASSED.heading(summary), RunItemStatus.PASSED.getLabel() + " (1)");
        assertEquals(TestRunResultAnalysis.FAILED.heading(summary), RunItemStatus.FAILED.getLabel() + " (2)");
        assertEquals(TestRunResultAnalysis.BLOCKED.heading(summary), RunItemStatus.BLOCKED.getLabel() + " (3)");
        assertEquals(TestRunResultAnalysis.UNTESTED.heading(summary), RunItemStatus.UNTESTED.getLabel() + " (4)");
    }

    // Rule-EDITOR-PANEL-191
    @Test
    public void aSectionLeftBlankIsNotSaved() {
        final @NotNull Map<TestRunResultAnalysis, String> typed = new EnumMap<>(TestRunResultAnalysis.class);
        typed.put(TestRunResultAnalysis.PASSED, "The checkout flow held up");
        typed.put(TestRunResultAnalysis.FAILED, "   ");
        typed.put(TestRunResultAnalysis.BLOCKED, "");

        assertEquals(TestRunResultAnalysis.written(typed).keySet(), Set.of(TestRunResultAnalysis.PASSED), "only the section with words in it is kept");
    }
}
