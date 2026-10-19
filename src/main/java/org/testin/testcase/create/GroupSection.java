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
import org.testin.model.Groups;
import org.testin.model.TestCase;
import org.testin.services.TestCaseCompletions;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.TestCaseDialogKey;

import java.util.List;
import java.util.Set;

// UC-EDITOR-PANEL-005
public class GroupSection extends AbstractMultiValueSection {
    public GroupSection(final @NotNull Project project) {
        super(project);
    }

    @Override
    protected @NotNull CreateTestCaseFields field() {
        return CreateTestCaseFields.GROUP;
    }

    @Override
    protected @NotNull Set<String> completions(final @NotNull TestCaseCompletions cache) {
        return cache.getGroups();
    }

    @Override
    protected @NotNull List<String> valuesOf(final @NotNull TestCase testCase) {
        return testCase.getGroups();
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-033
    @Override
    protected @NotNull TestCase write(final @NotNull TestCase testCase, final @NotNull List<String> values) {
        return testCase.edit().groups(Groups.read(String.join(",", values))).build();
    }

    // UC-EDITOR-PANEL-005, UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-199
    @Override
    public @NotNull List<TestCaseDialogKey> keys() {
        return List.of(TestCaseDialogKey.ADD, TestCaseDialogKey.AUTO_COMPLETE, TestCaseDialogKey.NAVIGATE_TAB);
    }
}
