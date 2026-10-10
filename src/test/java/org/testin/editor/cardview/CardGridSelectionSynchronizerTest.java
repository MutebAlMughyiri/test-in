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

package org.testin.editor.cardview;

import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testng.annotations.Test;

import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertEquals;

public class CardGridSelectionSynchronizerTest {

    private static final @NotNull TestCaseDto ONE = TestCaseDto.builder().description("one").build();

    private static final @NotNull TestCaseDto TWO = TestCaseDto.builder().description("two").build();

    private static final @NotNull TestCaseDto THREE = TestCaseDto.builder().description("three").build();

    private static final @NotNull List<TestCaseDto> ON_THE_GRID = List.of(ONE, TWO, THREE);

    @Test
    public void mapsListRowToGridRowAndPreservesColumn() {
        final @NotNull JBList<TestCaseDto> list = new JBList<>(ONE, TWO, THREE);
        final @NotNull JBTable table = new JBTable(new DefaultTableModel(3, 2));
        table.setCellSelectionEnabled(true);
        table.changeSelection(0, 1, false, false);
        list.setSelectedIndex(2);

        final @NotNull CardGridSelectionSynchronizer synchronizer = new CardGridSelectionSynchronizer(
                list,
                () -> Optional.of(table),
                () -> true,
                ON_THE_GRID::indexOf
        );
        synchronizer.valueChanged(new ListSelectionEvent(list, 2, 2, false));

        assertEquals(table.getSelectedRow(), 2);
        assertEquals(table.getSelectedColumn(), 1);
    }

    // Rule-EDITOR-PANEL-111
    @Test
    public void aGridStillShowingTheOldOrderSelectsTheSameTestCaseNotTheSameRow() {
        final @NotNull JBList<TestCaseDto> list = new JBList<>(THREE, ONE, TWO);
        final @NotNull JBTable table = new JBTable(new DefaultTableModel(3, 2));
        list.setSelectedIndex(0);

        final @NotNull CardGridSelectionSynchronizer synchronizer = new CardGridSelectionSynchronizer(
                list,
                () -> Optional.of(table),
                () -> true,
                ON_THE_GRID::indexOf
        );
        synchronizer.valueChanged(new ListSelectionEvent(list, 0, 0, false));

        assertEquals(table.getSelectedRow(), 2,
                "After a reorder the list is redrawn before the grid; picking the grid row by the list's index selected a"
                        + " different test case, and the two views corrected each other until the stack overflowed");
    }
}
