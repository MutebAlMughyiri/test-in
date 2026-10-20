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

package org.testin.codegen.agent;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.config.AutomationLanguage;
import org.testin.config.LanguageContributor;
import org.testin.model.TestCase;
import org.testin.util.FromContentModule;

import java.util.Optional;

// UC-CODEGEN-021
public interface TestBodyWriter extends LanguageContributor {
    @NotNull ExtensionPointName<TestBodyWriter> EP = ExtensionPointName.create("org.testin.testBodies");

    // Rule-CODEGEN-116
    static @NotNull Optional<TestBodyWriter> available(final @NotNull Project project) {
        return AutomationLanguage.of(project).contributor(EP);
    }

    @FromContentModule
    @NotNull Optional<String> fillBody(@NotNull Project project, @NotNull TestCase testCase, @NotNull String statements);

    @FromContentModule
    @NotNull Optional<String> replaceBody(@NotNull Project project, @NotNull TestCase testCase, @NotNull String statements);
}
