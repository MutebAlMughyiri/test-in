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
import org.jetbrains.annotations.NotNull;
import org.testin.model.StatusBarItem;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.util.Fonts;

import javax.swing.Icon;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.Arrays;

public class ShortcutStrip {
    private static final @NotNull String INNER_SEPARATOR = " ";

    private static final @NotNull String OUTER_SEPARATOR = "       ";

    private final @NotNull JBPanel<?> statusBar;

    private final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);

    private final @NotNull Color labelColor = JBUI.CurrentTheme.Label.foreground();

    private final @NotNull Color dotColor = JBUI.CurrentTheme.ContextHelp.FOREGROUND;

    private final @NotNull Color separatorColor = JBUI.CurrentTheme.ContextHelp.FOREGROUND;

    private final @NotNull Font font = Fonts.small();

    private final @NotNull Icon icon = AllIcons.General.Keyboard;

    private final @NotNull Border border = JBUI.Borders.emptyRight(Spacing.S);

    // UC-INTERNAL-007, Rule-INTERNAL-079
    public ShortcutStrip(final StatusBarItem @NotNull [] items) {
        this.statusBar = new OneLine();
        this.statusBar.setBorder(JBUI.Borders.empty(Spacing.XS, Spacing.L));
        this.statusBar.setOpaque(true);
        this.statusBar.setBackground(JBUI.CurrentTheme.Advertiser.background());

        updateItems(items);
        setShown(true);
    }

    // UC-SETTING-008, Rule-SETTING-029
    public void setShown(final boolean wanted) {
        statusBar.setVisible(wanted && settings.showShortcutHints);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-078
    public void updateItems(final StatusBarItem @NotNull [] items) {
        this.statusBar.removeAll();
        this.statusBar.add(setStatusBarIcon());

        for (int i = 0; i < items.length; i++) {
            final @NotNull JBPanel<?> key = new JBPanel<>(new GridBagLayout());
            key.setOpaque(false);

            final @NotNull GridBagConstraints onOneRow = new GridBagConstraints();
            onOneRow.gridy = 0;

            if (i > 0) key.add(createSeparator(), onOneRow);
            key.add(Keycap.of(items[i].getShortcutText()), onOneRow);
            key.add(createDot(), onOneRow);
            key.add(createLabel(items[i].getName()), onOneRow);

            this.statusBar.add(key);
        }

        this.statusBar.revalidate();
        this.statusBar.repaint();
    }

    private @NotNull JBLabel setStatusBarIcon() {
        final @NotNull JBLabel label = new JBLabel(icon);
        label.setBorder(border);
        return label;
    }

    private @NotNull JBLabel createDot() {
        final @NotNull JBLabel label = new JBLabel(INNER_SEPARATOR);
        label.setForeground(dotColor);
        return label;
    }

    private @NotNull JBLabel createLabel(final @NotNull String text) {
        final @NotNull JBLabel label = new JBLabel(text);
        label.setForeground(labelColor);
        label.setFont(font);
        return label;
    }

    private @NotNull JBLabel createSeparator() {
        final @NotNull JBLabel label = new JBLabel(OUTER_SEPARATOR);
        label.setForeground(separatorColor);
        label.setFont(font);
        return label;
    }

    public @NotNull JBPanel<?> getPanel() {
        return statusBar;
    }

    // Rule-INTERNAL-104
    private static final class OneLine extends JBPanel<OneLine> {
        private OneLine() {
            super(new KeysThatFit());
        }

        @Override
        public @NotNull Dimension getPreferredSize() {
            return new Dimension(0, super.getPreferredSize().height);
        }
    }

    // Rule-INTERNAL-078, Rule-INTERNAL-104
    private static final class KeysThatFit implements LayoutManager {
        @Override
        public void addLayoutComponent(final @NotNull String name, final @NotNull Component key) {
        }

        @Override
        public void removeLayoutComponent(final @NotNull Component key) {
        }

        @Override
        public @NotNull Dimension preferredLayoutSize(final @NotNull Container strip) {
            final @NotNull Insets insets = strip.getInsets();
            final int wide = Arrays.stream(strip.getComponents()).mapToInt(key -> key.getPreferredSize().width).sum();
            final int tall = Arrays.stream(strip.getComponents()).mapToInt(key -> key.getPreferredSize().height).max().orElse(0);
            return new Dimension(insets.left + wide + insets.right, insets.top + tall + insets.bottom);
        }

        @Override
        public @NotNull Dimension minimumLayoutSize(final @NotNull Container strip) {
            return preferredLayoutSize(strip);
        }

        @Override
        public void layoutContainer(final @NotNull Container strip) {
            final @NotNull Insets insets = strip.getInsets();
            final int room = strip.getWidth() - insets.right;
            final int tall = strip.getHeight() - insets.top - insets.bottom;

            int x = insets.left;
            boolean fits = true;
            for (final Component key : strip.getComponents()) {
                final @NotNull Dimension wanted = key.getPreferredSize();
                fits = fits && x + wanted.width <= room;
                key.setBounds(fits ? x : strip.getWidth(), insets.top + (tall - wanted.height) / 2, wanted.width, wanted.height);
                x += wanted.width;
            }
        }
    }
}
