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
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.open.LastOpenEditors;
import org.testin.logger.TestinLogger;
import org.testin.model.FileKind;
import org.testin.model.NodeType;
import org.testin.model.status.TestProjectStatus;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Service(Service.Level.PROJECT)
public final class TestProjectIndexer {
    private final @NotNull Project project;

    @Getter(AccessLevel.PACKAGE)
    private final @NotNull IndexerDataStore store;

    private final @NotNull ReadProblems problems;

    @Getter(AccessLevel.PACKAGE)
    private final @NotNull TestProjectScanCoordinator scanCoordinator;

    private final @NotNull AtomicBoolean indexed = new AtomicBoolean(false);

    private final @NotNull AtomicBoolean indexing = new AtomicBoolean(false);

    private final @NotNull AtomicBoolean restoreEditorsOnComplete = new AtomicBoolean(true);

    @Getter(AccessLevel.PACKAGE)
    private final @NotNull RunItemWriter runItemWriter;

    @Getter(AccessLevel.PACKAGE)
    private final @NotNull NodeFiles nodeFiles;

    private final @NotNull TestinRoot testinRoot;

    private final @NotNull Rescan rescan;

    private final @NotNull BoundTestProject boundTestProject;

    private final @NotNull NodeMapper directoryMapper;

    private final @NotNull LastOpenEditors lastOpenEditors;

    private volatile @NotNull CountDownLatch indexingLatch = new CountDownLatch(1);

    private volatile @NotNull CountDownLatch testCasesLatch = new CountDownLatch(1);

    public TestProjectIndexer(final @NotNull Project project) {
        this.project = project;
        this.testinRoot = Services.getInstance(project, TestinRoot.class);
        this.rescan = Services.getInstance(Rescan.class);
        this.boundTestProject = Services.getInstance(project, BoundTestProject.class);
        this.directoryMapper = Services.getInstance(project, NodeMapper.class);
        this.lastOpenEditors = Services.getInstance(project, LastOpenEditors.class);
        this.store = new IndexerDataStore(project);
        this.problems = new ReadProblems(project, store);
        this.scanCoordinator = new TestProjectScanCoordinator(new IndexingScanner(project, store, this::testCasesRead));
        this.runItemWriter = new RunItemWriter(project, store);
        this.nodeFiles = new NodeFiles(project, this, store);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-011, Rule-INTERNAL-012
    static boolean isTestCaseFile(final @NotNull Path file, final @NotNull Predicate<Path> isTestSet) {
        return FileKind.TEST_CASE.uuidIn(file).isPresent() && isTestSet.test(file.getParent());
    }

    // Rule-INTERNAL-111
    private static void waitFor(final @NotNull CountDownLatch latch) {
        if (ApplicationManager.getApplication().isReadAccessAllowed()) {
            TestinLogger.error("Waiting for the index while holding a read action - "
                    + "this blocks every write action in the IDE. Returning without waiting.");
            return;
        }

        try {
            latch.await();
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-013
    public void indexWithProgress() {
        try {
            if (indexed.get() || indexing.getAndSet(true)) {
                return;
            }

            final @NotNull Path absoluteRoot = absoluteRoot();
            if (absoluteRoot.toString().isEmpty()) {
                finishWithNothingToIndex();
                return;
            }

            final @NotNull List<Path> validTestProjects = boundOnly(collectValidTestProjects(absoluteRoot));
            if (validTestProjects.isEmpty()) {
                finishWithNothingToIndex();
                TestinLogger.warn("No valid test projects found at '" + absoluteRoot.toAbsolutePath() + "'");
                return;
            }

            final @NotNull AtomicInteger testProjectsLeft = new AtomicInteger(validTestProjects.size());
            final @NotNull AtomicBoolean allRead = new AtomicBoolean(true);
            final @NotNull CountDownLatch passLatch = indexingLatch;
            TestinLogger.info("Indexing " + validTestProjects.size() + " test projects..");

            for (final Path testProjectPath : validTestProjects)
                indexInBackground(testProjectPath, testProjectsLeft, allRead, passLatch);
        } catch (final Exception ex) {
            TestinLogger.error("indexWithProgress: " + FailureText.of(ex));
            indexing.set(false);

            testCasesLatch.countDown();
            indexingLatch.countDown();
        }
    }

    private void finishWithNothingToIndex() {
        indexing.set(false);
        indexed.set(true);
        testCasesLatch.countDown();
        indexingLatch.countDown();
    }

    // UC-INTERNAL-002, Rule-INTERNAL-141
    private void testCasesRead() {
        testCasesLatch.countDown();
        if (!project.isDisposed()) project.getMessageBus().syncPublisher(IndexChanged.TOPIC).testCasesRead();
    }

    // UC-INTERNAL-002, Rule-INTERNAL-013, Rule-INTERNAL-124
    private void indexInBackground(final @NotNull Path testProjectPath, final @NotNull AtomicInteger testProjectsLeft, final @NotNull AtomicBoolean allRead, final @NotNull CountDownLatch passLatch) {
        final @NotNull String testProjectName = testProjectPath.getFileName().toString();

        BackgroundWork.start(new Task.Backgroundable(project, Bundle.message("indexer.task.title", testProjectName), true) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setFraction(0.0);
                indicator.setText(Bundle.message("indexer.progress.indexing", testProjectName));

                final long started = System.nanoTime();
                final @NotNull Optional<String> failure = scanCoordinator.scan(testProjectPath, indicator);
                TestinLogger.info("Reading '" + testProjectName + "' took " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) + " ms");
                failure.ifPresent(why -> {
                    allRead.set(false);
                    problems.testProjectNotRead(testProjectName, why);
                });

                indicator.setFraction(1.0);
                indicator.setText(Bundle.message("indexer.progress.done", testProjectName));
            }

            @Override
            public void onCancel() {
                TestinLogger.info("Reading '" + testProjectName + "' was canceled, so it is read again on the next request.");
                allRead.set(false);
            }

            @Override
            public void onThrowable(final @NotNull Throwable error) {
                TestinLogger.error("Error indexing '" + testProjectName + "': " + FailureText.of(error));
                allRead.set(false);
            }

            @Override
            public void onFinished() {
                if (!oneTestProjectFinished(testProjectsLeft, passLatch)) return;

                if (allRead.get()) finishSuccessfully();
                else finishWithFailure();
            }
        });
    }

