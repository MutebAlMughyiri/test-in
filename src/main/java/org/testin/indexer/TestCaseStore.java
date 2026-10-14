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
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.FileKind;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testcase.TestCaseOrder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

final class TestCaseStore {
    private final @NotNull TestDataFiles testDataFiles;
    private final @NotNull AppSettingsState settings;
    @Getter(AccessLevel.PACKAGE)
    private final @NotNull Map<UUID, TestCase> testCasesById = new ConcurrentHashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private final @NotNull Map<String, List<UUID>> testCaseIdsByTestSet = new ConcurrentHashMap<>();

    // UC-SHARE-002, Rule-SHARE-001
    private final @NotNull Map<String, Set<String>> unreadable = new ConcurrentHashMap<>();

    TestCaseStore(final @NotNull Project project) {
        this.testDataFiles = Services.getInstance(project, TestDataFiles.class);
        this.settings = Services.getInstance(project, AppSettingsState.class);
    }

    static @NotNull List<UUID> testCaseIds(final @NotNull Collection<UUID> initial) {
        return new CopyOnWriteArrayList<>(initial);
    }

    static @NotNull Path named(final @NotNull Path testSetPath, final @NotNull UUID testCaseId) {
        return testSetPath.resolve(FileKind.TEST_CASE.fileName(testCaseId));
    }

    // UC-TREE-PANEL-011
    void renamed(final @NotNull Path oldPath, final @NotNull Path newPath) {
        for (final String set : List.copyOf(unreadable.keySet())) {
            final @NotNull Path setPath = Path.of(set);
            if (setPath.startsWith(oldPath)) {
                final @NotNull String moved = newPath.resolve(oldPath.relativize(setPath)).toString();
                Optional.ofNullable(unreadable.remove(set)).ifPresent(files -> unreadable.put(moved, files));
            }
        }
    }

    // UC-SHARE-002, Rule-SHARE-001
    @NotNull Set<String> unreadableIn(final @NotNull Path testSetPath) {
        return Set.copyOf(unreadable.getOrDefault(testSetPath.toString(), Set.of()));
    }

