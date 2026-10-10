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

package org.testin.ui.dialogs;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.ui.framework.Spacing;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CollapsiblePanel {
    // Rule-INTERNAL-099, Rule-INTERNAL-133
    public static @NotNull JBPanel<?> build(final @NotNull String title, final @NotNull JComponent content, final boolean initiallyVisible) {
        return build(new JBLabel(title), content, initiallyVisible, List.of());
    }

    // Rule-INTERNAL-099, Rule-INTERNAL-133, Rule-INTERNAL-137, Rule-INTERNAL-139
    public static @NotNull JBPanel<?> build(final @NotNull JBLabel titleLabel, final @NotNull JComponent content, final boolean initiallyVisible, final @NotNull List<JComponent> buttons) {
        final @NotNull JBLabel hintLabel = DialogStyle.hint();

        final @NotNull JBPanel<?> header = Caption.header(titleLabel, Optional.of(buttons.isEmpty() ? hintLabel : besideEachOther(hintLabel, buttons)));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        content.setVisible(initiallyVisible);

        final @NotNull JBPanel<?> wrapper = DialogStyle.section(header, content);

        final @NotNull Runnable syncHeader = () -> DialogStyle.showFold(header, titleLabel, hintLabel, content.isVisible());
        syncHeader.run();

        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                content.setVisible(!content.isVisible());
                syncHeader.run();
                wrapper.revalidate();
                wrapper.repaint();
            }
        });

        return wrapper;
    }

    // Rule-INTERNAL-139
    private static @NotNull JComponent besideEachOther(final @NotNull JComponent hint, final @NotNull List<JComponent> buttons) {
        final @NotNull JBPanel<?> row = new JBPanel<>();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.add(hint);
        buttons.forEach(button -> {
            row.add(Box.createHorizontalStrut(JBUI.scale(Spacing.S)));
            row.add(button);
        });
        return row;
    }
}
