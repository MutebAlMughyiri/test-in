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

import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RecordedRevisionTest {

    private static final @NotNull UUID REMOVED = UUID.fromString("77777777-7777-4777-8777-777777777152");

    private static final @NotNull RecordedRevision READ = new RecordedRevision("head-1", "commit-1", List.of(REMOVED), "commit-1");

    // Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    @Test
    public void aRevisionReadAnswersOnlyWhileTheHeadTheCommitAndTheRemovedTestCasesAreAllAsTheyWere() {
        assertTrue(READ.answers("head-1", "commit-1", List.of(REMOVED)), "nothing changed");
        assertFalse(READ.answers("head-2", "commit-1", List.of(REMOVED)), "the head moved");
        assertFalse(READ.answers("head-1", "commit-2", List.of(REMOVED)), "the test run names another commit");
        assertFalse(READ.answers("head-1", "commit-1", List.of()), "another test case was removed or put back");
    }
}
