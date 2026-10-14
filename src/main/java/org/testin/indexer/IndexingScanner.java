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

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.NodeType;
import org.testin.model.FileKind;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestCasesFolderNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.model.node.TestRunsFolderNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.model.testrun.RunItem;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.JsonCodec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

final class IndexingScanner {
    private final @NotNull IndexerDataStore store;

    private final @NotNull NodeMapper directoryMapper;

    private final @NotNull JsonCodec mapper;

    private final @NotNull ReadProblems problems;

    private final @NotNull TestDataFiles testDataFiles;

    IndexingScanner(final @NotNull Project project, final @NotNull IndexerDataStore store) {
        this.store = store;
        this.directoryMapper = Services.getInstance(project, NodeMapper.class);
        this.mapper = Services.getInstance(project, JsonCodec.class);
        this.problems = new ReadProblems(project, store);
        this.testDataFiles = Services.getInstance(project, TestDataFiles.class);
    }

    private static boolean looksLikeATestCaseFile(final @NotNull Path file) {
        return FileKind.TEST_CASE.uuidIn(file).isPresent();
    }

    // Rule-INTERNAL-091
    private static @NotNull ScannedTestProject scannedNode(final @NotNull Path testProjectPath, final @NotNull TestProjectNode testProject) {
        final @NotNull ScannedTestProject scanned = new ScannedTestProject();
        scanned.getTestProjects().put(testProjectPath.toString(), testProject);

        return scanned;
    }

    // Rule-INTERNAL-124
    @NotNull Optional<String> scanTestProject(final @NotNull Path testProjectPath, final @NotNull ProgressIndicator indicator) {
        try {
            return scanTestProjectContents(testProjectPath, indicator);
        } finally {
            store.invalidateChildrenIndex();
        }
    }

