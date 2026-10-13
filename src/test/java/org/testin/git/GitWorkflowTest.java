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
import org.testin.TempTree;
import org.testin.git.change.ChangeSubject;
import org.testin.git.change.ChangeType;
import org.testin.git.change.GitStatusKind;
import org.testin.git.change.PendingChangeReader;
import org.testin.git.change.PendingChange;
import org.testin.git.conflict.MergeOutcome;
import org.testin.git.conflict.TestCaseMerger;
import org.testin.model.Priority;
import org.testin.model.TestCase;
import org.testin.testcase.Rank;
import org.testin.testcase.TestCaseOrder;
import org.testin.util.RealJsonCodec;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.testin.git.LocalGit.git;
import static org.testin.git.LocalGit.mustGit;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GitWorkflowTest {

    private static final int WINDOWS_COMMAND_LINE_LIMIT = 32767;
    private Path remote;

    private Path work;

    private static @NotNull Map<String, String> contents(final Path directory, final String revision, final List<String> relativePaths) {
        try {
            final Process process = new ProcessBuilder("git", "cat-file", "--batch").directory(directory.toFile()).start();
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(GitCommandRunner.batchRequest(revision, relativePaths));
            }

            final byte[] batch = process.getInputStream().readAllBytes();
            assertEquals(process.waitFor(), 0, "git cat-file --batch failed in " + directory);
            return GitCommandRunner.objectsIn(relativePaths, batch);
        } catch (final IOException | InterruptedException ex) {
            throw new AssertionError(ex);
        }
    }

    @BeforeMethod
    public void createRepositories() {
        try {
            if (!LocalGit.onThePath()) {
                throw new SkipException("Git is not on the PATH, so the workflow cannot be exercised");
            }

            final Path base = Files.createTempDirectory("testin-workflow");
            remote = base.resolve("remote.git");
            work = base.resolve("work");
            Files.createDirectories(remote);
            Files.createDirectories(work);

            mustGit(remote, "init", "--bare", "--initial-branch=main");
            mustGit(work, "init", "--initial-branch=main");

            mustGit(work, "config", "user.name", "Testin Test");
            mustGit(work, "config", "user.email", "testin@example.invalid");
            mustGit(work, "remote", "add", "origin", remote.toUri().toString());
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @AfterMethod
    public void removeRepositories() {
        TempTree.delete(Objects.requireNonNull(remote.getParent(), "the remote has no folder"));
    }

    @Test
    public void aCommitTooLargeForTheCommandLineStillLands() {
        try {
            mustGit(work, "commit", "--allow-empty", "-m", "root");

            final Path set = work.resolve("test-01").resolve("Test Cases").resolve("pkg1").resolve("Login");
            Files.createDirectories(set);

            final Set<String> paths = new LinkedHashSet<>();
            for (int i = 0; i < 1200; i++) {
                final String name = UUID.randomUUID() + ".testcase";
                Files.writeString(set.resolve(name), "{}");
                paths.add("test-01/Test Cases/pkg1/Login/" + name);
            }

            final int asArguments = paths.stream().mapToInt(path -> path.length() + 3).sum();
            assertTrue(asArguments > WINDOWS_COMMAND_LINE_LIMIT,
                    "the point of this test is a list that cannot be passed as arguments, and this one is only "
                            + asArguments + " characters");

            final Path pathspec = Files.createTempFile("testin-pathspec", ".lst");
            Files.write(pathspec, GitCommandRunner.pathspecBytes(paths));

            mustGit(work, "add", "--pathspec-from-file=" + pathspec, "--pathspec-file-nul");
            mustGit(work, "commit", "--only", "-m", "imported 1200 test cases",
                    "--pathspec-from-file=" + pathspec, "--pathspec-file-nul");

            final long committed = mustGit(work, "show", "--name-only", "--pretty=format:", "HEAD")
                    .lines().filter(line -> !line.isBlank()).count();

            assertEquals(committed, 1200, "every selected test case belongs in the commit");
            assertTrue(mustGit(work, "status", "--porcelain").isBlank(), "nothing should be left behind");

            Files.deleteIfExists(pathspec);
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    private @NotNull TestCase testCase(final String description) {
        return TestCase.builder()
                .description(description)
                .expectedResult("the account dashboard opens")
                .steps(new ArrayList<>(List.of("open the app", "sign in")))
                .priority(Priority.HIGH)
                .module("authentication")
                .build();
    }

    private void write(final Path root, final String relativePath, final Object content) {

        try {
            final Path file = root.resolve(relativePath);
            Files.createDirectories(file.getParent() == null ? root : file.getParent());
            Files.writeString(file, content instanceof String text ? text : RealJsonCodec.build().writeValueAsString(content),
                    StandardCharsets.UTF_8);

        } catch (final IOException ex) {

            throw new AssertionError(ex);

        }

    }

    private @NotNull List<TestCase> writeTestProject() {
        write(work, ".testproject", "{\"status\":\"ACTIVE\"}");
        write(work, "Test Cases/.testcasesfolder", "{}");
        write(work, "Test Runs/.testrunsfolder", "{}");
        write(work, "Test Cases/login flow/.testset", "{}");

        final List<TestCase> testCases = List.of(
                testCase("a registered user signs in"),
                testCase("a wrong password is refused"));

        String rank = "";
        for (final TestCase testCase : testCases) {
            rank = Rank.after(rank);
            testCase.setOrder(rank);
        }

        for (final TestCase testCase : testCases) {
            write(work, "Test Cases/login flow/" + testCase.getId() + ".testcase", testCase);
        }
        return testCases;
    }

    private @NotNull List<PendingChange> review() {
        final List<String> gitStatus = GitStatusLines.records(mustGit(work, "status", "--porcelain", "-z", "-uall"));

        return PendingChangeReader.toDiffs(gitStatus, work, RealJsonCodec.build(), paths -> contents(work, "HEAD", paths), _ -> Optional.empty());
    }

    private @NotNull Set<String> stagedFor(final List<PendingChange> review) {
        final Set<String> paths = new LinkedHashSet<>(GitCommitAndPush.repoRelativePaths(review));
        paths.addAll(GitCommitAndPush.markersAlongside(work, paths));
        return paths;
    }

    private void commit(final Set<String> paths, final String message) {
        final Set<String> stageable = GitCommitAndPush.stageable(work, paths);

        if (!stageable.isEmpty()) {
            final List<String> add = new ArrayList<>(List.of("add", "--"));
            add.addAll(stageable);
            mustGit(work, add.toArray(String[]::new));
        }

        final List<String> commit = new ArrayList<>(List.of("commit", "--only", "-m", message, "--"));
        commit.addAll(paths);
        mustGit(work, commit.toArray(String[]::new));
    }

    private @NotNull Path cloneAsColleague() {

        final Path colleague = Objects.requireNonNull(remote.getParent(), remote + " has no folder").resolve("colleague");
        mustGit(remote.getParent(), "clone", remote.toUri().toString(), colleague.toString());
        return colleague;

    }

    @Test
    public void aNewTestProjectIsReviewedCommittedAndPushed() {
        final List<TestCase> testCases = writeTestProject();

        final List<PendingChange> pending = review();
        assertEquals(pending.stream().filter(change -> change.subject() == ChangeSubject.TEST_CASE).count(), 2,
                "both new test cases are in the review");
        assertTrue(pending.stream().anyMatch(change -> change.subject() == ChangeSubject.MARKER),
                "and so are the markers that make their directories nodes");
        assertTrue(pending.stream().allMatch(diff -> diff.gitStatusKind() == GitStatusKind.ADDED));

        commit(stagedFor(pending), "the first commit");
        assertTrue(git(work, "push", "-u", "origin", "main").isPresent(), "the push to an empty remote succeeded");

        final Path colleague = cloneAsColleague();
        for (final TestCase testCase : testCases) {
            assertTrue(Files.exists(colleague.resolve("Test Cases/login flow/" + testCase.getId() + ".testcase")),
                    "the colleague received " + testCase.getDescription());
        }
    }

    @Test
    public void whatTheColleagueClonesIsAUsableTestProject() {
        writeTestProject();

        commit(stagedFor(review()), "the first commit");
        mustGit(work, "push", "-u", "origin", "main");

        final Path colleague = cloneAsColleague();

        assertTrue(Files.exists(colleague.resolve(".testproject")), "the test project marker travelled");
        assertTrue(Files.exists(colleague.resolve("Test Cases/.testcasesfolder")), "the test cases container marker travelled");
        assertTrue(Files.exists(colleague.resolve("Test Cases/login flow/.testset")),
                "the test set marker travelled - without it the test cases are in a directory nothing recognises");
    }

    @Test
    public void onlyTheMarkersAboveTheSelectedTestCasesTravel() {
        writeTestProject();

        final Set<String> staged = stagedFor(review().stream()
                .filter(change -> change.subject() == ChangeSubject.TEST_CASE)
                .toList());

        assertTrue(staged.contains(".testproject"));
        assertTrue(staged.contains("Test Cases/.testcasesfolder"));
        assertTrue(staged.contains("Test Cases/login flow/.testset"));
        assertFalse(staged.contains("Test Runs/.testrunsfolder"),
                "no test case sits under Test Runs, so its marker is not part of this commit");
    }

    @Test
    public void editingATestCaseShowsExactlyWhatChanged() {
        final List<TestCase> testCases = writeTestProject();
        commit(stagedFor(review()), "the first commit");

        assertEquals(review(), List.of(), "nothing is pending straight after a commit");

        final TestCase edited = testCases.getFirst().setModule("payments");
        write(work, "Test Cases/login flow/" + edited.getId() + ".testcase", edited);

        final List<PendingChange> pending = review();

        assertEquals(pending.size(), 1);
        assertEquals(pending.getFirst().gitStatusKind(), GitStatusKind.MODIFIED);
        assertEquals(pending.getFirst().fieldChanges().size(), 1, "one field moved, so one row");
        assertEquals(pending.getFirst().fieldChanges().getFirst().changeType(), ChangeType.CHANGE_MODULE);
        assertEquals(pending.getFirst().fieldChanges().getFirst().oldValue(), "authentication");
        assertEquals(pending.getFirst().fieldChanges().getFirst().newValue(), "payments");
    }

    @Test
    public void addingATestCaseToACommittedTestSetIsReviewedAsAnAddition() {
        writeTestProject();
        commit(stagedFor(review()), "the first commit");

        final TestCase extra = testCase("a locked account cannot sign in");
        write(work, "Test Cases/login flow/" + extra.getId() + ".testcase", extra);

        final List<PendingChange> pending = review();

        assertEquals(pending.size(), 1);
        assertEquals(pending.getFirst().gitStatusKind(), GitStatusKind.ADDED);
        assertEquals(pending.getFirst().name(), "a locked account cannot sign in");
    }

    @Test
    public void deletingATestCaseIsReviewedFromWhatWasCommitted() {
        try {
            final List<TestCase> testCases = writeTestProject();
            commit(stagedFor(review()), "the first commit");

            Files.delete(work.resolve("Test Cases/login flow/" + testCases.getFirst().getId() + ".testcase"));

            final List<PendingChange> pending = review();

            assertEquals(pending.size(), 1);
            assertEquals(pending.getFirst().gitStatusKind(), GitStatusKind.DELETED);
            assertEquals(pending.getFirst().name(), "a registered user signs in");
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void aRenameStagedElsewhereCommitsBothSides() {
        final List<TestCase> testCases = writeTestProject();
        commit(stagedFor(review()), "the first commit");

        final String file = testCases.getFirst().getId() + ".testcase";
        mustGit(work, "mv", "Test Cases/login flow/" + file, "Test Cases/" + file);

        final List<PendingChange> pending = review();
        assertEquals(pending.stream().filter(change -> change.gitStatusKind() == GitStatusKind.DELETED).count(), 1);
        assertEquals(pending.stream().filter(change -> change.gitStatusKind() == GitStatusKind.ADDED).count(), 1);

        commit(stagedFor(pending), "the case moved out of the test set");

        final List<String> committed = mustGit(work, "ls-tree", "-r", "--name-only", "HEAD")
                .lines().filter(line -> line.endsWith(file)).toList();

        assertEquals(committed.size(), 1);
        assertEquals(committed.getFirst(), "Test Cases/" + file);
        assertEquals(mustGit(work, "status", "--porcelain", "-uall").strip(), "");
    }

    @Test
    public void aConflictedTestCaseIsMergedFieldByFieldAndTheRebaseFinishes() {
        try {
            final List<TestCase> testCases = writeTestProject();
            commit(stagedFor(review()), "the first commit");
            mustGit(work, "push", "-u", "origin", "main");

            final String relativePath = "Test Cases/login flow/" + testCases.getFirst().getId() + ".testcase";

            final Path colleague = cloneAsColleague();
            mustGit(colleague, "config", "user.name", "Colleague");
            mustGit(colleague, "config", "user.email", "colleague@example.invalid");

            final Path theirCopy = colleague.resolve(relativePath);
            final TestCase theirs = RealJsonCodec.build().readValue(Files.readString(theirCopy, StandardCharsets.UTF_8), TestCase.class);
            Files.writeString(theirCopy, RealJsonCodec.build().writeValueAsString(
                            theirs.setExpectedResult("the dashboard opens within two seconds").setUpdatedBy("colleague")),
                    StandardCharsets.UTF_8);
            mustGit(colleague, "commit", "-am", "tightened the expected result");
            mustGit(colleague, "push", "origin", "main");

            final Path myCopy = work.resolve(relativePath);
            final TestCase mine = RealJsonCodec.build().readValue(Files.readString(myCopy, StandardCharsets.UTF_8), TestCase.class);
            Files.writeString(myCopy, RealJsonCodec.build().writeValueAsString(
                            mine.setDescription("a registered user signs in with a valid password").setUpdatedBy("muteb")),
                    StandardCharsets.UTF_8);
            commit(stagedFor(review()), "reworded the description");

            assertTrue(git(work, "pull", "--rebase", "--autostash", "origin", "main").isEmpty(),
                    "the pull is expected to stop on the conflict");

            final List<String> conflicting = GitStatusLines.records(mustGit(work, "diff", "--name-only", "--diff-filter=U", "-z"));
            assertEquals(conflicting, List.of(relativePath));

            final Path gitDirectory = Path.of(mustGit(work, "rev-parse", "--absolute-git-dir").trim());
            assertTrue(Files.isDirectory(gitDirectory.resolve("rebase-merge")) || Files.isDirectory(gitDirectory.resolve("rebase-apply")),
                    "the stopped rebase is found under the directory Git names");

            final String base = contents(work, ":1", conflicting).get(relativePath);
            final String remote = contents(work, ":2", conflicting).get(relativePath);
            final String replayed = contents(work, ":3", conflicting).get(relativePath);

            final MergeOutcome merge = TestCaseMerger.of(RealJsonCodec.build(), Objects.requireNonNull(base, "no base"), Objects.requireNonNull(replayed, "no replayed side"), Objects.requireNonNull(remote, "no remote side"));
            assertTrue(merge.isSettled(), "different fields are not a disagreement");

            Files.writeString(myCopy, merge.merged().toPrettyString(), StandardCharsets.UTF_8);
            mustGit(work, "add", "--", relativePath);
            mustGit(work, "-c", "core.editor=true", "rebase", "--continue");

            final TestCase merged = RealJsonCodec.build().readValue(Files.readString(myCopy, StandardCharsets.UTF_8), TestCase.class);
            assertEquals(merged.getDescription(), "a registered user signs in with a valid password");
            assertEquals(merged.getExpectedResult(), "the dashboard opens within two seconds");
            assertEquals(mustGit(work, "status", "--porcelain", "-uall").strip(), "");
            assertEquals(review(), List.of(), "a resolved rebase leaves nothing pending");
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void twoTestersAddingTestCasesToOneSetDoNotConflictAtAll() {
        try {
            writeTestProject();
            commit(stagedFor(review()), "the first commit");
            mustGit(work, "push", "-u", "origin", "main");

            final Path colleague = cloneAsColleague();
            mustGit(colleague, "config", "user.name", "Colleague");
            mustGit(colleague, "config", "user.email", "colleague@example.invalid");

            final TestCase theirNewTestCase = testCase("a locked account cannot sign in").setOrder("s");
            write(colleague, "Test Cases/login flow/" + theirNewTestCase.getId() + ".testcase", theirNewTestCase);

            mustGit(colleague, "add", "-A");
            mustGit(colleague, "commit", "-m", "added the locked account test case");
            mustGit(colleague, "push", "origin", "main");

            final TestCase myNewTestCase = testCase("a signed-in user signs out").setOrder("s");
            write(work, "Test Cases/login flow/" + myNewTestCase.getId() + ".testcase", myNewTestCase);
            commit(stagedFor(review()), "added the sign out test case");

            assertTrue(git(work, "pull", "--rebase", "--autostash", "origin", "main").isPresent(),
                    "two appended test cases touch two files and merge on their own");

            final List<TestCase> after = new ArrayList<>();
            try (Stream<Path> files = Files.list(work.resolve("Test Cases/login flow"))) {
                for (final Path file : files.filter(candidate -> candidate.getFileName().toString().endsWith(".testcase")).sorted().toList()) {
                    after.add(RealJsonCodec.build().readValue(Files.readString(file, StandardCharsets.UTF_8), TestCase.class));
                }
            }

            assertEquals(after.size(), 4, "both testers keep their test case");

            final List<TestCase> ordered = TestCaseOrder.ordered(after);
            assertEquals(ordered, TestCaseOrder.ordered(new ArrayList<>(after.reversed())),
                    "the order does not depend on what order the files were read in");
            assertTrue(ordered.stream().anyMatch(testCase -> testCase.getId().equals(theirNewTestCase.getId())));
            assertTrue(ordered.stream().anyMatch(testCase -> testCase.getId().equals(myNewTestCase.getId())));
            assertEquals(mustGit(work, "status", "--porcelain", "-uall").strip(), "");
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void aColleaguesChangeArrivesOnAPull() {
        try {
            final List<TestCase> testCases = writeTestProject();
            commit(stagedFor(review()), "the first commit");
            mustGit(work, "push", "-u", "origin", "main");

            final Path colleague = cloneAsColleague();
            mustGit(colleague, "config", "user.name", "Colleague");
            mustGit(colleague, "config", "user.email", "colleague@example.invalid");

            final Path theirCopy = colleague.resolve("Test Cases/login flow/" + testCases.getFirst().getId() + ".testcase");
            final TestCase theirs = RealJsonCodec.build().readValue(Files.readString(theirCopy, StandardCharsets.UTF_8), TestCase.class);
            Files.writeString(theirCopy, RealJsonCodec.build().writeValueAsString(theirs.setExpectedResult("the dashboard opens within two seconds")),
                    StandardCharsets.UTF_8);

            mustGit(colleague, "commit", "-am", "tightened the expected result");
            mustGit(colleague, "push", "origin", "main");

            mustGit(work, "pull", "--rebase", "--autostash", "origin", "main");

            final TestCase pulled = RealJsonCodec.build().readValue(
                    Files.readString(work.resolve("Test Cases/login flow/" + testCases.getFirst().getId() + ".testcase"),
                            StandardCharsets.UTF_8), TestCase.class);

            assertEquals(pulled.getExpectedResult(), "the dashboard opens within two seconds");
            assertEquals(review(), List.of(), "a clean pull leaves nothing pending");
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void anEmptyRemoteNamesNoHeadBranch() {
        assertEquals(mustGit(work, "for-each-ref", GitRepositoryService.REMOTE_HEAD_BRANCH, "refs/remotes/origin/HEAD").trim(), "",
                "an empty remote names no branch, so the push falls back to the local one");
    }

    @Test
    public void aCloneNamesItsRemotesDefaultBranchWithoutAskingTheServer() {
        mustGit(work, "commit", "--allow-empty", "-m", "root");
        mustGit(work, "push", "-u", "origin", "main");

        final Path colleague = cloneAsColleague();
        mustGit(colleague, "branch", "feature/login");

        assertEquals(mustGit(colleague, "for-each-ref", GitRepositoryService.REMOTE_HEAD_BRANCH, "refs/remotes/origin/HEAD").trim(), "main");
        assertEquals(mustGit(colleague, "for-each-ref", GitRepositoryService.BRANCH_NAMES, "refs/heads", "refs/remotes").lines()
                        .filter(name -> !name.isBlank()).sorted().toList(),
                List.of("feature/login", "main", "origin/main"),
                "the remote's HEAD is a pointer, not a branch to offer");
    }

    @Test
    public void aLinkedWorktreeHasItsOwnGitDirectory() {
        mustGit(work, "commit", "--allow-empty", "-m", "root");
        final Path linked = Objects.requireNonNull(remote.getParent(), remote + " has no folder").resolve("linked");
        mustGit(work, "worktree", "add", "-b", "side", linked.toString());

        assertTrue(Files.isRegularFile(linked.resolve(".git")), "a linked worktree's .git is a file, not a folder");
        assertTrue(Files.isDirectory(Path.of(mustGit(linked, "rev-parse", "--absolute-git-dir").trim())),
                "Git still names the directory its rebase state is kept in");
    }

    @Test
    public void gitReportsNewTestCasesAsUntrackedWithTheirPathsAsWritten() {
        writeTestProject();

        final List<String> gitStatus = GitStatusLines.records(mustGit(work, "status", "--porcelain", "-z", "-uall"));

        assertTrue(gitStatus.stream().anyMatch(record -> record.startsWith("?? Test Cases/login flow/")),
                "a path with a space comes back unquoted: " + gitStatus);
        assertEquals(GitStatusLines.parseGitStatus(gitStatus).stream()
                .filter(entry -> entry.path().endsWith(".testcase")).count(), 2);
    }
}
