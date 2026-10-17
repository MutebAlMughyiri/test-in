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

package org.testin.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Priority;
import org.testin.model.TestCase;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestCaseStatus;
import org.testin.util.Bundle;
import org.testin.util.FixedColors;
import org.testin.util.Fonts;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Badges {
    static final int BADGE_RADIUS = 20;

    static final int TAG_NOTCH = 7;

    static final @NotNull String PAIR_JOIN = " / ";

    static final @NotNull Icon BUG_MARK = IconUtil.colorize(IconUtil.resizeSquared(AllIcons.Toolwindows.ToolWindowDebugger, 20), FixedColors.BLACK);

    private static final int BADGE_ICON_GAP = 4;

    private static final int BADGE_PAD_V = 2;

    private static final int BADGE_PAD_H = 10;

    private static final @NotNull Color TEXT_ON_LIGHT = FixedColors.DARK_GRAY;

    private static final @NotNull Color TEXT_ON_DARK = FixedColors.WHITE;

    private static final @NotNull Color GROUP_COLOR = JBColor.darkGray;

    private static final @NotNull Icon CLOCK = IconUtil.resizeSquared(AllIcons.Vcs.History, 14);

    // Rule-VIEW-PANEL-114, Rule-EDITOR-PANEL-268
    public static void addPriorityBadge(final @NotNull List<Badge> badges, final @NotNull TestCase testCase) {
        if (testCase.getPriority() == Priority.DEFAULT) return;

        badges.add(new Pill(testCase.getPriority().getLabel(), testCase.getPriority().getColor(), testCase.getPriority().tooltip()));
    }

    // UC-EDITOR-PANEL-001
    public static @NotNull List<Badge> testCaseBadges(final @NotNull TestCase testCase) {
        final @NotNull List<Badge> badges = new ArrayList<>();
        addPriorityBadge(badges, testCase);

        for (final String group : testCase.getGroups()) {
            badges.add(createGroupBadge(group));
        }

        return badges;
    }

    // Rule-EDITOR-PANEL-294
    public static @NotNull Badge createTestCaseStatusBadge(final @NotNull TestCaseStatus testCaseStatus) {
        return new Framed(testCaseStatus.getLabel(), EmptyIcon.ICON_0, FixedColors.CREAM, "");
    }

    public static @NotNull Badge createRunItemStatusBadge(final @NotNull RunItemStatus runItemStatus) {
        return new Pill(runItemStatus.getLabel(), runItemStatus.getRowColor());
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-253, Rule-VIEW-PANEL-086
    public static void addBugBadge(final @NotNull List<Badge> badges, final @NotNull String name, final @NotNull String value, final @NotNull Color color) {
        if (value.isBlank()) return;
        final @NotNull String said = name + ": " + value;

        for (int i = 0; i < badges.size(); i++) {
            final @NotNull Optional<Badge> paired = badges.get(i).pairedWith(value, said);
            if (paired.isPresent()) {
                badges.set(i, paired.orElseThrow());
                return;
            }
        }

        badges.add(new BugPill(value, color, said));
    }

    public static @NotNull Badge createGroupBadge(final @NotNull String group) {
        return new Tag(group, GROUP_COLOR);
    }

    // Rule-VIEW-PANEL-086
    // Rule-EDITOR-PANEL-270, Rule-VIEW-PANEL-086
    public static void addDurationBadge(final @NotNull List<Badge> badges, final @NotNull String duration) {
        if (!duration.isBlank())
            badges.add(new Framed(duration, CLOCK, UIUtil.getLabelForeground(), Bundle.message("field.run.item.duration")));
    }

    // Rule-VIEW-PANEL-085
    public static void showBadges(final @NotNull JBPanel<?> panel, final @NotNull List<Badge> badges) {
        panel.setVisible(!badges.isEmpty());

        while (panel.getComponentCount() < badges.size()) {
            panel.add(new BadgePill());
        }

        for (int i = 0; i < panel.getComponentCount(); i++) {
            final @NotNull BadgePill pill = (BadgePill) panel.getComponent(i);

            if (i < badges.size()) pill.show(badges.get(i));
            else pill.setVisible(false);
        }
    }

    // Rule-VIEW-PANEL-085, Rule-VIEW-PANEL-124
    public static @NotNull List<JComponent> pills(final @NotNull List<Badge> badges) {
        return badges.stream().map(badge -> {
            final @NotNull BadgePill pill = new BadgePill();
            pill.show(badge);
            return (JComponent) pill;
        }).toList();
    }

    static boolean isLight(final @NotNull Color background) {
        return 0.2126 * background.getRed() + 0.7152 * background.getGreen() + 0.0722 * background.getBlue() > 140;
    }

    // Rule-EDITOR-PANEL-252
    static @NotNull Color readableOn(final @NotNull Color fill) {
        return isLight(fill) ? TEXT_ON_LIGHT : TEXT_ON_DARK;
    }

    static void fillPill(final @NotNull Graphics2D g2, final @NotNull Color fill, final int width, final int height) {
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, width, height, BADGE_RADIUS, BADGE_RADIUS);
    }

    private static final class BadgePill extends JBLabel {
        private @NotNull Badge badge = new Pill("", JBColor.GRAY);

        private BadgePill() {
            setOpaque(false);
            setBorder(JBUI.Borders.empty(BADGE_PAD_V, BADGE_PAD_H));
            setIconTextGap(JBUI.scale(BADGE_ICON_GAP));
        }

        // Rule-VIEW-PANEL-086
        private void show(final @NotNull Badge badge) {
            this.badge = badge;

            setText(badge.text());
            setForeground(badge.ink());
            setIcon(badge.mark());
            setBorder(JBUI.Borders.empty(BADGE_PAD_V, BADGE_PAD_H, BADGE_PAD_V, BADGE_PAD_H + badge.notch()));

            setFont(Fonts.badge());
            Tooltip.set(this, badge.tooltip());

            setVisible(true);
        }

        @Override
        protected void paintComponent(final Graphics g) {
            final @NotNull Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            badge.paint(g2, getWidth(), getHeight());

            g2.dispose();

            super.paintComponent(g);
        }
    }
}
