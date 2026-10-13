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

package org.testin.editor.grid;

import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.testcase.TestSetEditorFields;
import org.testin.testrun.TestRunEditorFields;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class GridRowsTest {
    private static final @NotNull TestCaseDto LOGIN = TestCaseDto.builder().id(UUID.randomUUID()).description("Log in with a valid user").build();
    private static final @NotNull TestCaseDto LOGOUT = TestCaseDto.builder().id(UUID.randomUUID()).description("Log out").build();

    private static int position(final @NotNull TestCaseDto testCase) {
        return testCase == LOGIN ? 7 : 8;
    }

    // Rule-EDITOR-PANEL-020, Rule-EDITOR-PANEL-272
    @Test
    public void eachTestCaseIsOneRowWithItsPlaceInTheOrderColumn() {
        final @NotNull List<String[]> rows = GridRows.ofTestCases(List.of(LOGIN, LOGOUT), 11, GridRowsTest::position);

        assertEquals(rows.size(), 2, "the test cases are not one row each");
        assertEquals(rows.getFirst().length, TestSetEditorFields.values().length, "a row does not hold one cell per column");
        assertEquals(rows.getFirst()[TestSetEditorFields.ORDER.column()], "7", "the order column does not show the test case's place");
        assertEquals(rows.get(1)[TestSetEditorFields.DESCRIPTION.column()], "Log out", "the description column does not show the description");
        assertEquals(rows.getFirst()[TestSetEditorFields.SEQUENCE.column()], "11", "the # column does not count on from the page's first row");
        assertEquals(rows.get(1)[TestSetEditorFields.SEQUENCE.column()], "12", "the # column does not number the rows as shown");
    }

    // Rule-EDITOR-PANEL-020
    @Test
    public void aTestCaseWithNoRunItemYetIsStillARowShowingTheTestCase() {
        final @NotNull List<String[]> rows = GridRows.ofRunItems(List.of(LOGIN), Map.of(), 1, GridRowsTest::position);

        assertEquals(rows.size(), 1, "a test case with no run item yet has no row");
        assertEquals(rows.getFirst().length, TestRunEditorFields.values().length, "a row does not hold one cell per column");
        assertEquals(rows.getFirst()[TestRunEditorFields.ORDER.column()], "7", "the order column does not show the test case's place");
        assertEquals(rows.getFirst()[TestRunEditorFields.DESCRIPTION.column()], "Log in with a valid user", "the row does not show the test case it stands for");
    }

    // Rule-EDITOR-PANEL-014
    @Test
    public void aTestCaseWithNoPlaceInASetHasAnEmptyOrderCell() {
        final @NotNull List<String[]> rows = GridRows.ofRunItems(List.of(LOGIN), Map.of(), 1, _ -> 0);

        assertEquals(rows.getFirst()[TestRunEditorFields.ORDER.column()], "", "a test case in no set shows a number in the Order column");
    }
}
