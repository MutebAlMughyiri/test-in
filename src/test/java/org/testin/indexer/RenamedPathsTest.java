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
import org.testin.model.node.TestSetNode;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class RenamedPathsTest {
    private static final @NotNull Path CASES = Path.of("NAFATH", "Test Cases");
    private static final @NotNull RenamedPaths ACCOUNTS = new RenamedPaths(CASES.resolve("Accounts"), CASES.resolve("Users"));

    private static @NotNull TestSetNode aTestSetAt(final @NotNull Path path) {
        final @NotNull TestSetNode testSet = new TestSetNode();
        testSet.setPath(path);
        return testSet;
    }

    // Rule-TREE-PANEL-100
    @Test
    public void onlyWhatIsInsideTheRenamedFolderIsUnderIt() {
        assertTrue(ACCOUNTS.isUnder(CASES.resolve("Accounts").resolve("Login")));
        assertFalse(ACCOUNTS.isUnder(CASES.resolve("Accounts")), "the renamed folder itself was taken for something under it");
        assertFalse(ACCOUNTS.isUnder(CASES.resolve("Accounts and Roles")), "a sibling whose name starts the same was taken for something under it");
        assertEquals(ACCOUNTS.moved(CASES.resolve("Accounts").resolve("Login")), CASES.resolve("Users").resolve("Login"));
    }

    // Rule-TREE-PANEL-100
    @Test
    public void aNodeUnderTheRenamedFolderMovesWithItAndKeepsItsIdentity() {
        final @NotNull TestSetNode login = aTestSetAt(CASES.resolve("Accounts").resolve("Login"));
        final @NotNull TestSetNode payments = aTestSetAt(CASES.resolve("Payments"));
        final @NotNull Map<String, TestSetNode> testSets = new HashMap<>(Map.of(login.getPath().toString(), login, payments.getPath().toString(), payments));

        ACCOUNTS.moveNodesUnder(testSets);

        final @NotNull Path movedTo = CASES.resolve("Users").resolve("Login");
        assertEquals(testSets.keySet(), Set.of(movedTo.toString(), payments.getPath().toString()), "the test sets are not keyed by where they now are");
        assertSame(testSets.get(movedTo.toString()), login, "the moved test set is a different object");
        assertEquals(login.getPath(), movedTo, "the moved test set does not know where it now is");
        assertEquals(payments.getPath(), CASES.resolve("Payments"), "a test set elsewhere moved");
    }

    // Rule-TREE-PANEL-100
    @Test
    public void theRenamedEntryAndTheKeysUnderItMoveAndNothingElse() {
        final @NotNull Map<String, List<String>> held = new HashMap<>(Map.of(
                CASES.resolve("Accounts").toString(), List.of("the folder"),
                CASES.resolve("Accounts").resolve("Login").toString(), List.of("inside"),
                CASES.resolve("Payments").toString(), List.of("elsewhere")));

        ACCOUNTS.moveEntry(held, _ -> {
        });
        ACCOUNTS.moveKeysUnder(held);

        assertEquals(held, Map.of(
                CASES.resolve("Users").toString(), List.of("the folder"),
                CASES.resolve("Users").resolve("Login").toString(), List.of("inside"),
                CASES.resolve("Payments").toString(), List.of("elsewhere")));
    }
}
