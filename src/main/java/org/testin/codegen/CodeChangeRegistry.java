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

package org.testin.codegen;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.config.AutomationLanguage;
import org.testin.config.AutomationNeed;
import org.testin.config.LanguageContributor;
import org.testin.model.node.AbstractNode;
import org.testin.util.Bundle;
import org.testin.util.FromContentModule;

import java.util.Optional;

public interface CodeChangeRegistry extends LanguageContributor {
    @NotNull ExtensionPointName<CodeChangeRegistry> EP = ExtensionPointName.create("org.testin.codeGenerators");

    // UC-CODEGEN-019, Rule-CODEGEN-005, Rule-CODEGEN-116
    static <T> @NotNull CodeChange<T> find(final @NotNull Project project, final @NotNull CodeChangeType<T> type) {
        return AutomationLanguage.of(project).contributor(EP)
                .map(generators -> generators.actionFor(type))
                .orElseGet(() -> new NoCodeChange<>(type.description()));
    }

    // Rule-CODEGEN-005, Rule-CODEGEN-117
    static @NotNull AutomationNeed need(final @NotNull Project project) {
        return AutomationLanguage.of(project).toWorkOnCode(EP, Bundle.message("automation.work.code"));
    }

    // UC-CODEGEN-018, Rule-CODEGEN-113
    static @NotNull Optional<CodeChangeRegistry> available(final @NotNull Project project) {
        return AutomationLanguage.of(project).contributor(EP);
    }

    <T> @NotNull CodeChange<T> actionFor(final @NotNull CodeChangeType<T> type);

    // UC-CODEGEN-018, Rule-CODEGEN-059
    @FromContentModule
    @NotNull Optional<String> folderOf(@NotNull AbstractNode node);

    // UC-CODEGEN-018, Rule-CODEGEN-059
    @FromContentModule
    @NotNull Optional<String> fileOf(@NotNull AbstractNode testSet);
}
