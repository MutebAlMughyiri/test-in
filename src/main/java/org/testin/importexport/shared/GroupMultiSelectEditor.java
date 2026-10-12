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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.CheckBoxList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.Groups;
import org.testin.services.Services;
import org.testin.services.TestCaseCompletions;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GroupMultiSelectEditor extends AbstractCellEditor implements TableCellEditor {
    private final @NotNull JButton button = new JButton();
    private final @NotNull TestCaseCompletions testCaseValues;

    private @NotNull String currentValue = "";

    public GroupMultiSelectEditor(final @NotNull Project p) {
        this.testCaseValues = Services.getInstance(p, TestCaseCompletions.class);

        button.setBorderPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBackground(UIManager.getColor("Table.selectionBackground"));
        button.setForeground(UIManager.getColor("Table.selectionForeground"));

        button.addActionListener(_ -> showGroups());
    }

    // UC-SHARE-003, Rule-INTERNAL-095
    private void showGroups() {
        final @NotNull List<String> picked = new ArrayList<>(Groups.read(currentValue));

        final @NotNull CheckBoxList<String> list = new CheckBoxList<>();
        DialogStyle.styleContent(list);
        DialogStyle.asRow(list);

        testCaseValues.getGroups().stream().sorted()
                .forEach(group -> list.addItem(group, group, picked.contains(group)));

        list.setCheckBoxListListener((index, state) -> {
            final @NotNull String group = Objects.toString(list.getItemAt(index), "");

            if (state) picked.add(group);
            else picked.remove(group);

            currentValue = Groups.text(picked);
            button.setText(currentValue);
        });

        final @NotNull JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(list, list)
                .setRequestFocus(true)
                .createPopup();

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(final @NotNull LightweightWindowEvent event) {
                fireEditingStopped();
            }
        });

        popup.showUnderneathOf(button);
    }

    @Override
    public @NotNull Component getTableCellEditorComponent(final @NotNull JTable table, final @Nullable Object value, final boolean isSelected, final int row, final int column) {
        currentValue = Objects.toString(value, "");
        button.setText(currentValue);
        return button;
    }

    @Override
    public @NotNull Object getCellEditorValue() {
        return currentValue;
    }
}
