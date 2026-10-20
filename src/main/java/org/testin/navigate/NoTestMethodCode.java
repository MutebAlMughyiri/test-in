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
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.AutomationLanguage;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public final class NoTestMethodCode implements TestMethodCode {
    private final @NotNull AutomationLanguage language;

    @Override
    public @NotNull AutomationLanguage language() {
        return language;
    }

    @Override
    public void toCode(final @NotNull Project project, final @NotNull TestCase testCase) {
        TestinLogger.debug("No code navigation in this IDE; nothing opened for '" + testCase.getDescription() + "'");
    }

    @Override
    public @NotNull Map<UUID, Boolean> methodsFor(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        TestinLogger.debug("No code navigation in this IDE; no generated methods for " + testCases.size() + " test case(s)");

        return Map.of();
    }
}
