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
import org.testin.util.RealJsonCodec;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class PendingChangeFactoryTest {

    private static final Path PATH = Path.of("Test Cases", "login", "case.tc");

    private static @NotNull TestCase testCase(final String description) {
        return TestCase.builder()
                .description(description)
                .expectedResult("the balance is shown")
                .steps(new ArrayList<>(List.of("open the app", "log in")))
                .priority(Priority.LOW)
                .module("payments")
                .build();
    }

    private static @NotNull String json(final TestCase testCase) {
        return RealJsonCodec.build().writeValueAsString(testCase);
    }

    @Test
    public void anAddedFileIsOneCreateChangeCarryingTheDescription() {
        final TestCase added = testCase("a brand new case");

        final PendingChange diff = PendingChangeFactory.fromFile(
                StatusKind.ADDED, "", json(added), PATH, RealJsonCodec.build(), _ -> Optional.empty());

        assertNotNull(diff);
        assertEquals(diff.type(), StatusKind.ADDED);
        assertEquals(diff.testCaseId(), added.getId().toString());
        assertEquals(diff.relativeFilePath(), PATH);
        assertTrue(diff.committed().getDescription().isEmpty(), "an added file has no before state");

        assertEquals(diff.fieldChanges().size(), 1);
        final FieldChange change = diff.fieldChanges().getFirst();
        assertEquals(change.changeType(), ChangeType.CREATE_TEST_CASE);
        assertEquals(change.oldValue(), "");
        assertEquals(change.newValue(), "a brand new case");
    }

    @Test
    public void aDeletedFileIsOneRemoveChangeCarryingTheDescription() {
        final TestCase removed = testCase("a case that is going away");

        final PendingChange diff = PendingChangeFactory.fromFile(
                StatusKind.DELETED, json(removed), "", PATH, RealJsonCodec.build(), _ -> Optional.empty());

        assertNotNull(diff);
        assertEquals(diff.type(), StatusKind.DELETED);
        assertEquals(diff.committed().getDescription(), "a case that is going away");

        assertEquals(diff.fieldChanges().size(), 1);
        final FieldChange change = diff.fieldChanges().getFirst();
        assertEquals(change.changeType(), ChangeType.REMOVE_TEST_CASE);
        assertEquals(change.oldValue(), "a case that is going away");
        assertEquals(change.newValue(), "");
    }

    // Rule-SHARE-045
    @Test
    public void aModifiedFileListsOnlyTheFieldsThatChanged() {
        final TestCase before = testCase("the original description");
        final TestCase after = testCase("the original description")
                .setId(before.getId())
                .setModule("billing")
                .setPriority(Priority.HIGH);

        final PendingChange diff = PendingChangeFactory.fromFile(
                StatusKind.MODIFIED, json(before), json(after), PATH, RealJsonCodec.build(), _ -> Optional.empty());

        assertNotNull(diff);
        assertEquals(diff.type(), StatusKind.MODIFIED);
        assertEquals(diff.committed().getDescription(), "the original description");

        assertEquals(diff.fieldChanges().size(), 2, "two fields moved, so two rows in the review");
        assertEquals(diff.fieldChanges().stream().map(FieldChange::changeType).collect(Collectors.toSet()),
                Set.of(ChangeType.CHANGE_PRIORITY, ChangeType.CHANGE_MODULE));
    }

    // Rule-SHARE-046
    @Test
    public void aModifiedFileWithNoComparedFieldStillGetsARow() {
        final TestCase unchanged = testCase("identical on both sides");

        final PendingChange change = PendingChangeFactory.fromFile(
                StatusKind.MODIFIED, json(unchanged), json(unchanged), PATH, RealJsonCodec.build(), _ -> Optional.empty());

        assertNotNull(change, "a file git calls modified is a change to commit");
        assertEquals(change.fieldChanges().size(), 1);
        assertEquals(change.fieldChanges().getFirst().changeType(), ChangeType.CHANGE_FILE);
    }

    @Test
    public void aRunItemIsItsOwnKindOfChange() {
        final UUID testCaseId = UUID.randomUUID();
        final String before = """
                {"id":"%s"}""".formatted(testCaseId);
        final String after = """
                {"id":"%s","executions":[{"status":"PASSED","actualResult":"Signed in"}]}""".formatted(testCaseId);

        final PendingChange change = PendingChangeFactory.fromFile(
                StatusKind.MODIFIED, before, after, Path.of("Test Runs", "cycle 4", testCaseId + ".ri"), RealJsonCodec.build(), _ -> Optional.empty());

        assertEquals(change.subject(), ChangeSubject.RUN_ITEM);
        assertEquals(change.testCaseId(), testCaseId.toString(), "the result says which test case it is about");
        assertFalse(change.isRevertible(), "a run item status is a record of work, not an edit to undo");
        assertFalse(change.fieldChanges().isEmpty(), "and the row says what the run item status became");
        assertEquals(change.fieldChanges().getFirst().newValue(), "Passed");
    }

    @Test
    public void aTestRunsMarkerSaysWhichOfItsFactsChanged() {
        final PendingChange change = PendingChangeFactory.fromFile(
                StatusKind.MODIFIED,
                "{\"status\":\"CREATED\",\"createdBy\":\"mtb\",\"configuration\":{\"PLATFORM\":\"Web\"}}",
                "{\"status\":\"IN_PROGRESS\",\"createdBy\":\"mtb\",\"configuration\":{\"PLATFORM\":\"Mobile\"}}",
                Path.of("Test Runs", "cycle 4", ".tr"), RealJsonCodec.build(), _ -> Optional.empty());

        assertEquals(change.subject(), ChangeSubject.MARKER);
        assertTrue(change.fieldChanges().stream().anyMatch(field -> field.newValue().equals("Mobile")),
                "the configuration the tester changed is a row: " + change.fieldChanges());
    }

    @Test
    public void aMarkerChangeIsListedWithItsStatus() {
        final PendingChange change = PendingChangeFactory.fromFile(
                StatusKind.MODIFIED,
                "{\"status\":\"ACTIVE\",\"createdBy\":\"mtb\"}",
                "{\"status\":\"ARCHIVED\",\"createdBy\":\"mtb\"}",
                Path.of("Test Cases", "login", ".ts"), RealJsonCodec.build(), _ -> Optional.empty());

        assertNotNull(change);
        assertEquals(change.subject(), ChangeSubject.MARKER);
        assertEquals(change.name(), "login", "a marker is named by the node it belongs to");
        assertFalse(change.isRevertible());

        final FieldChange status = change.fieldChanges().getFirst();
        assertEquals(status.changeType(), ChangeType.CHANGE_MARKER);
        assertEquals(status.oldValue(), "ACTIVE");
        assertEquals(status.newValue(), "ARCHIVED");
    }

    @Test
    public void aMissingRevisionFailsByName() {
        final TestCase present = testCase("only one side survived");

        assertThrows(IllegalStateException.class, () -> PendingChangeFactory.fromFile(
                StatusKind.MODIFIED, "", json(present), PATH, RealJsonCodec.build(), _ -> Optional.empty()));

        assertThrows(IllegalStateException.class, () -> PendingChangeFactory.fromFile(
                StatusKind.MODIFIED, json(present), "", PATH, RealJsonCodec.build(), _ -> Optional.empty()));

        assertThrows(IllegalStateException.class, () -> PendingChangeFactory.fromFile(
                StatusKind.ADDED, json(present), "", PATH, RealJsonCodec.build(), _ -> Optional.empty()));

        assertThrows(IllegalStateException.class, () -> PendingChangeFactory.fromFile(
                StatusKind.DELETED, "", json(present), PATH, RealJsonCodec.build(), _ -> Optional.empty()));
    }

    @Test
    public void everyKindOfChangeCanNameItsTestCase() {
        final TestCase added = testCase("added");
        final TestCase before = testCase("before");
        final TestCase after = testCase("after").setId(before.getId());

        assertEquals(PendingChangeFactory.fromFile(StatusKind.ADDED, "", json(added), PATH, RealJsonCodec.build(), _ -> Optional.empty())
                .name(), "added");

        assertEquals(PendingChangeFactory.fromFile(StatusKind.DELETED, json(before), "", PATH, RealJsonCodec.build(), _ -> Optional.empty())
                .name(), "before", "a deletion is about the test case that was there");

        assertEquals(PendingChangeFactory.fromFile(StatusKind.MODIFIED, json(before), json(after), PATH, RealJsonCodec.build(), _ -> Optional.empty())
                .name(), "after", "a modification is about the test case as it is now");
    }

    @Test
    public void aTestCaseSurvivesBeingWrittenAndReadBack() {
        final TestCase original = testCase("survives the round trip");

        final PendingChange diff = PendingChangeFactory.fromFile(
                StatusKind.DELETED, json(original), "", PATH, RealJsonCodec.build(), _ -> Optional.empty());

        assertNotNull(diff);
        final TestCase readBack = diff.committed();

        assertEquals(TestCaseFieldChanges.compare(original, readBack), List.of(),
                "a test case written and read back must compare as unchanged");
    }

    // Rule-SHARE-051
    @Test
    public void onlyAChangeToATestCaseCanBePutBack() {
        final @NotNull TestCase before = testCase("before");
        final @NotNull TestCase after = testCase("after").setId(before.getId());
        final @NotNull UUID testCaseId = UUID.randomUUID();

        assertTrue(PendingChangeFactory.fromFile(StatusKind.ADDED, "", json(after), PATH, RealJsonCodec.build(), _ -> Optional.empty()).isRevertible(),
                "a new test case can be taken out again");
        assertTrue(PendingChangeFactory.fromFile(StatusKind.DELETED, json(before), "", PATH, RealJsonCodec.build(), _ -> Optional.empty()).isRevertible(),
                "a removed test case can be put back");
        assertTrue(PendingChangeFactory.fromFile(StatusKind.MODIFIED, json(before), json(after), PATH, RealJsonCodec.build(), _ -> Optional.empty()).isRevertible(),
                "a changed field can be put back");

        assertFalse(PendingChangeFactory.fromFile(StatusKind.MODIFIED, "{\"status\":\"PENDING\"}", "{\"status\":\"PASSED\"}",
                Path.of("Test Runs", "cycle 4", testCaseId + ".ri"), RealJsonCodec.build(), _ -> Optional.empty()).isRevertible(), "a run item status is not a change to a test case");
        assertFalse(PendingChangeFactory.fromFile(StatusKind.MODIFIED, "{\"status\":\"ACTIVE\"}", "{\"status\":\"ARCHIVED\"}",
                Path.of("Test Cases", "login", ".ts"), RealJsonCodec.build(), _ -> Optional.empty()).isRevertible(), "a marker is not a change to a test case");
        assertFalse(PendingChangeFactory.fromFile(StatusKind.ADDED, "", "notes",
                Path.of("Test Cases", "login", "notes.txt"), RealJsonCodec.build(), _ -> Optional.empty()).isRevertible(), "a file Testin does not own is not a change to a test case");
    }
}