    // UC-INTERNAL-004, Rule-INTERNAL-030
    @NotNull List<TestCase> getForTestSet(final @NotNull Path testSetPath) {
        final @NotNull List<UUID> testCaseIds = testCaseIdsByTestSet.getOrDefault(testSetPath.toString(), List.of());
        if (testCaseIds.isEmpty()) return List.of();

        final @NotNull Set<UUID> seen = new HashSet<>(testCaseIds.size());
        final @NotNull List<TestCase> testCases = new ArrayList<>(testCaseIds.size());

        for (final UUID testCaseId : testCaseIds) {
            if (seen.add(testCaseId)) Optional.ofNullable(testCasesById.get(testCaseId)).ifPresent(testCases::add);
        }

        return TestCaseOrder.ordered(testCases);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-033, Rule-INTERNAL-034
    boolean put(final @NotNull Path testSetPath, final @NotNull TestCase testCase) {
        if (testDataFiles.alreadyHolds(named(testSetPath, testCase.getId()), testCase))
            return false;

        final @NotNull String tester = settings.testerName;
        if (testCasesById.containsKey(testCase.getId())) testCase.touch(tester);
        else testCase.stampCreated(tester);

        return store(testSetPath, testCase);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-035
    boolean putVerbatim(final @NotNull Path testSetPath, final @NotNull TestCase testCase) {
        return store(testSetPath, testCase);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-033, Rule-INTERNAL-117
    private boolean store(final @NotNull Path testSetPath, final @NotNull TestCase testCase) {
        if (failedToWrite(testSetPath, testCase)) return false;

        testCasesById.merge(testCase.getId(), testCase, TestCase::takeValuesOf);
        return true;
    }

    // UC-INTERNAL-004, Rule-INTERNAL-033
    private boolean failedToWrite(final @NotNull Path testSetPath, final @NotNull TestCase testCase) {
        if (!testDataFiles.write(named(testSetPath, testCase.getId()), testCase)) return true;

        final @NotNull List<UUID> testCaseIds = testCaseIdsByTestSet.computeIfAbsent(testSetPath.toString(), _ -> testCaseIds(List.of()));
        if (!testCaseIds.contains(testCase.getId())) testCaseIds.add(testCase.getId());

        return false;
    }

    // UC-EDITOR-PANEL-017, Rule-INTERNAL-035
    boolean move(final @NotNull Path fromSet, final @NotNull Path toSet, final @NotNull TestCase testCase) {
        final @NotNull UUID testCaseId = testCase.getId();
        final @NotNull Path from = named(fromSet, testCaseId);
        final @NotNull Path to = named(toSet, testCaseId);
        final @NotNull Optional<TestCase> was = Optional.ofNullable(testCasesById.get(testCaseId));

        if (failedToWrite(toSet, testCase)) return false;

        testCasesById.put(testCaseId, testCase);
        if (from.equals(to)) return true;

        if (testDataFiles.discard(from)) {
            if (!fromSet.equals(toSet))
                Optional.ofNullable(testCaseIdsByTestSet.get(fromSet.toString())).ifPresent(testCaseIds -> testCaseIds.remove(testCaseId));
            return true;
        }

        testDataFiles.discard(to);
        if (!fromSet.equals(toSet))
            Optional.ofNullable(testCaseIdsByTestSet.get(toSet.toString())).ifPresent(testCaseIds -> testCaseIds.remove(testCaseId));
        was.ifPresent(original -> testCasesById.put(testCaseId, original));
        return false;
    }

    boolean remove(final @NotNull Path testSetPath, final @NotNull UUID testCaseId) {
        if (!testDataFiles.delete(named(testSetPath, testCaseId))) return false;

        testCasesById.remove(testCaseId);
        Optional.ofNullable(testCaseIdsByTestSet.get(testSetPath.toString()))
                .ifPresent(testCaseIds -> testCaseIds.remove(testCaseId));
        return true;
    }

    // UC-INTERNAL-004, Rule-INTERNAL-031, Rule-INTERNAL-117
    boolean updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCase> orderedList, final @NotNull List<TestCase> moved) {
        final @NotNull String path = testSetPath.toString();
        final @NotNull List<UUID> testCaseIds = new ArrayList<>(orderedList.size());
        final @NotNull Set<UUID> newIds = new HashSet<>();

        final @NotNull Map<UUID, TestCase> placed = new HashMap<>();
        for (final TestCase testCase : moved) placed.put(testCase.getId(), testCase);

        final @NotNull String tester = settings.testerName;
        boolean allWritten = true;

        for (final TestCase testCase : orderedList) {
            final @NotNull TestCase written = placed.getOrDefault(testCase.getId(), testCase);
            final boolean firstSight = !testCasesById.containsKey(testCase.getId());

            if (firstSight) {
                written.stampCreated(tester);
                if (failedToWrite(testSetPath, written)) {
                    allWritten = false;
                    continue;
                }

                testCasesById.put(testCase.getId(), testCase.takeValuesOf(written));
            } else if (placed.containsKey(testCase.getId())) {
                allWritten &= store(testSetPath, written);
            }

            testCaseIds.add(testCase.getId());
            newIds.add(testCase.getId());
        }

        Optional.ofNullable(testCaseIdsByTestSet.get(path)).ifPresent(oldIds -> oldIds.stream()
                .filter(testCaseId -> !newIds.contains(testCaseId))
                .forEach(testCasesById::remove));
        testCaseIdsByTestSet.put(path, testCaseIds(testCaseIds));
        return allWritten;
    }

    void removeForTestSet(final @NotNull String path) {
        Optional.ofNullable(testCaseIdsByTestSet.remove(path))
                .ifPresent(testCaseIds -> testCaseIds.forEach(testCasesById::remove));
        unreadable.remove(path);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-021
    void swapIn(final @NotNull Path testProjectPath, final @NotNull Map<UUID, TestCase> testCases, final @NotNull Map<String, List<UUID>> setTestCaseIds, final @NotNull Map<String, Set<String>> unreadableTestCases) {
        final @NotNull Set<UUID> held = testCaseIdsByTestSet.entrySet().stream()
                .filter(entry -> Path.of(entry.getKey()).startsWith(testProjectPath))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toCollection(HashSet::new));

        testCasesById.putAll(testCases);
        testCaseIdsByTestSet.putAll(setTestCaseIds);

        testCaseIdsByTestSet.keySet().removeIf(path -> Path.of(path).startsWith(testProjectPath) && !setTestCaseIds.containsKey(path));

        held.removeAll(testCases.keySet());
        held.forEach(testCasesById::remove);

        unreadable.putAll(unreadableTestCases);
        unreadable.keySet().removeIf(set -> Path.of(set).startsWith(testProjectPath) && !unreadableTestCases.containsKey(set));
    }

    void clear() {
        testCasesById.clear();
        testCaseIdsByTestSet.clear();
        unreadable.clear();
    }
}
