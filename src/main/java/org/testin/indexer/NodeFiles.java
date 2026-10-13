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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.NodeType;
import org.testin.model.FileKind;
import org.testin.model.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.JsonCodec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

final class NodeFiles {
    private final @NotNull Project project;
    private final @NotNull ProjectIndexer indexer;
    private final @NotNull IndexerDataStore store;
    private final @NotNull VfsExecutor vfs;
    private final @NotNull JsonCodec mapper;
    private final @NotNull TestDataFiles testDataFiles;
    private final @NotNull OwnWrites ownWrites;

    NodeFiles(final @NotNull Project project, final @NotNull ProjectIndexer indexer, final @NotNull IndexerDataStore store) {
        this.project = project;
        this.indexer = indexer;
        this.store = store;
        this.vfs = Services.getInstance(project, VfsExecutor.class);
        this.mapper = Services.getInstance(project, JsonCodec.class);
        this.testDataFiles = Services.getInstance(project, TestDataFiles.class);
        this.ownWrites = Services.getInstance(OwnWrites.class);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-112
    void remove(final @NotNull Path path, final @NotNull Runnable cacheUpdate, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        final @NotNull Path folder = Optional.ofNullable(path.getParent()).orElse(path);

        vfs.removeFile(indexer, path,
                deleted -> ApplicationManager.getApplication().executeOnPooledThread(() ->
                        LocalFileSystem.getInstance().refreshNioFiles(List.of(folder), true, false, () -> {
                            if (deleted) cacheUpdate.run();
                            onRemoved.accept(deleted);
                        })));
    }

    void move(final @NotNull Path oldPath, final @NotNull Path newPath, final @NotNull Consumer<@NotNull Boolean> onFinished) {
        final @NotNull Optional<Path> found = Optional.ofNullable(newPath.getParent());
        if (found.isEmpty()) {
            TestinLogger.warn("Move refused, target has no parent directory: " + newPath);
            onFinished.accept(false);
            return;
        }

        final @NotNull Path targetParent = found.orElseThrow();

        vfs.executeVfsAction(oldPath, targetParent, Bundle.message("vfs.move.failed.title"), (sourceFile, targetFile) -> {
            try {
                sourceFile.move(indexer, targetFile);
            } catch (final IOException ex) {
                TestinLogger.error(FailureText.of(ex));
                throw new RuntimeException(ex);
            }
        }, () -> followOnDisk(oldPath, newPath, () -> {
            TestinLogger.info("Moved successfully to: " + newPath);
            onFinished.accept(true);
        }), () -> onFinished.accept(false));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-112
    private void followOnDisk(final @NotNull Path oldPath, final @NotNull Path newPath, final @NotNull Runnable then) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            store.renameNode(oldPath, newPath);
            ApplicationManager.getApplication().invokeLater(then);
        });
    }

    // Rule-INTERNAL-113
    void copy(final @NotNull List<Path> sourcePaths, final @NotNull Path targetPath, final @NotNull IntConsumer onComplete) {
        if (sourcePaths.isEmpty()) {
            onComplete.accept(0);
            return;
        }

        final @NotNull AtomicInteger pending = new AtomicInteger(sourcePaths.size());
        final @NotNull AtomicInteger copied = new AtomicInteger();

        final @NotNull List<Path> arrived = new CopyOnWriteArrayList<>();

        final @NotNull Runnable operationFinished = () -> {
            if (pending.decrementAndGet() != 0) return;
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                arrived.forEach(this::reidentifyCopiedTestCases);

                indexer.refreshIndexedProject(targetPath);
                ApplicationManager.getApplication().invokeLater(() -> onComplete.accept(copied.get()));
            });
        };
        final @NotNull Runnable operationSucceeded = () -> {
            copied.incrementAndGet();
            operationFinished.run();
        };

        for (final Path sourcePath : sourcePaths) {
            final @NotNull Path copiedRoot = targetPath.resolve(sourcePath.getFileName());

            final @NotNull Runnable copySucceeded = () -> {
                arrived.add(copiedRoot);
                operationSucceeded.run();
            };

            ownWrites.record(project, copiedRoot);
            vfs.executeVfsAction(sourcePath, targetPath, Bundle.message("vfs.copy.failed.title"), (sourceFile, targetFile) -> {
                try {
                    sourceFile.copy(indexer, targetFile, sourceFile.getName());
                } catch (final IOException ex) {
                    TestinLogger.error(FailureText.of(ex));
                    throw new RuntimeException(ex);
                }
            }, copySucceeded, operationFinished);
        }
    }

    void rename(final @NotNull Path oldPath, final @NotNull Path newPath, final @NotNull Runnable onFinished, final @NotNull Runnable onFailed) {
        vfs.executeVfsAction(oldPath, file -> {
            try {
                file.rename(indexer, newPath.getFileName().toString());
            } catch (final IOException ex) {
                TestinLogger.error(FailureText.of(ex));
                throw new RuntimeException(ex);
            }

            followOnDisk(oldPath, newPath, onFinished);
        }, onFailed);
    }

    // UC-TREE-PANEL-014, Rule-TREE-PANEL-051
    private void reidentifyCopiedTestCases(final @NotNull Path copiedRoot) {
        final List<Path> testCaseFiles;
        final List<Path> markerFiles;

        try (Stream<Path> files = Files.walk(copiedRoot)) {
            final @NotNull List<Path> all = files.filter(Files::isRegularFile).toList();

            testCaseFiles = all.stream()
                    .filter(file -> ProjectIndexer.isTestCaseFile(file, node -> store.hasMarker(node, NodeType.TEST_SET)))
                    .toList();

            markerFiles = all.stream()
                    .filter(file -> NodeType.byMarker(String.valueOf(file.getFileName())).isPresent())
                    .toList();

        } catch (final IOException ex) {
            TestinLogger.error("Could not read the copied nodes at " + copiedRoot + ": " + FailureText.of(ex));
            return;
        }

        final long given = testCaseFiles.stream().filter(this::reidentify).count();
        TestinLogger.info("Gave " + given + " of " + testCaseFiles.size() + " copied test case(s) new ids under " + copiedRoot.getFileName());

        // Rule-INTERNAL-090
        final long folders = markerFiles.stream().filter(store::giveFreshMarkerId).count();
        TestinLogger.info("Gave " + folders + " of " + markerFiles.size() + " copied folder(s) ids of their own under " + copiedRoot.getFileName());
    }

    // UC-TREE-PANEL-014, Rule-TREE-PANEL-051, Rule-INTERNAL-113
    private boolean reidentify(final @NotNull Path testCaseFile) {
        try {
            final @NotNull TestCaseDto testCase = mapper.readValue(Files.readAllBytes(testCaseFile), TestCaseDto.class);
            final @NotNull UUID fresh = UUID.randomUUID();

            testCase.setId(fresh);
            if (!testDataFiles.write(testCaseFile.resolveSibling(FileKind.TEST_CASE.fileName(fresh)), testCase))
                return false;

            ownWrites.record(project, testCaseFile);
            Files.delete(testCaseFile);
            return true;

        } catch (final IOException | UncheckedIOException ex) {
            TestinLogger.error("Could not give the copied test case " + testCaseFile.getFileName() + " a new id: " + FailureText.of(ex));
            return false;
        }
    }
}
