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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.IconUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.Tooltip;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HoverButton {
    private static final float BASE_SCALE = 1.3f;

    private static final float HOVER_SCALE = 1.8f;

    // UC-VIEW-PANEL-012, UC-VIEW-PANEL-014, UC-EDITOR-PANEL-046, Rule-VIEW-PANEL-056, Rule-EDITOR-PANEL-243
    public static @NotNull JComponent of(final @NotNull Project project, final @NotNull Offered offered, final @NotNull Icon drawn, final @NotNull String name, final @NotNull Runnable press) {
        return of(project, offered.whyNot(), drawn, name, offered.works() ? Declared.shortcutText(offered.action().getActionId()) : "", press);
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-120, Rule-VIEW-PANEL-121
    public static @NotNull JComponent of(final @NotNull Project project, final @NotNull Optional<String> whyNot, final @NotNull Icon drawn, final @NotNull String name, final @NotNull String shortcut, final @NotNull Runnable press) {
        final boolean works = whyNot.isEmpty();
        final @NotNull JBLabel label = new JBLabel();
        final @NotNull Icon shown = works ? drawn : IconLoader.getDisabledIcon(drawn);
        final @NotNull Icon base = IconUtil.scale(shown, label, BASE_SCALE);
        final @NotNull Icon hover = IconUtil.scale(shown, label, works ? HOVER_SCALE : BASE_SCALE);
        label.setIcon(base);
        label.setCursor(Cursor.getPredefinedCursor(works ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));

        Tooltip.set(label, whyNot.orElse(name), shortcut);

        // Rule-INTERNAL-122
        label.getAccessibleContext().setAccessibleName(name);
        whyNot.ifPresent(reason -> label.getAccessibleContext().setAccessibleDescription(reason));

        label.setPreferredSize(new Dimension(hover.getIconWidth(), hover.getIconHeight()));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);

        label.addMouseListener(new MouseAdapter() {
            // UC-VIEW-PANEL-012, Rule-VIEW-PANEL-052
            @Override
            public void mouseEntered(final MouseEvent e) {
                label.setIcon(hover);
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                label.setIcon(base);
            }

            // UC-VIEW-PANEL-012, UC-VIEW-PANEL-014
            @Override
            public void mouseClicked(final MouseEvent e) {
                whyNot.ifPresentOrElse(reason -> Services.getInstance(project, Notifier.class).softRefuse(project, reason), press);
            }
        });

        return label;
    }
}
