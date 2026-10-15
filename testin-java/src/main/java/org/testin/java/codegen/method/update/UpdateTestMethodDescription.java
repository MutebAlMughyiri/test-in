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

package org.testin.java.codegen.method.update;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChange;
import org.testin.codegen.CodeChangeType;
import org.testin.model.TestCase;

import java.util.List;

// Rule-CODEGEN-020
public class UpdateTestMethodDescription extends BaseTestMethodChange implements CodeChange<TestCase> {
    // UC-CODEGEN-010, Rule-CODEGEN-001, Rule-CODEGEN-040
    @Override
    public void execute(final @NotNull Project project, final @NotNull TestCase testCase) {
        applyOrCreate(project, testCase, CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION.description(), testMethod -> {
            writeDescription(project, testMethod, testCase);
            reformat(project, testMethod);
        });
    }

    // UC-CODEGEN-010, UC-CODEGEN-003, Rule-CODEGEN-019
    @Override
    public void executeAll(final @NotNull Project project, final @NotNull List<? extends TestCase> testCases) {
        applyToEach(project, testCases, CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION.description(), (testMethod, testCase) -> writeDescription(project, testMethod, testCase));
        CodeChangeType.CREATE_TEST_CASE.executeAllNow(project, testCases);
    }
}
