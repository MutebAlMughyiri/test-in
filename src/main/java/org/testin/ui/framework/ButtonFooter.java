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

package org.testin.ui.framework;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Tooltip;
import org.testin.util.Fonts;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.util.Optional;

// UC-INTERNAL-007, Rule-INTERNAL-080
final class ButtonFooter {
    private final @NotNull JComponent button;
    private final @NotNull JBLabel tally = new JBLabel();
    private final @NotNull JBPanel<?> panel;

    // Rule-INTERNAL-095, Rule-INTERNAL-119
    ButtonFooter(final @NotNull JComponent button) {
        this.button = button;
        button.setOpaque(false);
        button.setFont(Fonts.row());
        tally.setFont(Fonts.small());
        tally.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.empty(Spacing.M, Spacing.XL));
        panel.add(tally, BorderLayout.WEST);
        panel.add(button, BorderLayout.EAST);
    }

    @NotNull JComponent getPanel() {
        return panel;
    }

    // UC-INTERNAL-007, Rule-INTERNAL-080
    void enableUnless(final @NotNull Optional<String> reason) {
        button.setEnabled(reason.isEmpty());
        Tooltip.set(button, reason.orElse(""));
    }

    // Rule-INTERNAL-080
    void showTally(final @NotNull String text) {
        tally.setText(text);
    }
}
