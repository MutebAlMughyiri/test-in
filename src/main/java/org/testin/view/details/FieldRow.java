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

import java.awt.GridBagConstraints;
import java.util.function.BiFunction;

// Rule-VIEW-PANEL-061
@RequiredArgsConstructor
public final class FieldRow extends AbstractDetails {
    private final @NotNull String caption;
    private final @NotNull BiFunction<Project, TestCaseDto, String> extractor;

    // UC-VIEW-PANEL-004
    @Override
    public int render(final @NotNull Project project, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto testCase, final int currentRow) {
        return addRow(panel, gbc, caption, extractor.apply(project, testCase), currentRow);
    }
}
