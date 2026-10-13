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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.testrun.RunItemCard;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItem;

import javax.swing.JList;

public class RunItemCardRenderer extends AbstractCardRenderer<TestRunEditor> {
    private final @NotNull RunItemCard card;

    public RunItemCardRenderer(final @NotNull Project project, final @NotNull TestRunEditor editor) {
        super(editor);
        this.card = new RunItemCard(project);
    }

    // UC-EDITOR-PANEL-030
    @Override
    protected @NotNull RunItemCard bindDataAndGetCard(final @NotNull JList<? extends TestCaseDto> list, final @NotNull TestCaseDto testCase, final int row, final boolean isSelected, final boolean isRowHovered, final @NotNull String hover) {
        final @NotNull RunItem runItem = editor.runItem(testCase.getId())
                .orElseGet(() -> RunItem.pendingFor(testCase));

        card.updateData(row, editor.getSelectedDetails(), runItem, editor.cardTitle(testCase));
        card.showTitleIcons(editor.getParent(), testCase);
        card.setActionsState(isSelected, isRowHovered, hover);
        card.applyCardViewLayout(list);

        return card;
    }
}