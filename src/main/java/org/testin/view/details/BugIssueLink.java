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
import org.testin.bug.BugIssueState;
import org.testin.bug.KnownBugIssueStates;
import org.testin.model.bug.BugIssueUrl;
import org.testin.services.Services;
import org.testin.ui.Badges;
import org.testin.ui.Tooltip;

import javax.swing.JComponent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BugIssueLink {
    private static final int GAP = 6;

    // UC-VIEW-PANEL-005, UC-VIEW-PANEL-007, Rule-VIEW-PANEL-075, Rule-VIEW-PANEL-080, Rule-VIEW-PANEL-091
    public static @NotNull JComponent of(final @NotNull Project project, final @NotNull String url) {
        final @NotNull JBPanel<?> line = AbstractDetails.row(GAP);

        final @NotNull ActionLink link = AbstractDetails.link(BugIssueUrl.shortReference(url), _ -> BugIssueUrl.open(url));
        Tooltip.set(link, url);
        line.add(link);

        final @NotNull BugIssueState state = Services.getInstance(project, KnownBugIssueStates.class).of(url);
        final @NotNull JBPanel<?> pill = AbstractDetails.row(0);
        Badges.showBadges(pill, state.pills());
        Tooltip.set(pill, state.tooltip());
        line.add(pill);

        return line;
    }
}
