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
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.testrun.Execution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.testrun.TestRunEditorAttributes;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RunItemChangeComparatorTest {

    private static final UUID JUDGED_TEST_CASE = UUID.fromString("11111111-1111-4111-8111-111111111101");

    private static final ZonedDateTime EXECUTED_AT =
            ZonedDateTime.of(2026, 9, 20, 11, 30, 0, 0, ZoneId.of("Asia/Riyadh"));

    private static @NotNull RunItem changed(final @NotNull Consumer<Execution> change) {
        final @NotNull RunItem after = base();
        after.defaultExecution().ifPresent(change);
        return after;
    }

    private static @NotNull RunItem base() {
        return RunItem.builder()
                .id(JUDGED_TEST_CASE)
                .bugSeverity(BugSeverity.MAJOR)
                .bugPriority(BugPriority.HIGH)
                .bugIssueUrl("https://github.com/mtb550/test-in/issues/305").executions(List.of(Execution.builder().status(RunItemStatus.FAILED).actualResult("The dashboard never opened").stacktrace("java.lang.AssertionError: no dashboard").executedBy("Mohammed AlZamil").executedAt(EXECUTED_AT).screenshots(List.of("a1b2c.png")).build()))
                .build();
    }

    private static @NotNull FieldChange onlyChange(final RunItem after) {
        final List<FieldChange> changes = RunItemChangeComparator.compare(base(), after);
        assertEquals(changes.size(), 1, "expected exactly one changed field, got " + changes);
        return changes.getFirst();
    }

    @Test
    public void comparesEverythingARunItemRecords() {
        final List<FieldChange> changes = RunItemChangeComparator.compare(new RunItem().setId(JUDGED_TEST_CASE), base());

        assertEquals(changes.size(), 9, "a field a tester filled in and the review does not list cannot be read"
                + " before committing it: " + changes);
        assertTrue(changes.stream().allMatch(change -> change.changeType() == ChangeType.CHANGE_RUN_ITEM),
                "every row of a changed result is a result change, whatever field it is about: " + changes);
    }

    @Test
    public void aRunItemStatusChangeShowsTheLabels() {
        final FieldChange change = onlyChange(changed(execution -> execution.setStatus(RunItemStatus.PASSED)));

        assertEquals(change.fieldName(), TestRunEditorAttributes.RUN_STATUS.getName());
        assertEquals(change.oldValue(), RunItemStatus.FAILED.getLabel());
        assertEquals(change.newValue(), RunItemStatus.PASSED.getLabel());
    }

    @Test
    public void aRunItemThatChangedNothingThisReadsStillGetsASelectableRow() {
        final FieldChange change = onlyChange(changed(execution -> execution.setDuration(Duration.ofSeconds(12))));

        assertEquals(change.changeType(), ChangeType.CHANGE_RUN_ITEM);
        assertFalse(change.newValue().isBlank(), "a row with nothing in it says nothing: " + change);

        assertFalse(RunItemChangeComparator.compare(base(), base()).isEmpty(),
                "a result Git reports as changed is always one row at least, or the file it is about cannot be"
                        + " staged at all");
    }

    @Test
    public void theScreenshotsAreNamedRatherThanCounted() {
        final FieldChange change = onlyChange(changed(execution -> execution.setScreenshots(List.of("a1b2c.png", "d3e4f.png"))));

        assertEquals(change.oldValue(), "a1b2c.png");
        assertEquals(change.newValue(), "a1b2c.png, d3e4f.png");
    }

    @Test
    public void oneLineSaysTheRunItemStatusAndWhatWasSeen() {
        assertEquals(RunItemChangeComparator.summary(base()),
                RunItemStatus.FAILED.getLabel() + " - The dashboard never opened");

        assertEquals(RunItemChangeComparator.summary(changed(execution -> {
            execution.setStatus(RunItemStatus.PASSED);
            execution.setActualResult("");
        })),
                RunItemStatus.PASSED.getLabel());
    }
}
