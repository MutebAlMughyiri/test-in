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
import org.testin.codegen.GenType;
import org.testin.model.TestCaseDto;

import java.util.List;

public class UpdateTestMethodDescription extends BaseTestMethodChange implements CodeChange<TestCaseDto> {
    // UC-CODEGEN-010, Rule-CODEGEN-001, Rule-CODEGEN-040
    @Override
    public void execute(final @NotNull Project p, final @NotNull TestCaseDto testCase) {
        applyOrCreate(p, testCase, GenType.UPDATE_TEST_CASE_DESCRIPTION.description(), pm -> {
            writeDescription(p, pm, testCase);
            reformat(p, pm);
        });
    }

    // UC-CODEGEN-010, UC-CODEGEN-003, Rule-CODEGEN-019
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<? extends TestCaseDto> testCases) {
        applyToEach(p, testCases, GenType.UPDATE_TEST_CASE_DESCRIPTION.description(), (pm, testCase) -> writeDescription(p, pm, testCase));
        GenType.CREATE_TEST_CASE.executeAllNow(p, testCases);
    }
}
