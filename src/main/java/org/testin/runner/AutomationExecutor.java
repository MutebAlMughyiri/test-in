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

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.util.Bundle;

import java.util.List;

public interface AutomationExecutor {
    @NotNull ExtensionPointName<AutomationExecutor> EP = ExtensionPointName.create("org.testin.testRunners");

    static @NotNull AutomationExecutor available() {
        return EP.getExtensionList().stream()
                .findFirst()
                .orElseGet(() -> (_, testCases) -> TestinLogger.debug(
                        Bundle.message("runner.none", String.valueOf(testCases.size()))));
    }

    // UC-CODEGEN-008, Rule-CODEGEN-031
    void run(final @NotNull Project project, final @NotNull List<TestCaseDto> testCases);
}
