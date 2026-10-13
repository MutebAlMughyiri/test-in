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
import org.testin.editor.card.AbstractCard;
import org.testin.model.Priority;
import org.testin.model.TestCase;
import org.testin.model.status.ExecutionStatus;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.testcase.TestSetEditorFields;
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

public class TestCaseCard extends AbstractCard {
    private final @NotNull List<Badge> badges = new ArrayList<>();
    private final @NotNull Map<String, String> details = new LinkedHashMap<>();
    private final @NotNull CutState cutState = Services.getInstance(project, CutState.class);
    private final @NotNull TestNGExecution testNGExecution = Services.getInstance(project, TestNGExecution.class);
    private boolean isPendingCut = false;

    public TestCaseCard(final @NotNull Project project) {
        super(project);
    }

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-021, Rule-EDITOR-PANEL-267, Rule-EDITOR-PANEL-294
    public void updateData(final int index, final @NotNull TestCase testCase, final @NotNull Set<?> activeDetails, final @NotNull String title) {
        badges.clear();
        details.clear();

        this.isPendingCut = cutState.isPending(testCase.getId());

        this.automation = automationState.of(testCase.getId());
        this.priority = activeDetails.contains(TestSetEditorFields.PRIORITY) ? testCase.getPriority() : Priority.DEFAULT;

        Arrays.stream(TestSetEditorFields.values())
                .filter(activeDetails::contains)
                .forEach(field -> field.applyToUI(testCase, badges, details));

        if (testCase.getStatus().isBadged()) badges.add(Badges.createTestCaseStatusBadge(testCase.getStatus()));

        final @NotNull ExecutionStatus executionStatus = testNGExecution.statusOf(testCase);

        if (executionStatus.hasBadge()) badges.add(Badges.createExecutionStatusBadge(executionStatus.getBadge()));

        titleArea.setForeground(testCase.getStatus().getDescriptionColor());
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