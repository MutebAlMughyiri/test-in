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
import com.intellij.util.ui.UIUtil;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.card.HoverButton;
import org.testin.model.TestCase;
import org.testin.model.node.TestRunNode;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.services.Services;
import org.testin.testrun.ChangedSinceCommit;
import org.testin.testrun.RunItemRecorder;
import org.testin.testrun.TestRunEditorFields;
import org.testin.ui.Badge;
import org.testin.ui.Badges;
import org.testin.ui.Pill;
import org.testin.ui.Tooltip;
import org.testin.util.Bundle;
import org.testin.util.Display;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

@AllArgsConstructor
public final class RunItemSummary extends AbstractDetails {
    private static final int GAP = 6;
    private static final int INSETS_TOP = 8;
    private static final int INSETS_SIDE = 16;
    private static final int RETRY_INDENT = 24;
    private static final @NotNull String ACTIONS_SHOWN = "shown";
    private static final @NotNull String ACTIONS_HIDDEN = "hidden";

    private final @NotNull RunItem runItem;
    private final @NotNull List<String> treePath;
    private final @NotNull Consumer<OptionalInt> showExecution;

    // Rule-EDITOR-PANEL-263
    private static @NotNull JBLabel changedSince() {
        final @NotNull JBLabel label = new JBLabel(Bundle.message("test.run.changed.since.commit"));
        label.setForeground(UIUtil.getContextHelpForeground());
        return label;
    }

    // Rule-CODEGEN-112
    private static @NotNull String whatItWas(final @NotNull RunItemExecution execution) {
        final @NotNull String kind = !execution.getParameters().isBlank() ? execution.getParameters()
                : execution.getRetry() > 0 ? Bundle.message("execution.retry", String.valueOf(execution.getRetry()))
                : execution.getInvocation() > 0 ? Bundle.message("execution.invocation", String.valueOf(execution.getInvocation()))
                : "";
        return execution.isRetried() ? (kind + " " + Bundle.message("execution.retried")).strip() : kind;
    }

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-086
    private static @NotNull List<JComponent> pills(final @NotNull RunItemStatus runItemStatus, final @NotNull String duration) {
        final @NotNull List<Badge> badges = new ArrayList<>();
        badges.add(new Pill(runItemStatus.getLabel(), runItemStatus.getRowColor()));
        Badges.addDurationBadge(badges, duration);
        return Badges.pills(badges);
    }

