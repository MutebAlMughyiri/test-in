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
import com.intellij.util.ui.JBUI;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.Nodes;
import org.testin.model.TestCaseDto;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;

import java.awt.GridBagConstraints;
import java.util.List;

@AllArgsConstructor
public final class BadgesAndActions extends AbstractDetails {
    private static final int GAP = 8;
    private static final int INSETS_TOP = 8;
    private static final int INSETS_SIDE = 16;

    private final @NotNull List<String> treePath;

    // UC-VIEW-PANEL-004, UC-VIEW-PANEL-012, Rule-VIEW-PANEL-085
    @Override
    public int render(final @NotNull Project project, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints constraints, final @NotNull TestCaseDto testCase, final int currentRow) {
        final @NotNull JBPanel<?> line = line(GAP);

        TestCaseBadges.of(project, testCase).forEach(line::add);
        TestCaseHoverButtons.of(project, testCase, Services.getInstance(project, Nodes.class).find(Services.getInstance(project, TestinRoot.class).resolve(treePath))).forEach(line::add);

        return addFullWidthRow(panel, constraints, line, JBUI.insets(INSETS_TOP, INSETS_SIDE, 0, INSETS_SIDE), currentRow);
    }
}
