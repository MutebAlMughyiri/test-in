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

    private static final Path PROJECT = ROOT.resolve("test-01");

    private static @NotNull Optional<Path> of(final Path changed) {
        return WatchedPath.testProjectOf(changed, ROOT);
    }

    // Rule-INTERNAL-021
    @Test
    public void aTestCaseThatChangedNamesItsProject() {
        assertEquals(of(PROJECT.resolve("Test Cases/Login/6197ec6e.json")), Optional.of(PROJECT),
                "the project is the unit the indexer re-reads, not the file");
    }

    @Test
    public void aMarkerNamesItsProjectToo() {
        assertEquals(of(PROJECT.resolve("Test Cases/Login/.ts")), Optional.of(PROJECT));
        assertEquals(of(PROJECT.resolve(".tp")), Optional.of(PROJECT));
    }

    // Rule-INTERNAL-021
    @Test
    public void everyFileOfOneProjectAnswersTheSameProject() {
        assertEquals(of(PROJECT.resolve("Test Runs/Cycle 1/Cycle 1.tr")), of(PROJECT.resolve(".tp")),
                "forty files changed in one project have to collapse to one scan");
    }

    @Test
    public void aSecondProjectIsItsOwnAnswer() {
        assertEquals(of(ROOT.resolve("test-02/.tp")), Optional.of(ROOT.resolve("test-02")));
    }

    // Rule-INTERNAL-017
    @Test
    public void gitsOwnFilesAreNotTestData() {
        assertEquals(of(PROJECT.resolve(".git/HEAD")), Optional.empty(),
                "a pull rewrites these constantly and none of it is test data");
        assertEquals(of(PROJECT.resolve(".git/index")), Optional.empty());
        assertEquals(of(PROJECT.resolve(".git/refs/heads/main")), Optional.empty());
    }

    // Rule-INTERNAL-017
    @Test
    public void aFolderCalledGitDeeperInIsStillGits() {
        assertEquals(of(PROJECT.resolve("Test Cases/.git/config")), Optional.empty(),
                "checking only the top segment would let every pull through");
    }

    // Rule-INTERNAL-016
    @Test
    public void anythingOutsideTheTestinRootIsIgnored() {
        assertEquals(of(Path.of("C:", "Users", "mtb", "IdeaProjects", "testin", "build", "Foo.class")),
                Optional.empty(), "this is nearly every event the listener will ever see");
        assertEquals(of(Path.of("C:", "Users", "mtb", "TestinOther", "test-01", ".tp")), Optional.empty(),
                "a sibling folder whose name merely starts the same way is not inside the root");
    }

    // Rule-INTERNAL-018
    @Test
    public void theRootItselfIsAFolderOfProjectsAndNotOne() {
        assertEquals(of(ROOT), Optional.empty());
    }

    @Test
    public void anUnconfiguredRootWatchesNothing() {
        assertTrue(WatchedPath.testProjectOf(PROJECT.resolve(".tp"), Path.of("")).isEmpty(),
                "there is nothing to watch before a tester has set a Testin folder");
    }
}
