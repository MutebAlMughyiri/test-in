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

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.editor.card.AbstractCard;
import org.testin.model.TestCaseDto;

import javax.swing.JList;
import javax.swing.ListCellRenderer;

@AllArgsConstructor
public abstract class AbstractCardRenderer<U extends TestinEditor> implements ListCellRenderer<TestCaseDto> {
    protected final @NotNull U editor;

    // UC-EDITOR-PANEL-001, UC-EDITOR-PANEL-030
    @Override
    public @NotNull AbstractCard getListCellRendererComponent(final JList<? extends TestCaseDto> list, final TestCaseDto tc, final int index, final boolean isSelected, final boolean cellHasFocus) {
        final boolean isRowHovered = (index == editor.getHoveredIndex());
        final @NotNull String hover = isRowHovered ? editor.getHoveredIconAction() : "";

        return bindDataAndGetCard(list, tc, index, isSelected, isRowHovered, hover);
    }

    protected abstract @NotNull AbstractCard bindDataAndGetCard(final @NotNull JList<? extends TestCaseDto> list, final @NotNull TestCaseDto tc, final int row, final boolean isSelected, final boolean isRowHovered, final @NotNull String hover);
}
