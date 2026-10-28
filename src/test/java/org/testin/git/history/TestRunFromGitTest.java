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
import org.testng.annotations.Test;

import java.util.Map;
import java.util.UUID;

import static org.testin.git.history.TestCaseHistory.RECORD;
import static org.testng.Assert.assertEquals;

public class TestRunFromGitTest {

    private static final @NotNull UUID TEST_CASE_UUID = UUID.fromString("77777777-7777-4777-8777-777777777151");

    private static final @NotNull String FILE = TEST_CASE_UUID + ".testcase";

    private static final @NotNull String PATH = "Test Cases/Login/" + FILE;

    @Test
    public void aTestCaseDeletedInACommitIsReadFromTheCommitBeforeAndOlderCommitsAreLeftOut() {
        final @NotNull String log = RECORD + "a".repeat(40) + "\n\nD\t" + PATH + "\n"
                + RECORD + "b".repeat(40) + "\n\nM\t" + PATH + "\n"
                + RECORD + "c".repeat(40) + "\n\nA\t" + PATH + "\n";

        assertEquals(TestRunFromGit.lastVersions(log, Map.of(FILE, TEST_CASE_UUID)), Map.of("a".repeat(40) + "^:" + PATH, TEST_CASE_UUID));
    }
}
