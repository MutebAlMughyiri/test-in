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

package org.testin.view.details;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.testrun.ExecutingNow;
import org.testin.model.TestCaseDto;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.ui.framework.Prose;
import org.testin.util.Bundle;
import org.testin.util.Fonts;
import org.testin.util.Icons;

import javax.swing.JTextArea;
import java.awt.GridBagConstraints;

public class Title extends AbstractDetails {
    final int INSETS_TOP = 20;
    final int INSETS_LEFT = 16;
    final int INSETS_BOTTOM = 0;
    final int INSETS_RIGHT = 16;

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-025, Rule-VIEW-PANEL-125
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto testCase, final int currentRow) {
        final @NotNull String titleText = TestSetEditorAttributes.DESCRIPTION.displayValue(testCase);
        final @NotNull String finalValue = titleText.trim().isEmpty() ? "-" : titleText;

        final boolean executing = ExecutingNow.is(p, testCase.getId());
        final @NotNull JTextArea mainTitleArea = Prose.of(finalValue, executing ? Icons.EXECUTING : EmptyIcon.ICON_0);
        if (executing)
            mainTitleArea.getAccessibleContext().setAccessibleDescription(Bundle.message("execution.executing"));

        mainTitleArea.setFont(Fonts.title());

        gbc.gridx = 0;
        gbc.gridy = currentRow;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = JBUI.insets(INSETS_TOP, INSETS_LEFT, INSETS_BOTTOM, INSETS_RIGHT);

        panel.add(mainTitleArea, gbc);

        return currentRow + 1;
    }
}