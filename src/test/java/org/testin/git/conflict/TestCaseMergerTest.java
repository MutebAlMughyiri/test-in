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

import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.RealJsonCodec;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestCaseMergerTest {

    private static @NotNull String testCase(final String description, final String expected, final String priority, final String updatedBy, final String updatedAt, final String rank) {
        return """
                {
                  "order" : "%s",
                  "description" : "%s",
                  "expectedResult" : "%s",
                  "status" : "PENDING",
                  "steps" : [ ],
                  "priority" : "%s",
                  "createdBy" : "muteb",
                  "updatedBy" : "%s",
                  "createdAt" : "Thursday 20-08-2026 At 03:33:24 [Asia/Riyadh]",
                  "updatedAt" : "%s"
                }
                """.formatted(rank, description, expected, priority, updatedBy, updatedAt);
    }

    private static @NotNull String at(final String time) {
        return "Thursday 20-08-2026 At " + time + " [Asia/Riyadh]";
    }

    // Rule-SHARE-078, Rule-SHARE-080
    @Test
    public void differentFieldsMergeWithoutAsking() {
        final String base = testCase("sign in", "dashboard opens", "LOW", "", at("09:00:00"), "m");
        final String mine = testCase("a registered user signs in", "dashboard opens", "LOW", "muteb", at("10:00:00"), "m");
        final String theirs = testCase("sign in", "the account dashboard opens", "LOW", "sara", at("11:00:00"), "m");

        final MergeOutcome merge = TestCaseMerger.of(RealJsonCodec.build(), base, mine, theirs);

        assertTrue(merge.isSettled(), "different fields are not a conflict");
        assertEquals(merge.merged().get("description").asText(), "a registered user signs in");
        assertEquals(merge.merged().get("expectedResult").asText(), "the account dashboard opens");
    }

    // Rule-SHARE-109
    @Test
    public void theAuditStampsAreNeverAQuestion() {
        final String base = testCase("sign in", "", "LOW", "", at("09:00:00"), "m");
        final String mine = testCase("signs in with a valid password", "", "LOW", "muteb", at("10:00:00"), "m");
        final String theirs = testCase("sign in", "the dashboard opens", "LOW", "sara", at("11:30:00"), "m");

        final MergeOutcome merge = TestCaseMerger.of(RealJsonCodec.build(), base, mine, theirs);

        assertTrue(merge.isSettled());
        assertEquals(merge.merged().get("updatedBy").asText(), "sara", "the later edit names who made it");
        assertEquals(merge.merged().get("updatedAt").asText(), at("11:30:00"));
    }

    // Rule-SHARE-078, Rule-SHARE-081
    @Test
    public void theSameFieldChangedBothWaysIsAskedAbout() {
        final String base = testCase("sign in", "", "LOW", "", at("09:00:00"), "m");
        final String mine = testCase("a registered user signs in", "", "LOW", "muteb", at("10:00:00"), "m");
        final String theirs = testCase("a known user signs in", "", "LOW", "sara", at("11:00:00"), "m");

        final MergeOutcome merge = TestCaseMerger.of(RealJsonCodec.build(), base, mine, theirs);

        assertFalse(merge.isSettled());
        assertEquals(merge.questions().size(), 1, "one field, one question - not one per differing line");

        final Question question = merge.questions().getFirst();
        assertEquals(question.field(), "description");
        assertEquals(question.mine(), "a registered user signs in");
        assertEquals(question.theirs(), "a known user signs in");
    }

    @Test
    public void answeringTakesTheOtherSideForThatFieldOnly() {
        final String base = testCase("sign in", "opens", "LOW", "", at("09:00:00"), "m");
        final String mine = testCase("mine", "opens", "HIGH", "muteb", at("10:00:00"), "m");
        final String theirs = testCase("theirs", "opens", "LOW", "sara", at("11:00:00"), "m");

        final MergeOutcome merge = TestCaseMerger.of(RealJsonCodec.build(), base, mine, theirs);
        merge.answer(RealJsonCodec.build(), merge.questions().getFirst(), true, theirs);

        assertEquals(merge.merged().get("description").asText(), "theirs");
        assertEquals(merge.merged().get("priority").asText(), "HIGH", "the priority only I changed is still mine");
    }

    @Test
    public void keepingMyAnswerChangesNothing() {
        final String base = testCase("sign in", "", "LOW", "", at("09:00:00"), "m");
        final String mine = testCase("mine", "", "LOW", "muteb", at("10:00:00"), "m");
        final String theirs = testCase("theirs", "", "LOW", "sara", at("11:00:00"), "m");

        final MergeOutcome merge = TestCaseMerger.of(RealJsonCodec.build(), base, mine, theirs);
        merge.answer(RealJsonCodec.build(), merge.questions().getFirst(), false, theirs);

        assertEquals(merge.merged().get("description").asText(), "mine");
    }

    // Rule-SHARE-109
    @Test
    public void aPositionIsSettledWithoutAsking() {
        final String base = testCase("sign in", "", "LOW", "", at("09:00:00"), "m");
        final String mine = testCase("sign in", "", "LOW", "muteb", at("10:00:00"), "c");
        final String theirs = testCase("sign in", "", "LOW", "sara", at("11:00:00"), "s");

        final MergeOutcome merge = TestCaseMerger.of(RealJsonCodec.build(), base, mine, theirs);

        assertTrue(merge.isSettled(), "where a test case sits is not a question a tester can answer about a merge");
        assertEquals(merge.merged().get("order").asText(), "s");
    }

    // Rule-SHARE-109
    @Test
    public void theSettledFieldsAreNamedForTheTester() {
        final @NotNull String base = testCase("sign in", "", "LOW", "", at("09:00:00"), "m");
        final @NotNull String mine = testCase("a registered user signs in", "", "LOW", "muteb", at("10:00:00"), "c");
        final @NotNull String theirs = testCase("sign in", "", "LOW", "sara", at("11:00:00"), "s");

        final @NotNull MergeOutcome merge = TestCaseMerger.of(RealJsonCodec.build(), base, mine, theirs);
        final @NotNull String said = MergeOutcome.settledSentence(merge.settled(), "\n");

        assertTrue(merge.isSettled());
        assertTrue(said.contains(Bundle.message("git.merge.audit")), "who changed it last is named as settled: " + said);
        assertTrue(said.contains(Bundle.message("git.merge.position")), "the position is named as settled: " + said);
    }

    @Test
    public void aMissingAncestorAsksAboutWhatDiffers() {
        final String mine = testCase("mine", "opens", "LOW", "muteb", at("10:00:00"), "m");
        final String theirs = testCase("theirs", "opens", "LOW", "sara", at("11:00:00"), "m");

        final MergeOutcome merge = TestCaseMerger.of(RealJsonCodec.build(), "", mine, theirs);

        assertEquals(merge.questions().size(), 1);
        assertEquals(merge.questions().getFirst().field(), "description");
    }

    @Test
    public void anUnreadableSideIsNotAFailure() {
        final String mine = testCase("mine", "", "LOW", "muteb", at("10:00:00"), "m");

        final MergeOutcome merge = TestCaseMerger.of(RealJsonCodec.build(), "", mine, "<<<<<<< HEAD not json at all");

        assertTrue(merge.isSettled());
        assertEquals(merge.merged().get("description").asText(), "mine");
    }

    @Test
    public void aTestCaseIsWhatThisCanMerge() {
        assertTrue(TestCaseMerger.isTestCase("Test Cases/Login/6197ec6e.testcase"));
        assertTrue(TestCaseMerger.isTestCase("Test Cases\\Login\\6197ec6e.testcase"),
                "Git names paths with slashes and Windows names them with backslashes");
    }

    @Test
    public void aMarkerATestRunOrAnythingElseIsNot() {
        assertFalse(TestCaseMerger.isTestCase("Test Cases/Login/.testset"), "a marker has no named fields to merge");
        assertFalse(TestCaseMerger.isTestCase(".testproject"));
        assertFalse(TestCaseMerger.isTestCase("Test Runs/Cycle 1/Cycle 1.json"),
                "a test run records what happened; it is not something two people edit into one");
        assertFalse(TestCaseMerger.isTestCase("Test Cases/Login/notes.txt"));
        assertFalse(TestCaseMerger.isTestCase("Test Runs/Cycle 1/6197ec6e.testcase"), "a test case file outside the Test Cases folder is not a test case");
    }

    // Rule-SHARE-078, Rule-SHARE-081
    @Test
    public void aFieldOneSideTookAwayIsAskedAboutWithNothingOnThatSideAndAListWithItsItems() {
        final String base = """
                { "order" : "m", "module" : "Login", "steps" : [ "Open the app" ] }
                """;
        final String mine = """
                { "order" : "m", "steps" : [ "Open the app", "Type the password" ] }
                """;
        final String theirs = """
                { "order" : "m", "module" : "Accounts", "steps" : [ "Open the app", "Press Sign in" ] }
                """;

        final List<Question> asked = TestCaseMerger.of(RealJsonCodec.build(), base, mine, theirs).questions();
        final Question module = asked.stream().filter(question -> question.field().equals("module")).findFirst().orElseThrow(() -> new AssertionError("the module was not asked about: " + asked));
        final Question steps = asked.stream().filter(question -> question.field().equals("steps")).findFirst().orElseThrow(() -> new AssertionError("the steps were not asked about: " + asked));

        assertEquals(module.mine(), "", "the side that took the field away is not asked about as nothing");
        assertEquals(module.theirs(), "Accounts");
        assertTrue(steps.mine().contains("Type the password"), "a list is not asked about with its items: " + steps.mine());
    }

    // Rule-SHARE-080
    @Test
    public void aFieldEmptiedOnBothSidesIsTheSameWhetherNullOrGone() {
        final String base = """
                { "order" : "m", "module" : "Login" }
                """;
        final String mine = """
                { "order" : "m", "module" : null }
                """;
        final String theirs = """
                { "order" : "m" }
                """;

        assertTrue(TestCaseMerger.of(RealJsonCodec.build(), base, mine, theirs).isSettled(), "a field emptied on both sides was asked about");
        assertTrue(TestCaseMerger.of(RealJsonCodec.build(), base, theirs, mine).isSettled(), "a field emptied on both sides was asked about when the remote side wrote null");
    }

    // Rule-SHARE-078, Rule-SHARE-081
    @Test
    public void aFieldOneSideSetToNullIsAskedAboutWithNothingOnThatSide() {
        final String base = """
                { "order" : "m", "module" : "Login" }
                """;
        final String mine = """
                { "order" : "m", "module" : null }
                """;
        final String theirs = """
                { "order" : "m", "module" : "Accounts" }
                """;

        final List<Question> asked = TestCaseMerger.of(RealJsonCodec.build(), base, mine, theirs).questions();

        assertEquals(asked.size(), 1, "the field set to null was not asked about once: " + asked);
        assertEquals(asked.getFirst().mine(), "", "the side that set the field to null is not asked about as nothing");
    }

    // Rule-SHARE-109
    @Test
    public void whoCreatedATestCaseIsNeverAQuestion() {
        final String base = """
                { "order" : "m", "createdBy" : "muteb" }
                """;
        final String mine = """
                { "order" : "m", "createdBy" : "sara" }
                """;
        final String theirs = """
                { "order" : "m", "createdBy" : "ali" }
                """;

        assertTrue(TestCaseMerger.of(RealJsonCodec.build(), base, mine, theirs).isSettled(), "who created the test case was asked about");
    }
}
