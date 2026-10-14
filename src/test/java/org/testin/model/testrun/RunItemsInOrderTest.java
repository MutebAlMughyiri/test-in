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

package org.testin.model.testrun;

import org.jetbrains.annotations.NotNull;
import org.testin.model.status.RunItemStatus;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;

public class RunItemsInOrderTest {

    private static final @NotNull UUID LOGIN_FIRST = UUID.fromString("11111111-1111-4111-8111-111111111101");

    private static final @NotNull UUID LOGIN_SECOND = UUID.fromString("11111111-1111-4111-8111-111111111102");

    private static final @NotNull UUID CHECKOUT_FIRST = UUID.fromString("11111111-1111-4111-8111-111111111103");

    private static final @NotNull UUID DELETED_TEST_CASE = UUID.fromString("11111111-1111-4111-8111-111111111104");

    private static final @NotNull UUID IN_A_RETIRED_SET = UUID.fromString("11111111-1111-4111-8111-111111111105");

    private static final @NotNull List<UUID> AS_THE_TREE_LISTS_THEM = List.of(LOGIN_FIRST, LOGIN_SECOND, CHECKOUT_FIRST);

    private static @NotNull RunItem runItem(final @NotNull UUID testCaseUuid, final @NotNull RunItemStatus runItemStatus) {
        return RunItem.builder().uuid(testCaseUuid).executions(List.of(RunItemExecution.builder().status(runItemStatus).build())).build();
    }

    private static @NotNull RunItems runItemsOf(final @NotNull RunItem... runItems) {
        return new RunItems().setAll(new ArrayList<>(List.of(runItems)));
    }

    private static @NotNull List<UUID> uuidsOf(final @NotNull RunItems runItems) {
        return runItems.getAll().stream().map(RunItem::getUuid).toList();
    }

    @Test
    public void theRunItemsComeTestSetByTestSetAsTheTreeListsThem() {
        final @NotNull RunItems asTheFolderListedThem = runItemsOf(
                runItem(CHECKOUT_FIRST, RunItemStatus.FAILED),
                runItem(LOGIN_SECOND, RunItemStatus.PASSED),
                runItem(LOGIN_FIRST, RunItemStatus.PASSED));

        assertEquals(uuidsOf(asTheFolderListedThem.inOrderOf(AS_THE_TREE_LISTS_THEM)), AS_THE_TREE_LISTS_THEM,
                "A test run is drawn, walked and reported test set by test set, each set in its own order, as the Create"
                        + " Test Run form lists them - never the order the folder happened to list the files in");
    }

    @Test
    public void aRunItemWhoseTestCaseIsGoneOrRetiredComesLastAndKeepsItsRecord() {
        final @NotNull RunItems asTheFolderListedThem = runItemsOf(
                runItem(DELETED_TEST_CASE, RunItemStatus.FAILED),
                runItem(LOGIN_SECOND, RunItemStatus.PASSED),
                runItem(IN_A_RETIRED_SET, RunItemStatus.BLOCKED),
                runItem(LOGIN_FIRST, RunItemStatus.PASSED));

        final @NotNull RunItems ordered = asTheFolderListedThem.inOrderOf(AS_THE_TREE_LISTS_THEM);

        assertEquals(uuidsOf(ordered), List.of(LOGIN_FIRST, LOGIN_SECOND, DELETED_TEST_CASE, IN_A_RETIRED_SET),
                "A run item whose test case is not in the tree has no place in it, so it comes after the others, in the"
                        + " order it was read, rather than being dropped or sorted among them");
        assertEquals(ordered.getAll().get(2).getStatus(), RunItemStatus.FAILED,
                "What the test run recorded about a test case that has since gone is still its record");
    }

    @Test
    public void orderingHandsBackANewListOfTheSameRunItems() {
        final @NotNull RunItem login = runItem(LOGIN_FIRST, RunItemStatus.PENDING);
        final @NotNull RunItems stored = runItemsOf(runItem(CHECKOUT_FIRST, RunItemStatus.PENDING), login);

        final @NotNull RunItems ordered = stored.inOrderOf(AS_THE_TREE_LISTS_THEM);

        assertNotSame(ordered.getAll(), stored.getAll(),
                "The stored list is read by the run item writer and the walk while it is ordered, so ordering never rearranges it");
        assertEquals(uuidsOf(stored), List.of(CHECKOUT_FIRST, LOGIN_FIRST), "Ordering left the stored list as it was");
        assertSame(ordered.getAll().getFirst(), login,
                "A run item status recorded on what the editor holds must reach the stored test run, so both share each run item");
    }
}
