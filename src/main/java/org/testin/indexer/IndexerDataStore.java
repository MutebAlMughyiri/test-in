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
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.TestinLogger;
import org.testin.model.NodeType;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItems;
import org.testin.model.markers.AbstractMarker;
import org.testin.model.markers.Marker;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestCasesFolderNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.model.node.TestRunsFolderNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class IndexerDataStore {
    private final @NotNull NodeChildrenIndex childrenIndex = new NodeChildrenIndex();

    private final @NotNull MarkerFiles markers;
    private final @NotNull TestCaseStore testCaseStore;

    @Getter
    private final @NotNull Map<String, TestProjectNode> testProjectsByPath = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull Map<String, TestSetNode> testSetNodesByPath = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull Map<String, TestRunNode> testRunNodesByPath = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull Map<String, TestSetPackageNode> testSetPackagesByPath = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull Map<String, TestRunPackageNode> testRunPackagesByPath = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull Map<String, TestCasesFolderNode> testCasesFoldersByPath = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull Map<String, TestRunsFolderNode> testRunsFoldersByPath = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull Map<String, RunItems> runItemsByPath = new ConcurrentHashMap<>();

    private final @NotNull List<Map<String, ? extends AbstractNode>> nodeMaps = List.of(
            testProjectsByPath,
            testSetNodesByPath,
            testRunNodesByPath,
            testSetPackagesByPath,
            testRunPackagesByPath,
            testCasesFoldersByPath,
            testRunsFoldersByPath);
    // Rule-INTERNAL-091
    private final @NotNull Map<String, String> refusedTestProjects = new ConcurrentHashMap<>();

    IndexerDataStore(final @NotNull Project project) {
        this.testCaseStore = new TestCaseStore(project);
        this.markers = new MarkerFiles(project);
    }

    private static @NotNull RunItems indexed(final @Nullable RunItems node, final @NotNull Path path) {
        if (node != null) return node;

        TestinLogger.error("No test run indexed at " + path);
        throw new IllegalStateException("No test run indexed at " + path);
    }

    private static void dropUnseen(final @NotNull Map<String, ?> held, final @NotNull Path testProjectPath, final @NotNull Map<String, ?> found) {
        held.keySet().removeIf(key -> Path.of(key).startsWith(testProjectPath) && !found.containsKey(key));
    }

    @NotNull Map<UUID, TestCase> getTestCasesByUuid() {
        return testCaseStore.getTestCasesByUuid();
    }

    @NotNull Set<String> unreadableTestCasesIn(final @NotNull Path testSetPath) {
        return testCaseStore.unreadableIn(testSetPath);
    }

    @NotNull Path testCaseFileOf(final @NotNull TestCase testCase) {
        return TestCaseStore.named(testCase.getParent().getPath(), testCase.getUuid());
    }

    @NotNull Map<String, List<UUID>> getTestCaseUuidsByTestSet() {
        return testCaseStore.getTestCaseUuidsByTestSet();
    }

    @NotNull List<TestCase> getTestCasesForTestSet(final @NotNull Path testSetPath) {
        return testCaseStore.getForTestSet(testSetPath);
    }

    @NotNull
    Optional<RunItems> findRunItems(final @NotNull Path testRunPath) {
        return Optional.ofNullable(runItemsByPath.get(testRunPath.toString()));
    }

    @NotNull Optional<TestRunNode> findTestRunNode(final @NotNull Path testRunPath) {
        return Optional.ofNullable(testRunNodesByPath.get(testRunPath.toString()));
    }

    @NotNull
    RunItems getRunItems(final @NotNull Path testRunPath) {
        return indexed(runItemsByPath.get(testRunPath.toString()), testRunPath);
    }

    @NotNull
    Optional<TestCase> findTestCase(final @NotNull UUID testCaseUuid) {
        return Optional.ofNullable(testCaseStore.getTestCasesByUuid().get(testCaseUuid));
    }

    // UC-INTERNAL-004, Rule-INTERNAL-033
    boolean putTestCase(final @NotNull Path testSetPath, final @NotNull TestCase testCase) {
        if (!testCaseStore.put(testSetPath, testCase)) return false;

        markTestSetModified(testSetPath);
        return true;
    }

    // UC-INTERNAL-004, Rule-INTERNAL-035
    boolean putTestCaseVerbatim(final @NotNull Path testSetPath, final @NotNull TestCase testCase) {
        Optional.ofNullable(testSetNodesByPath.get(testSetPath.toString())).ifPresent(testCase::setParent);
        if (!testCaseStore.putVerbatim(testSetPath, testCase)) return false;

        markTestSetModified(testSetPath);
        return true;
    }

    // UC-EDITOR-PANEL-017, Rule-INTERNAL-035
    boolean moveTestCase(final @NotNull Path fromSet, final @NotNull Path toSet, final @NotNull TestCase testCase) {
        if (!testCaseStore.move(fromSet, toSet, testCase)) return false;

        markTestSetModified(toSet);
        if (!fromSet.equals(toSet)) markTestSetModified(fromSet);
        return true;
    }

    boolean removeTestCase(final @NotNull Path testSetPath, final @NotNull UUID testCaseUuid) {
        if (!testCaseStore.remove(testSetPath, testCaseUuid)) return false;

        markTestSetModified(testSetPath);
        return true;
    }

    // UC-INTERNAL-004, Rule-INTERNAL-031
    boolean updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCase> orderedList, final @NotNull List<TestCase> moved) {
        final boolean allWritten = testCaseStore.updateSequence(testSetPath, orderedList, moved);
        markTestSetModified(testSetPath);
        return allWritten;
    }

    // UC-TREE-PANEL-027, Rule-TREE-PANEL-125
    private void markTestSetModified(final @NotNull Path testSetPath) {
        Optional.ofNullable(testSetNodesByPath.get(testSetPath.toString()))
                .ifPresent(testSet -> markers.touched(testSetPath, NodeType.TEST_SET.getMarker(), testSet.getMarker()));
    }

    void registerRunItems(final @NotNull Path testRunPath, final @NotNull RunItems runItems) {
        runItemsByPath.put(testRunPath.toString(), runItems);
    }

    boolean addTestSet(final @NotNull TestSetNode testSet) {
        return addNode(testSetNodesByPath, testSet, NodeType.TEST_SET.getMarker(), testSet.getMarker());
    }

    boolean addTestSetPackage(final @NotNull TestSetPackageNode testSetPackage) {
        return addNode(testSetPackagesByPath, testSetPackage, NodeType.TEST_SET_PACKAGE.getMarker(), testSetPackage.getMarker());
    }

    boolean addTestRunNode(final @NotNull TestRunNode testRunNode) {
        return addNode(testRunNodesByPath, testRunNode, NodeType.TEST_RUN.getMarker(), testRunNode.getMarker());
    }

    boolean addTestRunPackage(final @NotNull TestRunPackageNode testRunPackage) {
        return addNode(testRunPackagesByPath, testRunPackage, NodeType.TEST_RUN_PACKAGE.getMarker(), testRunPackage.getMarker());
    }

    private <V extends AbstractNode> boolean addNode(final @NotNull Map<String, V> map, final @NotNull V node, final @NotNull String markerFileName, final @NotNull Marker marker) {
        if (!markers.write(node.getPath(), markerFileName, marker)) return false;

        map.put(node.getPath().toString(), node);
        childrenIndex.invalidate();
        refreshDirectory(node.getPath());
        return true;
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    <M extends AbstractMarker> @NotNull M readMarker(final @NotNull Path directory, final @NotNull NodeType kind, final @NotNull Class<M> markerClass) {
        return markers.read(directory, kind, markerClass);
    }

    @NotNull List<Path> takeDamagedMarkers(final @NotNull Path testProjectPath) {
        return markers.takeDamaged(testProjectPath);
    }

    void refuse(final @NotNull Path testProjectPath, final @NotNull String reason) {
        refusedTestProjects.put(testProjectPath.toString(), reason);
    }

    void readable(final @NotNull Path testProjectPath) {
        refusedTestProjects.remove(testProjectPath.toString());
    }

    @NotNull Optional<String> whyNotRead(final @NotNull Path testProjectPath) {
        return Optional.ofNullable(refusedTestProjects.get(testProjectPath.toString()));
    }

    // Rule-INTERNAL-090, Rule-TREE-PANEL-051
    boolean giveFreshMarkerUuid(final @NotNull Path markerFile) {
        return markers.giveFreshUuid(markerFile);
    }

    boolean hasMarker(final @NotNull Path directory, final @NotNull NodeType kind) {
        return markers.has(directory, kind);
    }

    @NotNull Optional<NodeType> markedAs(final @NotNull Path directory, final @NotNull List<NodeType> family) {
        return markers.markedAs(directory, family);
    }

    void refreshDirectory(final @NotNull Path directory) {
        refresh(directory, true);
    }

    private void refresh(final @NotNull Path path, final boolean recursive) {
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                LocalFileSystem.getInstance().refreshNioFiles(List.of(path), true, recursive, null));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-021
    void swapIn(final @NotNull Path testProjectPath, final @NotNull ScannedTestProject scanned) {
        testProjectsByPath.putAll(scanned.getTestProjects());
        testCasesFoldersByPath.putAll(scanned.getTestCasesFolders());
        testRunsFoldersByPath.putAll(scanned.getTestRunsFolders());
        testSetPackagesByPath.putAll(scanned.getTestSetPackages());
        testRunPackagesByPath.putAll(scanned.getTestRunPackages());
        testSetNodesByPath.putAll(scanned.getTestSets());
        testRunNodesByPath.putAll(scanned.getTestRunNodes());
        runItemsByPath.putAll(scanned.getRunItemsByPath());

        testCaseStore.swapIn(testProjectPath, scanned.getTestCasesByUuid(), scanned.getTestCaseUuidsByTestSet(), scanned.getUnreadableTestCases());

        dropUnseen(testProjectsByPath, testProjectPath, scanned.getTestProjects());
        dropUnseen(testCasesFoldersByPath, testProjectPath, scanned.getTestCasesFolders());
        dropUnseen(testRunsFoldersByPath, testProjectPath, scanned.getTestRunsFolders());
        dropUnseen(testSetPackagesByPath, testProjectPath, scanned.getTestSetPackages());
        dropUnseen(testRunPackagesByPath, testProjectPath, scanned.getTestRunPackages());
        dropUnseen(testSetNodesByPath, testProjectPath, scanned.getTestSets());
        dropUnseen(testRunNodesByPath, testProjectPath, scanned.getTestRunNodes());
        dropUnseen(runItemsByPath, testProjectPath, scanned.getRunItemsByPath());

        childrenIndex.invalidate();
    }

    void removeTestProject(final @NotNull Path path) {
        final @NotNull String pathText = path.toString();
        testProjectsByPath.remove(pathText);
        testCasesFoldersByPath.entrySet().removeIf(entry -> entry.getValue().getPath().startsWith(path));
        testRunsFoldersByPath.entrySet().removeIf(entry -> entry.getValue().getPath().startsWith(path));

        removeTestSetPackagesUnder(path);
        removeTestRunPackagesUnder(path);
        removeTestSetsUnder(path);
        removeTestRunsUnder(path);
        childrenIndex.invalidate();

        TestinLogger.info("Test project dropped from the index: " + pathText);
    }

    private void removeTestSetPackagesUnder(final @NotNull Path path) {
        testSetPackagesByPath.entrySet().removeIf(entry -> entry.getValue().getPath().startsWith(path));
    }

    private void removeTestRunPackagesUnder(final @NotNull Path path) {
        testRunPackagesByPath.entrySet().removeIf(entry -> entry.getValue().getPath().startsWith(path));
    }

    private void removeTestSetsUnder(final @NotNull Path path) {
        final @NotNull List<String> toRemove = testSetNodesByPath.entrySet().stream()
                .filter(entry -> entry.getValue().getPath().startsWith(path))
                .map(Map.Entry::getKey)
                .toList();
        for (final String setPath : toRemove) {
            removeTestSet(Path.of(setPath));
        }
    }

    private void removeTestRunsUnder(final @NotNull Path path) {
        final @NotNull List<String> toRemove = testRunNodesByPath.entrySet().stream()
                .filter(entry -> entry.getValue().getPath().startsWith(path))
                .map(Map.Entry::getKey)
                .toList();
        for (final String key : toRemove) {
            testRunNodesByPath.remove(key);
        }

        final @NotNull List<String> toRemoveTestRuns = runItemsByPath.keySet().stream()
                .filter(key -> Path.of(key).startsWith(path))
                .toList();
        for (final String key : toRemoveTestRuns) {
            runItemsByPath.remove(key);
        }
    }

    void removeTestSet(final @NotNull Path path) {
        final @NotNull String pathText = path.toString();
        testSetNodesByPath.remove(pathText);
        testCaseStore.removeForTestSet(pathText);
        childrenIndex.invalidate();
        TestinLogger.info("Removed test set at: " + pathText);
    }

    void removeTestRun(final @NotNull Path path) {
        final @NotNull String pathText = path.toString();
        testRunNodesByPath.remove(pathText);
        runItemsByPath.remove(pathText);
        childrenIndex.invalidate();
        TestinLogger.info("Removed test run at: " + pathText);
    }

    void removeTestSetPackage(final @NotNull Path path) {
        final @NotNull String pathText = path.toString();
        testSetPackagesByPath.remove(pathText);

        removeTestSetPackagesUnder(path);
        removeTestSetsUnder(path);
        childrenIndex.invalidate();

        TestinLogger.info("Removed test set package at: " + pathText);
    }

    void removeTestRunPackage(final @NotNull Path path) {
        final @NotNull String pathText = path.toString();
        testRunPackagesByPath.remove(pathText);

        removeTestRunPackagesUnder(path);
        removeTestRunsUnder(path);
        childrenIndex.invalidate();

        TestinLogger.info("Removed test run package at: " + pathText);
    }

    boolean addTestProject(final @NotNull TestProjectNode testProject) {
        final boolean written = markers.write(testProject.getPath(), testProject.getMarkerFileName(), testProject.getMarker())
                && markers.write(testProject.getTestCasesFolder().getPath(), NodeType.TEST_CASES_FOLDER.getMarker(), testProject.getTestCasesFolder().getMarker())
                && markers.write(testProject.getTestRunsFolder().getPath(), NodeType.TEST_RUNS_FOLDER.getMarker(), testProject.getTestRunsFolder().getMarker());
        if (!written) return false;

        testProjectsByPath.put(testProject.getPath().toString(), testProject);
        testCasesFoldersByPath.put(testProject.getTestCasesFolder().getPath().toString(), testProject.getTestCasesFolder());
        testRunsFoldersByPath.put(testProject.getTestRunsFolder().getPath().toString(), testProject.getTestRunsFolder());
        childrenIndex.invalidate();

        refreshDirectory(testProject.getPath());
        refreshDirectory(testProject.getTestCasesFolder().getPath());
        refreshDirectory(testProject.getTestRunsFolder().getPath());
        return true;
    }

    boolean persistMarker(final @NotNull AbstractNode node) {
        final boolean written = markers.write(node.getPath(), node.getMarkerFileName(), node.getMarker());
        childrenIndex.invalidate();
        refresh(node.getPath().resolve(node.getMarkerFileName()), false);
        return written;
    }

    // Rule-INTERNAL-083, Rule-INTERNAL-090
    boolean persistTestRunMarker(final @NotNull Path testRunPath) {
        return findTestRunNode(testRunPath).map(this::persistMarker).orElse(false);
    }

    // Rule-INTERNAL-123
    void rereadTestRunMarker(final @NotNull Path testRunPath) {
        findTestRunNode(testRunPath).ifPresent(node -> node.setMarker(markers.read(testRunPath, NodeType.TEST_RUN, TestRunMarker.class)));
    }

    void renameNode(final @NotNull Path oldPath, final @NotNull Path newPath) {
        final @NotNull RenamedPaths rename = new RenamedPaths(oldPath, newPath);
        final @Nullable AbstractNode newParentNode = Optional.ofNullable(newPath.getParent())
                .flatMap(this::findByPath)
                .orElse(null);

        for (final Map<String, ? extends AbstractNode> map : nodeMaps) {
            rename.moveEntry(map, node -> updatePathAndParent(node, newPath, newParentNode));
            rename.moveNodesUnder(map);
        }


        rename.moveEntry(testCaseStore.getTestCaseUuidsByTestSet(), _ -> {
        });
        rename.moveEntry(runItemsByPath, _ -> {
        });
        rename.moveKeysUnder(testCaseStore.getTestCaseUuidsByTestSet());
        rename.moveKeysUnder(runItemsByPath);
        testCaseStore.renamed(oldPath, newPath);
        childrenIndex.invalidate();

        // UC-TREE-PANEL-011, Rule-TREE-PANEL-100
        findByPath(newPath).ifPresent(renamed -> renamed.fixedChildren().forEach(child ->
                updatePathAndParent(child, newPath.resolve(child.getPath().getFileName()), renamed)));

        findByPath(newPath)
                .ifPresent(renamed -> markers.touched(renamed.getPath(), renamed.getMarkerFileName(), renamed.getMarker()));
    }

    private void updatePathAndParent(final @NotNull AbstractNode node, final @NotNull Path newPath, final @Nullable AbstractNode newParent) {
        node.setPath(newPath);
        node.setName(newPath.getFileName().toString());
        node.setParent(newParent);
    }


    @NotNull
    Optional<AbstractNode> findByPath(final @NotNull Path path) {
        final @NotNull String key = path.toString();

        return nodeMaps.stream()
                .map(map -> map.get(key))
                .filter(Objects::nonNull)
                .map(AbstractNode.class::cast)
                .findFirst();
    }


    @NotNull List<AbstractNode> getChildren(final @NotNull Path parentPath) {
        return childrenIndex.get(parentPath, this::allNodes);
    }

    void invalidateChildrenIndex() {
        childrenIndex.invalidate();
    }

    @NotNull Collection<AbstractNode> allNodes() {
        final @NotNull List<AbstractNode> directories = new ArrayList<>();
        for (final Map<String, ? extends AbstractNode> map : nodeMaps) {
            directories.addAll(map.values());
        }
        return directories;
    }

    void clearAll() {
        testCaseStore.clear();
        nodeMaps.forEach(Map::clear);
        runItemsByPath.clear();
        childrenIndex.clear();

        TestinLogger.info("IndexerDataStore: all maps cleared");
    }
}
