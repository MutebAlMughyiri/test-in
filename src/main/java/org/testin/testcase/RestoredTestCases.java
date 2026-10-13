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

package org.testin.testcase;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChangeType;
import org.testin.model.TestCaseDto;

import java.util.ArrayList;
import java.util.List;

record RestoredTestCases(@NotNull List<TestCaseDto> removed, @NotNull List<TestCaseDto> comingBack, @NotNull List<TestCaseDto> landed) {
    RestoredTestCases() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    void generate(final @NotNull Project project) {
        if (!removed.isEmpty()) CodeChangeType.REMOVE_TEST_CASE.executeAll(project, removed);
        if (!comingBack.isEmpty()) CodeChangeType.CREATE_TEST_CASE.executeAll(project, comingBack);

        // UC-CODEGEN-002, Rule-CODEGEN-068
        if (!landed.isEmpty()) CodeChangeType.RECONCILE_TEST_CASE.executeAll(project, landed);
    }
}
