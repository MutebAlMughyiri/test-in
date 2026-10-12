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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class NoTestMethodCode implements TestMethodCode {
    @Override
    public void toCode(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        TestinLogger.debug("No code navigation in this IDE; nothing opened for '" + tc.getDescription() + "'");
    }

    @Override
    public @NotNull Map<UUID, Boolean> methodsFor(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        TestinLogger.debug("No code navigation in this IDE; no generated methods for " + testCases.size() + " test case(s)");

        return Map.of();
    }

    @Override
    public @NotNull Set<UUID> withAWrittenBody(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        return Set.of();
    }

    @Override
    public @NotNull Optional<String> fillBody(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String statements) {
        TestinLogger.debug("No code navigation in this IDE; no body written for '" + tc.getDescription() + "'");

        return Optional.empty();
    }

    @Override
    public @NotNull Optional<String> replaceBody(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String statements) {
        return fillBody(p, tc, statements);
    }

    @Override
    public @NotNull Map<UUID, List<String>> methodFqcnsOf(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        TestinLogger.debug("No code navigation in this IDE; no generated methods for " + testCases.size() + " test case(s)");

        return Map.of();
    }
}
