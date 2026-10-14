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
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

@AllArgsConstructor
public final class CardGridSelectionSynchronizer implements ListSelectionListener {
    private final @NotNull JBList<TestCase> list;

    private final @NotNull Supplier<Optional<JBTable>> tableSupplier;

    private final @NotNull BooleanSupplier gridActiveSupplier;

    private final @NotNull ToIntFunction<TestCase> rowOnGrid;

    // UC-EDITOR-PANEL-024, Rule-EDITOR-PANEL-111
    @Override
    public void valueChanged(final ListSelectionEvent event) {
        if (event.getValueIsAdjusting() || !gridActiveSupplier.getAsBoolean()) return;

        tableSupplier.get().ifPresent(table -> {
            final int row = Optional.ofNullable(list.getSelectedValue()).map(rowOnGrid::applyAsInt).orElse(-1);
            if (row < 0 || row >= table.getRowCount() || row == table.getSelectedRow()) return;

            table.changeSelection(row, Math.max(0, table.getSelectedColumn()), false, false);
        });
    }
}
