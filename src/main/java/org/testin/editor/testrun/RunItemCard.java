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

package org.testin.editor.testrun;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.card.AbstractCard;
import org.testin.model.Priority;
import org.testin.model.testrun.RunItem;
import org.testin.testrun.ChangedSinceCommit;
import org.testin.testrun.TestRunEditorFields;
import org.testin.ui.Badge;
import org.testin.util.Bundle;
import org.testin.util.Fonts;

import javax.swing.SwingConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RunItemCard extends AbstractCard {
    private final @NotNull List<Badge> badges = new ArrayList<>();

    private final @NotNull Map<String, String> details = new LinkedHashMap<>();

    private final @NotNull JBLabel changedSince = createDetailLabel();

    public RunItemCard(final @NotNull Project project) {
        super(project);
        changedSince.setText(Bundle.message("test.run.changed.since.commit"));
        content.add(changedSince, 2);
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-267, Rule-EDITOR-PANEL-294
    public void updateData(final @NotNull Integer index, final @NotNull Set<TestRunEditorFields> activeDetails, final @NotNull RunItem runItem, final @NotNull String title) {
        this.automation = automationState.of(runItem.getUuid());
        this.priority = activeDetails.contains(TestRunEditorFields.PRIORITY) ? runItem.shownTestCase().getPriority() : Priority.DEFAULT;

        badges.clear();
        details.clear();

        Arrays.stream(TestRunEditorFields.values())
                .filter(activeDetails::contains)
                .forEach(field -> field.applyToUI(runItem, badges, details));

        titleArea.setForeground(runItem.shownTestCase().getStatus().getDescriptionColor());
        updateUI(index, title, badges, details);

        // Rule-EDITOR-PANEL-263
        changedSince.setVisible(ChangedSinceCommit.of(runItem));

        Optional.ofNullable(fieldLabels.get(TestRunEditorFields.RUN_STATUS.getName())).ifPresent(runItemStatusLabel -> {
            runItemStatusLabel.setFont(Fonts.strong());
            runItemStatusLabel.setForeground(runItem.getStatus().getRowColor());
            // Rule-EDITOR-PANEL-287
            runItemStatusLabel.setIcon(ExecutionDots.of(runItem, runItemStatusLabel.getFont()));
            runItemStatusLabel.setHorizontalTextPosition(SwingConstants.LEADING);
        });
    }
}