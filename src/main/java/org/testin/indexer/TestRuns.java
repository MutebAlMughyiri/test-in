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
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.node.TestCasesFolderNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.RunItems;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service(Service.Level.PROJECT)
public final class TestRuns {
    private final @NotNull Project project;

    private final @NotNull TestProjectIndexer indexer;

    private final @NotNull Map<String, Map<UUID, Optional<TestCase>>> recorded = new ConcurrentHashMap<>();

    private final @NotNull Map<UUID, TestCase> lastInGit = new ConcurrentHashMap<>();

    private final @NotNull Map<Path, String> recordedAt = new ConcurrentHashMap<>();

    public TestRuns(final @NotNull Project project) {
        this.project = project;
        this.indexer = Services.getInstance(project, TestProjectIndexer.class);
    }

    private @NotNull IndexerDataStore store() {
        return indexer.getStore();
    }

    private @NotNull RunItemWriter runItemWriter() {
        return indexer.getRunItemWriter();
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-127, Rule-REPORT-026, Rule-EDITOR-PANEL-187
    public @NotNull RunItems getRunItems(final @NotNull Path testRunPath) {
        return withTestCasesShown(testRunPath, store().getRunItems(testRunPath)).inOrderOf(testCaseUuidsInTreeOrder(testRunPath));
    }

    // Rule-EDITOR-PANEL-127
    private @NotNull List<UUID> testCaseUuidsInTreeOrder(final @NotNull Path testRunPath) {
        return testCasesFolderOf(testRunPath)
                .map(Services.getInstance(project, TestCases.class)::getTestCasesUnder)
                .orElse(List.of())
                .stream()
                .map(TestCase::getUuid)
                .toList();
    }

    // Rule-EDITOR-PANEL-127, Rule-EDITOR-PANEL-260
    public @NotNull Optional<TestCasesFolderNode> testCasesFolderOf(final @NotNull Path testRunPath) {
        return indexer.testProjectHolding(testRunPath)
                .flatMap(testProject -> Optional.ofNullable(store().getTestProjectsByPath().get(testProject.toString())))
                .map(TestProjectNode::getTestCasesFolder);
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239, Rule-REPORT-021, Rule-VIEW-PANEL-083
    private @NotNull RunItems withTestCasesShown(final @NotNull Path testRunPath, final @NotNull RunItems runItems) {
        final @NotNull IndexerDataStore store = store();
        final @NotNull String commit = commitOf(testRunPath);
        final @NotNull Map<UUID, Optional<TestCase>> inCommit = recorded.getOrDefault(recordedAt.getOrDefault(testRunPath, commit), Map.of());
        final boolean committed = store.findTestRunNode(testRunPath).map(node -> node.getMarker().getStatus().isRecord()).orElse(false);
        runItems.getAll().forEach(runItem -> runItem.showing(store.findTestCase(runItem.getUuid()), inCommit.getOrDefault(runItem.getUuid(), Optional.empty()), Optional.ofNullable(lastInGit.get(runItem.getUuid())), committed));
        return runItems;
    }

    // Rule-EDITOR-PANEL-239
    public @NotNull String commitOf(final @NotNull Path testRunPath) {
        return store().findTestRunNode(testRunPath).map(node -> node.getMarker().getCommit()).orElse("");
    }

    // Rule-EDITOR-PANEL-239, Rule-SHARE-130
    public void rememberRecordedAt(final @NotNull Path testRunPath, final @NotNull String revision) {
        recordedAt.put(testRunPath, revision);
    }

    // Rule-EDITOR-PANEL-239
    public @NotNull Set<UUID> notReadFrom(final @NotNull String commit, final @NotNull Set<UUID> testCaseUuids) {
        final @NotNull Map<UUID, Optional<TestCase>> read = recorded.getOrDefault(commit, Map.of());
        return testCaseUuids.stream().filter(testCaseUuid -> !read.containsKey(testCaseUuid)).collect(Collectors.toSet());
    }

    // Rule-EDITOR-PANEL-126
    public void rememberLastInGit(final @NotNull Map<UUID, TestCase> testCases) {
        lastInGit.putAll(testCases);
    }

    // Rule-EDITOR-PANEL-239
    public void rememberRecorded(final @NotNull String commit, final @NotNull Map<UUID, Optional<TestCase>> testCases) {
        recorded.computeIfAbsent(commit, _ -> new ConcurrentHashMap<>()).putAll(testCases);
    }

    // Rule-VIEW-PANEL-092
    public @NotNull Map<Path, RunItems> getAllRunItems() {
        return store().getRunItemsByPath().entrySet().stream()
                .collect(Collectors.toMap(entry -> Path.of(entry.getKey()), Map.Entry::getValue));
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-107
    public @NotNull Map<Path, RunItem> runItemsOf(final @NotNull UUID testCaseUuid) {
        final @NotNull Map<Path, RunItem> runItems = new HashMap<>();
        store().getRunItemsByPath().forEach((testRunPath, held) -> held.runItemOf(testCaseUuid).ifPresent(runItem -> runItems.put(Path.of(testRunPath), runItem)));
        return runItems;
    }

    // UC-INTERNAL-006, Rule-INTERNAL-051
    public @NotNull Optional<TestRunNode> findTestRunNode(final @NotNull Path testRunPath) {
        return store().findTestRunNode(testRunPath);
    }

    public @NotNull Optional<RunItems> findRunItems(final @NotNull Path testRunPath) {
        return store().findRunItems(testRunPath).map(runItems -> withTestCasesShown(testRunPath, runItems));
    }

    // Rule-INTERNAL-093
    public void changeRunItems(final @NotNull Path testRunPath, final @NotNull Consumer<RunItems> change) {
        findRunItems(testRunPath).ifPresentOrElse(runItems -> {
            // Rule-INTERNAL-011
            final @NotNull Set<UUID> gone = runItems.coveredUuids();
            change.accept(runItems);
            gone.removeAll(runItems.coveredUuids());

            runItemWriter().persist(testRunPath, runItems, gone);
        }, () -> TestinLogger.warn("Test run no longer indexed, so a change to it was dropped: " + testRunPath.getFileName()));
    }

    // Rule-INTERNAL-011, Rule-PRODUCT-008
    public void changeRunItem(final @NotNull Path testRunPath, final @NotNull UUID testCaseUuid, final @NotNull Consumer<RunItem> change) {
        findRunItems(testRunPath).ifPresentOrElse(runItems -> runItems.runItemOf(testCaseUuid).ifPresentOrElse(runItem -> {
                    change.accept(runItem);
                    runItemWriter().persistRunItem(testRunPath, runItems, runItem);
                }, () -> TestinLogger.warn("'" + testRunPath.getFileName() + "' no longer covers " + testCaseUuid + ", so a change to its run item was dropped")),
                () -> TestinLogger.warn("Test run no longer indexed, so a change to it was dropped: " + testRunPath.getFileName()));
    }

    public void changeTestRunMarker(final @NotNull Path testRunPath, final @NotNull Consumer<TestRunMarker> change) {
        store().findTestRunNode(testRunPath).ifPresentOrElse(node -> {
            final @NotNull TestRunMarker marker = node.getMarker();
            change.accept(marker);
            runItemWriter().persistMarker(testRunPath, () -> notSaved(testRunPath));
            indexer.announce(testRunPath);
        }, () -> TestinLogger.warn("Test run no longer indexed, so a change to its marker was dropped: " + testRunPath.getFileName()));
    }

    // Rule-INTERNAL-123
    private void notSaved(final @NotNull Path testRunPath) {
        store().rereadTestRunMarker(testRunPath);
        ApplicationManager.getApplication().invokeLater(() -> {
            indexer.announce(testRunPath);
            Services.getInstance(project, Notifier.class).softRefuse(project, Bundle.message("test.run.marker.not.saved", testRunPath.getFileName()));
        });
    }

    public void saveRunItems(final @NotNull Path testRunPath) {
        changeRunItems(testRunPath, _ -> {
        });
    }

    public void putRunItems(final @NotNull Path testRunPath, final @NotNull RunItems runItems) {
        runItemWriter().create(testRunPath, runItems);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    public @NotNull List<String> storeScreenshots(final @NotNull Path testRunPath, final @NotNull List<byte[]> pngs) {
        return runItemWriter().storeScreenshots(testRunPath, pngs);
    }

    public byte @NotNull [] screenshot(final @NotNull Path testRunPath, final @NotNull String name) {
        return runItemWriter().readScreenshot(testRunPath, name);
    }

    // Rule-SHARE-130
    public void awaitWrites() {
        runItemWriter().awaitWrites();
    }

    public @NotNull List<byte[]> screenshots(final @NotNull Path testRunPath, final @NotNull RunItem runItem) {
        return runItem.shownExecution().getScreenshots().stream().map(name -> screenshot(testRunPath, name)).toList();
    }
}
