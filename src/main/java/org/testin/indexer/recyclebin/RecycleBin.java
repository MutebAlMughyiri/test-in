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

package org.testin.indexer.recyclebin;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.testin.logger.TestinLogger;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecycleBin {
    private static final @NotNull AtomicReference<Predicate<File>> BIN = new AtomicReference<>(RecycleBin::desktopBinTakes);

    // UC-INTERNAL-005, Rule-INTERNAL-036, Rule-INTERNAL-143
    public static boolean took(final @NotNull Project project, final @NotNull Path path) {
        if (!Files.exists(path)) return true;
        if (took(path, Objects.requireNonNull(BIN.get()))) return true;

        Services.getInstance(project, Notifier.class).error(project, Bundle.message("recycle.bin.refused.title"), Bundle.message("recycle.bin.refused.message", path.getFileName()));
        return false;
    }

    // UC-CODEGEN-018, Rule-CODEGEN-059, Rule-INTERNAL-143
    public static boolean took(final @NotNull Project project, final @NotNull VirtualFile file) {
        final @NotNull Optional<Path> onDisk = Optional.ofNullable(file.getFileSystem().getNioPath(file));
        if (onDisk.isEmpty()) return removedFromMemory(file);
        if (!took(project, onDisk.orElseThrow())) return false;

        Optional.ofNullable(file.getParent()).ifPresent(parent -> parent.refresh(false, true));
        return true;
    }

    // UC-INTERNAL-005, Rule-INTERNAL-036, Rule-INTERNAL-143
    static boolean took(final @NotNull Path path, final @NotNull Predicate<File> bin) {
        try {
            if (bin.test(path.toFile())) return true;

            TestinLogger.warn("The recycle bin did not take " + path + ", so it was left where it is.");
        } catch (final Exception ex) {
            TestinLogger.warn("The recycle bin did not take " + path + ", so it was left where it is: " + FailureText.of(ex));
        }
        return false;
    }

    @TestOnly
    public static void standIn(final @NotNull Disposable owner, final @NotNull Predicate<File> bin) {
        final @NotNull Predicate<File> before = Objects.requireNonNull(BIN.getAndSet(bin));
        Disposer.register(owner, () -> BIN.set(before));
    }

    private static boolean removedFromMemory(final @NotNull VirtualFile file) {
        try {
            file.delete(RecycleBin.class);
            return true;
        } catch (final IOException ex) {
            TestinLogger.warn("Could not remove " + file.getPath() + ", which is on no disk: " + FailureText.of(ex));
            return false;
        }
    }

    private static boolean desktopBinTakes(final @NotNull File file) {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH) && Desktop.getDesktop().moveToTrash(file);
    }
}
