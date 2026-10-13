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
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.ui.framework.Prose;
import org.testin.util.Display;
import org.testin.util.Fonts;

import javax.swing.BoxLayout;
import javax.swing.JTextArea;
import java.awt.GridBagConstraints;
import java.util.List;

public class Steps extends AbstractDetails {
    private static final int MARGIN_BOTTOM_PER_STEP = 8;

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-027
    @Override
    public int render(final @NotNull Project project, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto testCase, final int row) {
        final @NotNull List<String> steps = testCase.getSteps();

        if (steps.isEmpty() || steps.stream().allMatch(String::isBlank))
            return row;

        final @NotNull JBPanel<?> stepsContainer = new JBPanel<>();
        stepsContainer.setLayout(new BoxLayout(stepsContainer, BoxLayout.Y_AXIS));
        stepsContainer.setOpaque(false);

        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).isBlank()) continue;

            final @NotNull String stepText = Display.numberedStep(i, steps.get(i));
            final int marginBottom = (i == steps.size() - 1) ? 0 : MARGIN_BOTTOM_PER_STEP;
            stepsContainer.add(createStepComponent(stepText, marginBottom));
        }

        return addRow(panel, gbc, TestSetEditorAttributes.STEPS.getName(), stepsContainer, row);
    }

    private @NotNull JTextArea createStepComponent(final @NotNull String text, final int marginBottom) {
        final @NotNull JTextArea stepArea = Prose.of(text);
        stepArea.setFont(Fonts.body());

        stepArea.setBorder(JBUI.Borders.emptyBottom(marginBottom));
        return stepArea;
    }
}