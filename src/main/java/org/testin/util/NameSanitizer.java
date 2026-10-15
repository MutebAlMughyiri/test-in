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
import java.util.Set;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NameSanitizer {
    private static final @NotNull Pattern INVALID_NAME = Pattern.compile("[^a-zA-Z0-9 _]");

    private static final @NotNull Pattern SPACES = Pattern.compile("\\s{2,}");

    private static final @NotNull Pattern NOT_A_LETTER_OR_DIGIT = Pattern.compile("[^A-Za-z0-9]+");

    private static final @NotNull Set<String> OBJECT_METHODS = Set.of("getClass", "hashCode", "equals", "toString", "notify", "notifyAll", "wait", "clone", "finalize");

    // UC-CODEGEN-002, Rule-CODEGEN-011, Rule-CODEGEN-073
    public static @NotNull String packageName(final @NotNull String value) {
        final @NotNull String word = packageWord(value);

        if (word.isEmpty()) return "generated" + tag(value);
        if (!SourceVersion.isName(word)) return word + tag(value);
        return word;
    }

    // Rule-CODEGEN-008
    private static @NotNull String packageWord(final @NotNull String value) {
        final @NotNull String cleanName = INVALID_NAME.matcher(value.replace("-test-cases", ""))
                .replaceAll("").trim();
        final @NotNull String word = String.join("", cleanName.split("[\\s_]+", -1)).toLowerCase(Locale.ROOT);

        return notStartingWithADigit(word);
    }

    private static @NotNull String notStartingWithADigit(final @NotNull String word) {
        return !word.isEmpty() && Character.isDigit(word.charAt(0)) ? "_" + word : word;
    }

    // UC-CODEGEN-022, Rule-CODEGEN-103
    @FromContentModule
    public static @NotNull String modelFieldName(final @NotNull String key) {
        final @NotNull String name = modelName(key, false);
        return SourceVersion.isKeyword(name) || OBJECT_METHODS.contains(name) ? name + "Value" : name;
    }

    // UC-CODEGEN-022, Rule-CODEGEN-103
    @FromContentModule
    public static @NotNull String modelTypeName(final @NotNull String key) {
        return modelName(key, true);
    }

    // Rule-CODEGEN-103
    private static @NotNull String modelName(final @NotNull String key, final boolean capitalFirst) {
        final @NotNull StringBuilder name = new StringBuilder();
        for (final String word : NOT_A_LETTER_OR_DIGIT.split(key, -1)) {
            if (word.isEmpty()) continue;

            final @NotNull String kept = word.equals(word.toUpperCase(Locale.ROOT)) ? word.toLowerCase(Locale.ROOT) : word;
            final char first = name.isEmpty() && !capitalFirst ? Character.toLowerCase(kept.charAt(0)) : Character.toUpperCase(kept.charAt(0));
            name.append(first).append(kept.substring(1));
        }
        if (name.isEmpty()) name.append(capitalFirst ? "Field" : "field");
        return notStartingWithADigit(name.toString());
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
        if (Character.isDigit(result.charAt(0))) result.insert(0, '_');
        return result.append("Test").toString();
    }

    public static @NotNull String description(final @NotNull String rawDescription) {
        return SPACES.matcher(INVALID_NAME.matcher(rawDescription).replaceAll("")).replaceAll(" ").trim();
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

    public static @NotNull String removeSpecialChars(final @NotNull String value) {
        if (value.isEmpty()) return "";
        return value.chars()
                .mapToObj(character -> isSpecial((char) character) ? "" : String.valueOf((char) character))
                .reduce(new StringBuilder(value.length()), StringBuilder::append, StringBuilder::append)
                .toString();
    }

    private static boolean isSpecial(final char value) {
        return "!\"#$%&'()*+,./:;<=>?@[\\]^_`{|}~".indexOf(value) >= 0;
    }
}
