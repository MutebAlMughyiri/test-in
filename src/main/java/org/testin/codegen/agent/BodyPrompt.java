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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.model.TestCase;

import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BodyPrompt {
    public static final @NotNull List<String> PLACEHOLDERS =
            List.of("{description}", "{expectedResult}", "{steps}", "{testData}", "{preConditions}", "{module}", "{testSetPath}", "{class}", "{method}");

    // UC-CODEGEN-021, Rule-CODEGEN-086
    public static @NotNull String of(final @NotNull String template, final @NotNull TestCase testCase, final @NotNull String methodName) {
        final @NotNull Map<String, String> values = Map.of(
                "{description}", testCase.getDescription(),
                "{expectedResult}", testCase.getExpectedResult(),
                "{steps}", String.join("\n", testCase.getSteps()),
                "{testData}", testCase.getTestData(),
                "{preConditions}", testCase.getPreConditions(),
                "{module}", testCase.getModule(),
                "{testSetPath}", String.join(" > ", testCase.getParent().treePath()),
                "{class}", Fqcn.classOfMethod(testCase),
                "{method}", methodName);

        @NotNull String written = template;
        for (final Map.Entry<String, String> value : values.entrySet()) {
            written = written.replace(value.getKey(), value.getValue());
        }

        return written;
    }
}
