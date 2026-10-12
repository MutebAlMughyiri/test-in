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
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.NodeType;
import org.testin.model.markers.AbstractMarker;
import org.testin.model.markers.Marker;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.model.status.NodeStatus;
import org.testin.services.Services;
import org.testin.util.FailureText;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

@Service(Service.Level.PROJECT)
public final class Nodes {
    private final @NotNull Project p;
    private final @NotNull DeletedNodes deletedNodes;

    public Nodes(final @NotNull Project p) {
        this.p = p;
        this.deletedNodes = Services.getInstance(DeletedNodes.class);
    }

    private static boolean sameFile(final @NotNull Path one, final @NotNull Path other) {
        try {
            return Files.isSameFile(one, other);
        } catch (final IOException ex) {
            TestinLogger.warn("Could not compare " + one + " with " + other + ": " + FailureText.of(ex));
            return false;
        }
    }

    private @NotNull ProjectIndexer indexer() {
        return Services.getInstance(p, ProjectIndexer.class);
    }

    private @NotNull IndexerDataStore store() {
        return indexer().getStore();
    }

    public @NotNull Map<String, TestProjectNode> getTestProjectsByPath() {
        return store().getTestProjectsByPath();
    }

    // UC-TREE-PANEL-002, UC-TREE-PANEL-003, UC-TREE-PANEL-011, Rule-TREE-PANEL-004
    public boolean isTaken(final @NotNull Path wanted, final @NotNull Optional<Path> renaming) {
        if (!Files.exists(wanted)) return false;

        return renaming.map(self -> !sameFile(self, wanted)).orElse(true);
    }

    public @NotNull List<AbstractNode> getChildren(final @NotNull Path parentPath) {
        return store().getChildren(parentPath);
    }

    public @NotNull List<AbstractNode> getAllNodes() {
        return List.copyOf(store().allNodes());
    }

    // UC-INTERNAL-005
    public void removeTestProject(final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        removeVf(path, IndexerDataStore::removeTestProject, onRemoved);
    }

    public void removeTestSet(final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        removeVf(path, IndexerDataStore::removeTestSet, onRemoved);
    }

    public void removeTestRun(final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        removeVf(path, IndexerDataStore::removeTestRun, onRemoved);
    }

    public void removeTestSetPackage(final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        removeVf(path, IndexerDataStore::removeTestSetPackage, onRemoved);
    }

    public void removeTestRunPackage(final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        removeVf(path, IndexerDataStore::removeTestRunPackage, onRemoved);
    }

    // UC-TREE-PANEL-012, Rule-TREE-PANEL-042
    public void refuseRemove(final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        TestinLogger.info("Not removed: " + path.getFileName() + " is not removable from the tree");
        onRemoved.accept(false);
    }

    private void removeVf(final @NotNull Path path, final @NotNull BiConsumer<IndexerDataStore, Path> cacheUpdate, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        final @NotNull ProjectIndexer indexer = indexer();
        indexer.getNodeFiles().remove(path, () -> cacheUpdate.accept(indexer.getStore(), path), removed -> {
            onRemoved.accept(removed);
            if (removed) indexer.announce(path);
        });
    }

    public void moveNode(final @NotNull Path oldPath, final @NotNull Path newPath, final @NotNull Consumer<@NotNull Boolean> onFinished) {
        final @NotNull ProjectIndexer indexer = indexer();
        indexer.getNodeFiles().move(oldPath, newPath, moved -> {
            onFinished.accept(moved);
            if (!moved) return;

            indexer.announce(oldPath);
            indexer.announce(newPath);
        });
    }

    public void copyNodes(final @NotNull List<Path> sourcePaths, final @NotNull Path targetPath, final @NotNull IntConsumer onComplete) {
        final @NotNull ProjectIndexer indexer = indexer();
        indexer.getNodeFiles().copy(sourcePaths, targetPath, copied -> {
            onComplete.accept(copied);
            if (copied > 0) indexer.announceChildrenOf(targetPath);
        });
    }

    // UC-INTERNAL-005, Rule-INTERNAL-037, Rule-INTERNAL-041
    public @NotNull Optional<Path> keepAside(final @NotNull Path node) {
        return deletedNodes.keep(node);
    }

