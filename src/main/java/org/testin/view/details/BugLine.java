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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.bug.OpenBugReports;
import org.testin.bug.ReportBug;
import org.testin.bug.RunItemPath;
import org.testin.model.TestCase;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItem;
import org.testin.services.Services;
import org.testin.testrun.TestRunEditorFields;
import org.testin.ui.Badge;
import org.testin.ui.Badges;
import org.testin.ui.Tooltip;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BugLine {
    private static final int LINK_GAP = 10;

    // UC-VIEW-PANEL-005, UC-VIEW-PANEL-016, Rule-VIEW-PANEL-031, Rule-VIEW-PANEL-086
    public static @NotNull Optional<JComponent> of(final @NotNull Project project, final @NotNull RunItem runItem, final @NotNull List<String> treePath, final @NotNull TestCase testCase) {
        final @NotNull Optional<String> bugIssue = runItem.shownExecution().bugIssue();
        if (!runItem.recordsABug()) return Optional.empty();

        return AbstractDetails.testRunOf(project, treePath).map(testRunNode -> drawn(project, runItem, testCase, bugIssue, testRunNode));
    }

    // UC-VIEW-PANEL-005, UC-VIEW-PANEL-016, Rule-VIEW-PANEL-066, Rule-VIEW-PANEL-075, Rule-VIEW-PANEL-086
    private static @NotNull JComponent drawn(final @NotNull Project project, final @NotNull RunItem runItem, final @NotNull TestCase testCase, final @NotNull Optional<String> bugIssue, final @NotNull TestRunNode testRunNode) {
        final @NotNull JBPanel<?> line = AbstractDetails.row(LINK_GAP);

        chip(runItem).ifPresent(line::add);
        bugIssue.ifPresentOrElse(
                url -> line.add(BugIssueLink.of(project, url)),
                () -> line.add(report(project, runItem, testCase, testRunNode)));

        return line;
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-066, Rule-VIEW-PANEL-075
    private static @NotNull ActionLink report(final @NotNull Project project, final @NotNull RunItem runItem, final @NotNull TestCase testCase, final @NotNull TestRunNode testRunNode) {
        final @NotNull Optional<String> off = Services.getInstance(project, OpenBugReports.class)
                .whyReportBugIsOff(new RunItemPath(testRunNode.getPath(), runItem.getUuid()), runItem);

        final @NotNull ActionLink report = AbstractDetails.link(Bundle.message("bug.dialog.title"),
                _ -> ReportBug.start(project, testRunNode, runItem.getUuid(), testCase, () -> AbstractDetails.redraw(project, testCase, testRunNode)));

        report.setEnabled(off.isEmpty());
        Tooltip.set(report, off.orElse(""));

        return report;
    }

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-086
    private static @NotNull Optional<JComponent> chip(final @NotNull RunItem runItem) {
        if (!runItem.isFailed()) return Optional.empty();

        final @NotNull List<Badge> bug = new ArrayList<>();
        TestRunEditorFields.BUG_SEVERITY.applyToUI(runItem, bug, new HashMap<>());
        TestRunEditorFields.BUG_PRIORITY.applyToUI(runItem, bug, new HashMap<>());

        final @NotNull JBPanel<?> holder = AbstractDetails.row(0);
        Badges.showBadges(holder, bug);

        return Optional.of(holder);
    }
}
