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

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.ui.Caption;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Fonts;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Band extends AbstractDetails {
    private static final int INSETS_TOP = 18;
    private static final int INSETS_SIDE = 16;

    private final @NotNull String name;
    private final @NotNull String rememberedAs;
    private final @NotNull List<AbstractDetails> rows;

    // Rule-VIEW-PANEL-085
    public static @NotNull Band of(final @NotNull String name, final @NotNull List<AbstractDetails> rows) {
        return new Band(name, "", List.copyOf(rows));
    }

    // Rule-VIEW-PANEL-087
    public static @NotNull Band folding(final @NotNull String name, final @NotNull String rememberedAs, final @NotNull List<AbstractDetails> rows) {
        return new Band(name, rememberedAs, List.copyOf(rows));
    }

    // UC-VIEW-PANEL-004, UC-VIEW-PANEL-005, Rule-VIEW-PANEL-085, Rule-VIEW-PANEL-087
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto testCase, final int currentRow) {
        int row = addFullWidthRow(panel, (GridBagConstraints) gbc.clone(), heading(p), JBUI.insets(INSETS_TOP, INSETS_SIDE, 0, INSETS_SIDE), currentRow);

        if (!isOpen()) return row;

        for (final AbstractDetails field : rows) {
            row = field.render(p, panel, (GridBagConstraints) gbc.clone(), testCase, row);
        }

        return row;
    }

    private boolean alwaysOpen() {
        return rememberedAs.isEmpty();
    }

    // Rule-VIEW-PANEL-087
    private boolean isOpen() {
        return alwaysOpen() || PropertiesComponent.getInstance().getBoolean(rememberedAs, false);
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-085, Rule-VIEW-PANEL-087, Rule-INTERNAL-133
    private @NotNull JComponent heading(final @NotNull Project p) {
        final @NotNull JBLabel caption = Caption.of(name, Fonts.panelCaption());
        if (alwaysOpen()) return Caption.header(caption, Optional.empty());

        final @NotNull JBLabel hint = DialogStyle.hint();
        final @NotNull JBPanel<?> heading = Caption.header(caption, Optional.of(hint));
        DialogStyle.showFold(heading, caption, hint, isOpen());
        heading.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        heading.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                PropertiesComponent.getInstance().setValue(rememberedAs, !isOpen(), false);
                ViewToolWindowFactory.panel(p).ifPresent(ViewPanel::refreshCurrentView);
            }
        });

        return heading;
    }
}
