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
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.ExecutionPosition;
import org.testin.editor.WheelForwarding;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.testcase.TestCaseOrder;
import org.testin.testcase.TestSetEditorFields;
import org.testin.testrun.TestRunEditorFields;
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
import java.util.OptionalInt;
import java.util.function.Consumer;

public class DetailsTab {
    static final @NotNull String TEST_CASE_OPEN = "testin.viewPanel.testCaseOpen";
    final @NotNull String PLACEHOLDER_TEXT = Bundle.message("details.placeholder");
    final int INSETS_DEFAULT = 5;
    final double WEIGHT_X = 1.0;
    final double SPACER_WEIGHT_Y = 1.0;

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-061, Rule-VIEW-PANEL-085, Rule-VIEW-PANEL-086, Rule-VIEW-PANEL-103
    private static @NotNull Band testRunBand(final @NotNull RunItem runItem, final @NotNull List<String> treePath, final @NotNull OptionalInt shownLine, final @NotNull Consumer<OptionalInt> showExecution) {
        // Rule-VIEW-PANEL-122
        final @NotNull RunItemExecution shown = runItem.shownExecution(shownLine);

        return Band.of(Bundle.message("details.band.run"), List.of(
                new RunItemSummary(runItem, treePath, showExecution),
                new FieldRow(TestRunEditorFields.ACTUAL_RESULT.getName(), (_, _) -> shown.getActualResult()),
                new StacktraceLine(shown, treePath),
                new FieldRow(TestRunEditorFields.EXECUTED_BY.getName(), (_, _) -> Display.whoAndWhen(shown.getExecutedBy(), shown.getExecutedAt()))));
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-087, Rule-VIEW-PANEL-103
    private static @NotNull List<AbstractDetails> testCaseFields() {
        return List.of(
                new FieldRow(TestSetEditorFields.EXPECTED_RESULT.getName(), (_, testCase) -> TestSetEditorFields.EXPECTED_RESULT.displayValue(testCase)),
                new Steps(),
                new FieldRow(TestSetEditorFields.PRE_CONDITIONS.getName(), (_, testCase) -> TestSetEditorFields.PRE_CONDITIONS.displayValue(testCase)),
                new FieldRow(TestSetEditorFields.TEST_DATA.getName(), (_, testCase) -> TestSetEditorFields.TEST_DATA.displayValue(testCase)),
                new FieldRow(TestSetEditorFields.REFERENCE.getName(), (_, testCase) -> TestSetEditorFields.REFERENCE.displayValue(testCase)),
                new FieldRow(TestSetEditorFields.MODULE.getName(), (_, testCase) -> TestSetEditorFields.MODULE.displayValue(testCase)),
                new FieldRow(TestSetEditorFields.ORDER.getName(), (project, testCase) -> TestCaseOrder.placeText(ExecutionPosition.placesOf(project, List.of(testCase)).getOrDefault(testCase.getId(), 0))),
                new FieldRow(Bundle.message("details.created"), (_, testCase) -> Display.whoAndWhen(testCase.getCreatedBy(), testCase.getCreatedAt())),
                new FieldRow(Bundle.message("details.updated"), (_, testCase) -> Display.whoAndWhen(testCase.getUpdatedBy(), testCase.getUpdatedAt())));
    }

    // UC-VIEW-PANEL-004
    public void load(final @NotNull Project project, final @NotNull JBPanel<?> detailsTab, final @NotNull Optional<TestCaseDto> shownTestCase, final @NotNull Optional<RunItem> runItem, final @NotNull List<String> treePath, final @NotNull OptionalInt shownLine, final @NotNull Consumer<OptionalInt> showExecution) {
        detailsTab.removeAll();
        detailsTab.setLayout(new BorderLayout());
        detailsTab.setBorder(BorderFactory.createEmptyBorder());

        shownTestCase.ifPresentOrElse(
                testCase -> renderTestCase(project, detailsTab, testCase, detailRows(runItem, treePath, shownLine, showExecution)),
                () -> renderPlaceholder(detailsTab));

        detailsTab.revalidate();
        detailsTab.repaint();
    }

    private void renderTestCase(final @NotNull Project project, final @NotNull JBPanel<?> detailsTab, final @NotNull TestCaseDto testCase, final @NotNull List<AbstractDetails> rows) {
        final @NotNull JBPanel<?> contentPanel = new JBPanel<>(new GridBagLayout());
        contentPanel.setOpaque(false);

        renderRows(project, contentPanel, testCase, rows);

        FontSync.attachWheelZoom(project, contentPanel);

        contentPanel.addMouseWheelListener(WheelForwarding::forwardWheelToScrollPane);

        detailsTab.add(contentPanel, BorderLayout.CENTER);

        EditShownTestCase.bindTo(project, detailsTab);
    }

    private void renderPlaceholder(final @NotNull JBPanel<?> panel) {
        panel.setLayout(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(25, 16, 0, 0));
        final @NotNull JBLabel placeholder = new JBLabel(PLACEHOLDER_TEXT);
        placeholder.setForeground(JBColor.GRAY);
        placeholder.setFont(Fonts.body());
        panel.add(placeholder, BorderLayout.NORTH);
    }

    private void renderRows(final @NotNull Project project, final @NotNull JBPanel<?> panel, final @NotNull TestCaseDto testCase, final @NotNull List<AbstractDetails> rows) {
        final @NotNull GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(INSETS_DEFAULT);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = WEIGHT_X;

        final int row = setupFixedRows(project, panel, gbc, testCase, rows);
        addVerticalSpacer(panel, row);
    }

    // UC-VIEW-PANEL-004, UC-VIEW-PANEL-005, Rule-VIEW-PANEL-085, Rule-VIEW-PANEL-087
    private @NotNull List<AbstractDetails> detailRows(final @NotNull Optional<RunItem> runItem, final @NotNull List<String> treePath, final @NotNull OptionalInt shownLine, final @NotNull Consumer<OptionalInt> showExecution) {
        final @NotNull List<AbstractDetails> rows = new ArrayList<>(List.of(new Breadcrumb(treePath), new Title(), new BadgesAndActions(treePath)));

        runItem.ifPresentOrElse(shown -> {
            rows.add(testRunBand(shown, treePath, shownLine, showExecution));
            rows.add(Band.folding(Bundle.message("details.band.test.case"), TEST_CASE_OPEN, testCaseFields()));
        }, () -> rows.addAll(testCaseFields()));

        return List.copyOf(rows);
    }

    private int setupFixedRows(final @NotNull Project project, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto testCase, final @NotNull List<AbstractDetails> rows) {
        int row = 0;
        for (final AbstractDetails component : rows) {
            row = component.render(project, panel, (GridBagConstraints) gbc.clone(), testCase, row);
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
