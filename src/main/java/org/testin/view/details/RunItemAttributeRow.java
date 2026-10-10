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
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItem;
import org.testin.testrun.TestRunEditorAttributes;

import java.awt.GridBagConstraints;

@RequiredArgsConstructor
public final class RunItemAttributeRow extends AbstractDetails {
    private final @NotNull TestRunEditorAttributes attribute;
    private final @NotNull RunItem runItem;

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-031
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        return addRow(panel, gbc, attribute.getName(), attribute.getRunItemValueExtractor().apply(runItem), currentRow);
    }
}
