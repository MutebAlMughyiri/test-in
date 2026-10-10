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


import org.jetbrains.annotations.NotNull;
import org.testin.git.change.ChangeSubject;
import org.testin.git.change.DiffType;
import org.testin.git.change.PendingChange;
import org.testin.model.TestCaseDto;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GitRefsTest {

    private static @NotNull PendingChange diff(final Path relativePath) {
        return new PendingChange(ChangeSubject.TEST_CASE, "a case", "a test set", UUID.randomUUID().toString(),
                relativePath, DiffType.MODIFIED,
                TestCaseDto.builder().build(), List.of());
    }

    @Test
    public void everyFormGitCloneTakesIsAnAddress() {
        assertTrue(GitRefs.isRepositoryUrl("https://github.com/acme/repo.git"));
        assertTrue(GitRefs.isRepositoryUrl("ssh://git@github.com/acme/repo.git"));
        assertTrue(GitRefs.isRepositoryUrl("git@host:acme/repo.git"));
        assertTrue(GitRefs.isRepositoryUrl("http://localhost/x.git"), "plain http is a scheme git clone takes");
        assertTrue(GitRefs.isRepositoryUrl("git://host/x"), "the file's own rule used to drop this one");
        assertTrue(GitRefs.isRepositoryUrl("https://host/r.git?ref=main"), "a query is part of the address");
        assertTrue(GitRefs.isRepositoryUrl("  https://github.com/acme/repo.git  "), "surrounding space is trimmed");
    }

    @Test
    public void aProjectNameIsNotAnAddress() {
        assertFalse(GitRefs.isRepositoryUrl("NAFATH"));
        assertFalse(GitRefs.isRepositoryUrl("Checkout Regression"));
        assertFalse(GitRefs.isRepositoryUrl(""));
        assertFalse(GitRefs.isRepositoryUrl("   "));
    }

    @Test
    public void textWithCharactersNoAddressHasIsRefused() {
        assertFalse(GitRefs.isRepositoryUrl("https://x.com/r; rm -rf /"), "a semicolon is not in a clone address");
        assertFalse(GitRefs.isRepositoryUrl("https://x.com/r a"), "nor is a space inside one");
        assertFalse(GitRefs.isRepositoryUrl("https://x.com/\"r\""), "nor a quote");
        assertFalse(GitRefs.isRepositoryUrl("https://x.com/r|whoami"), "nor a pipe");
    }

    // Rule-SHARE-122
    @Test
    public void originWinsWhateverOrderTheRemotesArrive() {
        assertEquals(GitRefs.chooseRemote(List.of("upstream", "origin", "fork")), "origin");
    }

    // Rule-SHARE-122
    @Test
    public void withoutOriginTheFirstRemoteIsUsed() {
        assertEquals(GitRefs.chooseRemote(List.of("upstream", "fork")), "upstream");
    }

    @Test
    public void remoteNamesAreTrimmedAndBlankLinesIgnored() {
        assertEquals(GitRefs.chooseRemote(List.of("", "  ", "  upstream  ")), "upstream");
    }

    @Test
    public void noRemotesMeansNoRemoteToSyncWith() {
        assertEquals(GitRefs.chooseRemote(List.of()), "");
        assertEquals(GitRefs.chooseRemote(List.of("", "   ")), "");
    }

    @Test
    public void remoteBranchNamesLoseTheirRemotePrefix() {
        assertEquals(GitRefs.localNameOf("origin/main"), "main");
    }

    @Test
    public void aLocalBranchNameIsAlreadyItsLocalName() {
        assertEquals(GitRefs.localNameOf("main"), "main");
    }

    // Rule-SHARE-126
    @Test
    public void aLocalBranchWithASlashIsNotARemoteBranch() {
        assertFalse(GitRefs.isRemoteBranch("feature/login", List.of("feature/login", "main"), List.of("origin")));
    }

    @Test
    public void aRemoteBranchIsNamedAfterItsRemote() {
        assertTrue(GitRefs.isRemoteBranch("origin/main", List.of("main"), List.of("origin")));
        assertTrue(GitRefs.isRemoteBranch("origin/feature/login", List.of("main"), List.of("origin")));
        assertEquals(GitRefs.localNameOf("origin/feature/login"), "feature/login");
    }

    @Test
    public void aNameStartingWithNoRemoteIsNotARemoteBranch() {
        assertFalse(GitRefs.isRemoteBranch("feature/login", List.of("main"), List.of("origin")));
        assertFalse(GitRefs.isRemoteBranch("origin/main", List.of("main"), List.of()));
    }

    @Test
    public void pathsAreForwardSlashedForGitWhateverThePlatformUses() {
        final Set<String> paths = GitRefs.repoRelativePaths(List.of(
                diff(Path.of("testCases", "login", "case-1.json"))));

        assertEquals(paths, Set.of("testCases/login/case-1.json"));
    }

    @Test
    public void theSameFileSelectedTwiceIsStagedOnce() {
        final Path shared = Path.of("testCases", "login", "case-1.json");

        final Set<String> paths = GitRefs.repoRelativePaths(List.of(
                diff(shared),
                diff(Path.of("testCases", "login", "case-2.json")),
                diff(shared)));

        assertEquals(List.copyOf(paths),
                List.of("testCases/login/case-1.json", "testCases/login/case-2.json"));
    }

    @Test
    public void anUntrackedFileIsAnAddition() {
        final List<StatusEntry> entries = GitRefs.parseStatus(List.of("?? .tp"));

        assertEquals(entries.size(), 1);
        assertEquals(entries.getFirst().type(), DiffType.ADDED);
        assertEquals(entries.getFirst().path(), ".tp");
    }

    @Test
    public void everyNewTestCaseInANewTestSetIsReported() {
        final List<StatusEntry> entries = GitRefs.parseStatus(List.of(
                "?? .tp",
                "?? Test Cases/.tcf",
                "?? Test Cases/rp/.ts",
                "?? Test Cases/rp/73ebd4d7-4a2c-4813-92d5-30aebe3a3670.json",
                "?? Test Cases/rp/84e8bf04-815d-4a48-81e2-3985b1e25c75.json"));

        assertEquals(entries.size(), 5);
        assertEquals(entries.stream().filter(e -> e.path().endsWith(".json")).count(), 2);
        assertEquals(entries.get(3).path(), "Test Cases/rp/73ebd4d7-4a2c-4813-92d5-30aebe3a3670.json");
    }

    @Test
    public void aModifiedFileIsReportedWhicheverColumnCarriesIt() {
        assertEquals(GitRefs.parseStatus(List.of(" M a.json")).getFirst().type(), DiffType.MODIFIED);
        assertEquals(GitRefs.parseStatus(List.of("M  a.json")).getFirst().type(), DiffType.MODIFIED);
        assertEquals(GitRefs.parseStatus(List.of("MM a.json")).getFirst().type(), DiffType.MODIFIED);
    }

    @Test
    public void aDeletedFileIsReportedWhicheverColumnCarriesIt() {
        assertEquals(GitRefs.parseStatus(List.of(" D a.json")).getFirst().type(), DiffType.DELETED);
        assertEquals(GitRefs.parseStatus(List.of("D  a.json")).getFirst().type(), DiffType.DELETED);
    }

    @Test
    public void aStagedAdditionIsAnAddition() {
        assertEquals(GitRefs.parseStatus(List.of("A  a.json")).getFirst().type(), DiffType.ADDED);
    }

    // Rule-SHARE-048
    @Test
    public void aRenameIsBothTheDeletionAndTheAddition() {
        final List<StatusEntry> entries = GitRefs.parseStatus(
                List.of("R  Test Cases/new/a.json", "Test Cases/old/a.json"));

        assertEquals(entries.size(), 2);

        assertEquals(entries.getFirst().type(), DiffType.DELETED);
        assertEquals(entries.getFirst().path(), "Test Cases/old/a.json");

        assertEquals(entries.get(1).type(), DiffType.ADDED);
        assertEquals(entries.get(1).path(), "Test Cases/new/a.json");
    }

    @Test
    public void aCopyIsOnlyTheNewFile() {
        final List<StatusEntry> entries = GitRefs.parseStatus(List.of("C  b.json", "a.json", " M c.json"));

        assertEquals(entries.size(), 2);
        assertEquals(entries.getFirst().path(), "b.json");
        assertEquals(entries.get(1).path(), "c.json", "the copy's source is not read as the next change");
    }

    @Test
    public void aRenameThenDeletedIsTwoDeletions() {
        final List<StatusEntry> entries = GitRefs.parseStatus(List.of("RD b.json", "a.json"));

        assertEquals(entries.size(), 2);
        assertEquals(entries.getFirst().type(), DiffType.DELETED);
        assertEquals(entries.get(1).type(), DiffType.DELETED);
    }

    @Test
    public void aRenameOrACopyIsCountedAsOneChange() {
        assertEquals(GitRefs.changeCount(List.of("R  new.json", "old.json", "C  b.json", "a.json", " M c.json")), 3);
    }

    @Test
    public void aPathIsTakenAsGitWroteItWithNoQuotingToUndo() {
        assertEquals(GitRefs.parseStatus(List.of("?? Test Cases/\"quoted\" تسجيل/a.json")).getFirst().path(),
                "Test Cases/\"quoted\" تسجيل/a.json");
    }

    @Test
    public void nulSeparatedOutputSplitsIntoRecords() {
        assertEquals(GitRefs.records("?? a b.json\0R  new.json\0old.json\0"), List.of("?? a b.json", "R  new.json", "old.json"));
        assertEquals(GitRefs.records(""), List.of());
    }

    @Test
    public void everyDirectoryAboveASelectedTestCaseIsFound() {
        assertEquals(GitRefs.ancestorDirectories(List.of(
                        "Test Cases/rp/a.json",
                        "Test Cases/rp/b.json",
                        "Test Cases/login flow/c.json")),
                Set.of("", "Test Cases", "Test Cases/rp", "Test Cases/login flow"));
    }

    @Test
    public void aFileAtTheRepositoryRootHasOnlyTheRoot() {
        assertEquals(GitRefs.ancestorDirectories(List.of("a.json")), Set.of(""));
    }

    @Test
    public void anIgnoredFileIsNotAChange() {
        assertEquals(GitRefs.parseStatus(List.of("!! build/output.json")), List.of());
    }

    @Test
    public void emptyAndTruncatedLinesAreSkipped() {
        assertEquals(GitRefs.parseStatus(List.of("", "  ", "??")), List.of());
    }

    // Rule-SHARE-108
    @Test
    public void anEmailAddressIsSomethingAnAtSignAndADottedDomain() {
        assertTrue(GitRefs.isEmailAddress("sara@example.com"));
        assertTrue(GitRefs.isEmailAddress("sara.otaibi@qa.example.org"));
        assertTrue(GitRefs.isEmailAddress("  sara@example.com  "), "the space around the address is not part of it");
    }

    // Rule-SHARE-108
    @Test
    public void whatIsPlainlyNotAnEmailAddressIsRefused() {
        assertFalse(GitRefs.isEmailAddress("sara"), "no at sign");
        assertFalse(GitRefs.isEmailAddress("@example.com"), "nothing before the at sign");
        assertFalse(GitRefs.isEmailAddress("sara@localhost"), "no dot after the at sign");
        assertFalse(GitRefs.isEmailAddress("sara@example."), "nothing after the last dot");
        assertFalse(GitRefs.isEmailAddress("sara otaibi@example.com"), "a space inside the address");
        assertFalse(GitRefs.isEmailAddress("sara@@example.com"), "two at signs");
    }

    // Rule-SHARE-079
    @Test
    public void theConflictMessageNamesTheFilesStillInTheWay() {
        final @NotNull String said = GitRefs.conflictMessage(List.of("notes.txt", "Test Cases/login/a.tc"));

        assertTrue(said.contains("notes.txt") && said.contains("Test Cases/login/a.tc"), said);
    }

    // Rule-SHARE-079
    @Test
    public void aLongListNamesThreeFilesAndCountsTheRest() {
        final @NotNull String said = GitRefs.conflictMessage(List.of("a.tc", "b.tc", "c.tc", "d.tc", "e.tc"));

        assertTrue(said.contains("a.tc, b.tc, c.tc"), said);
        assertFalse(said.contains("d.tc"), said);
        assertTrue(said.contains("2 more"), said);
    }

}
