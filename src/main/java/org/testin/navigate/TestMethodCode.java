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
import org.testin.model.TestCaseDto;
import org.testin.util.FromContentModule;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TestMethodCode {
    @NotNull ExtensionPointName<TestMethodCode> EP = ExtensionPointName.create("org.testin.codeNavigation");

    static @NotNull TestMethodCode available() {
        return EP.getExtensionList().stream()
                .findFirst()
                .orElseGet(NoTestMethodCode::new);
    }

    @FromContentModule
    void toCode(final @NotNull Project project, final @NotNull TestCaseDto testCase);

    @FromContentModule
    @NotNull Map<UUID, List<String>> methodFqcnsOf(final @NotNull Project project, final @NotNull List<TestCaseDto> testCases);

    @FromContentModule
    @NotNull Map<UUID, Boolean> methodsFor(final @NotNull Project project, final @NotNull List<TestCaseDto> testCases);

    @FromContentModule
    @NotNull Set<UUID> withAWrittenBody(@NotNull Project project, @NotNull List<TestCaseDto> testCases);

    @FromContentModule
    @NotNull Optional<String> fillBody(@NotNull Project project, @NotNull TestCaseDto testCase, @NotNull String statements);

    @FromContentModule
    @NotNull Optional<String> replaceBody(@NotNull Project project, @NotNull TestCaseDto testCase, @NotNull String statements);
}
