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

package org.testin.testcase.create;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.services.TestCaseValues;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.util.Shortcuts;

import java.util.List;
import java.util.Set;

// UC-EDITOR-PANEL-005
public class GroupSection extends AbstractMultiValueSection {
    public GroupSection(final @NotNull Project p) {
        super(p);
    }

    @Override
    protected @NotNull CreateTestCaseFields field() {
        return CreateTestCaseFields.GROUP;
    }

    @Override
    protected @NotNull Set<String> completions(final @NotNull TestCaseValues cache) {
        return cache.getGroups();
    }

    @Override
    protected @NotNull List<String> valuesOf(final @NotNull TestCaseDto dto) {
        return dto.getGroups();
    }

    @Override
    protected @NotNull TestCaseDto write(final @NotNull TestCaseDto dto, final @NotNull List<String> values) {
        return dto.edit().groups(values).build();
    }

    @Override
    protected @NotNull Shortcuts addKey() {
        return Shortcuts.CreateTestCaseGroup;
    }
}
