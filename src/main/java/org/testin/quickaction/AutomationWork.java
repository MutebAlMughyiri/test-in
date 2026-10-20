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

package org.testin.quickaction;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.navigate.TestMethodCode;
import org.testin.config.AutomationNeed;
import org.testin.runner.AutomationExecutor;

import java.util.function.Function;

@AllArgsConstructor
enum AutomationWork {
    NAVIGATION(
            TestMethodCode::need,
            true
    ),

    RUNNING(
            AutomationExecutor::need,
            false
    );

    private final @NotNull Function<Project, AutomationNeed> need;

    @Getter
    private final boolean onlyWhereCodeIsOn;

    // Rule-CODEGEN-117
    @NotNull AutomationNeed of(final @NotNull Project project) {
        return need.apply(project);
    }
}
