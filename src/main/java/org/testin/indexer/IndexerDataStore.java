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
import org.testin.model.TestCaseDto;
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

    private final @NotNull List<Map<String, ? extends AbstractNode>> dirMaps = List.of(
            testProjectsByPath,
            testSetNodesByPath,
            testRunNodesByPath,
            testSetPackagesByPath,
            testRunPackagesByPath,
            testCasesFoldersByPath,
            testRunsFoldersByPath);
    // Rule-INTERNAL-091
    private final @NotNull Map<String, String> refusedProjects = new ConcurrentHashMap<>();

    IndexerDataStore(final @NotNull Project project) {
        this.testCaseStore = new TestCaseStore(project);
        this.markers = new MarkerFiles(project);
    }

    private static @NotNull RunItems indexed(final @Nullable RunItems node, final @NotNull Path path) {
        if (node != null) return node;

        TestinLogger.error("No test run indexed at " + path);
        throw new IllegalStateException("No test run indexed at " + path);
    }

    private static void dropUnseen(final @NotNull Map<String, ?> held, final @NotNull Path projectPath, final @NotNull Map<String, ?> found) {
        held.keySet().removeIf(key -> Path.of(key).startsWith(projectPath) && !found.containsKey(key));
    }

    @NotNull Map<UUID, TestCaseDto> getTestCasesById() {
        return testCaseStore.getTestCasesById();
    }

    @NotNull Set<String> unreadableTestCasesIn(final @NotNull Path testSetPath) {
        return testCaseStore.unreadableIn(testSetPath);
    }

    @NotNull Path testCaseFileOf(final @NotNull TestCaseDto testCase) {
        return testCaseStore.fileOf(testCase.getParent().getPath(), testCase.getId());
    }

    @NotNull Map<String, List<UUID>> getTestCaseIdsByTestSet() {
        return testCaseStore.getTestCaseIdsByTestSet();
    }

    @NotNull List<TestCaseDto> getTestCasesForTestSet(final @NotNull Path testSetPath) {
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
    Optional<TestCaseDto> findTestCase(final @NotNull UUID testCaseId) {
        return Optional.ofNullable(testCaseStore.getTestCasesById().get(testCaseId));
    }

    // UC-INTERNAL-004, Rule-INTERNAL-033
    boolean putTestCase(final @NotNull Path testSetPath, final @NotNull TestCaseDto testCase) {
        if (!testCaseStore.put(testSetPath, testCase)) return false;

        markTestSetModified(testSetPath);
        return true;
    }

    // UC-INTERNAL-004, Rule-INTERNAL-035
    boolean putTestCaseVerbatim(final @NotNull Path testSetPath, final @NotNull TestCaseDto testCase) {
        Optional.ofNullable(testSetNodesByPath.get(testSetPath.toString())).ifPresent(testCase::setParent);
        if (!testCaseStore.putVerbatim(testSetPath, testCase)) return false;

        markTestSetModified(testSetPath);
        return true;
    }

    // UC-EDITOR-PANEL-017, Rule-INTERNAL-035
    boolean moveTestCase(final @NotNull Path fromSet, final @NotNull Path toSet, final @NotNull TestCaseDto testCase) {
        if (!testCaseStore.move(fromSet, toSet, testCase)) return false;

        markTestSetModified(toSet);
        if (!fromSet.equals(toSet)) markTestSetModified(fromSet);
        return true;
    }

    boolean removeTestCase(final @NotNull Path testSetPath, final @NotNull UUID testCaseId) {
        if (!testCaseStore.remove(testSetPath, testCaseId)) return false;

        markTestSetModified(testSetPath);
        return true;
    }

    // UC-INTERNAL-004, Rule-INTERNAL-031
    boolean updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCaseDto> orderedList, final @NotNull List<TestCaseDto> moved) {
        final boolean allWritten = testCaseStore.updateSequence(testSetPath, orderedList, moved);
        markTestSetModified(testSetPath);
        return allWritten;
    }

    // UC-TREE-PANEL-027, Rule-TREE-PANEL-125
    private void markTestSetModified(final @NotNull Path testSetPath) {
        Optional.ofNullable(testSetNodesByPath.get(testSetPath.toString()))
                .ifPresent(testSet -> markers.touched(testSetPath, NodeType.TS.getMarker(), testSet.getMarker()));
    }

    void registerRunItems(final @NotNull Path testRunPath, final @NotNull RunItems runItems) {
        runItemsByPath.put(testRunPath.toString(), runItems);
    }

    boolean addTestSet(final @NotNull TestSetNode testSet) {
        return addNode(testSetNodesByPath, testSet, NodeType.TS.getMarker(), testSet.getMarker());
    }

    boolean addTestSetPackage(final @NotNull TestSetPackageNode testSetPackage) {
        return addNode(testSetPackagesByPath, testSetPackage, NodeType.TSP.getMarker(), testSetPackage.getMarker());
    }

    boolean addTestRunNode(final @NotNull TestRunNode testRunNode) {
        return addNode(testRunNodesByPath, testRunNode, NodeType.TR.getMarker(), testRunNode.getMarker());
    }

    boolean addTestRunPackage(final @NotNull TestRunPackageNode testRunPackage) {
        return addNode(testRunPackagesByPath, testRunPackage, NodeType.TRP.getMarker(), testRunPackage.getMarker());
    }

    private <V extends AbstractNode> boolean addNode(final @NotNull Map<String, V> map, final @NotNull V node, final @NotNull String markerFileName, final @NotNull Marker marker) {
        if (!markers.write(node.getPath(), markerFileName, marker)) return false;

        map.put(node.getPath().toString(), node);
        childrenIndex.invalidate();
        refreshDir(node.getPath());
        return true;
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    <M extends AbstractMarker> @NotNull M readMarker(final @NotNull Path dirPath, final @NotNull NodeType kind, final @NotNull Class<M> markerClass) {
        return markers.read(dirPath, kind, markerClass);
    }

    @NotNull List<Path> takeDamagedMarkers(final @NotNull Path projectPath) {
        return markers.takeDamaged(projectPath);
    }

    void refuse(final @NotNull Path projectPath, final @NotNull String reason) {
        refusedProjects.put(projectPath.toString(), reason);
    }

    void readable(final @NotNull Path projectPath) {
        refusedProjects.remove(projectPath.toString());
    }

    @NotNull Optional<String> whyNotRead(final @NotNull Path projectPath) {
        return Optional.ofNullable(refusedProjects.get(projectPath.toString()));
    }

    // Rule-INTERNAL-090, Rule-TREE-PANEL-051
    boolean giveFreshMarkerId(final @NotNull Path markerFile) {
        return markers.giveFreshId(markerFile);
    }

    boolean hasMarker(final @NotNull Path dirPath, final @NotNull NodeType kind) {
        return markers.has(dirPath, kind);
    }

    @NotNull Optional<NodeType> markedAs(final @NotNull Path dirPath, final @NotNull List<NodeType> family) {
        return markers.markedAs(dirPath, family);
    }

    void refreshDir(final @NotNull Path dirPath) {
        refresh(dirPath, true);
    }

    private void refresh(final @NotNull Path path, final boolean recursive) {
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                LocalFileSystem.getInstance().refreshNioFiles(List.of(path), true, recursive, null));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-021
    void swapIn(final @NotNull Path projectPath, final @NotNull ScannedProject scanned) {
        testProjectsByPath.putAll(scanned.getProjects());
        testCasesFoldersByPath.putAll(scanned.getTestCasesFolders());
        testRunsFoldersByPath.putAll(scanned.getTestRunsFolders());
        testSetPackagesByPath.putAll(scanned.getTestSetPackages());
        testRunPackagesByPath.putAll(scanned.getTestRunPackages());
        testSetNodesByPath.putAll(scanned.getTestSets());
        testRunNodesByPath.putAll(scanned.getTestRunNodes());
        runItemsByPath.putAll(scanned.getRunItemsByPath());

        testCaseStore.swapIn(projectPath, scanned.getTestCasesById(), scanned.getTestCaseIdsByTestSet(), scanned.handNamedFilesAlone(), scanned.getUnreadableTestCases());

        dropUnseen(testProjectsByPath, projectPath, scanned.getProjects());
        dropUnseen(testCasesFoldersByPath, projectPath, scanned.getTestCasesFolders());
        dropUnseen(testRunsFoldersByPath, projectPath, scanned.getTestRunsFolders());
        dropUnseen(testSetPackagesByPath, projectPath, scanned.getTestSetPackages());
        dropUnseen(testRunPackagesByPath, projectPath, scanned.getTestRunPackages());
        dropUnseen(testSetNodesByPath, projectPath, scanned.getTestSets());
        dropUnseen(testRunNodesByPath, projectPath, scanned.getTestRunNodes());
        dropUnseen(runItemsByPath, projectPath, scanned.getRunItemsByPath());

        childrenIndex.invalidate();
    }

    void removeTestProject(final @NotNull Path path) {
        final @NotNull String pathStr = path.toString();
        testProjectsByPath.remove(pathStr);
        testCasesFoldersByPath.entrySet().removeIf(entry -> entry.getValue().getPath().startsWith(path));
        testRunsFoldersByPath.entrySet().removeIf(entry -> entry.getValue().getPath().startsWith(path));

        removeTestSetPackagesUnder(path);
        removeTestRunPackagesUnder(path);
        removeTestSetsUnder(path);
        removeTestRunsUnder(path);
        childrenIndex.invalidate();

        TestinLogger.info("Test project dropped from the index: " + pathStr);
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
        final @NotNull String pathStr = path.toString();
        testSetNodesByPath.remove(pathStr);
        testCaseStore.removeForTestSet(pathStr);
        childrenIndex.invalidate();
        TestinLogger.info("Removed test set at: " + pathStr);
    }

    void removeTestRun(final @NotNull Path path) {
        final @NotNull String pathStr = path.toString();
        testRunNodesByPath.remove(pathStr);
        runItemsByPath.remove(pathStr);
        childrenIndex.invalidate();
        TestinLogger.info("Removed test run at: " + pathStr);
    }

    void removeTestSetPackage(final @NotNull Path path) {
        final @NotNull String pathStr = path.toString();
        testSetPackagesByPath.remove(pathStr);

        removeTestSetPackagesUnder(path);
        removeTestSetsUnder(path);
        childrenIndex.invalidate();

        TestinLogger.info("Removed test set package at: " + pathStr);
    }

    void removeTestRunPackage(final @NotNull Path path) {
        final @NotNull String pathStr = path.toString();
        testRunPackagesByPath.remove(pathStr);

        removeTestRunPackagesUnder(path);
        removeTestRunsUnder(path);
        childrenIndex.invalidate();

        TestinLogger.info("Removed test run package at: " + pathStr);
    }

    boolean addTestProject(final @NotNull TestProjectNode testProject) {
        final boolean written = markers.write(testProject.getPath(), testProject.getMarkerFileName(), testProject.getMarker())
                && markers.write(testProject.getTestCasesFolder().getPath(), NodeType.TCF.getMarker(), testProject.getTestCasesFolder().getMarker())
                && markers.write(testProject.getTestRunsFolder().getPath(), NodeType.TRF.getMarker(), testProject.getTestRunsFolder().getMarker());
        if (!written) return false;

        testProjectsByPath.put(testProject.getPath().toString(), testProject);
        testCasesFoldersByPath.put(testProject.getTestCasesFolder().getPath().toString(), testProject.getTestCasesFolder());
        testRunsFoldersByPath.put(testProject.getTestRunsFolder().getPath().toString(), testProject.getTestRunsFolder());
        childrenIndex.invalidate();

        refreshDir(testProject.getPath());
        refreshDir(testProject.getTestCasesFolder().getPath());
        refreshDir(testProject.getTestRunsFolder().getPath());
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
        findTestRunNode(testRunPath).ifPresent(node -> node.setMarker(markers.read(testRunPath, NodeType.TR, TestRunMarker.class)));
    }

    void renameNode(final @NotNull Path oldPath, final @NotNull Path newPath) {
        final @NotNull RenamedPaths rename = new RenamedPaths(oldPath, newPath);
        final @Nullable AbstractNode newParentNode = Optional.ofNullable(newPath.getParent())
                .flatMap(this::findByPath)
                .orElse(null);

        for (final Map<String, ? extends AbstractNode> map : dirMaps) {
            rename.moveEntry(map, node -> updatePathAndParent(node, newPath, newParentNode));
            rename.moveNodesUnder(map);
        }

        rebuildPath2Under(newPath);

        rename.moveEntry(testCaseStore.getTestCaseIdsByTestSet(), _ -> {
        });
        rename.moveEntry(runItemsByPath, _ -> {
        });
        rename.moveKeysUnder(testCaseStore.getTestCaseIdsByTestSet());
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

    private void rebuildPath2Under(final @NotNull Path newPath) {
        allNodes().stream()
                .filter(node -> node.getPath().startsWith(newPath))
                .forEach(this::rebuildPath2);
    }

    @NotNull
    Optional<AbstractNode> findByPath(final @NotNull Path path) {
        final @NotNull String key = path.toString();

        return dirMaps.stream()
                .map(map -> map.get(key))
                .filter(Objects::nonNull)
                .map(AbstractNode.class::cast)
                .findFirst();
    }

    private void rebuildPath2(final @NotNull AbstractNode node) {
        final @NotNull ArrayList<String> path2 = new ArrayList<>();
        for (final AbstractNode ancestor : node.selfAndAncestors()) {
            path2.addFirst(ancestor.getName());
        }
        node.setPath2(path2);
    }

    @NotNull List<AbstractNode> getChildren(final @NotNull Path parentPath) {
        return childrenIndex.get(parentPath, this::allNodes);
    }

    void invalidateChildrenIndex() {
        childrenIndex.invalidate();
    }

    @NotNull Collection<AbstractNode> allNodes() {
        final @NotNull List<AbstractNode> directories = new ArrayList<>();
        for (final Map<String, ? extends AbstractNode> map : dirMaps) {
            directories.addAll(map.values());
        }
        return directories;
    }

    void clearAll() {
        testCaseStore.clear();
        dirMaps.forEach(Map::clear);
        runItemsByPath.clear();
        childrenIndex.clear();

        TestinLogger.info("IndexerDataStore: all maps cleared");
    }
}
