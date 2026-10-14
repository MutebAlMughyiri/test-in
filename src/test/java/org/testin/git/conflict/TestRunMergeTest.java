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

package org.testin.git.conflict;

import org.testin.util.RealJsonCodec;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestRunMergeTest {

    private static String runItem(final String severity, final String... executions) {
        return chosen(severity, executions.length, executions);
    }

    private static String chosen(final String severity, final int defaultNumber, final String... executions) {
        return """
                { "bugSeverity" : "%s", "default" : %d, "executions" : [ %s ] }""".formatted(severity, defaultNumber, String.join(", ", executions));
    }

    private static final String EARLY = """
            { "status" : "PASSED", "actualResult" : "Signed in", "executedBy" : "Muteb", "executedAt" : "Monday 14-09-2026 At 10:00:00 [Asia/Riyadh]" }""";

    private static final String LATE = """
            { "status" : "FAILED", "actualResult" : "The lockout counter never reset", "stacktrace" : "boom", "executedBy" : "Sara", "executedAt" : "Monday 14-09-2026 At 11:30:00 [Asia/Riyadh]" }""";

    private static final String BOTH = """
            { "status" : "BLOCKED", "executedBy" : "Omar", "executedAt" : "Monday 14-09-2026 At 09:00:00 [Asia/Riyadh]" }""";

    private static final String MINE_RUN_ITEM = runItem("MINOR", BOTH, EARLY);

    private static final String THEIRS_RUN_ITEM = runItem("MAJOR", BOTH, LATE);

    private static final String BASE_MARKER = """
            {
              "createdBy" : "Muteb",
              "createdAt" : "Sunday 13-09-2026 At 09:00:00 [Asia/Riyadh]",
              "status" : "CREATED",
              "configuration" : { "platform" : "Web" }
            }""";
    private static final String MINE_MARKER = """
            {
              "createdBy" : "Muteb",
              "createdAt" : "Sunday 13-09-2026 At 09:00:00 [Asia/Riyadh]",
              "updatedBy" : "Muteb",
              "updatedAt" : "Monday 14-09-2026 At 10:05:00 [Asia/Riyadh]",
              "status" : "IN_PROGRESS",
              "configuration" : { "platform" : "Web", "browser" : "Chrome" },
              "executionStartedAt" : "Monday 14-09-2026 At 10:00:00 [Asia/Riyadh]",
              "executionEndedAt" : "Monday 14-09-2026 At 10:20:00 [Asia/Riyadh]"
            }""";
    private static final String THEIRS_MARKER = """
            {
              "createdBy" : "Muteb",
              "createdAt" : "Sunday 13-09-2026 At 09:00:00 [Asia/Riyadh]",
              "updatedBy" : "Sara",
              "updatedAt" : "Monday 14-09-2026 At 11:40:00 [Asia/Riyadh]",
              "status" : "COMPLETED",
              "configuration" : { "platform" : "Web", "component" : "Storefront" },
              "executionStartedAt" : "Monday 14-09-2026 At 09:30:00 [Asia/Riyadh]",
              "executionEndedAt" : "Monday 14-09-2026 At 11:35:00 [Asia/Riyadh]"
            }""";

    // Rule-SHARE-080, Rule-SHARE-081
    @Test
    public void bothSidesExecutionsAreJoinedInTimeOrder() {
        final MergeOutcome merge = RunItemMerger.of(RealJsonCodec.build(), MINE_RUN_ITEM, THEIRS_RUN_ITEM);

        assertTrue(merge.isSettled(), "a run item is settled by rule, never by asking");
        assertEquals(merge.merged().path("executions").size(), 3, "the execution both sides hold is kept once: " + merge.merged());
        assertEquals(merge.merged().path("executions").get(0).path("executedBy").asText(), "Omar");
        assertEquals(merge.merged().path("executions").get(1).path("executedBy").asText(), "Muteb");
        assertEquals(merge.merged().path("executions").get(2).path("stacktrace").asText(), "boom", "each execution keeps its own evidence");
        assertEquals(merge.merged().path("default").asInt(), 3, "the later side's default, by its place in the join");
        assertEquals(merge.settled().size(), 1, "and the tester is told a choice was made: " + merge.settled());
    }

    // Rule-SHARE-081
    @Test
    public void theBugFieldsComeFromWhoeverRecordedLastWhicheverSideIsMine() {
        assertEquals(RunItemMerger.of(RealJsonCodec.build(), MINE_RUN_ITEM, THEIRS_RUN_ITEM).merged().path("bugSeverity").asText(), "MAJOR");
        assertEquals(RunItemMerger.of(RealJsonCodec.build(), THEIRS_RUN_ITEM, MINE_RUN_ITEM).merged().path("bugSeverity").asText(), "MAJOR");
    }

    // Rule-SHARE-081, Rule-VIEW-PANEL-119
    @Test
    public void theLaterSidesChoiceIsWrittenByItsPlaceInTheJoin() {
        final String choseEarly = chosen("MINOR", 2, BOTH, EARLY);
        final String choseFirst = chosen("MAJOR", 1, BOTH, LATE);

        final MergeOutcome merge = RunItemMerger.of(RealJsonCodec.build(), choseEarly, choseFirst);

        assertEquals(merge.merged().path("executions").size(), 3);
        assertEquals(merge.merged().path("default").asInt(), 1, "the later side chose the execution both hold, now first in the join: " + merge.merged());
    }

    // Rule-SHARE-081, Rule-EDITOR-PANEL-136
    @Test
    public void anOpenExecutionIsDroppedWhenTheOtherSideRecorded() {
        final MergeOutcome merge = RunItemMerger.of(RealJsonCodec.build(), runItem("ENHANCEMENT", """
                { "status" : "PENDING", "duration" : 12.0 }"""), runItem("ENHANCEMENT", EARLY));

        assertEquals(merge.merged().path("executions").size(), 1, "no open execution is left beside a recorded one: " + merge.merged());
        assertEquals(merge.merged().path("executions").get(0).path("status").asText(), "PASSED");
        assertEquals(merge.merged().path("default").asInt(), 1);
    }

    // Rule-SHARE-081, Rule-TREE-PANEL-067
    @Test
    public void untestedWinsOverPendingWhenNeitherSideRecorded() {
        final MergeOutcome merge = RunItemMerger.of(RealJsonCodec.build(), runItem("ENHANCEMENT", """
                { "status" : "PENDING" }"""), runItem("ENHANCEMENT", """
                { "status" : "UNTESTED" }"""));

        assertEquals(merge.merged().path("executions").size(), 1);
        assertEquals(merge.merged().path("executions").get(0).path("status").asText(), "UNTESTED", "a signed-off side's word is kept: " + merge.merged());
    }

    @Test
    public void twoIdenticalRunItemsAreNoDisagreement() {
        final MergeOutcome merge = RunItemMerger.of(RealJsonCodec.build(), MINE_RUN_ITEM, MINE_RUN_ITEM);

        assertTrue(merge.isSettled());
        assertTrue(merge.settled().isEmpty(), "nothing was decided, because nothing differed");
        assertEquals(merge.merged().path("executions").size(), 2);
    }

    // Rule-SHARE-080
    @Test
    public void aTestRunRanFromTheFirstStartToTheLastStop() {
        final MergeOutcome merge = TestRunMarkerMerger.of(RealJsonCodec.build(), BASE_MARKER, MINE_MARKER, THEIRS_MARKER);

        assertTrue(merge.isSettled(), "none of this is the tester's to answer: " + merge.questions());
        assertEquals(merge.merged().path("executionStartedAt").asText(), "Monday 14-09-2026 At 09:30:00 [Asia/Riyadh]");
        assertEquals(merge.merged().path("executionEndedAt").asText(), "Monday 14-09-2026 At 11:35:00 [Asia/Riyadh]");
        assertEquals(merge.merged().path("status").asText(), "COMPLETED", "the status further along");
        assertEquals(merge.merged().path("updatedBy").asText(), "Sara", "the audit block takes the later edit");
    }

    // Rule-SHARE-130
    @Test
    public void aCommittedTestRunBringsItsCommitWithItsStatus() {
        final MergeOutcome merge = TestRunMarkerMerger.of(RealJsonCodec.build(), BASE_MARKER, MINE_MARKER, THEIRS_MARKER.replace("\"status\" : \"COMPLETED\"", "\"status\" : \"COMMITTED\", \"commit\" : \"ea9a501\""));

        assertEquals(merge.merged().path("status").asText(), "COMMITTED");
        assertEquals(merge.merged().path("commit").asText(), "ea9a501", "a Committed test run without its commit would read nothing");
    }

    // Rule-SHARE-130
    @Test
    public void aCommitIsKeptWhenTheOtherSideIsBehind() {
        final MergeOutcome merge = TestRunMarkerMerger.of(RealJsonCodec.build(), BASE_MARKER, MINE_MARKER.replace("\"status\" : \"IN_PROGRESS\"", "\"status\" : \"COMMITTED\", \"commit\" : \"ea9a501\""), THEIRS_MARKER);

        assertEquals(merge.merged().path("status").asText(), "COMMITTED");
        assertEquals(merge.merged().path("commit").asText(), "ea9a501");
    }

    @Test
    public void theAnswersTheyEachGaveAreBothKept() {
        final MergeOutcome merge = TestRunMarkerMerger.of(RealJsonCodec.build(), BASE_MARKER, MINE_MARKER, THEIRS_MARKER);

        assertEquals(merge.merged().path("configuration").path("browser").asText(), "Chrome");
        assertEquals(merge.merged().path("configuration").path("component").asText(), "Storefront");
        assertEquals(merge.merged().path("configuration").path("platform").asText(), "Web", "neither touched it");
    }

    // Rule-SHARE-081
    @Test
    public void onlyAFieldBothChangedIsAskedAbout() {
        final String theirs = THEIRS_MARKER.replace("\"platform\" : \"Web\"", "\"platform\" : \"Mobile\"");
        final String mine = MINE_MARKER.replace("\"platform\" : \"Web\"", "\"platform\" : \"Desktop\"");

        final MergeOutcome merge = TestRunMarkerMerger.of(RealJsonCodec.build(), BASE_MARKER, mine, theirs);

        assertEquals(merge.questions().size(), 1, "one question, about the one field both answered: " + merge.questions());
        assertEquals(merge.questions().getFirst().field(), "configuration.platform");
        assertEquals(merge.questions().getFirst().mine(), "Desktop");
        assertEquals(merge.questions().getFirst().theirs(), "Mobile");

        merge.answer(RealJsonCodec.build(), merge.questions().getFirst(), true, theirs);
        assertEquals(merge.merged().path("configuration").path("platform").asText(), "Mobile",
                "the tester's answer is written where the value lives, inside the configuration");
    }
}
