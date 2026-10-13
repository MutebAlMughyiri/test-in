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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.Nodes;
import org.testin.model.TestCaseDto;
import org.testin.search.GoTo;
import org.testin.search.Hit;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.Fonts;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class Breadcrumb extends AbstractDetails {
    final @NotNull Color DEFAULT_TEXT_COLOR = JBUI.CurrentTheme.ContextHelp.FOREGROUND;
    final int SEPARATOR_BORDER_V = 0;
    final int SEPARATOR_BORDER_H = 6;
    final int INSETS_TOP = 22;
    final int INSETS_LEFT = 32;
    final int INSETS_BOTTOM = 5;
    final int INSETS_RIGHT = 16;

    private final @NotNull List<String> treePath;

    // UC-VIEW-PANEL-010, Rule-VIEW-PANEL-041
    @Override
    public int render(final @NotNull Project project, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints constraints, final @NotNull TestCaseDto testCase, final int currentRow) {
        final @NotNull JBPanel<?> pathPanel = line(0);

        {
            for (int i = 0; i < treePath.size(); i++) {
                final @NotNull String labelText = treePath.get(i);
                final boolean isLast = (i == treePath.size() - 1);

                final int index = i;

                final @NotNull JBLabel folderLabel = new JBLabel(labelText);
                folderLabel.setFont(Fonts.label());
                folderLabel.setForeground(DEFAULT_TEXT_COLOR);
                folderLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                folderLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(final MouseEvent e) {
                        folderLabel.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
                        setUnderline(folderLabel, true);
                    }

                    @Override
                    public void mouseExited(final MouseEvent e) {
                        folderLabel.setForeground(DEFAULT_TEXT_COLOR);
                        setUnderline(folderLabel, false);
                    }

                    // UC-VIEW-PANEL-010, Rule-VIEW-PANEL-042, Rule-VIEW-PANEL-043
                    @Override
                    public void mouseClicked(final MouseEvent e) {
                        final @NotNull Path stepPath = Services.getInstance(project, TestinRoot.class).resolve(treePath.subList(0, index + 1));

                        Services.getInstance(project, Nodes.class).find(stepPath).map(Hit::of).ifPresent(hit -> GoTo.the(project, hit));
                    }
                });

                pathPanel.add(folderLabel);
                if (!isLast) {
                    final @NotNull JBLabel separator = new JBLabel(AllIcons.General.ArrowRight);
                    separator.setBorder(JBUI.Borders.empty(SEPARATOR_BORDER_V, SEPARATOR_BORDER_H));
                    pathPanel.add(separator);
                }
            }
        }

        constraints.gridx = 0;
        constraints.gridy = currentRow;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = JBUI.insets(INSETS_TOP, INSETS_LEFT, INSETS_BOTTOM, INSETS_RIGHT);

        panel.add(pathPanel, constraints);

        return currentRow + 1;
    }

    private void setUnderline(final @NotNull JBLabel label, final boolean underline) {
        final @NotNull Font font = label.getFont();
        final @NotNull Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
        attributes.put(TextAttribute.UNDERLINE, underline ? TextAttribute.UNDERLINE_ON : -1);
        label.setFont(font.deriveFont(attributes));
    }
}
