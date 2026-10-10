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
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItem;
import org.testin.testrun.ChangedSinceCommit;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.ui.Badge;
import org.testin.ui.Badges;
import org.testin.ui.Pill;
import org.testin.util.Bundle;

import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public final class RunItemSummary extends AbstractDetails {
    private static final int GAP = 6;
    private static final int INSETS_TOP = 8;
    private static final int INSETS_SIDE = 16;

    private final @NotNull RunItem runItem;
    private final @NotNull List<String> currentPath;

    private static @NotNull JBPanel<?> line() {
        final @NotNull JBPanel<?> line = row(GAP);

        return line;
    }

    // Rule-EDITOR-PANEL-263
    private static @NotNull JBLabel changedSince() {
        final @NotNull JBLabel label = new JBLabel(Bundle.message("test.run.changed.since.commit"));
        label.setForeground(UIUtil.getContextHelpForeground());
        return label;
    }

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-086
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        final @NotNull JBPanel<?> line = line();
        final @NotNull JBPanel<?> pills = line();

        Badges.showBadges(pills, facts());
        line.add(pills);

        Bug.of(p, runItem, currentPath, dto).ifPresent(line::add);

        final int next = addFullWidthRow(panel, gbc, line, JBUI.insets(INSETS_TOP, INSETS_SIDE, 0, INSETS_SIDE), currentRow);
        return ChangedSinceCommit.of(runItem) ? addFullWidthRow(panel, gbc, changedSince(), JBUI.insets(GAP, INSETS_SIDE, 0, INSETS_SIDE), next) : next;
    }

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-086
    private @NotNull List<Badge> facts() {
        final @NotNull List<Badge> badges = new ArrayList<>();
        badges.add(new Pill(runItem.shownStatus().getLabel(), runItem.shownStatus().getRowColor()));

        Badges.addDurationBadge(badges, TestRunEditorAttributes.DURATION.gridValue(runItem));

        return badges;
    }
}
