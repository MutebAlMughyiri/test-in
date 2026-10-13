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
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.RealJsonCodec;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GhIssueStatesAnswerTest {

    private static @NotNull GhIssueStatesAnswer answered(final @NotNull String stdout, final int exitCode, final Integer... numbers) {
        return GhIssueStatesAnswer.of(RealJsonCodec.build(), new ProcessOutput(stdout, "", exitCode, false, false), "github.com", List.of(numbers));
    }

    // Rule-VIEW-PANEL-091
    @Test
    public void openIsOpenAndClosedAsCompletedIsFixed() {
        final @NotNull GhIssueStatesAnswer states = answered("""
                {"data":{"repository":{"i392":{"state":"CLOSED","stateReason":"COMPLETED"},"i393":{"state":"OPEN","stateReason":null}}}}""", 0, 392, 393);

        assertEquals(states.stateOf(392).label(), Bundle.message("bug.state.fixed"));
        assertEquals(states.stateOf(393).label(), Bundle.message("bug.state.open"));
        assertTrue(states.problem().isEmpty());
    }

    // Rule-VIEW-PANEL-091
    @Test
    public void notPlannedAndDuplicateBothReadNotPlannedAndOnlyTheDuplicateSaysSo() {
        final @NotNull GhIssueStatesAnswer states = answered("""
                {"data":{"repository":{"i5":{"state":"CLOSED","stateReason":"NOT_PLANNED"},"i6":{"state":"CLOSED","stateReason":"DUPLICATE"}}}}""", 0, 5, 6);

        assertEquals(states.stateOf(5).label(), Bundle.message("bug.state.not.planned"));
        assertEquals(states.stateOf(6).label(), Bundle.message("bug.state.not.planned"));
        assertTrue(states.stateOf(5).tooltip().isEmpty());
        assertEquals(states.stateOf(6).tooltip(), Bundle.message("bug.state.duplicate.tooltip"));
    }

    // Rule-VIEW-PANEL-091
    @Test
    public void anIssueClosedBeforeGitHubGaveReasonsIsFixed() {
        assertEquals(answered("""
                {"data":{"repository":{"i9":{"state":"CLOSED","stateReason":null}}}}""", 0, 9).stateOf(9).label(), Bundle.message("bug.state.fixed"));
    }

    // Rule-VIEW-PANEL-091
    @Test
    public void aMissingIssueLosesNoneOfTheOthersThoughGhExitsWithOne() {
        final @NotNull GhIssueStatesAnswer states = answered("""
                {"data":{"repository":{"i392":{"state":"CLOSED","stateReason":"COMPLETED"},"i99999":null}},"errors":[{"type":"NOT_FOUND","path":["repository","i99999"],"message":"Could not resolve to an Issue with the number of 99999."}]}""", 1, 392, 99999);

        assertEquals(states.stateOf(392).label(), Bundle.message("bug.state.fixed"));
        assertEquals(states.stateOf(99999), BugIssueState.NOT_READ);
        assertTrue(states.problem().isEmpty(), "a missing issue is not a failure to ask: " + states.problem());
    }

    // Rule-VIEW-PANEL-091
    @Test
    public void aBoardColumnWinsOverTheIssueStateAndNamesItsGitHubProject() {
        final @NotNull BugIssueState state = answered("""
                {"data":{"repository":{"i14":{"state":"OPEN","stateReason":null,"projectItems":{"nodes":[{"project":{"title":"Sprint board"},"fieldValueByName":null},{"project":{"title":"QA board"},"fieldValueByName":{"name":"In progress","color":"BLUE"}}]}}}}}""", 0, 14).stateOf(14);

        assertEquals(state.label(), "In progress");
        assertEquals(state.tooltip(), Bundle.message("bug.state.board.tooltip", "QA board"));
        assertEquals(state.pills().size(), 1);
    }

    // Rule-VIEW-PANEL-094
    @Test
    public void aBoardTheTokenCannotReadIsRecognizedAsTheMissingPermission() {
        final @NotNull GhIssueStatesAnswer states = answered("""
                {"errors":[{"type":"INSUFFICIENT_SCOPES","message":"The 'title' field requires one of the following scopes: ['read:project']"}]}""", 0, 14);

        assertTrue(states.boardRefused());
        assertTrue(states.problem().isEmpty());
    }

    // Rule-VIEW-PANEL-093
    @Test
    public void aSignedOutGhIsNamedAndNothingIsRead() {
        final @NotNull GhIssueStatesAnswer states = GhIssueStatesAnswer.of(RealJsonCodec.build(), new ProcessOutput("", "To get started with GitHub CLI, please run:  gh auth login", 4, false, false), "github.com", List.of(14));

        assertEquals(states.problem(), Bundle.message("bug.reason.signed.out", "github.com"));
        assertEquals(states.stateOf(14), BugIssueState.NOT_READ);
        assertFalse(states.boardRefused());
    }

    // Rule-VIEW-PANEL-093
    @Test
    public void aGhThatNeverAnsweredSaysHowLongItWaited() {
        final @NotNull GhIssueStatesAnswer states = GhIssueStatesAnswer.of(RealJsonCodec.build(), new ProcessOutput("", "", -1, true, false), "github.com", List.of(14));

        assertEquals(states.problem(), Bundle.message("bug.states.timed.out", GitHubCli.TIMEOUT.toSeconds()));
    }

    // Rule-VIEW-PANEL-091
    @Test
    public void aStateNotReadDrawsNoPill() {
        assertTrue(BugIssueState.NOT_READ.pills().isEmpty());
    }
}
