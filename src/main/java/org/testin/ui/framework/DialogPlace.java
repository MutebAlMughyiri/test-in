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

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.util.Bundle;
import org.testin.util.Fonts;
import org.testin.util.Icons;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Optional;

// UC-INTERNAL-007, Rule-INTERNAL-108
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DialogPlace {
    private static final @NotNull String SEPARATOR = " › ";

    // Rule-INTERNAL-095, Rule-INTERNAL-108
    public static @NotNull JBLabel of(final @NotNull List<String> place) {
        return quiet(String.join(SEPARATOR, place));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-087, Rule-INTERNAL-108, Rule-INTERNAL-109
    public static @NotNull Optional<JComponent> row(final @NotNull List<String> from, final @NotNull List<String> to) {
        if (from.isEmpty() && to.isEmpty()) return Optional.empty();
        if (from.isEmpty()) return Optional.of(captioned(Bundle.message("caption.to"), to));
        if (to.isEmpty()) return Optional.of(captioned(Bundle.message("caption.from"), from));

        return Optional.of(leftAligned(transfer(from, to)));
    }

    // Rule-INTERNAL-087
    private static @NotNull JComponent captioned(final @NotNull String caption, final @NotNull List<String> place) {
        return leftAligned(Caption.above(caption, of(place)));
    }

    // Rule-INTERNAL-109
    private static @NotNull JComponent transfer(final @NotNull List<String> from, final @NotNull List<String> to) {
        final @NotNull JBPanel<?> row = sideBySide();
        row.add(of(from));
        row.add(arrow());
        row.add(destination(to));

        return row;
    }

    // Rule-INTERNAL-077, Rule-INTERNAL-109
    private static @NotNull JBLabel arrow() {
        final @NotNull JBLabel drawn = new JBLabel(Icons.gray(AllIcons.General.ArrowRight));
        drawn.setBorder(JBUI.Borders.empty(0, Spacing.M));

        return drawn;
    }

    // Rule-INTERNAL-109
    private static @NotNull JComponent destination(final @NotNull List<String> to) {
        final @NotNull JBLabel arriving = new JBLabel(to.getLast());
        arriving.setFont(Fonts.value());

        if (to.size() == 1) return arriving;

        final @NotNull JBPanel<?> row = sideBySide();
        row.add(quiet(String.join(SEPARATOR, to.subList(0, to.size() - 1)) + SEPARATOR));
        row.add(arriving);

        return row;
    }

    private static @NotNull JBPanel<?> sideBySide() {
        final @NotNull JBPanel<?> row = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);

        return row;
    }

    // Rule-INTERNAL-095
    private static @NotNull JBLabel quiet(final @NotNull String text) {
        final @NotNull JBLabel label = new JBLabel(text);
        label.setFont(Fonts.value());
        label.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        return label;
    }

    private static @NotNull JComponent leftAligned(final @NotNull JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);

        return component;
    }
}
