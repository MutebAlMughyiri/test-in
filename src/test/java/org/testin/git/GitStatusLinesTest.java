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

package org.testin.git;

import org.testin.git.change.GitStatusKind;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class GitStatusLinesTest {

    @Test
    public void anUntrackedFileIsAnAddition() {
        final List<GitStatusEntry> entries = GitStatusLines.parseGitStatus(List.of("?? .testproject"));

        assertEquals(entries.size(), 1);
        assertEquals(entries.getFirst().gitStatusKind(), GitStatusKind.ADDED);
        assertEquals(entries.getFirst().path(), ".testproject");
    }

    @Test
    public void everyNewTestCaseInANewTestSetIsReported() {
        final List<GitStatusEntry> entries = GitStatusLines.parseGitStatus(List.of(
                "?? .testproject",
                "?? Test Cases/.testcasesfolder",
                "?? Test Cases/rp/.testset",
                "?? Test Cases/rp/73ebd4d7-4a2c-4813-92d5-30aebe3a3670.json",
                "?? Test Cases/rp/84e8bf04-815d-4a48-81e2-3985b1e25c75.json"));

        assertEquals(entries.size(), 5);
        assertEquals(entries.stream().filter(e -> e.path().endsWith(".json")).count(), 2);
        assertEquals(entries.get(3).path(), "Test Cases/rp/73ebd4d7-4a2c-4813-92d5-30aebe3a3670.json");
    }

    @Test
    public void aModifiedFileIsReportedWhicheverColumnCarriesIt() {
        assertEquals(GitStatusLines.parseGitStatus(List.of(" M a.json")).getFirst().gitStatusKind(), GitStatusKind.MODIFIED);
        assertEquals(GitStatusLines.parseGitStatus(List.of("M  a.json")).getFirst().gitStatusKind(), GitStatusKind.MODIFIED);
        assertEquals(GitStatusLines.parseGitStatus(List.of("MM a.json")).getFirst().gitStatusKind(), GitStatusKind.MODIFIED);
    }

    @Test
    public void aDeletedFileIsReportedWhicheverColumnCarriesIt() {
        assertEquals(GitStatusLines.parseGitStatus(List.of(" D a.json")).getFirst().gitStatusKind(), GitStatusKind.DELETED);
        assertEquals(GitStatusLines.parseGitStatus(List.of("D  a.json")).getFirst().gitStatusKind(), GitStatusKind.DELETED);
    }

    @Test
    public void aStagedAdditionIsAnAddition() {
        assertEquals(GitStatusLines.parseGitStatus(List.of("A  a.json")).getFirst().gitStatusKind(), GitStatusKind.ADDED);
    }

    // Rule-SHARE-048
    @Test
    public void aRenameIsBothTheDeletionAndTheAddition() {
        final List<GitStatusEntry> entries = GitStatusLines.parseGitStatus(
                List.of("R  Test Cases/new/a.json", "Test Cases/old/a.json"));

        assertEquals(entries.size(), 2);

        assertEquals(entries.getFirst().gitStatusKind(), GitStatusKind.DELETED);
        assertEquals(entries.getFirst().path(), "Test Cases/old/a.json");

        assertEquals(entries.get(1).gitStatusKind(), GitStatusKind.ADDED);
        assertEquals(entries.get(1).path(), "Test Cases/new/a.json");
    }

    @Test
    public void aCopyIsOnlyTheNewFile() {
        final List<GitStatusEntry> entries = GitStatusLines.parseGitStatus(List.of("C  b.json", "a.json", " M c.json"));

        assertEquals(entries.size(), 2);
        assertEquals(entries.getFirst().path(), "b.json");
        assertEquals(entries.get(1).path(), "c.json", "the copy's source is not read as the next change");
    }

    @Test
    public void aRenameThenDeletedIsTwoDeletions() {
        final List<GitStatusEntry> entries = GitStatusLines.parseGitStatus(List.of("RD b.json", "a.json"));

        assertEquals(entries.size(), 2);
        assertEquals(entries.getFirst().gitStatusKind(), GitStatusKind.DELETED);
        assertEquals(entries.get(1).gitStatusKind(), GitStatusKind.DELETED);
    }

    @Test
    public void aRenameOrACopyIsCountedAsOneChange() {
        assertEquals(GitStatusLines.changeCount(List.of("R  new.json", "old.json", "C  b.json", "a.json", " M c.json")), 3);
    }

    @Test
    public void aPathIsTakenAsGitWroteItWithNoQuotingToUndo() {
        assertEquals(GitStatusLines.parseGitStatus(List.of("?? Test Cases/\"quoted\" تسجيل/a.json")).getFirst().path(),
                "Test Cases/\"quoted\" تسجيل/a.json");
    }

    @Test
    public void nulSeparatedOutputSplitsIntoRecords() {
        assertEquals(GitStatusLines.records("?? a b.json\0R  new.json\0old.json\0"), List.of("?? a b.json", "R  new.json", "old.json"));
        assertEquals(GitStatusLines.records(""), List.of());
    }

    @Test
    public void anIgnoredFileIsNotAChange() {
        assertEquals(GitStatusLines.parseGitStatus(List.of("!! build/output.json")), List.of());
    }

    @Test
    public void emptyAndTruncatedLinesAreSkipped() {
        assertEquals(GitStatusLines.parseGitStatus(List.of("", "  ", "??")), List.of());
    }
}
