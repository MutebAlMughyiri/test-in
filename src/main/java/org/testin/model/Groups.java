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

package org.testin.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Groups {
    public static final @NotNull String NONE = Bundle.message("groups.none");

    public static @NotNull List<String> read(final @NotNull String raw) {
        if (raw.isBlank() || raw.trim().equalsIgnoreCase(NONE)) return new ArrayList<>();

        final @NotNull List<String> read = new ArrayList<>();
        for (final String name : raw.split(",", -1)) {
            final @NotNull String group = name.trim();

            if (!group.isEmpty() && !read.contains(group)) read.add(group);
        }

        return read;
    }

    public static @NotNull String text(final @NotNull List<String> groups) {
        return String.join(", ", groups);
    }

    // Rule-EDITOR-PANEL-095
    public static @NotNull Set<String> in(final @NotNull List<TestCase> testCases) {
        final @NotNull Set<String> groups = new HashSet<>();
        for (final TestCase testCase : testCases) {
            for (final String group : testCase.getGroups()) {
                if (!group.isBlank()) groups.add(group.trim());
            }
        }
        return groups;
    }
}
