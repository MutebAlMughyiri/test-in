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

package org.testin.apimodel;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.NameSanitizer;

import javax.lang.model.SourceVersion;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ModelNames {
    private static final @NotNull Pattern NOT_A_LETTER_OR_DIGIT = Pattern.compile("[^A-Za-z0-9]+");

    private static final @NotNull Set<String> OBJECT_METHODS = Set.of("getClass", "hashCode", "equals", "toString", "notify", "notifyAll", "wait", "clone", "finalize");

    // UC-CODEGEN-022, Rule-CODEGEN-103
    static @NotNull String fieldName(final @NotNull String key) {
        final @NotNull String name = name(key, false);
        return SourceVersion.isKeyword(name) || OBJECT_METHODS.contains(name) ? name + "Value" : name;
    }

    // UC-CODEGEN-022, Rule-CODEGEN-103
    static @NotNull String typeName(final @NotNull String key) {
        return name(key, true);
    }

    // Rule-CODEGEN-103
    private static @NotNull String name(final @NotNull String key, final boolean capitalFirst) {
        final @NotNull StringBuilder name = new StringBuilder();
        for (final String word : NOT_A_LETTER_OR_DIGIT.split(key, -1)) {
            if (word.isEmpty()) continue;

            final @NotNull String kept = word.equals(word.toUpperCase(Locale.ROOT)) ? word.toLowerCase(Locale.ROOT) : word;
            final char first = name.isEmpty() && !capitalFirst ? Character.toLowerCase(kept.charAt(0)) : Character.toUpperCase(kept.charAt(0));
            name.append(first).append(kept.substring(1));
        }
        if (name.isEmpty()) name.append(capitalFirst ? "Field" : "field");
        return NameSanitizer.notStartingWithADigit(name.toString());
    }
}
