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

package org.testin.git.change;


import org.jetbrains.annotations.NotNull;
import org.testin.model.Priority;
import org.testin.model.TestCase;
import org.testin.model.status.TestCaseStatus;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ChangeTypeRevertTest {

    private static @NotNull TestCase committed() {
        return TestCase.builder()
                .description("committed description")
                .expectedResult("committed result")
                .steps(new ArrayList<>(List.of("committed first", "committed second")))
                .priority(Priority.LOW)
                .status(TestCaseStatus.PENDING)
                .reference("REF-committed")
                .module("committed module")
                .testData("committed data")
                .preConditions("committed preconditions")
                .groups(new ArrayList<>(List.of("Smoke")))
                .build();
    }

    private static @NotNull TestCase edited() {
        return TestCase.builder()
                .description("edited description")
                .expectedResult("edited result")
                .steps(new ArrayList<>(List.of("edited first")))
                .priority(Priority.HIGH)
                .status(TestCaseStatus.REVIEWED)
                .reference("REF-edited")
                .module("edited module")
                .testData("edited data")
                .preConditions("edited preconditions")
                .groups(new ArrayList<>(List.of("Regression")))
                .build();
    }

    private static @NotNull TestCase reverted(final ChangeType type, final TestCase current, final TestCase committed) {
        final TestCase.TestCaseBuilder draft = current.edit();
        type.getRevertAction().apply(draft, committed);
        return draft.build();
    }

    @Test
    public void everyChangeTheReviewCanShowCanBeReverted() {
        final List<FieldChange> changes = TestCaseFieldChanges.compare(committed(), edited());

        assertEquals(changes.size(), 10, "every editable field differs between the two states");
        for (final FieldChange change : changes) {
            assertTrue(change.changeType().isRevertible(),
                    change.changeType() + " appears in a review, so it must be revertible");
        }
    }

    @Test
    public void creatingOrRemovingAWholeTestCaseHasNoFieldToRevert() {
        assertFalse(ChangeType.CREATE_TEST_CASE.isRevertible());
        assertFalse(ChangeType.REMOVE_TEST_CASE.isRevertible());
    }

    // Rule-SHARE-052
    @Test
    public void eachRevertRestoresItsOwnFieldAndNothingElse() {
        for (final FieldChange change : TestCaseFieldChanges.compare(committed(), edited())) {
            final TestCase current = reverted(change.changeType(), edited(), committed());

            final List<FieldChange> left = TestCaseFieldChanges.compare(committed(), current);

            assertEquals(left.size(), 9,
                    change.changeType() + " should revert exactly one field, leaving nine changed");
            assertTrue(left.stream().noneMatch(remaining -> remaining.changeType() == change.changeType()),
                    change.changeType() + " reverted something other than its own field");
        }
    }

    @Test
    public void revertingEveryChangeRestoresTheCommittedTestCase() {
        TestCase current = edited();
        for (final FieldChange change : TestCaseFieldChanges.compare(committed(), edited())) {
            current = reverted(change.changeType(), current, committed());
        }

        assertEquals(TestCaseFieldChanges.compare(committed(), current), List.of(),
                "reverting every listed change leaves nothing to review");
    }

    // Rule-SHARE-052
    @Test
    public void revertingTheLastChangePutsBackTheCommittedAudit() {
        final TestCase committed = committed()
                .setCreatedBy("Mohammed AlZamil")
                .setUpdatedBy("Mohammed AlZamil")
                .setUpdatedAt(ZonedDateTime.parse("2026-09-01T10:00:00Z"));
        final TestCase current = reverted(ChangeType.CHANGE_MODULE, committed().setModule("edited module").setUpdatedBy("Muteb"), committed);
        assertEquals(TestCaseFieldChanges.compare(committed, current), List.of(), "nothing reviewable is left");

        current.takeAuditOf(committed);

        assertEquals(current.getCreatedBy(), committed.getCreatedBy());
        assertEquals(current.getCreatedAt(), committed.getCreatedAt());
        assertEquals(current.getUpdatedBy(), committed.getUpdatedBy(), "the reverting tester's stamp stayed on a test case nobody changed");
        assertEquals(current.getUpdatedAt(), committed.getUpdatedAt());
    }

    @Test
    public void revertingAListCopiesItRatherThanSharingIt() {
        final TestCase committed = committed();
        final TestCase current = reverted(ChangeType.CHANGE_GROUP, reverted(ChangeType.CHANGE_STEPS, edited(), committed), committed);

        current.getSteps().add("typed after the revert");
        current.getGroups().add("Regression");

        assertEquals(committed.getSteps(), List.of("committed first", "committed second"),
                "the committed steps were shared, not copied");
        assertEquals(committed.getGroups(), List.of("Smoke"),
                "the committed groups were shared, not copied");
    }

    @Test
    public void noTwoChangeTypesShareALabel() {
        final Set<String> labels = new HashSet<>();
        for (final ChangeType type : ChangeType.values()) {
            assertTrue(labels.add(type.getLabel()), type + " repeats another change type's label");
        }
    }

}
