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

import static org.testin.git.history.TestCaseHistory.FIELD;
import static org.testin.git.history.TestCaseHistory.RECORD;
import static org.testng.Assert.assertEquals;

public class BugHistoryTest {

    private static final @NotNull String RUN_ITEM = "77777777-7777-4777-8777-777777777397.ri";

    // Rule-VIEW-PANEL-106
    @Test
    public void everyCommitIsReadWithTheRunItemsItAddedChangedMovedOrRemoved() {
        final @NotNull String log = RECORD + "a".repeat(40) + FIELD + "Sara" + FIELD + "2026-10-05T17:27:00+03:00\n\n"
                + "M\tTest Runs/Cycle 3/" + RUN_ITEM + "\n"
                + "M\tTest Cases/Login/77777777-7777-4777-8777-777777777397.tc\n"
                + "R100\tTest Runs/Cycle 2/" + RUN_ITEM + "\tTest Runs/Cycle 2 (Android)/" + RUN_ITEM + "\n"
                + RECORD + "b".repeat(40) + FIELD + "Muteb" + FIELD + "2026-10-04T20:42:00+03:00\n\n"
                + "A\tTest Runs/Cycle 3/" + RUN_ITEM + "\n"
                + "D\tTest Runs/Cycle 1/" + RUN_ITEM + "\n";

        final @NotNull List<BugCommit> commits = BugHistory.commits(log);

        assertEquals(commits.size(), 2);
        assertEquals(commits.getFirst().who(), "Sara");
        assertEquals(commits.getFirst().files().size(), 2, "the test case's own file is not a run item");
        assertEquals(commits.getFirst().files(), List.of(
                new ChangedFile("Test Runs/Cycle 3/" + RUN_ITEM, "Test Runs/Cycle 3/" + RUN_ITEM),
                new ChangedFile("Test Runs/Cycle 2/" + RUN_ITEM, "Test Runs/Cycle 2 (Android)/" + RUN_ITEM)));
        assertEquals(commits.get(1).files(), List.of(
                new ChangedFile("", "Test Runs/Cycle 3/" + RUN_ITEM),
                new ChangedFile("Test Runs/Cycle 1/" + RUN_ITEM, "")));
        assertEquals(commits.get(1).objectNames().toList(), List.of(
                "b".repeat(40) + ":Test Runs/Cycle 3/" + RUN_ITEM,
                "b".repeat(40) + "^:Test Runs/Cycle 1/" + RUN_ITEM), "an added run item is read only after, a removed one only before");
    }
}
