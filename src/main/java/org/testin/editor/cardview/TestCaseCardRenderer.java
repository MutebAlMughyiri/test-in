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
import org.testin.editor.TestinEditor;
import org.testin.editor.testset.TestCaseCard;
import org.testin.editor.testset.TestSetEditor;
import org.testin.model.TestCaseDto;

import javax.swing.JList;

public class TestCaseCardRenderer extends AbstractCardRenderer<TestinEditor> {
    private final @NotNull TestCaseCard card;

    public TestCaseCardRenderer(final @NotNull Project p, final @NotNull TestSetEditor editor) {
        super(editor);
        this.card = new TestCaseCard(p);
    }

    // UC-EDITOR-PANEL-001
    @Override
    protected @NotNull TestCaseCard bindDataAndGetCard(final @NotNull JList<? extends TestCaseDto> list, final @NotNull TestCaseDto testCase, final int row, final boolean isSelected, final boolean isRowHovered, final @NotNull String hover) {
        card.updateData(row, testCase, editor.getSelectedDetails(), editor.cardTitle(testCase));
        card.showTitleIcons(editor.getParent(), testCase);
        card.setActionsState(isSelected, isRowHovered, hover);
        card.applyCardViewLayout(list);

        return card;
    }
}