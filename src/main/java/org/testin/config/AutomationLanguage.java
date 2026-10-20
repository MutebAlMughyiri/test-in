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

package org.testin.config;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.services.OptionalPlugin;
import org.testin.util.Bundle;

import java.util.Arrays;
import java.util.Optional;

// Rule-CODEGEN-116
@Getter
@AllArgsConstructor
public enum AutomationLanguage {
    JAVA(
            "JavaSDK",
            Bundle.message("automation.language.java"),
            Optional.of(OptionalPlugin.JAVA),
            Optional.of(OptionalPlugin.TESTNG)
    ),

    ROBOT_FRAMEWORK(
            "Python SDK",
            Bundle.message("automation.language.robot"),
            Optional.empty(),
            Optional.empty()
    );

    private final @NotNull String sdkType;

    private final @NotNull String displayName;

    private final @NotNull Optional<OptionalPlugin> codePlugin;

    private final @NotNull Optional<OptionalPlugin> runPlugin;

    // UC-CODEGEN-023, Rule-CODEGEN-116
    public static @NotNull AutomationLanguage of(final @NotNull Project project) {
        final @NotNull String interpreter = Optional.ofNullable(ProjectRootManager.getInstance(project).getProjectSdk()).map(sdk -> sdk.getSdkType().getName()).orElse("");
        return Arrays.stream(values()).filter(language -> language.sdkType.equals(interpreter)).findFirst().orElse(JAVA);
    }

    // Rule-CODEGEN-005, Rule-CODEGEN-117
    public @NotNull AutomationNeed toWorkOnCode(final @NotNull ExtensionPointName<? extends LanguageContributor> point, final @NotNull String workName) {
        return new AutomationNeed(this, codePlugin, point, workName);
    }

    // Rule-CODEGEN-005, Rule-CODEGEN-117
    public @NotNull AutomationNeed toRun(final @NotNull ExtensionPointName<? extends LanguageContributor> point, final @NotNull String workName) {
        return new AutomationNeed(this, runPlugin, point, workName);
    }

    // Rule-CODEGEN-116, Rule-CODEGEN-117
    public <T extends LanguageContributor> @NotNull Optional<T> contributor(final @NotNull ExtensionPointName<T> point) {
        return point.getExtensionList().stream().filter(contributor -> contributor.language() == this).findFirst();
    }
}