    // UC-INTERNAL-005, Rule-INTERNAL-042
    public @NotNull List<Path> restoreNodes(final @NotNull Map<Path, Path> originalByKept) {
        final @NotNull List<Path> back = new ArrayList<>();
        final @NotNull List<Path> lost = new ArrayList<>();
        for (final Map.Entry<Path, Path> one : originalByKept.entrySet()) {
            if (deletedNodes.putBack(p, one.getKey(), one.getValue())) back.add(one.getValue());
            else lost.add(one.getValue());
        }

        final @NotNull ProjectIndexer indexer = indexer();
        back.forEach(this::refreshDirectory);
        back.stream().flatMap(original -> indexer.testProjectHolding(original).stream()).distinct().forEach(indexer.getScanCoordinator()::rescanExclusively);
        back.forEach(indexer::announce);
        return lost;
    }

    // UC-INTERNAL-005, Rule-INTERNAL-043
    public void forgetKept(final @NotNull Path kept) {
        deletedNodes.forget(kept);
    }

    // UC-TREE-PANEL-002
    public boolean addTestProject(final @NotNull TestProjectNode tp) {
        return indexer().announcedIf(store().addTestProject(tp), tp.getPath());
    }

    public boolean addTestSet(final @NotNull TestSetNode ts) {
        return indexer().announcedIf(store().addTestSet(ts), ts.getPath());
    }

    public boolean addTestSetPackage(final @NotNull TestSetPackageNode tsp) {
        return indexer().announcedIf(store().addTestSetPackage(tsp), tsp.getPath());
    }

    public boolean addTestRunNode(final @NotNull TestRunNode testRunNode) {
        return indexer().announcedIf(store().addTestRunNode(testRunNode), testRunNode.getPath());
    }

    public boolean addTestRunPackage(final @NotNull TestRunPackageNode trp) {
        return indexer().announcedIf(store().addTestRunPackage(trp), trp.getPath());
    }

    public boolean persistMarker(final @NotNull AbstractNode dto) {
        return indexer().announcedIf(store().persistMarker(dto), dto.getPath());
    }

    // UC-TREE-PANEL-015, Rule-TREE-PANEL-055, Rule-INTERNAL-117
    public boolean reorder(final @NotNull AbstractNode node, final int order) {
        final @NotNull Marker marker = node.getMarker();
        final int was = marker.getOrder();
        marker.setOrder(order);

        if (persistMarker(node)) return true;

        marker.setOrder(was);
        return false;
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-062, Rule-INTERNAL-117
    public boolean mark(final @NotNull AbstractNode node, final @NotNull NodeStatus status, final @NotNull String tester) {
        final @NotNull Marker marker = node.getMarker();
        final @NotNull NodeStatus before = marker.status();
        final @NotNull String updatedByBefore = marker.getUpdatedBy();
        final @NotNull ZonedDateTime updatedAtBefore = marker.getUpdatedAt();

        marker.applyStatus(status);
        marker.touch(tester);

        if (persistMarker(node)) return true;

        marker.applyStatus(before);
        marker.setUpdatedBy(updatedByBefore);
        marker.setUpdatedAt(updatedAtBefore);
        return false;
    }

    <M extends AbstractMarker> @NotNull M readMarker(final @NotNull Path dirPath, final @NotNull NodeType kind, final @NotNull Class<M> markerClass) {
        return store().readMarker(dirPath, kind, markerClass);
    }

    public @NotNull Optional<AbstractNode> find(final @NotNull Path path) {
        return store().findByPath(path);
    }

    public boolean nodeExists(final @NotNull Path path) {
        return store().findByPath(path).isPresent();
    }

    public void refreshDirectory(final @NotNull Path path) {
        store().refreshDir(path);
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-133
    public void renameNode(final @NotNull Path oldPath, final @NotNull Path newPath, final @NotNull Runnable onFinished, final @NotNull Runnable onFailed) {
        final @NotNull ProjectIndexer indexer = indexer();
        indexer.getNodeFiles().rename(oldPath, newPath, () -> {
            onFinished.run();
            indexer.announce(newPath);
        }, onFailed);
    }
}
