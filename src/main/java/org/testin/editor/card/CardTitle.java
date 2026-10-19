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

package org.testin.editor.card;

import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Automated;
import org.testin.quickaction.Offered;
import org.testin.util.Fonts;
import org.testin.util.Icons;

import javax.swing.Icon;
import javax.swing.JList;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.IntStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CardTitle {
    // UC-EDITOR-PANEL-048, Rule-EDITOR-PANEL-235, Rule-EDITOR-PANEL-291
    public static @NotNull TitleActionIcons descriptionActionIcons(final int titleWidth, final @NotNull List<Offered> buttons, final boolean executing) {
        final int afterDot = executing ? 1 : 0;

        return new TitleActionIcons(IntStream.range(0, buttons.size())
                .mapToObj(i -> new Slot(buttons.get(i), slot(titleWidth, afterDot + i)))
                .toList());
    }

    // Rule-EDITOR-PANEL-291
    public static @NotNull Rectangle executingDot(final int titleWidth) {
        return slot(titleWidth, 0);
    }

    // Rule-EDITOR-PANEL-235, Rule-EDITOR-PANEL-291
    public static int slotCount(final @NotNull List<Offered> buttons, final boolean executing) {
        return buttons.size() + (executing ? 1 : 0);
    }

    private static @NotNull Rectangle slot(final int titleWidth, final int place) {
        final @NotNull Icon slot = Icons.TEST_CASE;
        final int first = JBUI.scale(16) + titleWidth + JBUI.scale(10);
        final int step = slot.getIconWidth() + JBUI.scale(8);

        return new Rectangle(first + place * step, JBUI.scale(12), slot.getIconWidth(), slot.getIconHeight());
    }

    // Rule-EDITOR-PANEL-003, Rule-EDITOR-PANEL-235
    public static int titleWidth(final @NotNull JList<?> list, final @NotNull String title, final int count) {
        return Math.min(list.getFontMetrics(Fonts.title()).stringWidth(title), titleColumnWidth(list.getWidth(), count));
    }

    public static int titleColumnWidth(final int listWidth, final int count) {
        final @NotNull Icon slot = Icons.TEST_CASE;
        final int forIcons = JBUI.scale(10) + count * slot.getIconWidth() + Math.max(0, count - 1) * JBUI.scale(8);
        final int column = listWidth - JBUI.scale(16) * 2 - forIcons;

        return column > forIcons ? column : Integer.MAX_VALUE;
    }

    public static void drawDescriptionActionIcons(final @NotNull Component component, final @NotNull Graphics g, final int titleWidth, final @NotNull String hoveredAction, final @NotNull List<Offered> buttons, final @NotNull Automated automation, final boolean executing) {
        descriptionActionIcons(titleWidth, buttons, executing).slots()
                .forEach(slot -> draw(component, g, slot.button(), slot.button().action().iconOn(automation), slot.at(), hoveredAction));
    }

    // UC-EDITOR-PANEL-047, Rule-CODEGEN-062, Rule-EDITOR-PANEL-235
    private static void draw(final @NotNull Component component, final @NotNull Graphics g, final @NotNull Offered button, final @NotNull Icon icon, final @NotNull Rectangle at, final @NotNull String hoveredAction) {
        final @NotNull Icon shown = button.works() ? icon : IconLoader.getDisabledIcon(icon);

        drawHoverableIcon(component, g, shown, at.x + (at.width - shown.getIconWidth()) / 2, at.y + (at.height - shown.getIconHeight()) / 2,
                button.works() && button.action().name().equals(hoveredAction));
    }

    private static void drawHoverableIcon(final @NotNull Component component, final @NotNull Graphics g, final @NotNull Icon baseIcon, final int x, final int y, final boolean isHovered) {
        if (isHovered) {
            final @NotNull Icon scaledIcon = IconUtil.scale(baseIcon, component, 1.5f);
            final int offsetX = (scaledIcon.getIconWidth() - baseIcon.getIconWidth()) / 2;
            final int offsetY = (scaledIcon.getIconHeight() - baseIcon.getIconHeight()) / 2;
            scaledIcon.paintIcon(component, g, x - offsetX, y - offsetY);
        } else {
            baseIcon.paintIcon(component, g, x, y);
        }
    }

    // Rule-EDITOR-PANEL-267
    public static @NotNull Rectangle priorityMargin(final int height) {
        return new Rectangle(0, 0, JBUI.scale(16), height);
    }
}
