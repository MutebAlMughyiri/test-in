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

package org.testin.setting;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.services.Services;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Rule-PRODUCT-001
@Service(Service.Level.PROJECT)
public final class TestinRoot {
    public static final @NotNull Path NONE = Path.of("");

    private final @NotNull Project project;

    private final @NotNull AppSettingsState settings;

    public TestinRoot(final @NotNull Project project) {
        this.project = project;
        this.settings = Services.getInstance(project, AppSettingsState.class);
    }

    // UC-SETTING-002, Rule-SETTING-011
    public static @NotNull Path normalize(final @Nullable String rawPath) {
        return Path.of(Objects.requireNonNullElse(rawPath, "").trim());
    }

    // UC-SETTING-002, Rule-SETTING-011, Rule-SETTING-046
    public static @NotNull Path stored(final @Nullable String rawPath) {
        try {
            final @NotNull Path root = normalize(rawPath);
            return root.isAbsolute() ? root : NONE;
        } catch (final InvalidPathException notAPath) {
            return NONE;
        }
    }

    // UC-SETTING-002, Rule-SETTING-011
    public static boolean isConfigured(final @NotNull Path root) {
        return !NONE.equals(root);
    }

    // Rule-SETTING-004
    public static boolean isRootChanged(final @Nullable String before, final @Nullable String after) {
        return !normalize(before).equals(normalize(after));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-108
    public static @NotNull List<String> place(final @NotNull Path root, final @NotNull String rawPath) {
        final @NotNull Optional<Path> read = pathOf(rawPath);
        if (read.isEmpty()) return asItStands(rawPath);

        final @NotNull Path under = read.orElseThrow();
        if (!isConfigured(root) || !under.startsWith(root)) return asItStands(rawPath);

        final @NotNull List<String> segments = new ArrayList<>();
        for (final Path segment : root.relativize(under)) {
            final @NotNull String name = segment.toString();
            if (!name.isEmpty()) segments.add(name);
        }

        return segments.isEmpty() ? List.of(folderName(root)) : List.copyOf(segments);
    }

    // Rule-INTERNAL-108
    private static @NotNull List<String> asItStands(final @NotNull String rawPath) {
        return rawPath.isBlank() ? List.of() : List.of(rawPath.trim());
    }

    // Rule-INTERNAL-108
    private static @NotNull String folderName(final @NotNull Path folder) {
        return Objects.toString(folder.getFileName(), folder.toString());
    }

    private static @NotNull Optional<Path> pathOf(final @NotNull String rawPath) {
        if (rawPath.isBlank()) return Optional.empty();

        try {
            return Optional.of(Path.of(rawPath.trim()));
        } catch (final InvalidPathException ex) {
            return Optional.empty();
        }
    }

    // UC-SETTING-002, Rule-SETTING-046
    public @NotNull Path getPath() {
        return stored(settings.rootTestinPath);
    }

    // UC-SETTING-002, Rule-SETTING-011
    public boolean isConfigured() {
        return isConfigured(getPath());
    }

    // UC-SETTING-002, Rule-SETTING-013, Rule-SETTING-046
    public @NotNull Path absolutePath() {
        return getPath();
    }

    // UC-INTERNAL-007, Rule-INTERNAL-108
    public @NotNull List<String> place(final @NotNull String rawPath) {
        return place(absolutePath(), rawPath);
    }

    private @NotNull Path basePath() {
        return Path.of(Objects.toString(project.getBasePath(), ""));
    }

    // UC-SETTING-002, Rule-SETTING-010
    public @NotNull Path resolve(final @NotNull List<String> segments) {
        Path resolved = isConfigured() ? absolutePath() : basePath();

        for (final String segment : segments) {
            resolved = resolved.resolve(segment);
        }

        return resolved;
    }
}
