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

package org.testin.model;


import org.jetbrains.annotations.NotNull;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.testrun.Failure;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.testrun.TestRunEditorAttributes;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestRunGridEditingTest {

    private static @NotNull RunItem runItem() {
        return RunItem.builder().id(UUID.randomUUID()).build();
    }

    // Rule-EDITOR-PANEL-171
    @Test
    public void actualResultIsTheOneColumnATesterCanTypeInto() {
        final List<TestRunEditorAttributes> editable = Arrays.stream(TestRunEditorAttributes.values())
                .filter(TestRunEditorAttributes::isEdited)
                .toList();

        assertEquals(editable, List.of(TestRunEditorAttributes.ACTUAL_RESULT),
                "a status, a severity or a description typed into a cell is a way of recording it "
                        + "that nothing else in the plugin knows about");
    }

    @Test
    public void theRunItemStatusColumnsRefuseEditingByTheirOwnDeclaration() {
        assertFalse(TestRunEditorAttributes.RUN_STATUS.isEdited(), "P, F and B set a status");
        assertFalse(TestRunEditorAttributes.BUG_SEVERITY.isEdited(), "the failure dialog collects this");
        assertFalse(TestRunEditorAttributes.BUG_PRIORITY.isEdited(), "and this");
        assertFalse(TestRunEditorAttributes.EXECUTED_BY.isEdited(), "recorded, not claimed");
        assertFalse(TestRunEditorAttributes.EXECUTED_AT.isEdited(), "recorded, not claimed");
    }

    @Test
    public void typingIntoTheCellIsWhatTheTestRunThenHolds() {
        final RunItem row = runItem();

        TestRunEditorAttributes.ACTUAL_RESULT.getRunItemValueSetter().execute(row, "The balance showed 0.00");

        assertEquals(row.getActualResult(), "The balance showed 0.00");
    }

    @Test
    public void aRowWithNothingTypedLosesNothingByPassing() {
        assertEquals(runItem().wouldClear(RunItemStatus.PASSED, Failure.NONE), List.of(),
                "the ordinary case asks the tester nothing");
    }

    @Test
    public void passingNamesEverythingItWouldErase() {
        final RunItem row = runItem();
        row.setActualResult("The balance showed 0.00");
        row.setStacktrace("java.lang.AssertionError");
        row.setBugSeverity(BugSeverity.MAJOR);
        row.setBugPriority(BugPriority.HIGH);

        assertEquals(row.wouldClear(RunItemStatus.PASSED, Failure.NONE),
                List.of("the actual result", "the stacktrace", "the bug severity", "the bug priority"));
    }

    @Test
    public void onlyWhatIsActuallyFilledInIsNamed() {
        final RunItem row = runItem();
        row.setActualResult("The balance showed 0.00");

        assertEquals(row.wouldClear(RunItemStatus.PASSED, Failure.NONE), List.of("the actual result"),
                "warning about three empty fields teaches the tester to dismiss the warning");
    }

    @Test
    public void aRunItemStatusThatErasesNothingAsksNothing() {
        final RunItem row = runItem();
        row.setActualResult("The balance showed 0.00");

        assertEquals(row.wouldClear(RunItemStatus.FAILED, Failure.NONE), List.of(),
                "a failing test case still has something to explain, so nothing is cleared");
        assertEquals(row.wouldClear(RunItemStatus.BLOCKED, Failure.NONE), List.of());
    }

    @Test
    public void passingStillClearsWhatItAlwaysCleared() {
        final RunItem row = runItem();
        row.setActualResult("The balance showed 0.00");
        row.setStacktrace("java.lang.AssertionError");
        row.setBugSeverity(BugSeverity.MAJOR);
        row.setBugPriority(BugPriority.HIGH);

        row.recordRunItemStatus(RunItemStatus.PASSED, "mtb");

        assertEquals(row.getActualResult(), "");
        assertEquals(row.getStacktrace(), "");
        assertEquals(row.getBugSeverity(), BugSeverity.ENHANCEMENT);
        assertEquals(row.getBugPriority(), BugPriority.LOW);
        assertEquals(row.getStatus(), RunItemStatus.PASSED);
        assertEquals(row.getExecutedBy(), "mtb");
    }

    @Test
    public void failingKeepsWhatTheTesterTyped() {
        final RunItem row = runItem();
        row.setActualResult("The balance showed 0.00");

        row.recordRunItemStatus(RunItemStatus.FAILED, "mtb");

        assertEquals(row.getActualResult(), "The balance showed 0.00",
                "the actual result is the point of a failure");
    }

    @Test
    public void whatAPassClearsAndWhatItWarnsAboutAreTheOneList() {
        final RunItem row = runItem();
        row.setActualResult("The balance showed 0.00");
        row.setBugSeverity(BugSeverity.MAJOR);

        final List<String> warned = row.wouldClear(RunItemStatus.PASSED, Failure.NONE);
        row.recordRunItemStatus(RunItemStatus.PASSED, "mtb");

        assertTrue(row.wouldClear(RunItemStatus.PASSED, Failure.NONE).isEmpty(),
                "everything named in the warning is gone afterward, or the warning was wrong: " + warned);
        assertEquals(warned.size(), 2);
    }
}
