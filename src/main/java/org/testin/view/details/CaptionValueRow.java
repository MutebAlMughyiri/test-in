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

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.ui.Tooltip;
import org.testin.ui.framework.Prose;
import org.testin.util.Display;
import org.testin.util.Fonts;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import java.awt.GridBagConstraints;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CaptionValueRow {
    private static final int SPACE_ABOVE = 12;

    private static final int CAPTION_GAP = 2;

    private static final int SIDE = 16;

    // Rule-VIEW-PANEL-006, Rule-INTERNAL-132, Rule-VIEW-PANEL-024
    public static int add(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints constraints, final @NotNull String caption, final @NotNull String valueText, final int row) {
        if (valueText.trim().isEmpty()) return row;

        final @NotNull JTextArea valueArea = Prose.of(Display.shortDates(valueText));
        valueArea.setFont(Fonts.body());
        Tooltip.set(valueArea, Display.dateTooltip(valueText));

        return add(panel, constraints, caption, valueArea, row);
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-082
    public static int add(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints constraints, final @NotNull String caption, final @NotNull JComponent value, final int row) {
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;

        constraints.gridy = row;
        constraints.insets = JBUI.insets(SPACE_ABOVE, SIDE, CAPTION_GAP, SIDE);
        panel.add(Caption.of(caption, Fonts.panelCaption()), constraints);

        constraints.gridy = row + 1;
        constraints.insets = JBUI.insets(0, SIDE);
        panel.add(value, constraints);

        return row + 2;
    }
}
