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
    private final @NotNull Map<UUID, TestCase> testCasesByUuid = new ConcurrentHashMap<>();

    @Getter(AccessLevel.PACKAGE)
    private final @NotNull Map<String, List<UUID>> testCaseUuidsByTestSet = new ConcurrentHashMap<>();

    // UC-SHARE-002, Rule-SHARE-001
    private final @NotNull Map<String, Set<String>> unreadable = new ConcurrentHashMap<>();

    // Rule-INTERNAL-145
    private final @NotNull Map<UUID, String> createdSinceReadBegan = new ConcurrentHashMap<>();

    TestCaseStore(final @NotNull Project project) {
        this.testDataFiles = Services.getInstance(project, TestDataFiles.class);
        this.settings = Services.getInstance(project, AppSettingsState.class);
    }

    static @NotNull List<UUID> testCaseUuids(final @NotNull Collection<UUID> initial) {
        return new CopyOnWriteArrayList<>(initial);
    }

    static @NotNull Path named(final @NotNull Path testSetPath, final @NotNull UUID testCaseUuid) {
        return testSetPath.resolve(FileKind.TEST_CASE.fileName(testCaseUuid));
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
        final @NotNull List<UUID> testCaseUuids = testCaseUuidsByTestSet.getOrDefault(testSetPath.toString(), List.of());
        if (testCaseUuids.isEmpty()) return List.of();

        final @NotNull Set<UUID> seen = new HashSet<>(testCaseUuids.size());
        final @NotNull List<TestCase> testCases = new ArrayList<>(testCaseUuids.size());

        for (final UUID testCaseUuid : testCaseUuids) {
            if (seen.add(testCaseUuid))
                Optional.ofNullable(testCasesByUuid.get(testCaseUuid)).ifPresent(testCases::add);
        }

        return TestCaseOrder.ordered(testCases);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-033, Rule-INTERNAL-034, Rule-SETTING-018
    boolean put(final @NotNull Path testSetPath, final @NotNull TestCase testCase) {
        if (testDataFiles.alreadyHolds(named(testSetPath, testCase.getUuid()), testCase))
            return false;

        final @NotNull String tester = settings.testerName;
        if (testCasesByUuid.containsKey(testCase.getUuid())) testCase.touch(tester);
        else testCase.stampCreated(tester);

        return store(testSetPath, testCase);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-035
    boolean putVerbatim(final @NotNull Path testSetPath, final @NotNull TestCase testCase) {
        return store(testSetPath, testCase);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-033, Rule-INTERNAL-117, Rule-INTERNAL-145
    private boolean store(final @NotNull Path testSetPath, final @NotNull TestCase testCase) {
        final boolean firstSight = !testCasesByUuid.containsKey(testCase.getUuid());
        if (failedToWrite(testSetPath, testCase)) return false;

        testCasesByUuid.merge(testCase.getUuid(), testCase, TestCase::takeValuesOf);
        if (firstSight) createdSinceReadBegan.put(testCase.getUuid(), testSetPath.toString());
        return true;
    }

    // UC-INTERNAL-004, Rule-INTERNAL-033
    private boolean failedToWrite(final @NotNull Path testSetPath, final @NotNull TestCase testCase) {
        if (!testDataFiles.write(named(testSetPath, testCase.getUuid()), testCase)) return true;

        final @NotNull List<UUID> testCaseUuids = testCaseUuidsByTestSet.computeIfAbsent(testSetPath.toString(), _ -> testCaseUuids(List.of()));
        if (!testCaseUuids.contains(testCase.getUuid())) testCaseUuids.add(testCase.getUuid());

        return false;
    }

    // UC-EDITOR-PANEL-017, Rule-INTERNAL-035, Rule-INTERNAL-112, Rule-INTERNAL-145
    boolean move(final @NotNull Path fromSet, final @NotNull Path toSet, final @NotNull TestCase testCase) {
        final @NotNull UUID testCaseUuid = testCase.getUuid();
        final @NotNull Path from = named(fromSet, testCaseUuid);
        final @NotNull Path to = named(toSet, testCaseUuid);
        final @NotNull Optional<TestCase> was = Optional.ofNullable(testCasesByUuid.get(testCaseUuid));

        if (failedToWrite(toSet, testCase)) return false;

        testCasesByUuid.put(testCaseUuid, testCase);
        if (from.equals(to)) return true;

        if (testDataFiles.discard(from)) {
            if (!fromSet.equals(toSet))
                Optional.ofNullable(testCaseUuidsByTestSet.get(fromSet.toString())).ifPresent(testCaseUuids -> testCaseUuids.remove(testCaseUuid));
            createdSinceReadBegan.computeIfPresent(testCaseUuid, (_, _) -> toSet.toString());
            return true;
        }

        testDataFiles.discard(to);
        if (!fromSet.equals(toSet))
            Optional.ofNullable(testCaseUuidsByTestSet.get(toSet.toString())).ifPresent(testCaseUuids -> testCaseUuids.remove(testCaseUuid));
        was.ifPresent(original -> testCasesByUuid.put(testCaseUuid, original));
        return false;
    }

    // Rule-INTERNAL-112, Rule-INTERNAL-145
    boolean remove(final @NotNull Path testSetPath, final @NotNull UUID testCaseUuid) {
        if (!testDataFiles.remove(named(testSetPath, testCaseUuid))) return false;

        testCasesByUuid.remove(testCaseUuid);
        createdSinceReadBegan.remove(testCaseUuid);
        Optional.ofNullable(testCaseUuidsByTestSet.get(testSetPath.toString()))
                .ifPresent(testCaseUuids -> testCaseUuids.remove(testCaseUuid));
        return true;
    }

    // UC-INTERNAL-004, Rule-INTERNAL-031, Rule-INTERNAL-117, Rule-INTERNAL-029, Rule-INTERNAL-145
    boolean updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCase> orderedList, final @NotNull List<TestCase> moved) {
        final @NotNull String path = testSetPath.toString();
        final @NotNull List<UUID> testCaseUuids = new ArrayList<>(orderedList.size());
        final @NotNull Set<UUID> newUuids = new HashSet<>();

        final @NotNull Map<UUID, TestCase> placed = new HashMap<>();
        for (final TestCase testCase : moved) placed.put(testCase.getUuid(), testCase);

        final @NotNull String tester = settings.testerName;
        boolean allWritten = true;

        for (final TestCase testCase : orderedList) {
            final @NotNull TestCase written = placed.getOrDefault(testCase.getUuid(), testCase);
            final boolean firstSight = !testCasesByUuid.containsKey(testCase.getUuid());

            if (firstSight) {
                written.stampCreated(tester);
                if (failedToWrite(testSetPath, written)) {
                    allWritten = false;
                    continue;
                }

                testCasesByUuid.put(testCase.getUuid(), testCase.takeValuesOf(written));
                createdSinceReadBegan.put(testCase.getUuid(), path);
            } else if (placed.containsKey(testCase.getUuid())) {
                allWritten &= store(testSetPath, written);
            }

            testCaseUuids.add(testCase.getUuid());
            newUuids.add(testCase.getUuid());
        }

        Optional.ofNullable(testCaseUuidsByTestSet.get(path)).ifPresent(oldUuids -> oldUuids.stream()
                .filter(testCaseUuid -> !newUuids.contains(testCaseUuid))
                .forEach(testCaseUuids::add));
        testCaseUuidsByTestSet.put(path, testCaseUuids(testCaseUuids));
        return allWritten;
    }

    void removeForTestSet(final @NotNull String path) {
        Optional.ofNullable(testCaseUuidsByTestSet.remove(path))
                .ifPresent(testCaseUuids -> testCaseUuids.forEach(testCasesByUuid::remove));
        unreadable.remove(path);
    }

    private static @NotNull List<UUID> withTestCase(final @NotNull List<UUID> listed, final @NotNull UUID testCaseUuid) {
        if (listed.contains(testCaseUuid)) return listed;

        final @NotNull List<UUID> testCaseUuids = testCaseUuids(listed);
        testCaseUuids.add(testCaseUuid);
        return testCaseUuids;
    }

    // Rule-INTERNAL-145
    void readBegins(final @NotNull Path testProjectPath) {
        createdSinceReadBegan.values().removeIf(testSetPath -> Path.of(testSetPath).startsWith(testProjectPath));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-021, Rule-INTERNAL-145
    void swapIn(final @NotNull Path testProjectPath, final @NotNull Map<UUID, TestCase> testCases, final @NotNull Map<String, List<UUID>> setTestCaseUuids, final @NotNull Map<String, Set<String>> unreadableTestCases) {
        final @NotNull Set<UUID> held = testCaseUuidsByTestSet.entrySet().stream()
                .filter(entry -> Path.of(entry.getKey()).startsWith(testProjectPath))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toCollection(HashSet::new));
        final @NotNull Map<UUID, String> created = createdSinceReadBegan.entrySet().stream()
                .filter(entry -> Path.of(entry.getValue()).startsWith(testProjectPath) && testCasesByUuid.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        testCasesByUuid.putAll(testCases);
        testCaseUuidsByTestSet.putAll(setTestCaseUuids);

        testCaseUuidsByTestSet.keySet().removeIf(path -> Path.of(path).startsWith(testProjectPath) && !setTestCaseUuids.containsKey(path));
        created.forEach((testCaseUuid, testSetPath) -> testCaseUuidsByTestSet.merge(testSetPath, testCaseUuids(List.of(testCaseUuid)), (listed, _) -> withTestCase(listed, testCaseUuid)));

        held.removeAll(testCases.keySet());
        held.removeAll(created.keySet());
        held.forEach(testCasesByUuid::remove);

        unreadable.putAll(unreadableTestCases);
        unreadable.keySet().removeIf(set -> Path.of(set).startsWith(testProjectPath) && !unreadableTestCases.containsKey(set));
    }

    void clear() {
        testCasesByUuid.clear();
        testCaseUuidsByTestSet.clear();
        unreadable.clear();
    }
}
