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
import org.testin.config.AutomationLanguage;
import org.testin.config.AutomationNeed;
import org.testin.config.LanguageContributor;
import org.testin.model.TestCase;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;

public interface AutomationExecutor extends LanguageContributor {
    @NotNull ExtensionPointName<AutomationExecutor> EP = ExtensionPointName.create("org.testin.testRunners");

    // Rule-CODEGEN-116
    static @NotNull Optional<AutomationExecutor> available(final @NotNull Project project) {
        return AutomationLanguage.of(project).contributor(EP);
    }

    // Rule-CODEGEN-005, Rule-CODEGEN-117
    static @NotNull AutomationNeed need(final @NotNull Project project) {
        return AutomationLanguage.of(project).toRun(EP, Bundle.message("automation.work.run"));
    }

    // UC-CODEGEN-008, Rule-CODEGEN-031
    void run(final @NotNull Project project, final @NotNull List<TestCase> testCases);
}
