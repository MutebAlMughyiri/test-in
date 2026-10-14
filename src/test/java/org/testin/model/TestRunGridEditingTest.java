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
import org.testin.model.testrun.RunItem;
import org.testin.testrun.TestRunEditorFields;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class TestRunGridEditingTest {

    private static @NotNull RunItem runItem() {
        return RunItem.builder().uuid(UUID.randomUUID()).build();
    }

    // Rule-EDITOR-PANEL-171
    @Test
    public void actualResultIsTheOneColumnATesterCanTypeInto() {
        final List<TestRunEditorFields> editable = Arrays.stream(TestRunEditorFields.values())
                .filter(TestRunEditorFields::isEdited)
                .toList();

        assertEquals(editable, List.of(TestRunEditorFields.ACTUAL_RESULT),
                "a status, a severity or a description typed into a cell is a way of recording it "
                        + "that nothing else in the plugin knows about");
    }

    @Test
    public void theRunItemStatusColumnsRefuseEditingByTheirOwnDeclaration() {
        assertFalse(TestRunEditorFields.RUN_STATUS.isEdited(), "P, F and B set a status");
        assertFalse(TestRunEditorFields.BUG_SEVERITY.isEdited(), "the failure dialog collects this");
        assertFalse(TestRunEditorFields.BUG_PRIORITY.isEdited(), "and this");
        assertFalse(TestRunEditorFields.EXECUTED_BY.isEdited(), "recorded, not claimed");
        assertFalse(TestRunEditorFields.EXECUTED_AT.isEdited(), "recorded, not claimed");
    }

    @Test
    public void typingIntoTheCellIsWhatTheTestRunThenHolds() {
        final RunItem row = runItem();

        TestRunEditorFields.ACTUAL_RESULT.getRunItemValueSetter().execute(row, "The balance showed 0.00");

        assertEquals(row.shownExecution().getActualResult(), "The balance showed 0.00");
    }
}
