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
import org.testin.model.TestCaseDto;
import org.testin.services.Services;
import org.testin.services.TestCaseCompletions;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.util.Shortcuts;
import org.testin.util.EditorFields;

public class ModuleSection extends AbstractOneLineSection {
    public ModuleSection(final @NotNull Project p) {
        super(EditorFields.createCompletionField(p, new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(p, TestCaseCompletions.class).getModules(), CreateTestCaseFields.MODULE.getIcon()), ""),
                CreateTestCaseFields.MODULE, Shortcuts.CreateTestCaseModule);
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-032
    @Override
    public @NotNull TestCaseDto applyTo(final @NotNull TestCaseDto testCase) {
        return testCase.edit().module(field.getText().trim()).build();
    }

    @Override
    public void fillData(final @NotNull TestCaseDto testCase) {
        field.setText(testCase.getModule());
    }
}
