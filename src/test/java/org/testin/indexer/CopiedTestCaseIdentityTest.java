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
import java.util.Set;
import java.util.function.Predicate;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CopiedTestCaseIdentityTest {

    private static final @NotNull Path TEST_SET = Path.of("root", "Test Cases", "Login");
    private static final @NotNull Path TEST_RUN = Path.of("root", "Test Runs", "Cycle 1");

    private static final @NotNull Predicate<Path> IS_TEST_SET = Set.of(TEST_SET)::contains;

    @Test
    public void aTestCaseTestinNamedIsATestCaseFile() {
        assertTrue(TestProjectIndexer.isTestCaseFile(TEST_SET.resolve("3f2b9c14-0d5e-4a71-9c33-8e1f4b2a7d60.testcase"), IS_TEST_SET));
    }

    // Rule-INTERNAL-011
    @Test
    public void aTestCaseTheTesterNamedIsATestCaseFileToo() {
        assertTrue(TestProjectIndexer.isTestCaseFile(TEST_SET.resolve("login.testcase"), IS_TEST_SET),
                "a hand-named test case file is still a test case, so a copy of it has to get an id of its own");
    }

    @Test
    public void aTestRunsOwnFileIsNotATestCaseFile() {
        assertFalse(TestProjectIndexer.isTestCaseFile(TEST_RUN.resolve("Cycle 1.json"), IS_TEST_SET),
                "a test run's file is named for its folder and must keep the test case ids it executed");
    }

    @Test
    public void aMarkerIsNotATestCaseFile() {
        assertFalse(TestProjectIndexer.isTestCaseFile(TEST_SET.resolve(".testset"), IS_TEST_SET));
    }

    // Rule-INTERNAL-011
    @Test
    public void jsonOutsideATestSetIsNotATestCaseFile() {
        assertFalse(TestProjectIndexer.isTestCaseFile(Path.of("root", "Test Cases", "notes.testcase"), IS_TEST_SET));
    }
}
