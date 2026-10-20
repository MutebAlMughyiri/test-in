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

package org.testin.navigate;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.config.AutomationNeed;
import org.testin.config.AutomationLanguage;
import org.testin.config.LanguageContributor;
import org.testin.model.TestCase;
import org.testin.util.FromContentModule;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TestMethodCode extends LanguageContributor {
    @NotNull ExtensionPointName<TestMethodCode> EP = ExtensionPointName.create("org.testin.codeNavigation");

    // Rule-CODEGEN-005, Rule-CODEGEN-117
    static @NotNull AutomationNeed need(final @NotNull Project project) {
        return AutomationLanguage.of(project).toWorkOnCode(EP, Bundle.message("automation.work.navigate"));
    }

    // Rule-CODEGEN-116
    static @NotNull TestMethodCode available(final @NotNull Project project) {
        final @NotNull AutomationLanguage language = AutomationLanguage.of(project);
        return language.contributor(EP).orElseGet(() -> new NoTestMethodCode(language));
    }

    @FromContentModule
    void toCode(final @NotNull Project project, final @NotNull TestCase testCase);

    @FromContentModule
    @NotNull Map<UUID, Boolean> methodsFor(final @NotNull Project project, final @NotNull List<TestCase> testCases);
}
