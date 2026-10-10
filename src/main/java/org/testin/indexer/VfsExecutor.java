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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.ThreadingAssertions;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
final class VfsExecutor {
    private final @NotNull Project p;

    private static @NotNull Optional<VirtualFile> find(final @NotNull Path path) {
        ThreadingAssertions.assertBackgroundThread();
        return Optional.ofNullable(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path));
    }

    // UC-INTERNAL-003, Rule-INTERNAL-019, Rule-INTERNAL-113
    private void claim(final @NotNull Path path) {
        Services.getInstance(OwnWrites.class).record(p, path);
    }

    void executeVfsAction(final @NotNull Path path, final @NotNull VfsOperation operation, final @NotNull Runnable onFailure) {
        final @NotNull String errorTitle = Bundle.message("vfs.rename.failed.title");
        claim(path);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Optional<VirtualFile> vf = find(path);

            ApplicationManager.getApplication().invokeLater(() -> vf.ifPresentOrElse(
                    file -> WriteAction.run(() -> {
                        try {
                            operation.execute(file);
                        } catch (final Exception ex) {
                            Services.getInstance(p, Notifier.class).error(p, errorTitle, Bundle.message("vfs.operation.failed", FailureText.of(ex)));
                            onFailure.run();
                        }
                    }),
                    () -> {
                        Services.getInstance(p, Notifier.class).error(p, errorTitle, Bundle.message("vfs.path.not.found", path));
                        onFailure.run();
                    }));
        });
    }

    void executeVfsAction(final @NotNull Path sourcePath, final @NotNull Path targetPath, final @NotNull String errorTitle, final @NotNull VfsBiOperation operation, final @NotNull Runnable onSuccess, final @NotNull Runnable onFailure) {
        claim(sourcePath);
        claim(targetPath);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Optional<VirtualFile> sourceVf = find(sourcePath);
            final @NotNull Optional<VirtualFile> targetVf = find(targetPath);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (sourceVf.isEmpty() || targetVf.isEmpty()) {
                    Services.getInstance(p, Notifier.class).error(p, errorTitle, Bundle.message("vfs.source.or.target.not.found"));
                    onFailure.run();
                    return;
                }

                WriteAction.run(() -> {
                    try {
                        operation.execute(sourceVf.get(), targetVf.get());
                        onSuccess.run();
                    } catch (final Exception ex) {
                        Services.getInstance(p, Notifier.class).error(p, errorTitle, Bundle.message("vfs.operation.failed", FailureText.of(ex)));
                        onFailure.run();
                    }
                });
            });
        });
    }

    // UC-INTERNAL-005, Rule-INTERNAL-036
    void removeVf(final @NotNull Object requester, final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onDeleted) {
        claim(path);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (Trash.accepted(p, path)) {
                ApplicationManager.getApplication().invokeLater(() -> onDeleted.accept(true));
                return;
            }

            final @NotNull Optional<VirtualFile> vf = find(path);

            ApplicationManager.getApplication().invokeLater(() -> {
                final @NotNull AtomicBoolean deleted = new AtomicBoolean(true);

                WriteAction.run(() -> {
                    try {
                        if (vf.isPresent()) vf.get().delete(requester);
                    } catch (final IOException ex) {
                        deleted.set(false);
                        Services.getInstance(p, Notifier.class).error(p, Bundle.message("vfs.delete.failed.title"), Bundle.message("vfs.delete.failed", FailureText.of(ex)));
                    }
                });

                onDeleted.accept(deleted.get());
            });
        });
    }
}
