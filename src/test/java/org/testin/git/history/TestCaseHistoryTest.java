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
import org.testin.clipboard.CopyChoice;
import org.testin.model.Priority;
import org.testin.model.TestCaseDto;
import org.testin.util.Bundle;
import org.testin.util.Mapper;
import org.testin.util.RealMapper;
import org.testng.annotations.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.testin.git.history.TestCaseHistory.FIELD;
import static org.testin.git.history.TestCaseHistory.RECORD;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestCaseHistoryTest {

    private static final @NotNull Mapper MAPPER = RealMapper.build();
    private static final @NotNull UUID ID = UUID.fromString("77777777-7777-4777-8777-777777777150");
    private static final @NotNull String PATH = "Test Cases/Login/" + ID + ".tc";

    private static @NotNull TestCaseDto version(final @NotNull String expectedResult) {
        return TestCaseDto.builder().id(ID).description("Log in with a valid user").expectedResult(expectedResult).createdBy("Muteb").build();
    }

    private static @NotNull HistoryCommit commit(final int at, final @NotNull String message) {
        return new HistoryCommit("%040d".formatted(at), "Sara", ZonedDateTime.parse("2026-10-0%dT16:40:00+03:00".formatted(at % 9 + 1)), message, PATH);
    }

    // Rule-INTERNAL-132
    @Test
    public void aCommitsTimeIsReadInTheIdesOwnZone() {
        final @NotNull ZonedDateTime at = TestCaseHistory.authoredAt("2026-10-08T01:00:00+03:00");

        assertEquals(at.getZone(), ZoneId.systemDefault(), "a zone name the way Testin stores its own dates, not Git's offset");
        assertEquals(at.toInstant(), ZonedDateTime.parse("2026-10-08T01:00:00+03:00").toInstant(), "the same moment");
    }

    private static @NotNull Map<String, String> versions(final @NotNull List<HistoryCommit> commits, final @NotNull List<TestCaseDto> versions) {
        final @NotNull Map<String, String> byObject = new HashMap<>();
        for (int at = 0; at < commits.size(); at++)
            byObject.put(commits.get(at).objectName(), MAPPER.writeValueAsString(versions.get(at)));
        return byObject;
    }

    // Rule-VIEW-PANEL-096
    @Test
    public void everyCommitIsReadWithItsHashAuthorDateMessageAndThePathItHadThen() {
        final @NotNull String log = RECORD + "a".repeat(40) + FIELD + "Sara" + FIELD + "2026-10-02T16:40:00+03:00" + FIELD + "Cycle 4 review: steps | expected\n\n" + PATH + "\n"
                + RECORD + "b".repeat(40) + FIELD + "Muteb" + FIELD + "2026-09-24T16:07:00+03:00" + FIELD + "\n\nTest Cases/Old name/" + ID + ".tc\n";

        final @NotNull List<HistoryCommit> commits = TestCaseHistory.commits(log, PATH);

        assertEquals(commits.size(), 2);
        assertEquals(commits.getFirst().message(), "Cycle 4 review: steps | expected");
        assertEquals(commits.getFirst().who(), "Sara");
        assertEquals(commits.getFirst().path(), PATH);
        assertEquals(commits.get(1).message(), "", "a commit with an empty subject is still an entry");
        assertEquals(commits.get(1).path(), "Test Cases/Old name/" + ID + ".tc", "a moved test case is read where it was then");
        assertEquals(commits.get(1).objectName(), "b".repeat(40) + ":Test Cases/Old name/" + ID + ".tc");
    }

    // Rule-VIEW-PANEL-096, Rule-VIEW-PANEL-097
    @Test
    public void eachCommitIsComparedWithTheOneBeforeItAndTheOldestIsCreated() {
        final @NotNull List<HistoryCommit> commits = List.of(commit(2, "Cycle 4 review"), commit(1, "UC-10"));
        final @NotNull TestCaseDto now = version("The dashboard opens within 2 seconds");

        final @NotNull List<HistoryEntry> entries = TestCaseHistory.entries(MAPPER, commits, versions(commits, List.of(now, version("The dashboard opens"))), Optional.of(now));

        assertEquals(entries.size(), 2, "nothing uncommitted, so no Not committed yet entry: " + entries);
        assertEquals(entries.getFirst().kind(), HistoryEntryKind.UPDATED);
        assertEquals(entries.getFirst().shortHash(), "0000000");
        assertEquals(entries.getFirst().changes().getFirst().oldValue(), "The dashboard opens");
        assertEquals(entries.getFirst().changes().getFirst().newValue(), "The dashboard opens within 2 seconds");
        assertEquals(entries.get(1).kind(), HistoryEntryKind.CREATED);
        assertTrue(entries.stream().allMatch(HistoryEntry::isCommitted));
    }

    // Rule-VIEW-PANEL-098
    @Test
    public void anEditNotYetCommittedHeadsTheListWithNoHash() {
        final @NotNull List<HistoryCommit> commits = List.of(commit(1, "UC-10"));
        final @NotNull TestCaseDto now = version("The home page opens");
        now.setPriority(Priority.HIGH);
        now.setUpdatedBy("Muteb");

        final @NotNull List<HistoryEntry> entries = TestCaseHistory.entries(MAPPER, commits, versions(commits, List.of(version("The dashboard opens"))), Optional.of(now));

        assertFalse(entries.getFirst().isCommitted());
        assertEquals(entries.getFirst().hash(), "");
        assertEquals(entries.getFirst().who(), "Muteb");
        assertEquals(entries.getFirst().changes().size(), 2, "the expected result and the priority: " + entries.getFirst().changes());
    }

    // Rule-VIEW-PANEL-098
    @Test
    public void aTestCaseNeverCommittedIsOneUncommittedEntryMarkedCreated() {
        final @NotNull List<HistoryEntry> entries = TestCaseHistory.entries(MAPPER, List.of(), Map.of(), Optional.of(version("The dashboard opens")));

        assertEquals(entries.size(), 1);
        assertEquals(entries.getFirst().kind(), HistoryEntryKind.CREATED);
        assertFalse(entries.getFirst().isCommitted());
        assertEquals(entries.getFirst().who(), "Muteb");
    }

    // Rule-VIEW-PANEL-096
    @Test
    public void aVersionThatNoLongerParsesStillShowsWhoAndWhen() {
        final @NotNull List<HistoryCommit> commits = List.of(commit(2, "broken"), commit(1, "UC-10"));
        final @NotNull TestCaseDto now = version("The dashboard opens");
        final @NotNull Map<String, String> versions = versions(commits, List.of(now, now));
        versions.put(commits.getFirst().objectName(), "{ not a test case");

        final @NotNull HistoryEntry broken = TestCaseHistory.entries(MAPPER, commits, versions, Optional.of(now)).getFirst();

        assertEquals(broken.kind(), HistoryEntryKind.UPDATED);
        assertEquals(broken.note(), Bundle.message("view.history.unreadable"));
        assertEquals(broken.who(), "Sara");
        assertEquals(broken.message(), "broken");
    }

    // Rule-VIEW-PANEL-096
    @Test
    public void theVersionAfterAnUnreadableOneIsNotCalledReordered() {
        final @NotNull List<HistoryCommit> commits = List.of(commit(3, "Cycle 4 review"), commit(2, "broken"), commit(1, "UC-10"));
        final @NotNull TestCaseDto now = version("The dashboard opens");
        final @NotNull Map<String, String> versions = versions(commits, List.of(now, now, now));
        versions.put(commits.get(1).objectName(), "{ not a test case");

        final @NotNull HistoryEntry after = TestCaseHistory.entries(MAPPER, commits, versions, Optional.of(now)).getFirst();

        assertEquals(after.kind(), HistoryEntryKind.UPDATED);
        assertEquals(after.note(), Bundle.message("view.history.uncompared"));
        assertEquals(after.message(), "Cycle 4 review");
    }

    // Rule-VIEW-PANEL-117
    @Test
    public void anEntryCarriesTheTestCaseAsItWasAndAsItBecame() {
        final @NotNull List<HistoryCommit> commits = List.of(commit(2, "Cycle 4 review"), commit(1, "UC-10"));
        final @NotNull TestCaseDto before = version("The dashboard opens");
        final @NotNull TestCaseDto after = version("The dashboard opens within a second");

        final @NotNull List<HistoryEntry> entries = TestCaseHistory.entries(MAPPER, commits, versions(commits, List.of(after, before)), Optional.of(after));

        assertEquals(entries.getFirst().was(), CopyChoice.ALL_DETAILS.from(before));
        assertEquals(entries.getFirst().now(), CopyChoice.ALL_DETAILS.from(after));
        assertEquals(entries.getLast().was(), "", "nothing came before the commit that created it");
    }

    // Rule-VIEW-PANEL-096
    @Test
    public void aCommitThatChangedNoneOfTheFieldsHasNoRows() {
        final @NotNull List<HistoryCommit> commits = List.of(commit(2, "Reordered"), commit(1, "UC-10"));
        final @NotNull TestCaseDto now = version("The dashboard opens");

        final @NotNull HistoryEntry reordered = TestCaseHistory.entries(MAPPER, commits, versions(commits, List.of(now, now)), Optional.of(now)).getFirst();

        assertEquals(reordered.kind(), HistoryEntryKind.UPDATED);
        assertTrue(reordered.changes().isEmpty());
        assertEquals(reordered.note(), Bundle.message("git.change.no.field"));
    }

    // Rule-VIEW-PANEL-101
    @Test
    public void aLongHistoryIsReadWhole() {
        final @NotNull List<HistoryCommit> commits = IntStream.range(0, 200).mapToObj(at -> commit(200 - at, "Edit " + (200 - at))).toList();
        final @NotNull List<TestCaseDto> versions = new ArrayList<>();
        commits.forEach(commit -> versions.add(version(commit.message())));

        assertEquals(TestCaseHistory.entries(MAPPER, commits, versions(commits, versions), Optional.of(versions.getFirst())).size(), 200);
    }
}
