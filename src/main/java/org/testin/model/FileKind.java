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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.node.TestRunNode;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

// Rule-INTERNAL-011
@Getter
@AllArgsConstructor
public enum FileKind {
    MARKER(""),

    TEST_CASE(".tc"),

    RUN_ITEM(".ri"),

    SCREENSHOT(".png"),

    OTHER("");

    private final @NotNull String extension;

    // Rule-INTERNAL-011
    public static @NotNull FileKind of(final @NotNull Path file) {
        final @NotNull String name = String.valueOf(file.getFileName());
        if (NodeType.byMarker(name).isPresent()) return MARKER;
        if (name.endsWith(TEST_CASE.extension)) return TEST_CASE;
        if (name.endsWith(RUN_ITEM.extension)) return RUN_ITEM;

        return OTHER;
    }

    // Rule-INTERNAL-011
    public static @NotNull FileKind of(final @NotNull Path file, final @NotNull NodeType folder) {
        final @NotNull FileKind byName = of(file);
        if (byName != OTHER) return byName;

        return folder.holdsScreenshots() && TestRunNode.isScreenshotName(String.valueOf(file.getFileName())) ? SCREENSHOT : OTHER;
    }

    // Rule-INTERNAL-012
    public @NotNull String fileName(final @NotNull UUID testCaseId) {
        if (extension.isEmpty()) {
            throw new IllegalStateException("A " + this + " is not named by an id: its name is fixed, or it is not Testin's file");
        }

        return testCaseId + extension;
    }

    // Rule-INTERNAL-012, Rule-INTERNAL-084
    public @NotNull Optional<UUID> idIn(final @NotNull Path file) {
        final @NotNull String name = String.valueOf(file.getFileName());
        if (!name.endsWith(extension) || extension.isEmpty()) return Optional.empty();

        try {
            return Optional.of(UUID.fromString(name.substring(0, name.length() - extension.length())));
        } catch (final IllegalArgumentException notAnId) {
            return Optional.empty();
        }
    }
}
