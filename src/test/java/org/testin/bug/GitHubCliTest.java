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
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GitHubCliTest {

    private static final String READY_VERSION = """
            gh version 2.100.0 (2026-09-03)
            https://github.com/cli/cli/releases/tag/v2.100.0
            """;

    private static final String ISSUE = "https://github.com/mtb550/test-in/issues/412";

    private static @NotNull ProcessOutput answer(final String stdout, final String stderr, final int exitCode) {
        return new ProcessOutput(stdout, stderr, exitCode, false, false);
    }

    private static @NotNull Optional<String> whyNot(final FakeGh gh, final String bugRepoUrl) {
        return gh.cli().whyItCannotSend(bugRepoUrl);
    }

    // Rule-VIEW-PANEL-073
    @Test
    public void ghThatTimedOutOrWasCancelledDidNotSucceedWhateverItsExitCode() {
        assertTrue(GitHubCli.succeeded(answer("", "", 0)));
        assertFalse(GitHubCli.succeeded(new ProcessOutput("", "", 0, true, false)), "gh that timed out succeeded");
        assertFalse(GitHubCli.succeeded(new ProcessOutput("", "", 0, false, true)), "gh that was cancelled succeeded");
    }

    @Test
    public void noBugRepoUrlNeverStartsGh() {
        final FakeGh gh = new FakeGh();

        assertEquals(whyNot(gh, " "), Optional.of(Bundle.message("bug.reason.no.bug.repo.url")));
        assertTrue(gh.asked.isEmpty());
    }

    @Test
    public void aBugRepoUrlThatIsNotARepositoryNeverStartsGh() {
        final FakeGh gh = new FakeGh();

        assertEquals(whyNot(gh, "https://github.com/mtb550/test-in/issues"), Optional.of(Bundle.message("bug.reason.not.a.repository")));
        assertTrue(gh.asked.isEmpty());
    }

    @Test
    public void noGhOnThePathIsTheReason() {
        assertEquals(whyNot(new FakeGh(), "https://github.com/mtb550/test-in"), Optional.of(Bundle.message("bug.reason.no.gh")));
    }

    @Test
    public void anOldGhIsTheReasonAndSignInIsNotAsked() {
        final FakeGh gh = new FakeGh(answer("gh version 2.98.3 (2026-08-01)\n", "", 0));

        assertEquals(whyNot(gh, "https://github.com/mtb550/test-in"), Optional.of(Bundle.message("bug.reason.old.gh", "2.98.3", "2.99.0")));
        assertEquals(gh.asked, List.of(List.of("--version")));
    }

    @Test
    public void versionsAreComparedNumberByNumber() {
        assertTrue(GitHubCli.isNewEnough(READY_VERSION), "2.100.0 is newer than 2.99.0");
        assertTrue(GitHubCli.isNewEnough("gh version 2.99.0 (2026-09-01)"));
        assertTrue(GitHubCli.isNewEnough("gh version 2.99.1-rc.1"));
        assertTrue(GitHubCli.isNewEnough("gh version 3.0.0"));
        assertTrue(GitHubCli.isNewEnough("gh version DEV"), "somebody's own build");
        assertFalse(GitHubCli.isNewEnough("gh version 2.9.100"));
        assertFalse(GitHubCli.isNewEnough("gh version 1.200.0"));
        assertFalse(GitHubCli.isNewEnough("gh version 2.98"));
        assertFalse(GitHubCli.isNewEnough("gh version 2.99"), "a version too short to compare is not taken for new enough");
        assertFalse(GitHubCli.isNewEnough("gh version something"));
        assertFalse(GitHubCli.isNewEnough(""));
    }

    @Test
    public void signedOutNamesTheHostAndOnlyTheActiveAccountIsAsked() {
        final FakeGh gh = new FakeGh(answer(READY_VERSION, "", 0), answer("", "You are not logged into any GitHub hosts.", 1));

        assertEquals(whyNot(gh, "git@github.example.com:qa/product.git"), Optional.of(Bundle.message("bug.reason.signed.out", "github.example.com")));
        assertEquals(gh.asked.get(1), List.of("auth", "status", "--active", "--hostname", "github.example.com"));
    }

    @Test
    public void signedInIsReady() {
        assertEquals(whyNot(new FakeGh(answer(READY_VERSION, "", 0), answer("github.com\n  ✓ Logged in", "", 0)), "https://github.com/mtb550/test-in"), Optional.empty());
    }

    @Test
    public void theIssueIsFiledFromAFreshFolderThatIsGoneAfterwards() {
        final byte[] first = {(byte) 0x89, 'P', 'N', 'G', 1};
        final byte[] second = {(byte) 0x89, 'P', 'N', 'G', 2};
        final List<Path> folders = new ArrayList<>();
        final List<List<String>> asked = new ArrayList<>();

        final GitHubCli cli = new GitHubCli((arguments, folder) -> {
            folders.add(folder);
            asked.add(arguments);
            try {
                final byte[] body = Files.readAllBytes(folder.resolve("bug.md"));
                assertEquals(new String(body, StandardCharsets.UTF_8), "| 🟡 Minor |\nباگ", "the body is written as UTF-8");
                assertFalse(body.length >= 3 && body[0] == (byte) 0xEF && body[1] == (byte) 0xBB && body[2] == (byte) 0xBF, "and without a BOM");
                assertEquals(Files.readAllBytes(folder.resolve("screenshot-1.png")), first);
                assertEquals(Files.readAllBytes(folder.resolve("screenshot-2.png")), second);
            } catch (final IOException ex) {
                throw new AssertionError("gh would have found nothing to send", ex);
            }
            return Optional.of(answer(ISSUE + "\n", "", 0));
        });

        final IssueCreation created = cli.create(new BugRepository("github.com", "mtb550", "test-in"), "-v is not a flag", "| 🟡 Minor |\nباگ", List.of(first, second));

        assertEquals(created, new IssueCreation(Optional.of(ISSUE), 0, ""));
        assertEquals(asked.getFirst(), List.of("issue", "create", "--repo", "github.com/mtb550/test-in", "--title=-v is not a flag", "--body-file", "bug.md",
                "--attach", "./screenshot-1.png", "--attach", "./screenshot-2.png"));
        assertFalse(Files.exists(folders.getFirst()), "the temporary folder was left behind");
    }

    @Test
    public void anAddressOnStandardOutputIsAnIssueWhateverTheExitCode() {
        final ProcessOutput partly = answer(ISSUE + "\n", "failed to upload ./screenshot-10.png: file too large", 1);

        assertEquals(IssueCreation.of(partly, "github.com", 10), new IssueCreation(Optional.of(ISSUE), 1, "failed to upload ./screenshot-10.png: file too large"),
                "screenshot-10 is not also screenshot-1");
    }

    @Test
    public void exitCodeFourIsSignedOut() {
        assertEquals(IssueCreation.of(answer("", "authentication required", 4), "github.com", 0),
                IssueCreation.failed(Bundle.message("bug.reason.signed.out", "github.com")));
    }

    // Rule-VIEW-PANEL-073
    @Test
    public void aTimeoutSaysNobodyKnowsWhetherTheIssueExists() {
        assertEquals(IssueCreation.of(new ProcessOutput("", "", -1, true, false), "github.com", 0),
                IssueCreation.failed(Bundle.message("bug.send.timed.out", 120L)));
    }

    @Test
    public void aRefusalIsReportedInGhsWords() {
        assertEquals(IssueCreation.of(answer("", "  attachments are not supported by this host\n", 1), "github.example.com", 1),
                IssueCreation.failed("attachments are not supported by this host"));
        assertEquals(IssueCreation.of(answer("", "", 1), "github.com", 0), IssueCreation.failed(Bundle.message("bug.send.failed", 1)));
    }

    // Rule-VIEW-PANEL-092
    @Test
    public void oneRequestAsksForEveryIssueInTheRepository() {
        final List<String> arguments = GitHubCli.stateArguments(new BugRepository("github.example.com", "qa", "product"), List.of(5, 14), false);

        assertEquals(arguments.subList(0, 4), List.of("api", "graphql", "--hostname", "github.example.com"));
        final String query = arguments.get(5);
        assertTrue(query.contains("i5: issue(number: 5) { state stateReason }") && query.contains("i14: issue(number: 14) { state stateReason }"), query);
        assertFalse(query.contains("\""), "a double quote in an argument is mangled on the Windows command line: " + query);
        assertTrue(arguments.containsAll(List.of("owner=qa", "name=product")));
        assertFalse(query.contains("projectItems") || arguments.contains("field=Status"), "a board is asked for only when wanted");
    }

    // Rule-VIEW-PANEL-094
    @Test
    public void theBoardIsAskedForByItsGitHubStatusField() {
        final List<String> arguments = GitHubCli.stateArguments(new BugRepository("github.com", "mtb550", "test-03"), List.of(14), true);

        assertTrue(arguments.get(5).contains("projectItems") && arguments.get(5).contains("$field: String!"));
        assertTrue(arguments.contains("field=Status"));
    }

    private static final class FakeGh {
        private final List<List<String>> asked = new ArrayList<>();

        private final Deque<ProcessOutput> answers;

        private FakeGh(final ProcessOutput... answers) {
            this.answers = new ArrayDeque<>(List.of(answers));
        }

        private GitHubCli cli() {
            return new GitHubCli((arguments, _) -> {
                asked.add(arguments);
                return Optional.ofNullable(answers.poll());
            });
        }
    }
}
