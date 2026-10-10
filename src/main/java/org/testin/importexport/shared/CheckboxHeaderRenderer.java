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
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

@AllArgsConstructor
public class CheckboxHeaderRenderer implements TableCellRenderer {
    private final @NotNull JBCheckBox headerCheckbox;

    @Override
    public @NotNull Component getTableCellRendererComponent(final @NotNull JTable table, final @Nullable Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        final @NotNull JTableHeader header = table.getTableHeader();
        headerCheckbox.setBackground(header.getBackground());
        headerCheckbox.setForeground(header.getForeground());
        headerCheckbox.setFont(header.getFont());
        headerCheckbox.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        return headerCheckbox;
    }
}
