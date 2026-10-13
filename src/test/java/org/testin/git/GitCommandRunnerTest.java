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
import org.testin.util.Bundle;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GitCommandRunnerTest {

    @Test
    public void entriesAreSeparatedByTheOneByteAPathCannotContain() {
        final String written = new String(
                GitCommandRunner.pathspecBytes(List.of("test-01/Test Cases/Login/a.json", "test-01/.testproject")),
                StandardCharsets.UTF_8);

        assertEquals(written, "test-01/Test Cases/Login/a.json\0test-01/.testproject",
                "a space is an ordinary character in a test set name, so the separator cannot be one");
    }

    // Rule-SHARE-132
    @Test
    public void aGitCommandRefusedInSafeModeIsSaidInOneLineNamingTrustProject() {
        final @NotNull GitFailed refused = GitCommandRunner.inSafeMode(new IllegalStateException("Shouldn't be possible to run a Git command in the safe mode"));

        final @NotNull String said = String.valueOf(refused.getMessage());

        assertEquals(said, Bundle.message("git.safe.mode"));
        assertFalse(said.contains("Shouldn't"), "Git's own words reached the tester");
        assertFalse(said.contains("\n"), "the refusal is more than one line");
    }

    @Test
    public void nothingFollowsTheLastEntry() {
        final byte[] written = GitCommandRunner.pathspecBytes(List.of("test-01/.testproject"));

        assertTrue(written.length > 0);
        assertEquals(written[written.length - 1], (byte) 't',
                "a trailing separator leaves an empty pathspec behind it, and Git rejects the whole command over one");
    }

    @Test
    public void onePathIsJustThatPath() {
        assertEquals(new String(GitCommandRunner.pathspecBytes(List.of("a.json")), StandardCharsets.UTF_8), "a.json");
    }

    @Test
    public void oneBatchAnswersEveryFileByItsByteSize() {
        final String arabic = "{\"description\":\"تسجيل\"}\n";
        final byte[] batch = """
                1111 blob 3
                abc
                HEAD:gone.testcase missing
                2222 blob %d
                %s
                """.formatted(arabic.getBytes(StandardCharsets.UTF_8).length, arabic).getBytes(StandardCharsets.UTF_8);

        assertEquals(GitCommandRunner.objectsIn(List.of("a.testcase", "gone.testcase", "b.testcase"), batch),
                Map.of("a.testcase", "abc", "b.testcase", arabic),
                "a missing file is absent, and a size counted in bytes still ends on the right letter");
    }

    @Test
    public void theBatchAsksForEachFileAtTheRevisionOnItsOwnLine() {
        assertEquals(new String(GitCommandRunner.batchRequest(":2", List.of("Test Cases/a b.testcase", "c.testcase")), StandardCharsets.UTF_8),
                """
                        :2:Test Cases/a b.testcase
                        :2:c.testcase
                        """);
    }

    // Rule-SHARE-004, Rule-SHARE-057, Rule-SHARE-062
    @Test
    public void aTokenInARemoteUrlNeverReachesTheLogOrTheTester() {
        assertEquals(GitSafeText.withoutCredentials(
                        "fatal: could not read from https://ghp_secret@github.com/owner/repo.git"),
                "fatal: could not read from https://***@github.com/owner/repo.git",
                "a token used as the username is the common form, and it has no colon to find it by");

        assertEquals(GitSafeText.withoutCredentials(
                        "remote: https://user:p%40ss@example.com/x rejected"),
                "remote: https://***@example.com/x rejected",
                "an escaped at-sign in the password is not the one that introduces the host");
    }

    @Test
    public void aUrlWithNothingToHideIsLeftAsItIs() {
        final String said = "fatal: repository 'https://github.com/owner/repo.git' not found";

        assertEquals(GitSafeText.withoutCredentials(said), said,
                "redacting what carries no credentials would make every ordinary failure unreadable");
    }
}
