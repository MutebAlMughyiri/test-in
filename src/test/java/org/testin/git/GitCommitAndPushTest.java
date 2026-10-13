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
import org.testin.git.change.StatusKind;
import org.testin.git.change.PendingChange;
import org.testin.model.TestCaseDto;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class GitCommitAndPushTest {

    private static @NotNull PendingChange diff(final Path relativePath) {
        return new PendingChange(ChangeSubject.TEST_CASE, "a case", "a test set", UUID.randomUUID().toString(),
                relativePath, StatusKind.MODIFIED,
                TestCaseDto.builder().build(), List.of());
    }

    @Test
    public void pathsAreForwardSlashedForGitWhateverThePlatformUses() {
        final Set<String> paths = GitCommitAndPush.repoRelativePaths(List.of(
                diff(Path.of("testCases", "login", "case-1.json"))));

        assertEquals(paths, Set.of("testCases/login/case-1.json"));
    }

    @Test
    public void theSameFileSelectedTwiceIsStagedOnce() {
        final Path shared = Path.of("testCases", "login", "case-1.json");

        final Set<String> paths = GitCommitAndPush.repoRelativePaths(List.of(
                diff(shared),
                diff(Path.of("testCases", "login", "case-2.json")),
                diff(shared)));

        assertEquals(List.copyOf(paths),
                List.of("testCases/login/case-1.json", "testCases/login/case-2.json"));
    }

    @Test
    public void everyDirectoryAboveASelectedTestCaseIsFound() {
        assertEquals(GitCommitAndPush.ancestorDirectories(List.of(
                        "Test Cases/rp/a.json",
                        "Test Cases/rp/b.json",
                        "Test Cases/login flow/c.json")),
                Set.of("", "Test Cases", "Test Cases/rp", "Test Cases/login flow"));
    }

    @Test
    public void aFileAtTheRepositoryRootHasOnlyTheRoot() {
        assertEquals(GitCommitAndPush.ancestorDirectories(List.of("a.json")), Set.of(""));
    }
}
