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
import org.testin.codegen.GenAction;
import org.testin.codegen.GenType;
import org.testin.model.TestCaseDto;

import java.util.List;

public class UpdateTestMethodGroups extends UpdateTestBase implements GenAction<TestCaseDto> {
    // UC-CODEGEN-012, Rule-CODEGEN-045
    @Override
    public void execute(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        applyUpdate(p, tc, GenType.UPDATE_TEST_CASE_GROUP.description(), pm -> {
            writeGroups(p, pm, tc);
            reformat(p, pm);
        });
    }

    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<? extends TestCaseDto> items) {
        applyToEach(p, items, GenType.UPDATE_TEST_CASE_GROUP.description(), (pm, tc) -> writeGroups(p, pm, tc));
    }
}
