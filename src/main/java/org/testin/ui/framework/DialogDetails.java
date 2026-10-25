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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.DetailRow;
import org.testin.ui.Caption;
import org.testin.ui.Tooltip;
import org.testin.util.Display;
import org.testin.util.Fonts;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.util.List;

public final class DialogDetails implements DialogComponent {
    private static final int VALUE_CHARACTERS = 36;

    private final @NotNull JBPanel<?> panel;

    DialogDetails(final @NotNull List<DetailRow> rows) {
        final @NotNull JBPanel<?> stack = new JBPanel<>();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        stack.setBorder(JBUI.Borders.empty(Spacing.XS, 0));

        for (final DetailRow row : rows) {
            // Rule-INTERNAL-087
            final @NotNull JBLabel value = wrappingValue(row.value());
            row.icon().ifPresent(icon -> {
                value.setIcon(icon);
                value.setIconTextGap(JBUI.scale(Spacing.M));
            });
            stack.add(Caption.above(row.caption(), value));
        }

        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        panel.add(stack, BorderLayout.NORTH);
    }

    // Rule-INTERNAL-096
    // Rule-INTERNAL-132
    private static @NotNull JBLabel wrappingValue(final @NotNull String value) {
        final @NotNull JBLabel label = new JBLabel();
        label.setFont(Fonts.value());
        label.setText("<html><div style='width:" + label.getFontMetrics(Fonts.value()).charWidth('m') * VALUE_CHARACTERS + "px'>"
                + StringUtil.escapeXmlEntities(Display.shortDates(value)) + "</div></html>");
        label.setBorder(JBUI.Borders.emptyLeft(Spacing.XL));
        Tooltip.set(label, Display.dateTooltip(value));
        return label;
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return panel;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    @Override
    public boolean wantsFocus() {
        return false;
    }
}
