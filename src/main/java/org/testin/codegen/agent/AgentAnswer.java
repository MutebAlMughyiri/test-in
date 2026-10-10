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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AgentAnswer {
    private static final @NotNull String FENCE = "```";

    // UC-CODEGEN-021, Rule-CODEGEN-088
    public static @NotNull Optional<String> statementsIn(final @NotNull String printed) {
        final @NotNull String written = largestBlockIn(printed).orElse(printed).strip();

        return written.isBlank() || !looksLikeJava(written) ? Optional.empty() : Optional.of(written);
    }

    // Rule-CODEGEN-088
    private static @NotNull Optional<String> largestBlockIn(final @NotNull String printed) {
        final @NotNull List<String> parts = Arrays.asList(printed.split(FENCE, -1));
        if (parts.size() < 3) return Optional.empty();

        return IntStream.range(0, parts.size())
                .filter(index -> index % 2 == 1)
                .mapToObj(parts::get)
                .map(AgentAnswer::withoutItsLanguage)
                .max(Comparator.comparingInt(String::length));
    }

    private static @NotNull String withoutItsLanguage(final @NotNull String block) {
        final int firstLine = block.indexOf('\n');
        if (firstLine == -1) return block;

        return block.substring(0, firstLine).isBlank() || block.substring(0, firstLine).strip().matches("[A-Za-z]+")
                ? block.substring(firstLine + 1)
                : block;
    }

    // Rule-CODEGEN-088
    private static boolean looksLikeJava(final @NotNull String written) {
        return written.endsWith(";") || written.endsWith("}");
    }
}
