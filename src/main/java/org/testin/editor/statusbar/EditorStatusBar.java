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

package org.testin.editor.statusbar;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.EditorColors;
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.git.history.HistoryCard;
import org.testin.logger.TestinLogger;
import org.testin.model.Automated;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.testrun.Segment;
import org.testin.model.status.TestRunStatus;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.ui.Tooltip;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class EditorStatusBar extends JBPanel<EditorStatusBar> {
    private static final int EDGE = 2;

    private final @NotNull JBLabel statusBarLabel = new JBLabel();

    private final @NotNull JBLabel testRunStatusLabel = new JBLabel();

    private final @NotNull JBPanel<?> runItemStatusesRow = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));

    private final @NotNull JBLabel executionTimeLabel = new JBLabel();

    private final @NotNull JBLabel automatedLabel = new JBLabel();

    private final @NotNull Map<PageStep, PageButton> pageButtons = new EnumMap<>(PageStep.class);

    private final @NotNull JBLabel currentPageLabel = new JBLabel(Bundle.message("statusbar.page.of", "1", "1"));

    @Getter
    private final @NotNull JBTextField pageSizeField = new JBTextField("", 4);

    private final @NotNull JBPanel<?> navigationRow;
    private final @NotNull JBPanel<?> rightRow;

    public EditorStatusBar() {
        super(null);

        setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0),
                JBUI.Borders.empty(0, EDGE)
        ));
        setBackground(JBUI.CurrentTheme.EditorTabs.background());
        statusBarLabel.setForeground(UIUtil.getContextHelpForeground());
        testRunStatusLabel.setForeground(UIUtil.getContextHelpForeground());
        testRunStatusLabel.setBorder(JBUI.Borders.emptyRight(10));
        testRunStatusLabel.setIconTextGap(JBUI.scale(4));

        runItemStatusesRow.setOpaque(false);
        runItemStatusesRow.setBorder(JBUI.Borders.emptyRight(10));

        executionTimeLabel.setForeground(UIUtil.getInactiveTextColor());
        executionTimeLabel.setBorder(JBUI.Borders.emptyRight(10));
        Tooltip.set(executionTimeLabel, Bundle.message("statusbar.test.run.time.tip"));

        automatedLabel.setForeground(UIUtil.getInactiveTextColor());
        automatedLabel.setBorder(JBUI.Borders.emptyRight(10));
        Tooltip.set(automatedLabel, Bundle.message("statusbar.automated.tip"));

        testRunStatusLabel.setVisible(false);
        runItemStatusesRow.setVisible(false);
        executionTimeLabel.setVisible(false);
        automatedLabel.setVisible(false);

        pageSizeField.setHorizontalAlignment(SwingConstants.CENTER);
        final @NotNull String pageSize = Bundle.message("statusbar.page.size.tip");
        Tooltip.set(pageSizeField, pageSize);
        // Rule-INTERNAL-122
        pageSizeField.getAccessibleContext().setAccessibleName(pageSize);

        for (final PageStep step : PageStep.values()) pageButtons.put(step, new PageButton(step));

        navigationRow = centeredRow(button(PageStep.FIRST), button(PageStep.PREVIOUS), currentPageLabel,
                button(PageStep.NEXT), button(PageStep.LAST));
        rightRow = centeredRow(testRunStatusLabel, runItemStatusesRow, executionTimeLabel, automatedLabel, pageSizeField);

        add(statusBarLabel);
        add(navigationRow);
        add(rightRow);
    }

    // Rule-EDITOR-PANEL-251
    static int arrowsAt(final int inner, final int sentence, final int arrows, final int figures) {
        return Math.clamp((inner - arrows) / 2, sentence, Math.max(sentence, inner - figures - arrows));
    }

    private static @NotNull JBPanel<?> centeredRow(final @NotNull JComponent... components) {
        final @NotNull JBPanel<?> row = new JBPanel<>(new GridBagLayout());
        row.setOpaque(false);
        row.setBackground(JBUI.CurrentTheme.EditorTabs.background());

        final @NotNull GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.insets = JBUI.insetsLeft(4);

        for (final JComponent item : components) row.add(item, constraints);

        return row;
    }

    private static @NotNull JBLabel painted(final @NotNull String text, final @NotNull Color color) {
        final @NotNull JBLabel label = new JBLabel(text);

        label.setForeground(color);
        Tooltip.set(label, Bundle.message("statusbar.test.run.progress.tip"));

        return label;
    }

    // Rule-EDITOR-PANEL-251
    private static @NotNull String oneLine(final @NotNull String text) {
        return "<html><nobr>" + text + "</nobr></html>";
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-009
    private static @NotNull String narrowedFrom(final int shownCount, final int totalCount) {
        if (shownCount == totalCount) return "";

        return String.format(Locale.ENGLISH, " <font color='#%02x%02x%02x'>%s</font>",
                EditorColors.FILTER_ACTIVE.getRed(), EditorColors.FILTER_ACTIVE.getGreen(),
                EditorColors.FILTER_ACTIVE.getBlue(),
                Bundle.message("statusbar.filtered.from", String.valueOf(totalCount)));
    }

    // Rule-EDITOR-PANEL-265
    static @NotNull String besideTheIcon(final @NotNull TestRunMarker marker) {
        if (marker.getStatus() == TestRunStatus.ASSIGNED)
            return Services.getInstance(AppSettingsState.class).testerName;
        if (marker.getStatus().isRecord()) return HistoryCard.shortOf(marker.getCommit());
        return "";
    }

    @Override
    public void doLayout() {
        final @NotNull Insets insets = getInsets();
        final int top = insets.top;
        final int height = getHeight() - insets.top - insets.bottom;
        final int left = insets.left;
        final int right = getWidth() - insets.right;

        final int arrows = navigationRow.getPreferredSize().width;
        final int figures = rightRow.getPreferredSize().width;
        final int arrowsAt = arrowsAt(right - left, statusBarLabel.getPreferredSize().width, arrows, figures);

        rightRow.setBounds(right - figures, top, figures, height);
        navigationRow.setBounds(left + arrowsAt, top, arrows, height);
        statusBarLabel.setBounds(left, top, arrowsAt, height);
    }

    @Override
    public @NotNull Dimension getPreferredSize() {
        int width = 0;
        int height = 0;

        for (final Component region : getComponents()) {
            final @NotNull Dimension size = region.getPreferredSize();
            width += size.width;
            height = Math.max(height, size.height);
        }

        final @NotNull Insets insets = getInsets();

        return new Dimension(width + insets.left + insets.right, AbstractToolbarPanel.barHeight(height + insets.top + insets.bottom));
    }

    // Rule-EDITOR-PANEL-251, Rule-EDITOR-PANEL-257
    @Override
    public @NotNull Dimension getMinimumSize() {
        return getPreferredSize();
    }

    // UC-EDITOR-PANEL-042, Rule-EDITOR-PANEL-178
    public void showExecutionTime(final @NotNull String formatted) {
        executionTimeLabel.setText(formatted);
        executionTimeLabel.setVisible(!formatted.isEmpty());
    }

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-210, Rule-EDITOR-PANEL-211
    public void showAutomated(final int written, final int known) {
        automatedLabel.setText(Bundle.message("statusbar.automated.count", Automated.WRITTEN.getLabel(), String.valueOf(written), String.valueOf(known)));
        automatedLabel.setVisible(known > 0);

        TestinLogger.debug("Automated count: written=" + written + " known=" + known
                + " shown=" + automatedLabel.isVisible() + " text='" + automatedLabel.getText() + "'");

        revalidate();
        repaint();
    }

    // UC-EDITOR-PANEL-042, Rule-EDITOR-PANEL-175
    public void showRunItemStatuses(final @NotNull List<Segment> runItemStatuses) {
        runItemStatusesRow.removeAll();

        for (final Segment runItemStatus : runItemStatuses) {
            if (runItemStatusesRow.getComponentCount() > 0)
                runItemStatusesRow.add(painted(" · ", UIUtil.getInactiveTextColor()));

            runItemStatusesRow.add(painted(runItemStatus.text(), runItemStatus.color()));
        }

        runItemStatusesRow.setVisible(!runItemStatuses.isEmpty());

        revalidate();
        repaint();
    }

    // UC-EDITOR-PANEL-042, Rule-EDITOR-PANEL-179, Rule-EDITOR-PANEL-265
    public void showTestRunStatus(final @NotNull TestRunMarker marker) {
        final @NotNull TestRunStatus testRunStatus = marker.getStatus();
        testRunStatusLabel.setIcon(testRunStatus.getIcon());
        testRunStatusLabel.setText(besideTheIcon(marker));
        Tooltip.set(testRunStatusLabel, Bundle.message("statusbar.test.run.status.tip", testRunStatus.getLabel()));
        testRunStatusLabel.getAccessibleContext().setAccessibleName(testRunStatus.getLabel());
        testRunStatusLabel.setVisible(true);
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-102
    public void updatePaginationState(final int currentPage, final int totalPages) {
        currentPageLabel.setText(Bundle.message("statusbar.page.of", String.valueOf(currentPage), String.valueOf(Math.max(1, totalPages))));

        pageButtons.forEach((step, button) -> button.setEnabled(step.isAvailable(currentPage, totalPages)));
    }

    public @NotNull PageButton button(final @NotNull PageStep step) {
        return Objects.requireNonNull(pageButtons.get(step), step.name());
    }

    // UC-EDITOR-PANEL-024, Rule-EDITOR-PANEL-280
    public void updateSelectionState(final int @NotNull [] selectedIndices, final int firstSelectedPosition, final int shownCount, final int totalCount, final @NotNull String sortedBy) {
        final int selectedCount = selectedIndices.length;
        final @NotNull String testCases = shownCount == 1
                ? Bundle.message("statusbar.test.cases.one")
                : Bundle.message("statusbar.test.cases.many", String.valueOf(shownCount));
        final @NotNull String of = testCases + narrowedFrom(shownCount, totalCount) + (sortedBy.isEmpty() ? "" : " " + Bundle.message("statusbar.sorted", sortedBy));

        if (selectedCount > 1) {
            statusBarLabel.setText(oneLine(Bundle.message("statusbar.selected.of", String.valueOf(selectedCount), of)));

        } else if (selectedCount == 1) {
            statusBarLabel.setText(oneLine(Bundle.message("statusbar.position.of", String.valueOf(firstSelectedPosition + 1), of)));

        } else {
            statusBarLabel.setText(oneLine(of));
        }
    }
}
