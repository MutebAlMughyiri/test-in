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

package org.testin.indexer;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class WatchedPathTest {

    private static final Path ROOT = Path.of("C:", "Users", "mtb", "Testin");

    private static final Path TEST_PROJECT = ROOT.resolve("test-01");

    private static @NotNull Optional<Path> of(final Path changed) {
        return WatchedPath.testProjectOf(changed, ROOT);
    }

    // Rule-INTERNAL-021
    @Test
    public void aTestCaseThatChangedNamesItsTestProject() {
        assertEquals(of(TEST_PROJECT.resolve("Test Cases/Login/6197ec6e.json")), Optional.of(TEST_PROJECT),
                "the project is the unit the indexer re-reads, not the file");
    }

    @Test
    public void aMarkerNamesItsTestProjectToo() {
        assertEquals(of(TEST_PROJECT.resolve("Test Cases/Login/.testset")), Optional.of(TEST_PROJECT));
        assertEquals(of(TEST_PROJECT.resolve(".testproject")), Optional.of(TEST_PROJECT));
    }

    // Rule-INTERNAL-021
    @Test
    public void everyFileOfOneTestProjectAnswersTheSameTestProject() {
        assertEquals(of(TEST_PROJECT.resolve("Test Runs/Cycle 1/Cycle 1.testrun")), of(TEST_PROJECT.resolve(".testproject")),
                "forty files changed in one project have to collapse to one scan");
    }

    @Test
    public void aSecondTestProjectIsItsOwnAnswer() {
        assertEquals(of(ROOT.resolve("test-02/.testproject")), Optional.of(ROOT.resolve("test-02")));
    }

    // Rule-INTERNAL-017
    @Test
    public void gitsOwnFilesAreNotTestData() {
        assertEquals(of(TEST_PROJECT.resolve(".git/HEAD")), Optional.empty(),
                "a pull rewrites these constantly and none of it is test data");
        assertEquals(of(TEST_PROJECT.resolve(".git/index")), Optional.empty());
        assertEquals(of(TEST_PROJECT.resolve(".git/refs/heads/main")), Optional.empty());
    }

    // Rule-INTERNAL-017
    @Test
    public void aFolderCalledGitDeeperInIsStillGits() {
        assertEquals(of(TEST_PROJECT.resolve("Test Cases/.git/config")), Optional.empty(),
                "checking only the top segment would let every pull through");
    }

    // Rule-INTERNAL-016
    @Test
    public void anythingOutsideTheTestinRootIsIgnored() {
        assertEquals(of(Path.of("C:", "Users", "mtb", "IdeaProjects", "testin", "build", "Foo.class")),
                Optional.empty(), "this is nearly every event the listener will ever see");
        assertEquals(of(Path.of("C:", "Users", "mtb", "TestinOther", "test-01", ".testproject")), Optional.empty(),
                "a sibling folder whose name merely starts the same way is not inside the root");
    }

    // Rule-INTERNAL-018
    @Test
    public void theRootItselfIsAFolderOfTestProjectsAndNotOne() {
        assertEquals(of(ROOT), Optional.empty());
    }

    @Test
    public void anUnconfiguredRootWatchesNothing() {
        assertTrue(WatchedPath.testProjectOf(TEST_PROJECT.resolve(".testproject"), Path.of("")).isEmpty(),
                "there is nothing to watch before a tester has set a Testin folder");
    }
}
