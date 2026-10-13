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

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestCaseFieldChangesTest {

    private static @NotNull TestCase base() {
        return TestCase.builder()
                .description("a login case")
                .expectedResult("the balance is shown")
                .steps(new ArrayList<>(List.of("open the app", "log in")))
                .priority(Priority.LOW)
                .status(TestCaseStatus.PENDING)
                .reference("REF-1")
                .module("payments")
                .testData("account=1")
                .preConditions("authenticated")
                .groups(new ArrayList<>(List.of("Smoke")))
                .build();
    }

    private static @NotNull FieldChange onlyChange(final TestCase after) {
        final List<FieldChange> changes = TestCaseFieldChanges.compare(base(), after);
        assertEquals(changes.size(), 1, "expected exactly one changed field, got " + changes);
        return changes.getFirst();
    }

    @Test
    public void comparesAllEditableFields() {
        final TestCase oldState = TestCase.builder().build();
        final TestCase newState = TestCase.builder()
                .description("new description")
                .expectedResult("new result")
                .steps(List.of("first", "second"))
                .priority(Priority.HIGH)
                .status(TestCaseStatus.REVIEWED)
                .reference("REF-1")
                .module("payments")
                .testData("account=1")
                .preConditions("authenticated")
                .groups(List.of("Smoke"))
                .build();

        final List<FieldChange> changes = TestCaseFieldChanges.compare(oldState, newState);

        assertEquals(changes.size(), 10);
        assertTrue(changes.stream().anyMatch(change -> change.changeType() == ChangeType.CHANGE_STEPS));
        assertTrue(changes.stream().anyMatch(change -> change.changeType() == ChangeType.CHANGE_PRECONDITIONS));
    }

    @Test
    public void twoIdenticalTestCasesHaveNothingToReview() {
        assertEquals(TestCaseFieldChanges.compare(base(), base()), List.of());
    }

    @Test
    public void oneChangedFieldIsOneRowCarryingBothValues() {
        final FieldChange change = onlyChange(base().setModule("billing"));

        assertEquals(change.changeType(), ChangeType.CHANGE_MODULE);
        assertEquals(change.fieldName(), "Module");
        assertEquals(change.oldValue(), "payments");
        assertEquals(change.newValue(), "billing");
    }

    @Test
    public void aStatusChangeShowsTheLabels() {
        final FieldChange change = onlyChange(base().setStatus(TestCaseStatus.REVIEWED));

        assertEquals(change.changeType(), ChangeType.CHANGE_STATUS);
        assertEquals(change.oldValue(), TestCaseStatus.PENDING.getLabel());
        assertEquals(change.newValue(), TestCaseStatus.REVIEWED.getLabel());
    }

    // Rule-VIEW-PANEL-118, Rule-SHARE-131
    @Test
    public void aStepsChangeIsOneRowNamingOnlyTheStepsThatChanged() {
        final FieldChange change = onlyChange(
                base().setSteps(new ArrayList<>(List.of("open the app", "log in as admin"))));

        assertEquals(change.changeType(), ChangeType.CHANGE_STEPS);
        assertEquals(change.oldValue(), "2- Log in.");
        assertEquals(change.newValue(), "2- Log in as admin.");
    }

    // Rule-VIEW-PANEL-118
    @Test
    public void aStepInsertedDoesNotMakeTheStepsAfterItReadAsChanged() {
        final FieldChange change = onlyChange(
                base().setSteps(new ArrayList<>(List.of("open the app", "choose an account", "log in"))));

        assertEquals(change.oldValue(), "");
        assertEquals(change.newValue(), "2- Choose an account.");
    }

    // Rule-VIEW-PANEL-118
    @Test
    public void aStepRemovedIsNamedWithTheNumberItHad() {
        final FieldChange change = onlyChange(base().setSteps(new ArrayList<>(List.of("log in"))));

        assertEquals(change.oldValue(), "1- Open the app.");
        assertEquals(change.newValue(), "");
    }

    // Rule-VIEW-PANEL-118
    @Test
    public void stepsRemovedAltogetherAreEveryStep() {
        final FieldChange change = onlyChange(base().setSteps(new ArrayList<>()));

        assertEquals(change.oldValue(), "1- Open the app.\n2- Log in.");
        assertEquals(change.newValue(), "");
    }

    @Test
    public void addingAGroupIsOneChangeListingAllOfThem() {
        final FieldChange change = onlyChange(
                base().setGroups(new ArrayList<>(List.of("Smoke", "Regression"))));

        assertEquals(change.changeType(), ChangeType.CHANGE_GROUP);
        assertEquals(change.oldValue(), "Smoke");
        assertEquals(change.newValue(), "Smoke, Regression");
    }

    @Test
    public void reorderingGroupsIsAChangeBecauseTheFileChanged() {
        final TestCase before = base().setGroups(new ArrayList<>(List.of("Smoke", "Regression")));
        final TestCase after = base().setGroups(new ArrayList<>(List.of("Regression", "Smoke")));

        final List<FieldChange> changes = TestCaseFieldChanges.compare(before, after);

        assertEquals(changes.size(), 1);
        assertEquals(changes.getFirst().changeType(), ChangeType.CHANGE_GROUP);
    }

    @Test
    public void editingAFieldBackToItsOldValueLeavesNothing() {
        final TestCase edited = base().setDescription("something else").setDescription("a login case");

        assertEquals(TestCaseFieldChanges.compare(base(), edited), List.of());
    }
}
