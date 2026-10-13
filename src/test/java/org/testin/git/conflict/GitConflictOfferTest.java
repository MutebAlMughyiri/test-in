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

package org.testin.git.conflict;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GitConflictOfferTest {

    // Rule-SHARE-079
    @Test
    public void theConflictMessageNamesTheFilesStillInTheWay() {
        final @NotNull String said = GitConflictOffer.conflictMessage(List.of("notes.txt", "Test Cases/login/a.testcase"));

        assertTrue(said.contains("notes.txt") && said.contains("Test Cases/login/a.testcase"), said);
    }

    // Rule-SHARE-079
    @Test
    public void aLongListNamesThreeFilesAndCountsTheRest() {
        final @NotNull String said = GitConflictOffer.conflictMessage(List.of("a.testcase", "b.testcase", "c.testcase", "d.testcase", "e.testcase"));

        assertTrue(said.contains("a.testcase, b.testcase, c.testcase"), said);
        assertFalse(said.contains("d.testcase"), said);
        assertTrue(said.contains("2 more"), said);
    }
}
