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
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.FileKind;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItem;
import org.testin.services.Services;
import org.testin.util.FailureText;
import org.testin.util.JsonCodec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

final class RunItemWriter {
    private final @NotNull IndexerDataStore store;
    private final @NotNull TestDataFiles files;
    private final @NotNull JsonCodec mapper;

    private final @NotNull ExecutorService queue =
            AppExecutorUtil.createBoundedApplicationPoolExecutor("Testin Run Item Writer", 1);

    private final @NotNull Map<Path, byte[]> unwritten = new ConcurrentHashMap<>();

    RunItemWriter(final @NotNull Project project, final @NotNull IndexerDataStore store) {
        this.store = store;
        this.files = Services.getInstance(project, TestDataFiles.class);
        this.mapper = Services.getInstance(project, JsonCodec.class);
    }

    private static @NotNull Set<String> namedScreenshots(final @NotNull RunItems runItems) {
        return runItems.getAll().stream()
                .flatMap(runItem -> runItem.allScreenshots().stream())
                .collect(Collectors.toSet());
    }

    void create(final @NotNull Path testRunPath, final @NotNull RunItems runItems) {
        store.registerRunItems(testRunPath, runItems);
        write(testRunPath, runItems, runItems.getAll(), Set.of());
    }

    void persist(final @NotNull Path testRunPath, final @NotNull RunItems runItems, final @NotNull Set<UUID> gone) {
        if (store.findRunItems(testRunPath).isEmpty()) {
            TestinLogger.info("Test run no longer indexed, so it was not written back: " + testRunPath.getFileName());
            return;
        }

        store.registerRunItems(testRunPath, runItems);
        write(testRunPath, runItems, runItems.getAll(), gone);
    }

    // Rule-INTERNAL-011
    void persistRunItem(final @NotNull Path testRunPath, final @NotNull RunItems runItems, final @NotNull RunItem runItem) {
        write(testRunPath, runItems, List.of(runItem), Set.of());
    }

    // Rule-INTERNAL-011
    private void write(final @NotNull Path testRunPath, final @NotNull RunItems runItems, final @NotNull List<RunItem> changed, final @NotNull Set<UUID> gone) {
        final @NotNull Set<String> named = namedScreenshots(runItems);

        final @NotNull Map<Path, byte[]> snapshots = new LinkedHashMap<>();
        for (final RunItem runItem : changed) {
            snapshot(runItem).ifPresent(bytes -> snapshots.put(testRunPath.resolve(FileKind.RUN_ITEM.fileName(runItem.getId())), bytes));
        }

        queue.execute(() -> {
            try {
                if (store.findRunItems(testRunPath).isEmpty()) {
                    TestinLogger.info("Test run removed before its run items were written, so nothing was written: " + testRunPath.getFileName());
                    return;
                }

                snapshots.forEach((file, bytes) -> {
                    if (files.alreadyHolds(file, bytes)) return;

                    files.write(file, bytes);
                    TestinLogger.trace("Run item written for " + testRunPath.getFileName() + ": " + file.getFileName());
                });

                removeRunItemsOf(testRunPath, gone);
                sweepScreenshots(testRunPath, named);
            } catch (final Exception ex) {
                TestinLogger.error("Failed to persist test run data: " + FailureText.of(ex));
            }
        });
    }

    // UC-TREE-PANEL-022, Rule-INTERNAL-011
    private void removeRunItemsOf(final @NotNull Path testRunPath, final @NotNull Set<UUID> gone) {
        for (final UUID testCaseId : gone) {
            final @NotNull Path file = testRunPath.resolve(FileKind.RUN_ITEM.fileName(testCaseId));
            if (!Files.exists(file)) continue;

            TestinLogger.info("Removing the run item of a test case the test run no longer covers: " + file.getFileName());
            files.delete(file);
        }
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    private void sweepScreenshots(final @NotNull Path testRunPath, final @NotNull Set<String> named) {
        files.screenshotsIn(testRunPath).stream()
                .filter(file -> !named.contains(file.getFileName().toString()))
                .forEach(files::delete);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    @NotNull List<String> storeScreenshots(final @NotNull Path testRunPath, final @NotNull List<byte[]> pngs) {
        final @NotNull Set<String> taken = new HashSet<>(store.findRunItems(testRunPath).map(RunItemWriter::namedScreenshots).orElse(Set.of()));
        final @NotNull Map<String, byte[]> byName = new LinkedHashMap<>();
        for (final byte[] png : pngs) {
            final @NotNull String name = TestRunNode.newScreenshotName(taken);
            taken.add(name);
            byName.put(name, png);
        }

        byName.forEach((name, png) -> unwritten.put(TestRunNode.screenshotFile(testRunPath, name), png));

        if (!byName.isEmpty()) queue.execute(() -> {
            try {
                if (store.findRunItems(testRunPath).isEmpty()) {
                    TestinLogger.info("Test run removed before its screenshots were written, so nothing was written: " + testRunPath.getFileName());
                    return;
                }

                byName.forEach((name, png) -> files.write(TestRunNode.screenshotFile(testRunPath, name), png));
            } catch (final Exception ex) {
                TestinLogger.error("Failed to write the screenshots of " + testRunPath.getFileName() + ": " + FailureText.of(ex));
            } finally {
                byName.forEach((name, png) -> unwritten.remove(TestRunNode.screenshotFile(testRunPath, name), png));
            }
        });

        return List.copyOf(byName.keySet());
    }

    byte @NotNull [] readScreenshot(final @NotNull Path testRunPath, final @NotNull String name) {
        if (!TestRunNode.isScreenshotName(name)) return new byte[0];

        final @NotNull Path file = TestRunNode.screenshotFile(testRunPath, name);
        return Optional.ofNullable(unwritten.get(file)).orElseGet(() -> files.readBytes(file));
    }

    // Rule-INTERNAL-090
    // Rule-INTERNAL-123
    void persistMarker(final @NotNull Path testRunPath, final @NotNull Runnable onFailed) {
        queue.execute(() -> {
            try {
                if (store.findRunItems(testRunPath).isEmpty()) {
                    TestinLogger.info("Test run removed before its marker was written, so nothing was written: " + testRunPath.getFileName());
                    return;
                }

                if (store.persistTestRunMarker(testRunPath)) {
                    TestinLogger.trace("Marker persisted for " + testRunPath.getFileName());
                    return;
                }
                TestinLogger.warn("The marker of " + testRunPath.getFileName() + " was not written");
            } catch (final Exception ex) {
                TestinLogger.error("Failed to persist marker: " + FailureText.of(ex));
            }
            onFailed.run();
        });
    }

    void awaitWrites() {
        try {
            queue.submit(() -> {
            }).get(10, TimeUnit.SECONDS);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while the test run writer was writing", ex);
        } catch (final ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("The test run writer did not finish: " + FailureText.of(ex), ex);
        }
    }

    private @NotNull Optional<byte[]> snapshot(final @NotNull RunItem runItem) {
        try {
            return Optional.of(mapper.writeValueAsBytes(runItem));
        } catch (final Exception ex) {
            TestinLogger.error("Failed to snapshot run item: " + FailureText.of(ex));
            return Optional.empty();
        }
    }
}
