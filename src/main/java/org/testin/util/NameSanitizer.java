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

package org.testin.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.SourceVersion;
import java.util.Locale;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NameSanitizer {
    private static final @NotNull String TEST_CASES_SUFFIX = "-test-cases";

    private static final @NotNull Pattern INVALID_NAME = Pattern.compile("[^a-zA-Z0-9 _]");

    // UC-CODEGEN-002, Rule-CODEGEN-011, Rule-CODEGEN-073
    public static @NotNull String packageName(final @NotNull String value) {
        final @NotNull String word = packageWord(value);

        if (word.isEmpty()) return "generated" + tag(value);
        if (!SourceVersion.isName(word)) return word + tag(value);
        return word;
    }

    // Rule-CODEGEN-008
    private static @NotNull String packageWord(final @NotNull String value) {
        final @NotNull String cleanName = INVALID_NAME.matcher(withoutTestCases(value))
                .replaceAll("").trim();
        final @NotNull String word = String.join("", cleanName.split("[\\s_]+", -1)).toLowerCase(Locale.ROOT);

        return notStartingWithADigit(word);
    }

    // Rule-CODEGEN-008, Rule-TREE-PANEL-107
    public static @NotNull String withoutTestCases(final @NotNull String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(TEST_CASES_SUFFIX) ? name.substring(0, name.length() - TEST_CASES_SUFFIX.length()) : name;
    }

    public static @NotNull String notStartingWithADigit(final @NotNull String word) {
        return !word.isEmpty() && Character.isDigit(word.charAt(0)) ? "_" + word : word;
    }

    // UC-CODEGEN-002, Rule-CODEGEN-011
    public static boolean canMakePackageName(final @NotNull String value) {
        return SourceVersion.isName(packageWord(value));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-011
    private static @NotNull String tag(final @NotNull String value) {
        return Integer.toHexString(value.hashCode());
    }

    // UC-CODEGEN-002, Rule-CODEGEN-011
    public static @NotNull String className(final @NotNull String value) {
        if (value.trim().isEmpty()) return "DefaultTest";

        final @NotNull String cleanName = INVALID_NAME.matcher(value).replaceAll("").trim();
        final @NotNull StringBuilder result = new StringBuilder();
        for (final String word : cleanName.split("[\\s_]+", -1)) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        if (result.isEmpty()) return "Generated" + tag(value) + "Test";
        return notStartingWithADigit(result.append("Test").toString());
    }

    // Rule-CODEGEN-012, Rule-CODEGEN-016
    public static @NotNull String methodName(final @NotNull String description) {
        final @NotNull StringBuilder result = new StringBuilder();
        for (final String word : description.split("[^a-zA-Z0-9]+", -1)) {
            if (word.isEmpty()) continue;
            if (result.isEmpty()) {
                result.append(word.toLowerCase(Locale.ROOT));
            } else {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) result.append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return result.toString();
    }

    public static @NotNull String methodKey(final @NotNull String methodName) {
        return methodName.replace("_", "").toLowerCase(Locale.ROOT);
    }

    public static boolean cannotMakeMethodName(final @NotNull String description) {
        return !SourceVersion.isName(methodName(description));
    }
}
