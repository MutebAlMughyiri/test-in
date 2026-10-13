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

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.TimeoutUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.NodeType;
import org.testin.model.FileKind;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.JsonCodec;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service(Service.Level.PROJECT)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class TestDataFiles {
    private static final int MOVE_ATTEMPTS = 6;
    private static final long MOVE_PAUSE_MILLIS = 25;

    private final @NotNull Project project;

    private final @NotNull OwnWrites ownWrites = Services.getInstance(OwnWrites.class);

    // Rule-INTERNAL-125
    static @NotNull Path besideItself(final @NotNull Path path) {
        return path.resolveSibling(path.getFileName() + ".writing");
    }

    private static void forget(final @NotNull Path beside) {
        try {
            Files.deleteIfExists(beside);
        } catch (final IOException ex) {
            TestinLogger.warn("Could not remove " + beside + " after a write that failed: " + FailureText.of(ex));
        }
    }

    // UC-INTERNAL-004, Rule-INTERNAL-033
    <T> boolean alreadyHolds(final @NotNull Path path, final @NotNull T content) {
        try {
            return Arrays.equals(Files.readAllBytes(path), Services.getInstance(project, JsonCodec.class).writeValueAsBytes(content));
        } catch (final IOException absentOrUnreadable) {
            return false;
        }
    }

    boolean alreadyHolds(final @NotNull Path path, final byte @NotNull [] bytes) {
        try {
            return Arrays.equals(Files.readAllBytes(path), bytes);
        } catch (final IOException absentOrUnreadable) {
            return false;
        }
    }

    <T> boolean write(final @NotNull Path path, final @NotNull T content) {
        return writeBytes(path, Services.getInstance(project, JsonCodec.class).writeValueAsBytes(content));
    }

    void write(final @NotNull Path path, final byte @NotNull [] jsonBytes) {
        writeBytes(path, jsonBytes);
    }

    byte @NotNull [] readBytes(final @NotNull Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (final IOException missingOrUnreadable) {
            TestinLogger.warn("Could not read " + path + ": " + FailureText.of(missingOrUnreadable));
            return new byte[0];
        }
    }

    // Rule-INTERNAL-011
    @NotNull List<Path> runItemsIn(final @NotNull Path testRunPath) {
        try (Stream<Path> inside = Files.list(testRunPath)) {
            return inside.filter(file -> FileKind.of(file) == FileKind.RUN_ITEM).toList();
        } catch (final IOException ex) {
            TestinLogger.warn("Could not list the results in " + testRunPath + ": " + FailureText.of(ex));
            return List.of();
        }
    }

    @NotNull List<Path> screenshotsIn(final @NotNull Path testRunPath) {
        try (Stream<Path> inside = Files.list(testRunPath)) {
            return inside.filter(file -> FileKind.of(file, NodeType.TR) == FileKind.SCREENSHOT).toList();
        } catch (final IOException ex) {
            TestinLogger.warn("Could not list the screenshots in " + testRunPath + ": " + FailureText.of(ex));
            return List.of();
        }
    }

    // UC-INTERNAL-003, Rule-INTERNAL-019, Rule-INTERNAL-113, Rule-INTERNAL-125
    private boolean writeBytes(final @NotNull Path path, final byte @NotNull [] jsonBytes) {
        if (jsonBytes.length == 0) {
            TestinLogger.error("Refusing to write an empty file, which would erase it: " + path);
            Services.getInstance(project, Notifier.class).error(project, Bundle.message("files.nothing.written", path.getFileName()));
            return false;
        }

        final @NotNull Path beside = besideItself(path);
        try {
            ownWrites.record(project, path);
            ownWrites.record(project, beside);

            FileUtil.createParentDirs(path.toFile());
            Files.write(beside, jsonBytes);
        } catch (final IOException ex) {
            reportWriteFailure(path, ex);
            forget(beside);
            return false;
        }

        if (!movedInto(beside, path)) {
            forget(beside);
            return false;
        }

        ownWrites.wrote(project, path, jsonBytes);
        return true;
    }

    // Rule-INTERNAL-113
    private boolean movedInto(final @NotNull Path beside, final @NotNull Path path) {
        @NotNull Optional<IOException> failure = moveOnce(beside, path);
        for (int attempt = 1; attempt < MOVE_ATTEMPTS && isHeldByAReader(failure); attempt++) {
            TestinLogger.debug("Writing " + path.getFileName() + " waits for a reader to let go of it, attempt " + attempt);
            TimeoutUtil.sleep(MOVE_PAUSE_MILLIS * attempt);
            failure = moveOnce(beside, path);
        }

        failure.ifPresent(ex -> reportWriteFailure(path, ex));
        return failure.isEmpty();
    }

    private static @NotNull Optional<IOException> moveOnce(final @NotNull Path beside, final @NotNull Path path) {
        try {
            Files.move(beside, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return Optional.empty();
        } catch (final AtomicMoveNotSupportedException notOnThisFileSystem) {
            return movePlainly(beside, path);
        } catch (final IOException ex) {
            return Optional.of(ex);
        }
    }

    private static @NotNull Optional<IOException> movePlainly(final @NotNull Path beside, final @NotNull Path path) {
        try {
            Files.move(beside, path, StandardCopyOption.REPLACE_EXISTING);
            return Optional.empty();
        } catch (final IOException ex) {
            return Optional.of(ex);
        }
    }

    private static boolean isHeldByAReader(final @NotNull Optional<IOException> failure) {
        return failure.filter(FileSystemException.class::isInstance).isPresent();
    }

    // UC-INTERNAL-005, Rule-INTERNAL-036, Rule-INTERNAL-113
    boolean delete(final @NotNull Path path) {
        try {
            ownWrites.record(project, path);

            if (!Trash.accepted(project, path)) Files.deleteIfExists(path);
        } catch (final IOException ex) {
            reportRemoveFailure(path, ex);
            return false;
        }

        return true;
    }

    // Rule-INTERNAL-113
    boolean discard(final @NotNull Path path) {
        try {
            ownWrites.record(project, path);

            Files.deleteIfExists(path);
        } catch (final IOException ex) {
            reportRemoveFailure(path, ex);
            return false;
        }

        return true;
    }

    private void reportRemoveFailure(final @NotNull Path path, final @NotNull IOException ex) {
        Services.getInstance(project, Notifier.class).error(project, Bundle.message("files.unable.to.remove", FailureText.of(ex)));
        TestinLogger.error("unable to remove " + path + ": " + FailureText.of(ex));
    }

    private void reportWriteFailure(final @NotNull Path path, final @NotNull IOException ex) {
        Services.getInstance(project, Notifier.class).error(project, Bundle.message("files.unable.to.write", FailureText.of(ex)));
        TestinLogger.error("unable to write content: " + FailureText.of(ex));
        TestinLogger.error("path" + path);
    }
}
