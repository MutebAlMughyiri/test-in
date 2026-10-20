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

package org.testin.robot;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.NodeCode;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RobotNames {
    private static final @NotNull Pattern NOT_IN_A_FILE_NAME = Pattern.compile("[<>:\"/\\\\|?*\\p{Cntrl}]");

    private static final @NotNull Pattern TEST_CASES_SUFFIX = Pattern.compile("-test-cases$", Pattern.CASE_INSENSITIVE);

    private static final @NotNull Pattern SPACES = Pattern.compile("\\s+");

    private static final @NotNull Pattern NOT_A_TEST_NAME_START = Pattern.compile("^[#.\\[\\s]+");

    // Rule-CODEGEN-121
    static @NotNull String fileNameOf(final @NotNull String treeName) {
        final @NotNull String kept = NOT_IN_A_FILE_NAME.matcher(treeName).replaceAll("").strip();
        return SPACES.matcher(TEST_CASES_SUFFIX.matcher(kept).replaceAll("").strip()).replaceAll("_");
    }

    // Rule-CODEGEN-121
    static @NotNull Optional<String> folderOf(final @NotNull List<String> treePath) {
        final @NotNull List<String> folders = NodeCode.codePathOf(treePath).stream().map(RobotNames::fileNameOf).toList();
        return folders.isEmpty() || folders.contains("") ? Optional.empty() : Optional.of(String.join("/", folders));
    }

    // Rule-CODEGEN-121
    static @NotNull Optional<String> fileOf(final @NotNull List<String> treePath) {
        return folderOf(treePath).map(path -> path + "." + RobotSuite.EXTENSION);
    }

    // Rule-CODEGEN-122
    public static @NotNull Optional<String> testNameOf(final @NotNull String description) {
        final @NotNull String name = NOT_A_TEST_NAME_START.matcher(SPACES.matcher(description).replaceAll(" ")).replaceAll("").strip();
        return name.isEmpty() ? Optional.empty() : Optional.of(name);
    }
}
