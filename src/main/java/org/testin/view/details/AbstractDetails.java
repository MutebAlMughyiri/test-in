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

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.WrapLayout;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.open.TestinEditors;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestRunNode;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.Fonts;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.JComponent;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Optional;

public abstract class AbstractDetails {
    private static final int ROW_GAP = 4;

    // Rule-VIEW-PANEL-080, Rule-VIEW-PANEL-082
    static @NotNull JBPanel<?> row(final int gap) {
        final @NotNull JBPanel<?> row = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(gap), 0));

        row.setOpaque(false);

        return row;
    }

    // Rule-VIEW-PANEL-124
    static @NotNull JBPanel<?> line(final int gap) {
        final @NotNull JBPanel<?> line = new JBPanel<>(new WrapLayout(FlowLayout.LEFT, JBUI.scale(gap), JBUI.scale(ROW_GAP)));

        line.setBorder(JBUI.Borders.empty(-ROW_GAP, 0));
        line.setOpaque(false);

        return line;
    }

    // Rule-VIEW-PANEL-080
    public static @NotNull ActionLink link(final @NotNull String text, final @NotNull ActionListener onClick) {
        final @NotNull ActionLink link = new ActionLink(text, onClick);
        link.setAutoHideOnDisable(false);
        link.setFocusable(false);
        link.setFont(Fonts.body());
        return link;
    }

    // UC-VIEW-PANEL-005
    static @NotNull Optional<TestRunNode> testRunOf(final @NotNull Project project, final @NotNull List<String> currentPath) {
        return Services.getInstance(project, TestRuns.class).findTestRunNode(Services.getInstance(project, TestinRoot.class).resolve(currentPath));
    }

    // UC-VIEW-PANEL-016, UC-VIEW-PANEL-004
    static void redraw(final @NotNull Project project, final @NotNull TestCaseDto testCase, final @NotNull TestRunNode testRunNode) {
        ViewToolWindowFactory.refreshIfShowing(project, List.of(testCase));
        Services.getInstance(project, TestinEditors.class).testRunEditorFor(testRunNode).ifPresent(TestRunEditor::refreshView);
    }

    public abstract int render(final @NotNull Project project, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto testCase, final int currentRow);

    protected int addRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull String caption, final @NotNull String value, final int row) {
        return CaptionValueRow.add(panel, gbc, caption, value, row);
    }

    protected int addRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull String caption, final @NotNull JComponent value, final int row) {
        return CaptionValueRow.add(panel, gbc, caption, value, row);
    }

    // Rule-VIEW-PANEL-085, Rule-VIEW-PANEL-124
    protected int addFullWidthRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull JComponent component, final @NotNull Insets insets, final int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = insets;

        panel.add(component, gbc);

        return row + 1;
    }
}
