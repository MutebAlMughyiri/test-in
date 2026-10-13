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
import com.intellij.ui.TextFieldWithAutoCompletion;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.services.TestCaseCompletions;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.util.EditorFields;

public class ExpectedResultSection extends AbstractMultiLineSection {
    public ExpectedResultSection(final @NotNull Project project) {
        super(project, EditorFields.createCompletionField(project, new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(project, TestCaseCompletions.class).getExpectedResults(), CreateTestCaseFields.EXPECTED_RESULT.getIcon()), ""), CreateTestCaseFields.EXPECTED_RESULT);
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-032
    @Override
    public @NotNull TestCase applyTo(final @NotNull TestCase testCase) {
        return testCase.edit().expectedResult(field.getText().trim()).build();
    }

    @Override
    public void fillData(final @NotNull TestCase testCase) {
        field.setText(testCase.getExpectedResult());
    }

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-036
    @Override
    public boolean isShownWith(final @NotNull TestCase testCase) {
        return !testCase.getExpectedResult().isEmpty();
    }
}