    void scanTestProject(final @NotNull Path testProjectPath) {
        try {
            scanTestProjectContents(testProjectPath, new EmptyProgressIndicator());
        } finally {
            store.invalidateChildrenIndex();
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-005, Rule-INTERNAL-007, Rule-INTERNAL-091
    private @NotNull Optional<String> scanTestProjectContents(final @NotNull Path testProjectPath, final @NotNull ProgressIndicator indicator) {
        try {
            final @NotNull TestProjectNode testProject = directoryMapper.getTestProjectNode(testProjectPath);

            // Rule-INTERNAL-091
            final @NotNull Optional<String> refused = testProject.getMarker().whyNotReadable();
            if (refused.isPresent()) {
                TestinLogger.warn("Not reading " + testProjectPath.getFileName() + ": " + refused.orElseThrow());

                store.refuse(testProjectPath, refused.orElseThrow());
                store.swapIn(testProjectPath, scannedNode(testProjectPath, testProject));
                indicator.setFraction(1.0);
                return Optional.empty();
            }

            // UC-INTERNAL-003, Rule-INTERNAL-021
            final @NotNull ScannedTestProject scanned = new ScannedTestProject();
            scanned.getTestProjects().put(testProjectPath.toString(), testProject);
            store.readable(testProjectPath);

            // UC-TREE-PANEL-001, Rule-TREE-PANEL-100
            if (!testProject.getMarker().getStatus().isActive()) {
                TestinLogger.info("Inactive test project, indexed without its contents: " + testProjectPath.getFileName());
                store.swapIn(testProjectPath, scanned);
                indicator.setFraction(1.0);
                return Optional.empty();
            }

            indicator.setFraction(0.1);
            indicator.setText(Bundle.message("indexer.progress.test.sets", testProject.getName()));

            final @NotNull List<Path> unread = new ArrayList<>();

            final @NotNull TestCasesFolderNode testCasesFolder = testProject.getTestCasesFolder();
            scanned.getTestCasesFolders().put(testCasesFolder.getPath().toString(), testCasesFolder);
            scanTestSets(testCasesFolder.getPath(), testCasesFolder, indicator, unread, scanned);

            indicator.setFraction(0.5);
            indicator.setText(Bundle.message("indexer.progress.test.runs", testProject.getName()));

            final @NotNull TestRunsFolderNode testRunsFolder = testProject.getTestRunsFolder();
            scanned.getTestRunsFolders().put(testRunsFolder.getPath().toString(), testRunsFolder);
            scanTestRunNodes(testRunsFolder.getPath(), testRunsFolder, indicator, unread, scanned);

            if (indicator.isCanceled()) {
                TestinLogger.info("Scan canceled, so the index was left as it was: " + testProjectPath.getFileName());
                return Optional.empty();
            }

            store.swapIn(testProjectPath, scanned);

            indicator.setFraction(1.0);
            indicator.setText(Bundle.message("indexer.progress.test.project.done", testProject.getName()));

            problems.unreadFolders(testProject.getName(), unread);
            problems.damagedMarkers(testProject.getName(), store.takeDamagedMarkers(testProjectPath));
            problems.unreadableRunItems(testProject.getName(), scanned.getUnreadableRunItems());
            problems.handNamedTestCases(testProject.getName(), scanned.getHandNamedTestCases());
            problems.handNamedRunItems(testProject.getName(), scanned.getHandNamedRunItems());
            problems.clashingTestCases(testProject.getName(), List.copyOf(scanned.getClashingTestCases()));
            return Optional.empty();
        } catch (final Exception ex) {
            TestinLogger.error("Failed to scan project: " + testProjectPath.getFileName() + " - " + FailureText.of(ex));
            return Optional.of(FailureText.of(ex));
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-008, Rule-INTERNAL-015
    private void scanTestSets(final @NotNull Path testCaseDirectory, final @NotNull AbstractNode parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedTestProject scanned) {
        try (Stream<Path> paths = Files.list(testCaseDirectory)) {
            final @NotNull List<Path> directories = paths.filter(Files::isDirectory).toList();

            for (final Path directory : directories) {
                if (indicator.isCanceled()) return;

                scanTestSetOrPackage(directory, parent, indicator, unread, scanned);
            }
        } catch (final NoSuchFileException absent) {
            TestinLogger.info("No test sets to read: " + testCaseDirectory);
        } catch (final IOException ex) {
            throw new UncheckedIOException("the folder " + testCaseDirectory.getFileName() + " could not be listed: " + FailureText.of(ex), ex);
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-008, Rule-INTERNAL-015
    private void scanTestSetOrPackage(final @NotNull Path directory, final @NotNull AbstractNode parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedTestProject scanned) {
        store.markedAs(directory, NodeType.UNDER_TEST_CASES).ifPresentOrElse(
                marked -> {
                    if (marked == NodeType.TEST_SET) scanTestSet(directory, parent, indicator, scanned);
                    else scanTestSetPackage(directory, parent, indicator, unread, scanned);
                },
                () -> skipped(directory, NodeType.UNDER_TEST_CASES, unread));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-008, Rule-INTERNAL-015
    private void scanTestSetPackage(final @NotNull Path path, final @NotNull AbstractNode parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedTestProject scanned) {
        try {
            final @NotNull TestSetPackageNode testSetPackage = directoryMapper.getTestSetPackageNode(path, parent);

            scanned.getTestSetPackages().put(path.toString(), testSetPackage);

            try (Stream<Path> subPaths = Files.list(path)) {
                subPaths.filter(Files::isDirectory)
                        .forEach(subPath -> scanTestSetOrPackage(subPath, testSetPackage, indicator, unread, scanned));
            }

        } catch (final IOException | UncheckedIOException ex) {
            TestinLogger.error("Failed to scan test set package: " + path.getFileName() + ": " + FailureText.of(ex));
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-011
    private void scanTestSet(final @NotNull Path path, final @NotNull AbstractNode parent, final @NotNull ProgressIndicator indicator, final @NotNull ScannedTestProject scanned) {
        try {
            final @NotNull TestSetNode testSet = directoryMapper.getTestSetNode(path, parent);

            scanned.getTestSets().put(path.toString(), testSet);

            final @NotNull List<UUID> testCaseUuids = TestCaseStore.testCaseUuids(List.of());

            try (Stream<Path> files = Files.list(path)) {
                files.filter(Files::isRegularFile)
                        .filter(file -> FileKind.of(file) == FileKind.TEST_CASE)
                        .parallel()
                        .forEach(filePath -> {
                            // Rule-INTERNAL-012
                            final @NotNull Optional<UUID> testCaseUuid = FileKind.TEST_CASE.uuidIn(filePath);
                            if (testCaseUuid.isEmpty()) {
                                scanned.getHandNamedTestCases().add(path.getFileName() + "/" + filePath.getFileName());
                                return;
                            }

                            try {
                                final @NotNull TestCase testCase = TestCase.read(mapper, Files.readString(filePath), testCaseUuid.orElseThrow());
                                testCase.setParent(testSet);

                                if (scanned.getTestCasesByUuid().put(testCase.getUuid(), testCase) != null) {
                                    scanned.getClashingTestCases().add(testSet.getName() + "/" + filePath.getFileName());
                                }

                                testCaseUuids.add(testCase.getUuid());
                            } catch (final IOException | UncheckedIOException ex) {
                                TestinLogger.error("Failed to read test case '" + filePath.toAbsolutePath() +
                                        "': " + FailureText.of(ex));

                                scanned.getUnreadableTestCases().computeIfAbsent(path.toString(), _ -> ConcurrentHashMap.newKeySet())
                                        .add(filePath.getFileName().toString());
                            }
                        });
            }

            scanned.getTestCaseUuidsByTestSet().put(path.toString(), testCaseUuids);

            indicator.setText(Bundle.message("indexer.progress.test.set", testSet.getName(), String.valueOf(testCaseUuids.size())));

        } catch (final IOException | UncheckedIOException ex) {
            TestinLogger.error("Failed to scan test set '" +
                    path.getFileName().toString() + "': " + FailureText.of(ex));
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-010, Rule-INTERNAL-015
    private void scanTestRunNodes(final @NotNull Path testRunDirectory, final @NotNull AbstractNode parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedTestProject scanned) {
        try (Stream<Path> paths = Files.list(testRunDirectory)) {
            final @NotNull List<Path> directories = paths.filter(Files::isDirectory).toList();

            for (final Path directory : directories) {
                if (indicator.isCanceled()) return;

                scanTestRunOrPackage(directory, parent, indicator, unread, scanned);
            }
        } catch (final NoSuchFileException absent) {
            TestinLogger.info("No test runs to read: " + testRunDirectory);
        } catch (final IOException ex) {
            throw new UncheckedIOException("the folder " + testRunDirectory.getFileName() + " could not be listed: " + FailureText.of(ex), ex);
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-010, Rule-INTERNAL-015
    private void scanTestRunOrPackage(final @NotNull Path directory, final @NotNull AbstractNode parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedTestProject scanned) {
        store.markedAs(directory, NodeType.UNDER_TEST_RUNS).ifPresentOrElse(
                marked -> {
                    if (marked == NodeType.TEST_RUN) scanTestRun(directory, parent, indicator, scanned);
                    else scanTestRunPackageNode(directory, parent, indicator, unread, scanned);
                },
                () -> skipped(directory, NodeType.UNDER_TEST_RUNS, unread));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-010, Rule-INTERNAL-015
    private void scanTestRunPackageNode(final @NotNull Path path, final @NotNull AbstractNode parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedTestProject scanned) {
        try {
            final @NotNull TestRunPackageNode testRunPackage = directoryMapper.getTestRunPackageNode(path, parent);

            scanned.getTestRunPackages().put(path.toString(), testRunPackage);

            try (Stream<Path> subPaths = Files.list(path)) {
                subPaths.filter(Files::isDirectory)
                        .forEach(subPath -> scanTestRunOrPackage(subPath, testRunPackage, indicator, unread, scanned));
            }

        } catch (final IOException | UncheckedIOException ex) {
            TestinLogger.error("Failed to scan test run package: " + path.getFileName() + ": " + FailureText.of(ex));
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-015
    private void skipped(final @NotNull Path directory, final @NotNull List<NodeType> family, final @NotNull List<Path> unread) {
        TestinLogger.warn("Skipping unmarked directory (missing " + NodeType.markerNames(family) + "): " + directory);

        if (holdsTestCases(directory)) unread.add(directory);
    }

    private boolean holdsTestCases(final @NotNull Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).anyMatch(IndexingScanner::looksLikeATestCaseFile);
        } catch (final IOException | UncheckedIOException unreadable) {
            return false;
        }
    }

    // UC-INTERNAL-002
    private void scanTestRun(final @NotNull Path path, final @NotNull AbstractNode parent, final @NotNull ProgressIndicator indicator, final @NotNull ScannedTestProject scanned) {
        final @NotNull TestRunNode testRun = directoryMapper.getTestRunNode(path, parent);

        scanned.getTestRunNodes().put(path.toString(), testRun);

        // Rule-INTERNAL-011
        scanned.getRunItemsByPath().put(path.toString(), new RunItems().setAll(runItemsIn(path, scanned)));

        indicator.setText(Bundle.message("indexer.progress.test.run", path.getFileName()));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-011, Rule-INTERNAL-012
    private @NotNull List<RunItem> runItemsIn(final @NotNull Path testRunPath, final @NotNull ScannedTestProject scanned) {
        final @NotNull List<RunItem> read = new ArrayList<>();

        for (final Path file : testDataFiles.runItemsIn(testRunPath)) {
            // Rule-INTERNAL-094
            final @NotNull Optional<UUID> testCaseUuid = FileKind.RUN_ITEM.uuidIn(file);
            if (testCaseUuid.isEmpty()) {
                scanned.getHandNamedRunItems().add(testRunPath.getFileName() + "/" + file.getFileName());
                continue;
            }

            try {
                final @NotNull RunItem runItem = mapper.readValue(Files.readAllBytes(file), RunItem.class);
                runItem.setUuid(testCaseUuid.orElseThrow());
                read.add(runItem);

            } catch (final IOException | UncheckedIOException ex) {
                TestinLogger.error("Failed to read the run item '" + file.toAbsolutePath() + "': " + FailureText.of(ex));
                scanned.getUnreadableRunItems().add(testRunPath.getFileName() + "/" + file.getFileName());
            }
        }

        return read;
    }
}
