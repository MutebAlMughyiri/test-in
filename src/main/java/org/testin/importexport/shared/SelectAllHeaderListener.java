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

package org.testin.importexport.shared;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.table.JBTable;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@AllArgsConstructor
public class SelectAllHeaderListener extends MouseAdapter {
    private final @NotNull JBTable table;
    private final @NotNull DefaultTableModel model;
    private final @NotNull JBCheckBox headerCheckbox;

    // UC-SHARE-003, Rule-SHARE-018
    @Override
    public void mouseClicked(final @NotNull MouseEvent e) {
        final int column = table.columnAtPoint(e.getPoint());
        if (column == 0) {
            final boolean newState = !headerCheckbox.isSelected();
            headerCheckbox.setSelected(newState);

            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(newState, i, 0);
            }
            table.getTableHeader().repaint();
        }
    }
}
