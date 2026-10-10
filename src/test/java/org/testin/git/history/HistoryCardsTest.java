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

package org.testin.git.history;

import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.RunItem;
import org.testin.util.Bundle;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class HistoryCardsTest {

    private static @NotNull ZonedDateTime on(final int day) {
        return ZonedDateTime.parse("2026-10-0%dT16:40:00+03:00".formatted(day));
    }

    private static @NotNull HistoryEntry entry(final @NotNull String hash, final int day) {
        return new HistoryEntry(HistoryEntryKind.UPDATED, hash, "Sara", on(day), "", "", List.of(), "", "");
    }

    private static @NotNull BugCard bug(final @NotNull String hash, final int day) {
        return new BugCard(hash, "Sara", on(day), new BugEvent(BugEventKind.RECORDED, Path.of("Cycle 3"), RunItem.builder().build(), List.of()));
    }

    // Rule-VIEW-PANEL-115
    @Test
    public void everyCardSaysWhatHappenedAndWhetherItIsCommitted() {
        final @NotNull HistoryEntry uncommitted = entry("", 1);
        final @NotNull BugCard committedBug = bug("4f1c9e2a7d" + "0".repeat(30), 2);

        assertEquals(uncommitted.kind().getLabel(), Bundle.message("view.history.updated"));
        assertFalse(uncommitted.isCommitted());
        assertEquals(committedBug.kind().getLabel(), Bundle.message("view.history.bug"));
        assertEquals(committedBug.shortHash(), "4f1c9e2", "a committed card shows its short hash");
    }

    // Rule-VIEW-PANEL-105, Rule-VIEW-PANEL-107
    @Test
    public void cardsFollowTheCommitOrderWithWhatIsNotCommittedOnTopAndTheTestCaseCardFirstWithinOneCommit() {
        final @NotNull HistoryEntry uncommitted = entry("", 1);
        final @NotNull HistoryEntry rebased = entry("d".repeat(40), 1);
        final @NotNull HistoryEntry review = entry("c".repeat(40), 9);
        final @NotNull HistoryEntry created = entry("a".repeat(40), 3);
        final @NotNull BugCard uncommittedBug = bug("", 5);
        final @NotNull BugCard sameCommit = bug("c".repeat(40), 9);
        final @NotNull BugCard between = bug("b".repeat(40), 2);
        final @NotNull List<String> order = List.of("d".repeat(40), "c".repeat(40), "b".repeat(40), "a".repeat(40));

        final @NotNull History history = History.read(List.of(uncommitted, rebased, review, created)).with(List.of(uncommittedBug, sameCommit, between), order);

        assertEquals(history.cards(), List.of(uncommitted, uncommittedBug, rebased, review, sameCommit, between, created), "a rebased commit keeps its old author date and still comes first");
    }

    // Rule-VIEW-PANEL-106
    @Test
    public void bugsGitCouldNotReadSaySoUnderTheTestCaseCards() {
        final @NotNull History history = History.read(List.of(entry("a".repeat(40), 1))).withoutBugs("fatal: bad object");

        assertEquals(history.cards().size(), 1);
        assertEquals(history.problem(), Bundle.message("view.history.bugs.failed", "fatal: bad object"));
        assertEquals(History.NOT_UNDER_GIT.withoutBugs("fatal").problem(), History.NOT_UNDER_GIT.problem(), "the first problem stays the one shown");
    }

    // Rule-VIEW-PANEL-100
    @Test
    public void aTestProjectNotUnderGitKeepsItsLineBesideTheBugsItHoldsNow() {
        final @NotNull History history = History.NOT_UNDER_GIT.with(List.of(bug("", 5)), List.of());

        assertEquals(history.cards().size(), 1);
        assertEquals(history.problem(), History.NOT_UNDER_GIT.problem());
    }
}