    private boolean oneTestProjectFinished(final @NotNull AtomicInteger testProjectsLeft, final @NotNull CountDownLatch passLatch) {
        if (testProjectsLeft.decrementAndGet() != 0) return false;

        passLatch.countDown();
        if (!passLatch.equals(indexingLatch)) return false;

        testCasesLatch.countDown();
        return true;
    }

    private void finishSuccessfully() {
        if (!indexed.compareAndSet(false, true)) return;

        indexing.set(false);
        logSummary();
        restoreOpenEditorsOnce();
        announceIndexed();
    }

    // UC-INTERNAL-002, Rule-EDITOR-PANEL-239
    private void announceIndexed() {
        if (!project.isDisposed()) project.getMessageBus().syncPublisher(IndexChanged.TOPIC).indexed();
    }

    // UC-INTERNAL-002
    private void finishWithFailure() {
        indexing.set(false);
        TestinLogger.info("Indexing ended before every test project was read, so they are read again on the next request.");
    }

    private void restoreOpenEditorsOnce() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (restoreEditorsOnComplete.getAndSet(false)) {
                TestinLogger.info("Indexing finished, restoring open editors.");
                lastOpenEditors.reopen();
            } else {
                TestinLogger.info("Indexing finished, skipping editor restore.");
            }
        });
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-118
    public boolean isIndexed() {
        return indexed.get();
    }

    // UC-INTERNAL-002, Rule-INTERNAL-111, Rule-INTERNAL-115
    public void awaitIndexing() {
        if (indexed.get()) return;

        indexWithProgress();
        waitFor(indexingLatch);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-141
    public void awaitTestCases() {
        if (indexed.get()) return;

        indexWithProgress();
        waitFor(testCasesLatch);
    }

    public void resetForReindex() {
        scanCoordinator.exclusively(() -> {
            restoreEditorsOnComplete.set(false);
            store.clearAll();
            indexed.set(false);
            indexing.set(false);
            testCasesLatch = new CountDownLatch(1);
            indexingLatch = new CountDownLatch(1);
            TestinLogger.info("Indexer reset for re-indexing");
        });
    }

    private @NotNull Path absoluteRoot() {
        return testinRoot.absolutePath();
    }

    // UC-INTERNAL-002, Rule-INTERNAL-114
    void announce(final @NotNull Path changed) {
        if (project.isDisposed()) return;

        final @NotNull IndexChanged listeners = project.getMessageBus().syncPublisher(IndexChanged.TOPIC);
        if (absoluteRoot().equals(changed.getParent())) listeners.testProjectsChanged();
        else listeners.nodesChanged(Set.of(changed.getParent()));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-114
    boolean announcedIf(final boolean changed, final @NotNull Path path) {
        if (changed) announce(path);
        return changed;
    }

    // UC-INTERNAL-002, Rule-INTERNAL-114
    void announceChildrenOf(final @NotNull Path folder) {
        if (!project.isDisposed())
            project.getMessageBus().syncPublisher(IndexChanged.TOPIC).nodesChanged(Set.of(folder));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-114
    private void announceReadAgain() {
        if (!project.isDisposed()) project.getMessageBus().syncPublisher(IndexChanged.TOPIC).readAgain();
    }

    // UC-INTERNAL-002, Rule-INTERNAL-006
    private @NotNull List<Path> boundOnly(final @NotNull List<Path> testProjectPaths) {
        final @NotNull String bound = boundTestProject.name();
        if (bound.isEmpty()) return testProjectPaths;

        final @NotNull List<Path> scoped = testProjectPaths.stream()
                .filter(this::isBound)
                .toList();

        if (scoped.isEmpty()) {
            TestinLogger.warn("This repository's test project '" + bound + "' is not a test project under the root");
            return scoped;
        }

        TestinLogger.info("Indexing only the bound project '" + bound + "'");
        return scoped;
    }

    private boolean isBound(final @NotNull Path testProjectPath) {
        final @NotNull String bound = boundTestProject.name();
        return bound.isEmpty() || bound.equals(testProjectPath.getFileName().toString());
    }

    public @NotNull Map<String, TestProjectStatus> testProjects() {
        final @NotNull Map<String, TestProjectStatus> byName = new LinkedHashMap<>();
        final @NotNull Path root = absoluteRoot();
        if (root.toString().isEmpty()) return byName;

        for (final Path path : collectValidTestProjects(root)) {
            byName.put(path.getFileName().toString(), directoryMapper.getTestProjectNode(path).getMarker().getStatus());
        }

        return byName;
    }

    // UC-INTERNAL-002, Rule-INTERNAL-003, Rule-INTERNAL-004, Rule-TREE-PANEL-016, Rule-SETTING-010
    private @NotNull List<Path> collectValidTestProjects(final @NotNull Path rootPath) {
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) return List.of();

        final Path[] testProjectPaths;
        try (Stream<Path> directories = Files.list(rootPath)) {
            testProjectPaths = directories.filter(Files::isDirectory).toArray(Path[]::new);
        } catch (final IOException | UncheckedIOException ex) {
            TestinLogger.error("Failed to list root directory: " + FailureText.of(ex));
            return List.of();
        }

        return Arrays.stream(testProjectPaths).filter(this::isListedTestProject).toList();
    }

    private boolean isListedTestProject(final @NotNull Path folder) {
        final boolean marked = isTestProjectFolder(folder);
        if (!marked)
            TestinLogger.warn("Skipping directory without a " + NodeType.TEST_PROJECT.getMarker() + " marker (not a test project): " + folder);
        return marked;
    }

    private boolean isTestProjectFolder(final @NotNull Path folder) {
        return store.hasMarker(folder, NodeType.TEST_PROJECT);
    }

    private void logSummary() {
        TestinLogger.info("Indexing complete: " +
                store.getTestCasesByUuid().size() + " test cases, " +
                store.getRunItemsByPath().size() + " test runs, " +
                store.getTestProjectsByPath().size() + " test projects, " +
                store.getTestSetNodesByPath().size() + " test sets, " +
                store.getTestRunNodesByPath().size() + " test run dirs, " +
                store.getTestSetPackagesByPath().size() + " test set packages, " +
                store.getTestRunPackagesByPath().size() + " test run packages");
    }

    void refreshIndexedTestProject(final @NotNull Path changedPath) {
        testProjectHolding(changedPath).ifPresent(scanCoordinator::rescanExclusively);
    }

    @NotNull Optional<Path> testProjectHolding(final @NotNull Path path) {
        return store.getTestProjectsByPath().keySet().stream()
                .map(Path::of)
                .filter(path::startsWith)
                .max(Comparator.comparingInt(Path::getNameCount));
    }

    // UC-INTERNAL-003, Rule-INTERNAL-016
    public void rescanChangedTestProject(final @NotNull Path testProjectPath, final @NotNull ProgressIndicator indicator) {
        if (isTestProjectFolder(testProjectPath) && isBound(testProjectPath)) {
            scanSingleTestProject(testProjectPath, indicator);
            return;
        }

        TestinLogger.info("Not a test project Testin reads, so not scanned: " + testProjectPath);
        store.removeTestProject(testProjectPath);
        store.invalidateChildrenIndex();
        announceReadAgain();
    }

    public void scanSingleTestProject(final @NotNull Path testProjectPath) {
        scanSingleTestProject(testProjectPath, new EmptyProgressIndicator());
    }

    // UC-SHARE-016, Rule-SHARE-073, Rule-INTERNAL-013
    public void readAgainInBackground(final @NotNull Path testProjectPath) {
        final @NotNull String testProjectName = testProjectPath.getFileName().toString();

        BackgroundWork.start(new Task.Backgroundable(project, Bundle.message("indexer.task.title", testProjectName), true) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setText(Bundle.message("indexer.progress.indexing", testProjectName));
                scanSingleTestProject(testProjectPath, indicator);
            }
        });
    }

    // UC-INTERNAL-003, Rule-INTERNAL-021
    public void scanSingleTestProject(final @NotNull Path testProjectPath, final @NotNull ProgressIndicator indicator) {
        TestinLogger.info("Scanning single project: " + testProjectPath.getFileName());
        rescan.coveredByAScan(testProjectPath);
        scanCoordinator.scan(testProjectPath, indicator).ifPresent(why -> problems.testProjectNotRead(testProjectPath.getFileName().toString(), why));

        announceReadAgain();
    }

    // Rule-INTERNAL-091
    public @NotNull Optional<String> whyNotRead(final @NotNull Path testProjectPath) {
        return store.whyNotRead(testProjectPath);
    }
}
