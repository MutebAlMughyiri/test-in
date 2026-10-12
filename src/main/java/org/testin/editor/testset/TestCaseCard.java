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

package org.testin.editor.testset;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.clipboard.CutState;
import org.testin.editor.card.BaseCard;
import org.testin.model.Priority;
import org.testin.model.TestCaseDto;
import org.testin.model.status.ExecutionStatus;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.ui.Badge;
import org.testin.ui.Badges;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestCaseCard extends BaseCard {
    private final @NotNull List<Badge> badges = new ArrayList<>();
    private final @NotNull Map<String, String> details = new LinkedHashMap<>();
    private final @NotNull CutState cutState = Services.getInstance(p, CutState.class);
    private final @NotNull TestNGExecution testNGExecution = Services.getInstance(p, TestNGExecution.class);
    private boolean isPendingCut = false;

    public TestCaseCard(final @NotNull Project p) {
        super(p);
    }

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-021, Rule-EDITOR-PANEL-267, Rule-EDITOR-PANEL-294
    public void updateData(final int index, final @NotNull TestCaseDto tc, final @NotNull Set<?> activeDetails, final @NotNull String title) {
        badges.clear();
        details.clear();

        this.isPendingCut = cutState.isPending(tc.getId());

        this.automation = automationState.of(tc.getId());
        this.priority = activeDetails.contains(TestSetEditorAttributes.PRIORITY) ? tc.getPriority() : Priority.DEFAULT;

        Arrays.stream(TestSetEditorAttributes.values())
                .filter(activeDetails::contains)
                .forEach(attr -> attr.applyToUI(tc, badges, details));

        if (tc.getStatus().isBadged()) badges.add(Badges.createTestCaseStatusBadge(tc.getStatus()));

        final @NotNull ExecutionStatus executionStatus = testNGExecution.statusOf(tc);

        if (executionStatus.hasBadge()) badges.add(Badges.createExecutionStatusBadge(executionStatus.getBadge()));

        titleArea.setForeground(tc.getStatus().getDescriptionColor());
        updateUI(index, title, badges, details);
    }

    // UC-EDITOR-PANEL-016, Rule-EDITOR-PANEL-078
    @Override
    public void paint(final Graphics g) {
        if (isPendingCut) {
            final @NotNull Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            super.paint(g2);
            g2.dispose();
        } else {
            super.paint(g);
        }
    }
}