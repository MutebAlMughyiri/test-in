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
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.model.status.ExecutionStatus;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.ui.Badge;
import org.testin.ui.Badges;

import javax.swing.JComponent;
import java.util.List;

public final class TestCaseBadges {
    private static final int FLOW_GAP = 6;

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-085
    public static @NotNull JComponent of(final @NotNull Project p, final @NotNull TestCaseDto dto) {
        final @NotNull JBPanel<?> badgesPanel = AbstractDetails.row(FLOW_GAP);

        final @NotNull List<Badge> badges = Badges.testCaseBadges(dto);

        final @NotNull ExecutionStatus tempStatus = Services.getInstance(p, TestNGExecution.class).statusOf(dto);
        if (tempStatus.hasBadge()) badges.add(Badges.createExecutionStatusBadge(tempStatus.getBadge()));

        Badges.showBadges(badgesPanel, badges);

        return badgesPanel;
    }
}