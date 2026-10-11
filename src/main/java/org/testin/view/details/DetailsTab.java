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
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.ExecutionPosition;
import org.testin.editor.WheelForwarding;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.Execution;
import org.testin.model.testrun.RunItem;
import org.testin.testcase.TestCaseOrder;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.ui.FontSync;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.Fonts;

import javax.swing.BorderFactory;
import javax.swing.Box;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;

public class DetailsTab {
    static final @NotNull String TEST_CASE_OPEN = "testin.viewPanel.testCaseOpen";
    final int SCROLL_UNIT_INCREMENT = 16;
    final @NotNull String PLACEHOLDER_TEXT = Bundle.message("details.placeholder");
    final int INSETS_DEFAULT = 5;
    final double WEIGHT_X = 1.0;
    final double SPACER_WEIGHT_Y = 1.0;

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-061, Rule-VIEW-PANEL-085, Rule-VIEW-PANEL-086, Rule-VIEW-PANEL-103
    private static @NotNull Band testRunBand(final @NotNull RunItem runItem, final @NotNull List<String> currentPath, final int shownLine, final @NotNull IntConsumer showExecution) {
        // Rule-VIEW-PANEL-122
        final @NotNull Optional<Execution> shown = runItem.shownExecution(shownLine);

        return Band.of(Bundle.message("details.band.run"), List.of(
                new RunItemSummary(runItem, currentPath, showExecution),
                new AttributeRow(TestRunEditorAttributes.ACTUAL_RESULT.getName(), (_, _) -> shown.map(Execution::getActualResult).orElseGet(runItem::getActualResult)),
                new StacktraceLine(shown, currentPath),
                new AttributeRow(TestRunEditorAttributes.EXECUTED_BY.getName(), (_, _) -> shown.map(execution -> Display.whoAndWhen(execution.getExecutedBy(), execution.getExecutedAt())).orElse(""))));
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-087, Rule-VIEW-PANEL-103
    private static @NotNull List<AbstractDetails> testCaseFields() {
        return List.of(
                new AttributeRow(TestSetEditorAttributes.EXPECTED_RESULT.getName(), (_, dto) -> TestSetEditorAttributes.EXPECTED_RESULT.displayValue(dto)),
                new Steps(),
                new AttributeRow(TestSetEditorAttributes.PRE_CONDITIONS.getName(), (_, dto) -> TestSetEditorAttributes.PRE_CONDITIONS.displayValue(dto)),
                new AttributeRow(TestSetEditorAttributes.TEST_DATA.getName(), (_, dto) -> TestSetEditorAttributes.TEST_DATA.displayValue(dto)),
                new AttributeRow(TestSetEditorAttributes.REFERENCE.getName(), (_, dto) -> TestSetEditorAttributes.REFERENCE.displayValue(dto)),
                new AttributeRow(TestSetEditorAttributes.MODULE.getName(), (_, dto) -> TestSetEditorAttributes.MODULE.displayValue(dto)),
                new AttributeRow(TestSetEditorAttributes.ORDER.getName(), (p, dto) -> TestCaseOrder.placeText(ExecutionPosition.placesOf(p, List.of(dto)).getOrDefault(dto.getId(), 0))),
                new AttributeRow(Bundle.message("details.created"), (_, dto) -> Display.whoAndWhen(dto.getCreatedBy(), dto.getCreatedAt())),
                new AttributeRow(Bundle.message("details.updated"), (_, dto) -> Display.whoAndWhen(dto.getUpdatedBy(), dto.getUpdatedAt())));
    }

    // UC-VIEW-PANEL-004
    public void load(final @NotNull Project p, final @NotNull JBPanel<?> detailsTab, final @NotNull Optional<TestCaseDto> dto, final @NotNull Optional<RunItem> runItem, final @NotNull List<String> currentPath, final int shownLine, final @NotNull IntConsumer showExecution) {
        detailsTab.removeAll();
        detailsTab.setLayout(new BorderLayout());
        detailsTab.setBorder(BorderFactory.createEmptyBorder());

        dto.ifPresentOrElse(
                testCase -> renderTestCase(p, detailsTab, testCase, runItem, currentPath, shownLine, showExecution),
                () -> renderPlaceholder(detailsTab));

        detailsTab.revalidate();
        detailsTab.repaint();
    }

    private void renderTestCase(final @NotNull Project p, final @NotNull JBPanel<?> detailsTab, final @NotNull TestCaseDto dto, final @NotNull Optional<RunItem> runItem, final @NotNull List<String> currentPath, final int shownLine, final @NotNull IntConsumer showExecution) {
        final @NotNull JBPanel<?> contentPanel = new JBPanel<>(new GridBagLayout());
        contentPanel.setOpaque(false);

        renderRows(p, contentPanel, dto, runItem, currentPath, shownLine, showExecution);

        final @NotNull JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);

        FontSync.attachWheelZoom(p, contentPanel);

        contentPanel.addMouseWheelListener(WheelForwarding::forwardWheelToScrollPane);

        detailsTab.add(scrollPane, BorderLayout.CENTER);

        EditShownTestCase.bindTo(p, detailsTab);
    }

    private void renderPlaceholder(final @NotNull JBPanel<?> panel) {
        panel.setLayout(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(25, 16, 0, 0));
        final @NotNull JBLabel placeholder = new JBLabel(PLACEHOLDER_TEXT);
        placeholder.setForeground(JBColor.GRAY);
        placeholder.setFont(Fonts.body());
        panel.add(placeholder, BorderLayout.NORTH);
    }

    private void renderRows(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull TestCaseDto dto, final @NotNull Optional<RunItem> runItem, final @NotNull List<String> currentPath, final int shownLine, final @NotNull IntConsumer showExecution) {
        final @NotNull GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(INSETS_DEFAULT);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = WEIGHT_X;

        final int row = setupFixedRows(p, panel, gbc, dto, runItem, currentPath, shownLine, showExecution);
        addVerticalSpacer(panel, row);
    }

    // UC-VIEW-PANEL-004, UC-VIEW-PANEL-005, Rule-VIEW-PANEL-085, Rule-VIEW-PANEL-087
    private @NotNull List<AbstractDetails> detailRows(final @NotNull Optional<RunItem> runItem, final @NotNull List<String> currentPath, final int shownLine, final @NotNull IntConsumer showExecution) {
        final @NotNull List<AbstractDetails> rows = new ArrayList<>(List.of(new Breadcrumb(currentPath), new Title(), new BadgesAndActions(currentPath)));

        runItem.ifPresentOrElse(shown -> {
            rows.add(testRunBand(shown, currentPath, shownLine, showExecution));
            rows.add(Band.folding(Bundle.message("details.band.test.case"), TEST_CASE_OPEN, testCaseFields()));
        }, () -> rows.addAll(testCaseFields()));

        return List.copyOf(rows);
    }

    private int setupFixedRows(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final @NotNull Optional<RunItem> runItem, final @NotNull List<String> currentPath, final int shownLine, final @NotNull IntConsumer showExecution) {
        int row = 0;
        for (final AbstractDetails component : detailRows(runItem, currentPath, shownLine, showExecution)) {
            row = component.render(p, panel, (GridBagConstraints) gbc.clone(), dto, row);
        }
        return row;
    }

    private void addVerticalSpacer(final @NotNull JBPanel<?> panel, final int lastRow) {
        final @NotNull GridBagConstraints spacerGbc = new GridBagConstraints();
        spacerGbc.gridy = lastRow;
        spacerGbc.weighty = SPACER_WEIGHT_Y;
        panel.add(Box.createVerticalGlue(), spacerGbc);
    }
}
