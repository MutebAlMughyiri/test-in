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

package org.testin.config;


import org.jetbrains.annotations.NotNull;
import org.testin.bug.BugRepository;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class TestinConfigTest {

    private static final String FULL = """
            # testin.yml
            location: remote
            testinProject: checkout-testcases
            repoUrl: https://github.com/acme/checkout-testcases
            """;

    private static @NotNull Map<String, String> savedLines() {
        return TestinYml.lines("NAFATH", "https://github.com/acme/nafath-test-cases.git");
    }

    @Test
    public void aBugRepositoryIsReadFromItsOwnKey() {
        final TestinProjectConfig config = TestinYml.parse(
                """
                        testinProject: checkout
                        bugRepoUrl: https://mtb550:ghp_secret@github.com/mtb550/product.git
                        """, "bug repo");

        assertEquals(config.bugRepoUrl(), "https://github.com/mtb550/product.git", "a token never survives, as for RepoUrl");
        assertEquals(BugRepository.of(config.bugRepoUrl()).map(BugRepository::ghRepo).orElse(""), "github.com/mtb550/product");
    }

    @Test
    public void aBugRepoUrlThatNamesNoRepositoryIsKeptAndNotUsed() {
        assertTrue(BugRepository.of(TestinYml.parse("testinProject: checkout\n", "none").bugRepoUrl()).isEmpty());
        assertEquals(TestinProjectConfig.EMPTY.bugRepoUrl(), "");

        final TestinProjectConfig wrong = TestinYml.parse(
                "bugRepoUrl: https://github.com/mtb550/product/issues\n", "issues page");
        assertEquals(wrong.bugRepoUrl(), "https://github.com/mtb550/product/issues");
        assertTrue(BugRepository.of(wrong.bugRepoUrl()).isEmpty());
    }

    @Test
    public void readsEveryKey() {
        final TestinProjectConfig config = TestinYml.parse(FULL, "full");

        assertEquals(config.repoUrl(), "https://github.com/acme/checkout-testcases");
        assertEquals(config.testProjectName(), "checkout-testcases", "the key names the project");
        assertTrue(config.hasRepoUrl());
    }

    // Rule-INTERNAL-089
    @Test
    public void absentKeysAreEmpty() {
        final TestinProjectConfig config = TestinYml.parse("# nothing but a comment\n", "partial");

        assertEquals(config.repoUrl(), "");
        assertEquals(config.testProjectName(), "", "no name in the file and no URL to take one from");
        assertFalse(config.hasRepoUrl());
    }

    @Test
    public void unknownKeyDoesNotStopTheRest() {
        final TestinProjectConfig config = TestinYml.parse("""
                testinProject: checkout-regression
                somethingFromALaterBuild: true
                """, "unknown-key");

        assertEquals(config.testProjectName(), "checkout-regression",
                "a key from a later build must not cost this one its binding");
    }

    @Test
    public void brokenFilesOpenUnbound() {
        assertSame(TestinYml.parse("testinProject: [unclosed\n", "malformed"), TestinProjectConfig.EMPTY);
        assertSame(TestinYml.parse("   \n", "blank"), TestinProjectConfig.EMPTY);
    }

    // Rule-TREE-PANEL-119
    @Test
    public void aBrokenFileIsTheSameValueAndNotTheSameState() {
        final ParsedTestinYml broken = TestinYml.parsed("testinProject: [unclosed\n", "malformed");
        final ParsedTestinYml absent = TestinYml.parsed("   \n", "blank");

        assertEquals(broken.config(), absent.config(), "a file that would not parse has told us no more than one that is not there");

        assertFalse(broken.readable(), "the broken file is the one state a caller can act on differently");
        assertTrue(absent.readable(), "an absent file is not a broken one, and must not be reported as one");
    }

    // Rule-TREE-PANEL-117
    @Test
    public void keepsTheRepoUrlItWasGiven() {
        assertEquals(TestinYml.parse("repoUrl: \"https://x.com/r; rm -rf /\"\n", "injected").repoUrl(),
                "https://x.com/r; rm -rf /", "kept in the file, and never offered as a clone");
        assertEquals(TestinYml.parse("repoUrl: file:///etc/passwd\n", "scheme").repoUrl(), "file:///etc/passwd");
        assertEquals(TestinYml.parse("repoUrl: git@github.com:acme/checkout.git\n", "ssh").repoUrl(),
                "git@github.com:acme/checkout.git");
    }

    // Rule-TREE-PANEL-117
    @Test
    public void stillTakesTheTokenOutOfARepoUrl() {
        assertEquals(TestinYml.parse("repoUrl: https://ghp_secret@github.com/acme/checkout.git\n", "token").repoUrl(),
                "https://github.com/acme/checkout.git");
    }

    @Test
    public void aTestProjectIsLocalUntilTheFileSaysOtherwise() {
        final TestinProjectConfig quiet = TestinYml.parse("""
                        testinProject: checkout
                        repoUrl: https://github.com/acme/checkout.git
                        """,
                "an address and no location");

        assertEquals(quiet.location(), TestinLocation.LOCAL, "left out, it is local");
        assertFalse(quiet.hasRepoUrl());
    }

    @Test
    public void anAddressAloneDoesNotMakeATestProjectRemote() {
        final TestinProjectConfig stillLocal = TestinYml.parse(
                """
                        location: local
                        repoUrl: https://github.com/acme/checkout.git
                        """, "local with an address left in");

        assertFalse(stillLocal.hasRepoUrl(), "local wins over everything below it");
    }

    @Test
    public void aFileThatStillSaysConnectionGitStillClones() {
        final TestinProjectConfig old = TestinYml.parse(
                """
                        location: remote
                        connection: git
                        repoUrl: https://github.com/acme/checkout.git
                        testinProject: checkout
                        """, "old git");

        assertTrue(old.hasRepoUrl());
        assertEquals(old.testProjectName(), "checkout");
    }

    @Test
    public void aFileThatStillSaysSftpIsReadAsNotShared() {
        final TestinProjectConfig old = TestinYml.parse("""
                location: remote
                connection: sftp
                sftpHost: qa.internal
                sftpPort: 2222
                sftpPath: /srv/testin
                testinProject: test-01
                """, "sftp");

        assertFalse(old.hasRepoUrl());
        assertEquals(old.testProjectName(), "test-01", "the server keys must not cost the file its project");
    }

    @Test
    public void aGitTestProjectIsReadFromItsUrl() {
        final TestinProjectConfig inGit = TestinYml.parse(
                """
                        location: remote
                        testinProject: checkout
                        repoUrl: https://github.com/acme/checkout.git
                        """, "git");

        assertTrue(inGit.hasRepoUrl());
        assertEquals(inGit.testProjectName(), "checkout");
    }

    // Rule-TREE-PANEL-124
    @Test
    public void aWordNobodyCanReadIsLocalRatherThanAGuess() {
        assertEquals(TestinYml.parse("location: somewhere\n", "nonsense").location(),
                TestinLocation.LOCAL, "a project nobody can reach is better left on this machine");
    }

    // Rule-TREE-PANEL-124
    @Test
    public void aRemoteTestProjectWithNoAddressIsReadAsItIsWritten() {
        final @NotNull TestinProjectConfig remote = TestinYml.parse("""
                location: remote
                testinProject: checkout
                """, "remote with no address");

        assertEquals(remote.location(), TestinLocation.REMOTE, "the file says remote, and it was read as something else");
        assertEquals(remote.testProjectName(), "checkout", "a remote project with no address cost the file its project");
        assertFalse(remote.hasRepoUrl());
    }

    @Test
    public void aGitTestProjectIsNamedByTheKey() {
        final TestinProjectConfig named = TestinYml.parse("""
                location: remote
                repoUrl: https://github.com/mtb550/test-01.git
                testinProject: checkout
                """, "git");

        assertEquals(named.testProjectName(), "checkout", "the file says so, not the URL");
        assertTrue(named.hasRepoUrl());
    }

    @Test
    public void aUrlWithNoNameLeavesTheRepositoryUnbound() {
        final TestinProjectConfig unnamed = TestinYml.parse("""
                location: remote
                repoUrl: https://github.com/mtb550/test-01.git
                """, "no name");

        assertEquals(unnamed.testProjectName(), "", "the tester picks once, on this machine");
        assertTrue(unnamed.hasRepoUrl(), "and it can still be cloned");
    }

    @Test
    public void aLocalTestProjectIsNamedByTheKey() {
        final TestinProjectConfig here = TestinYml.parse(
                """
                        location: local
                        testinProject: test-01
                        """, "local");

        assertEquals(here.testProjectName(), "test-01");
        assertFalse(here.hasRepoUrl());
    }

    @Test
    public void aTokenIsStrippedOutOfACloneUrl() {
        assertEquals(TestinProjectConfig.withoutCredentials(
                        "https://mtb550:ghp_secret@github.com/mtb550/test-01.git"),
                "https://github.com/mtb550/test-01.git");

        assertEquals(TestinYml.parse("""
                        location: remote
                        testinProject: test-01
                        repoUrl: https://mtb550:ghp_secret@github.com/mtb550/test-01.git
                        """, "token").repoUrl(), "https://github.com/mtb550/test-01.git",
                "stripped on the way in too, however it got there");
    }

    @Test
    public void aTokenWithNoColonIsStrippedOutOfAnHttpsUrl() {
        assertEquals(TestinProjectConfig.withoutCredentials("https://ghp_secret@github.com/mtb550/test-01.git"),
                "https://github.com/mtb550/test-01.git");
        assertEquals(TestinProjectConfig.withoutCredentials("HTTP://ghp_secret@intranet/qa/checkout.git"),
                "HTTP://intranet/qa/checkout.git");
        assertEquals(TestinProjectConfig.withoutCredentials("https://ghp_secret@github.com"), "https://github.com", "an address with no path loses its token too");
    }

    @Test
    public void aPasswordInAnSshUrlIsStrippedWithItsAccount() {
        assertEquals(TestinProjectConfig.withoutCredentials("ssh://git:secret@host/qa/checkout.git"), "ssh://host/qa/checkout.git");
    }

    @Test
    public void anSshAccountSurvives() {
        assertEquals(TestinProjectConfig.withoutCredentials("git@github.com:mtb550/test-01.git"),
                "git@github.com:mtb550/test-01.git");
        assertEquals(TestinProjectConfig.withoutCredentials("ssh://git@host:2222/qa/checkout.git"),
                "ssh://git@host:2222/qa/checkout.git");
        assertEquals(TestinProjectConfig.withoutCredentials("https://github.com/mtb550/test-01.git"),
                "https://github.com/mtb550/test-01.git");
    }

    // Rule-TREE-PANEL-113, Rule-SHARE-004
    @Test
    public void theLinesSaySharedOnlyWithARemote() {
        assertEquals(TestinYml.lines("NAFATH", ""), Map.of("testinProject", "NAFATH", "location", "local"));
        assertEquals(TestinYml.lines("NAFATH", "https://ghp_secret@github.com/acme/nafath.git").get("repoUrl"), "https://github.com/acme/nafath.git");
        assertEquals(TestinYml.lines("NAFATH"), Map.of("testinProject", "NAFATH"));
    }

    @Test
    public void aNewFileIsTheThreeLines() {
        final String written = TestinYml.withLines("", savedLines());

        assertEquals(written, """
                testinProject: NAFATH
                location: remote
                repoUrl: https://github.com/acme/nafath-test-cases.git
                """);
        assertEquals(TestinYml.parse(written, "saved").testProjectName(), "NAFATH");
        assertTrue(TestinYml.parse(written, "saved").hasRepoUrl(), "and a colleague's first open can clone it");
    }

    // Rule-TREE-PANEL-114
    @Test
    public void theRestOfTheFileIsKept() {
        final String before = """
                # Which test project this repository drives.
                testinProject: Checkout
                bugRepoUrl: https://github.com/acme/app
                location: local
                # testinProject: test-01       which test project, local or shared
                somethingFromALaterBuild: true
                """;

        assertEquals(TestinYml.withLines(before, savedLines()), """
                # Which test project this repository drives.
                testinProject: NAFATH
                bugRepoUrl: https://github.com/acme/app
                location: remote
                # testinProject: test-01       which test project, local or shared
                somethingFromALaterBuild: true
                repoUrl: https://github.com/acme/nafath-test-cases.git
                """);
    }

    @Test
    public void theFileKeepsItsLineEndings() {
        assertEquals(TestinYml.withLines("testinProject: Checkout\r\nlocation: local\r\n", Map.of("testinProject", "NAFATH")),
                "testinProject: NAFATH\r\nlocation: local\r\n");
        assertEquals(TestinYml.withLines("location: local\ntestinProject: Checkout", Map.of("testinProject", "NAFATH")),
                "location: local\ntestinProject: NAFATH");
        assertEquals(TestinYml.withLines("location: local\r\n", Map.of("testinProject", "NAFATH")),
                "location: local\r\ntestinProject: NAFATH\r\n", "an added line takes the file's ending too");
    }

    @Test
    public void aLineIsAddedAfterALastLineWithNoNewline() {
        assertEquals(TestinYml.withLines("location: remote", Map.of("testinProject", "NAFATH")),
                """
                        location: remote
                        testinProject: NAFATH
                        """);
    }

    @Test
    public void aNestedKeyIsNotTheKey() {
        assertEquals(TestinYml.withLines("""
                        report:
                          testinProject: inner
                        """, Map.of("testinProject", "NAFATH")),
                """
                        report:
                          testinProject: inner
                        testinProject: NAFATH
                        """);
    }

    @Test
    public void aKeyWrittenTwiceIsSetEverywhere() {
        final String written = TestinYml.withLines("""
                testinProject: Checkout
                testinProject: Old
                """, Map.of("testinProject", "NAFATH"));

        assertEquals(written, """
                testinProject: NAFATH
                testinProject: NAFATH
                """);
        assertEquals(TestinYml.parse(written, "twice").testProjectName(), "NAFATH");
    }

    @Test
    public void aNameYamlWouldMisreadIsQuoted() {
        for (final String name : new String[]{"#1 Smoke", "Smoke #1", "Smoke:", "#1 O'Brien", "Tests: smoke", "null", "Yes", "- draft", "O'Brien's test cases", "@home"}) {
            final String written = TestinYml.withLines("", Map.of("testinProject", name));
            assertEquals(TestinYml.parse(written, name).testProjectName(), name, "written as " + written.strip());
        }
        assertEquals(TestinYml.withLines("", Map.of("testinProject", "Nafath App")), "testinProject: Nafath App\n", "an ordinary name stays plain");
        assertEquals(TestinYml.withLines("", Map.of("testinProject", "Smoke ")), "testinProject: 'Smoke '\n", "a space at the end is kept by quoting");
    }

    @Test
    public void thePreviewReadsWhatIsWritten() {
        final Map<String, String> values = TestinYml.valuesIn("""
                        # testinProject: old
                        testinProject: '#1 O''Brien'
                        location: local  # for now
                        """,
                Set.of("testinProject", "location", "RepoUrl"));

        assertEquals(values, Map.of("testinProject", "#1 O'Brien", "location", "local"));
        assertEquals(TestinYml.valuesIn("testinProject: [unclosed\n", Set.of("testinProject")), Map.of(), "a file that does not parse has no values");
        assertEquals(TestinYml.valuesIn("  \n", Set.of("testinProject")), Map.of(), "a blank file has no values");
    }
}