    // Rule-VIEW-PANEL-120
    private static void showOnHover(final @NotNull JBPanel<?> line, final @NotNull JBPanel<?> actions, final @NotNull List<Runnable> hideEveryLine) {
        final @NotNull CardLayout slots = new CardLayout();
        final @NotNull JBPanel<?> slot = new JBPanel<>(slots).andTransparent();
        slot.add(actions, ACTIONS_SHOWN);
        slot.add(new JBPanel<>().andTransparent(), ACTIONS_HIDDEN);
        slots.show(slot, ACTIONS_HIDDEN);
        line.add(slot);
        hideEveryLine.add(() -> slots.show(slot, ACTIONS_HIDDEN));

        final @NotNull MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                hideEveryLine.forEach(Runnable::run);
                slots.show(slot, ACTIONS_SHOWN);
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                if (!line.contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), line)))
                    slots.show(slot, ACTIONS_HIDDEN);
            }
        };
        UIUtil.uiTraverser(line).forEach(part -> part.addMouseListener(hover));
    }

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-086, Rule-VIEW-PANEL-123
    @Override
    public int render(final @NotNull Project project, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints constraints, final @NotNull TestCase testCase, final int currentRow) {
        int row = currentRow;
        if (runItem.executedAtMostOnce()) {
            final @NotNull JBPanel<?> line = line(GAP);
            pills(runItem.shownStatus(), TestRunEditorFields.DURATION.gridValue(runItem)).forEach(line::add);
            BugLine.of(project, runItem, treePath, testCase).ifPresent(line::add);
            row = addFullWidthRow(panel, constraints, line, JBUI.insets(INSETS_TOP, INSETS_SIDE, 0, INSETS_SIDE), row);
        } else {
            final int defaultAt = runItem.defaultIndex();
            final @NotNull Optional<TestRunNode> testRun = testRunOf(project, treePath);
            final @NotNull Optional<String> whyNot = testRun.flatMap(shownIn -> Services.getInstance(project, RunItemRecorder.class).whyExecutionsAreFixed(shownIn.getPath(), runItem.getId()));
            final @NotNull List<Runnable> hideEveryLine = new ArrayList<>();

            for (int at = 0; at < runItem.getExecutions().size(); at++) {
                final int index = at;
                final boolean isDefault = index == defaultAt;
                final @NotNull JBPanel<?> line = executionLine(project, testCase, index, isDefault);
                final @NotNull JBPanel<?> actions = testRun.<JBPanel<?>>map(shownIn -> actions(project, testCase, shownIn, index, isDefault, whyNot)).orElseGet(() -> row(0));
                showOnHover(line, actions, hideEveryLine);
                showOnClick(line, actions, index);

                final int indent = runItem.getExecutions().get(index).getRetry() > 0 ? RETRY_INDENT : 0;
                row = addFullWidthRow(panel, constraints, line, JBUI.insets(INSETS_TOP, INSETS_SIDE + indent, 0, INSETS_SIDE), row);
            }
        }

        return ChangedSinceCommit.of(runItem) ? addFullWidthRow(panel, constraints, changedSince(), JBUI.insets(GAP, INSETS_SIDE, 0, INSETS_SIDE), row) : row;
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-086, Rule-VIEW-PANEL-119
    private @NotNull JBPanel<?> executionLine(final @NotNull Project project, final @NotNull TestCase testCase, final int at, final boolean isDefault) {
        final @NotNull RunItemExecution execution = runItem.getExecutions().get(at);

        final @NotNull JBPanel<?> line = line(GAP);
        line.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        pills(execution.getStatus(), Display.formatDuration(execution.getDuration())).forEach(line::add);

        final @NotNull JBLabel what = new JBLabel(whatItWas(execution));
        if (execution.isRetried()) what.setForeground(UIUtil.getContextHelpForeground());
        line.add(what);

        if (isDefault) {
            final @NotNull JBLabel star = new JBLabel(AllIcons.Nodes.Favorite);
            Tooltip.set(star, Bundle.message("execution.default"), "");
            line.add(star);
            BugLine.of(project, runItem, treePath, testCase).ifPresent(line::add);
        }

        return line;
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-120, Rule-VIEW-PANEL-121
    private @NotNull JBPanel<?> actions(final @NotNull Project project, final @NotNull TestCase testCase, final @NotNull TestRunNode testRun, final int at, final boolean isDefault, final @NotNull Optional<String> whyNot) {
        final @NotNull RunItemRecorder runItemRecorder = Services.getInstance(project, RunItemRecorder.class);
        final @NotNull JBPanel<?> actions = row(0);

        if (!isDefault) {
            actions.add(HoverButton.of(project, whyNot, AllIcons.Nodes.NotFavoriteOnHover, Bundle.message("execution.set.default"), "", () -> {
                if (runItemRecorder.chooseExecution(testRun.getPath(), runItem.getId(), at))
                    redraw(project, testCase, testRun);
            }));
        }
        actions.add(HoverButton.of(project, whyNot, AllIcons.Actions.Close, Bundle.message("execution.remove"), "", () -> {
            if (!runItemRecorder.removeExecution(testRun.getPath(), runItem.getId(), at)) return;

            showExecution.accept(OptionalInt.empty());
            redraw(project, testCase, testRun);
        }));

        return actions;
    }

    // Rule-VIEW-PANEL-122
    private void showOnClick(final @NotNull JBPanel<?> line, final @NotNull JBPanel<?> actions, final int at) {
        final @NotNull MouseAdapter click = new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                showExecution.accept(OptionalInt.of(at));
            }
        };
        UIUtil.uiTraverser(line).filter(part -> !SwingUtilities.isDescendingFrom(part, actions)).forEach(part -> part.addMouseListener(click));
    }
}
