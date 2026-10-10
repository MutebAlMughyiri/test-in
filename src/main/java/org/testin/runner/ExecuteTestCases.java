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

package org.testin.runner;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeOn;
import org.testin.help.Guide;
import org.testin.help.Guides;
import org.testin.model.TestCaseDto;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecuteTestCases {
    // UC-CODEGEN-008, Rule-CODEGEN-031, Rule-CODEGEN-033, Rule-CODEGEN-035
    public static void run(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        if (testCases.isEmpty()) return;
        Services.getInstance(p, Guides.class).add(Guide.AUTOMATION_CODE);

        // Rule-CODEGEN-082
        if (CodeOn.isOffAndWarned(p)) return;
        if (OptionalPlugin.TESTNG.isMissingAndWarned(p)) return;

        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        final @NotNull List<TestCaseDto> starting = testCases.stream()
                .filter(tc -> !execution.isRunning(tc.getId()))
                .toList();

        if (starting.isEmpty()) return;

        TestRunner.available().run(p, starting);
    }
}
