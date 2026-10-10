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

import com.intellij.openapi.Disposable;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public record GridView(@NotNull JBTable table, @NotNull JComponent scrollPane, @NotNull Disposable fontSync) {
    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-119
    public boolean isCellOpen() {
        return table.isEditing();
    }

    // UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-019
    public void commitOpenCell() {
        if (!isCellOpen()) return;

        table.getCellEditor().stopCellEditing();
    }

    public boolean hasKeyboard() {
        return table.hasFocus() || isCellOpen();
    }

    // Rule-EDITOR-PANEL-249
    public boolean handOver() {
        final boolean keyboard = hasKeyboard();

        commitOpenCell();

        return keyboard;
    }
}
