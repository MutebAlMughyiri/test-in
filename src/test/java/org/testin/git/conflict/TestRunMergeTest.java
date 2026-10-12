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

import org.testin.util.RealMapper;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestRunMergeTest {

    private static String runItem(final String severity, final String... executions) {
        return """
                { "id" : "11111111-1111-4111-8111-111111111101", "bugSeverity" : "%s", "executions" : [ %s ] }""".formatted(severity, String.join(", ", executions));
    }

    private static final String EARLY = """
            { "status" : "PASSED", "actualResult" : "Signed in", "executedBy" : "Muteb", "executedAt" : "Monday 14-09-2026 At 10:00:00 [Asia/Riyadh]" }""";

    private static final String LATE = """
            { "status" : "FAILED", "actualResult" : "The lockout counter never reset", "stacktrace" : "boom", "executedBy" : "Sara", "executedAt" : "Monday 14-09-2026 At 11:30:00 [Asia/Riyadh]" }""";

    private static final String BOTH = """
            { "status" : "BLOCKED", "executedBy" : "Omar", "executedAt" : "Monday 14-09-2026 At 09:00:00 [Asia/Riyadh]" }""";

    private static final String MINE_ITEM = runItem("MINOR", BOTH, EARLY);

    private static final String THEIRS_ITEM = runItem("MAJOR", BOTH, LATE);

    private static final String BASE_MARKER = """
            {
              "createdBy" : "Muteb",
              "createdAt" : "Sunday 13-09-2026 At 09:00:00 [Asia/Riyadh]",
              "status" : "CREATED",
              "configuration" : { "PLATFORM" : "Web" }
            }""";
    private static final String MINE_MARKER = """
            {
              "createdBy" : "Muteb",
              "createdAt" : "Sunday 13-09-2026 At 09:00:00 [Asia/Riyadh]",
              "updatedBy" : "Muteb",
              "updatedAt" : "Monday 14-09-2026 At 10:05:00 [Asia/Riyadh]",
              "status" : "IN_PROGRESS",
              "configuration" : { "PLATFORM" : "Web", "BROWSER" : "Chrome" },
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
              "configuration" : { "PLATFORM" : "Web", "COMPONENT" : "Storefront" },
              "executionStartedAt" : "Monday 14-09-2026 At 09:30:00 [Asia/Riyadh]",
              "executionEndedAt" : "Monday 14-09-2026 At 11:35:00 [Asia/Riyadh]"
            }""";

    // Rule-SHARE-080, Rule-SHARE-081
    @Test
    public void bothSidesExecutionsAreJoinedInTimeOrder() {
        final MergeOutcome merge = RunItemMerger.of(RealMapper.build(), MINE_ITEM, THEIRS_ITEM);

        assertTrue(merge.isSettled(), "a run item is settled by rule, never by asking");
        assertEquals(merge.merged().path("executions").size(), 3, "the execution both sides hold is kept once: " + merge.merged());
        assertEquals(merge.merged().path("executions").get(0).path("executedBy").asText(), "Omar");
        assertEquals(merge.merged().path("executions").get(1).path("executedBy").asText(), "Muteb");
        assertEquals(merge.merged().path("executions").get(2).path("stacktrace").asText(), "boom", "each execution keeps its own evidence");
        assertEquals(merge.settled().size(), 1, "and the tester is told a choice was made: " + merge.settled());
    }

    // Rule-SHARE-081
    @Test
    public void theBugFieldsComeFromWhoeverRecordedLastWhicheverSideIsMine() {
        assertEquals(RunItemMerger.of(RealMapper.build(), MINE_ITEM, THEIRS_ITEM).merged().path("bugSeverity").asText(), "MAJOR");
        assertEquals(RunItemMerger.of(RealMapper.build(), THEIRS_ITEM, MINE_ITEM).merged().path("bugSeverity").asText(), "MAJOR");
    }

    // Rule-SHARE-081, Rule-VIEW-PANEL-119
    @Test
    public void onlyTheLaterSidesChosenDefaultStays() {
        final String chosenEarly = runItem("MINOR", BOTH.replace("{", "{ \"default\" : true,"), EARLY);
        final String chosenLate = runItem("MAJOR", BOTH, LATE.replace("{", "{ \"default\" : true,"));

        final MergeOutcome merge = RunItemMerger.of(RealMapper.build(), chosenEarly, chosenLate);

        assertEquals(merge.merged().path("executions").size(), 3, "a flag does not make the same execution a second one");
        assertTrue(merge.merged().path("executions").get(2).path("default").asBoolean(), "the later side's choice stays: " + merge.merged());
        assertEquals(merge.merged().findValues("default").size(), 1, "and it is the only one");
    }

    @Test
    public void twoIdenticalRunItemsAreNoDisagreement() {
        final MergeOutcome merge = RunItemMerger.of(RealMapper.build(), MINE_ITEM, MINE_ITEM);

        assertTrue(merge.isSettled());
        assertTrue(merge.settled().isEmpty(), "nothing was decided, because nothing differed");
        assertEquals(merge.merged().path("executions").size(), 2);
    }

    // Rule-SHARE-080
    @Test
    public void aTestRunRanFromTheFirstStartToTheLastStop() {
        final MergeOutcome merge = TestRunMarkerMerger.of(RealMapper.build(), BASE_MARKER, MINE_MARKER, THEIRS_MARKER);

        assertTrue(merge.isSettled(), "none of this is the tester's to answer: " + merge.questions());
        assertEquals(merge.merged().path("executionStartedAt").asText(), "Monday 14-09-2026 At 09:30:00 [Asia/Riyadh]");
        assertEquals(merge.merged().path("executionEndedAt").asText(), "Monday 14-09-2026 At 11:35:00 [Asia/Riyadh]");
        assertEquals(merge.merged().path("status").asText(), "COMPLETED", "the status further along");
        assertEquals(merge.merged().path("updatedBy").asText(), "Sara", "the audit block takes the later edit");
    }

    // Rule-SHARE-130
    @Test
    public void aCommittedTestRunBringsItsCommitWithItsStatus() {
        final MergeOutcome merge = TestRunMarkerMerger.of(RealMapper.build(), BASE_MARKER, MINE_MARKER, THEIRS_MARKER.replace("\"status\" : \"COMPLETED\"", "\"status\" : \"COMMITTED\", \"commit\" : \"ea9a501\""));

        assertEquals(merge.merged().path("status").asText(), "COMMITTED");
        assertEquals(merge.merged().path("commit").asText(), "ea9a501", "a Committed test run without its commit would read nothing");
    }

    // Rule-SHARE-130
    @Test
    public void aCommitIsKeptWhenTheOtherSideIsBehind() {
        final MergeOutcome merge = TestRunMarkerMerger.of(RealMapper.build(), BASE_MARKER, MINE_MARKER.replace("\"status\" : \"IN_PROGRESS\"", "\"status\" : \"COMMITTED\", \"commit\" : \"ea9a501\""), THEIRS_MARKER);

        assertEquals(merge.merged().path("status").asText(), "COMMITTED");
        assertEquals(merge.merged().path("commit").asText(), "ea9a501");
    }

    @Test
    public void theAnswersTheyEachGaveAreBothKept() {
        final MergeOutcome merge = TestRunMarkerMerger.of(RealMapper.build(), BASE_MARKER, MINE_MARKER, THEIRS_MARKER);

        assertEquals(merge.merged().path("configuration").path("BROWSER").asText(), "Chrome");
        assertEquals(merge.merged().path("configuration").path("COMPONENT").asText(), "Storefront");
        assertEquals(merge.merged().path("configuration").path("PLATFORM").asText(), "Web", "neither touched it");
    }

    // Rule-SHARE-081
    @Test
    public void onlyAFieldBothChangedIsAskedAbout() {
        final String theirs = THEIRS_MARKER.replace("\"PLATFORM\" : \"Web\"", "\"PLATFORM\" : \"Mobile\"");
        final String mine = MINE_MARKER.replace("\"PLATFORM\" : \"Web\"", "\"PLATFORM\" : \"Desktop\"");

        final MergeOutcome merge = TestRunMarkerMerger.of(RealMapper.build(), BASE_MARKER, mine, theirs);

        assertEquals(merge.questions().size(), 1, "one question, about the one field both answered: " + merge.questions());
        assertEquals(merge.questions().getFirst().field(), "configuration.PLATFORM");
        assertEquals(merge.questions().getFirst().mine(), "Desktop");
        assertEquals(merge.questions().getFirst().theirs(), "Mobile");

        merge.answer(RealMapper.build(), merge.questions().getFirst(), true, theirs);
        assertEquals(merge.merged().path("configuration").path("PLATFORM").asText(), "Mobile",
                "the tester's answer is written where the value lives, inside the configuration");
    }
}
