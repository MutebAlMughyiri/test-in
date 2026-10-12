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

package org.testin.indexer;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.NodeType;
import org.testin.model.markers.AbstractMarker;
import org.testin.model.markers.Marker;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.util.FailureText;
import org.testin.util.Mapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class MarkerFiles {

    private final @NotNull Mapper mapper;

    private final @NotNull TestDataFiles testDataFiles;

    private final @NotNull AppSettingsState settings;

    private final @NotNull Set<Path> damaged = ConcurrentHashMap.newKeySet();

    MarkerFiles(final @NotNull Project p) {
        this.mapper = Services.getInstance(p, Mapper.class);
        this.testDataFiles = Services.getInstance(p, TestDataFiles.class);
        this.settings = Services.getInstance(p, AppSettingsState.class);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    <M extends AbstractMarker> @NotNull M read(final @NotNull Path dirPath, final @NotNull NodeType kind, final @NotNull Class<M> markerClass) {
        final @NotNull Path markerFile = dirPath.resolve(kind.getMarker());

        if (!Files.exists(markerFile)) return defaultFor(markerClass, kind);

        try {
            return mapper.readValue(Files.readAllBytes(markerFile), markerClass);

        } catch (final IOException | UncheckedIOException ex) {
            TestinLogger.warn("Unreadable " + kind.getMarkerKind() + " marker " + markerFile + ", using defaults: " + FailureText.of(ex));

            damaged.add(dirPath);
            return defaultFor(markerClass, kind);
        }
    }

    boolean write(final @NotNull Path dirPath, final @NotNull String markerFileName, final @NotNull Marker marker) {
        final @NotNull Path file = dirPath.resolve(markerFileName);

        // UC-INTERNAL-002, Rule-INTERNAL-083
        if (marker.getCreatedBy().isEmpty()) {
            if (!Files.exists(file)) {
                marker.stampCreated(tester());
            } else if (!parses(file, marker.getClass())) {
                TestinLogger.warn("Left the unreadable marker " + file + " as it is rather than writing defaults over it");
                return false;
            }
        }

        // Rule-INTERNAL-090
        if (marker.getId().isEmpty()) marker.setId(UUID.randomUUID().toString());

        return testDataFiles.write(file, marker);
    }

    // Rule-INTERNAL-090, Rule-TREE-PANEL-051
    boolean giveFreshId(final @NotNull Path markerFile) {
        final @NotNull Optional<NodeType> kind = NodeType.byMarker(String.valueOf(markerFile.getFileName()));
        if (kind.isEmpty()) return false;

        try {
            final @NotNull AbstractMarker marker = mapper.readValue(Files.readAllBytes(markerFile), kind.orElseThrow().getMarkerClass());
            marker.setId(UUID.randomUUID().toString());

            return testDataFiles.write(markerFile, marker);

        } catch (final IOException | UncheckedIOException ex) {
            TestinLogger.warn("Left the copied marker " + markerFile + " without an id of its own: " + FailureText.of(ex));
            return false;
        }
    }

    private boolean parses(final @NotNull Path file, final @NotNull Class<?> markerClass) {
        try {
            mapper.readValue(Files.readAllBytes(file), markerClass);
            return true;
        } catch (final IOException | UncheckedIOException unreadable) {
            return false;
        }
    }

    void touched(final @NotNull Path dirPath, final @NotNull String markerFileName, final @NotNull Marker marker) {
        final @NotNull String updatedByBefore = marker.getUpdatedBy();
        final @NotNull ZonedDateTime updatedAtBefore = marker.getUpdatedAt();

        marker.touch(tester());
        if (testDataFiles.alreadyHolds(dirPath.resolve(markerFileName), marker)) return;
        if (write(dirPath, markerFileName, marker)) return;

        // Rule-INTERNAL-123
        marker.setUpdatedBy(updatedByBefore);
        marker.setUpdatedAt(updatedAtBefore);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    @NotNull List<Path> takeDamaged(final @NotNull Path projectPath) {
        final @NotNull List<Path> taken = damaged.stream().filter(dirPath -> dirPath.startsWith(projectPath)).toList();
        taken.forEach(damaged::remove);

        return taken;
    }

    boolean has(final @NotNull Path dirPath, final @NotNull NodeType kind) {
        return Files.exists(dirPath.resolve(kind.getMarker()));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-009
    @NotNull Optional<NodeType> markedAs(final @NotNull Path dirPath, final @NotNull List<NodeType> family) {
        return family.stream().filter(kind -> has(dirPath, kind)).findFirst();
    }

    private @NotNull String tester() {
        return settings.testerName;
    }

    private <M> @NotNull M defaultFor(final @NotNull Class<M> markerClass, final @NotNull NodeType kind) {
        try {
            return markerClass.getDeclaredConstructor().newInstance();
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot create default " + kind.getMarkerKind() + " marker", ex);
        }
    }
}
