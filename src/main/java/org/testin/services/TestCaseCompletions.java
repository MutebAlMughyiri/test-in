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

package org.testin.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Service(Service.Level.PROJECT)
public final class TestCaseCompletions implements Disposable {
    private final @NotNull Set<String> descriptions = ConcurrentHashMap.newKeySet();
    private final @NotNull Set<String> expectedResults = ConcurrentHashMap.newKeySet();
    private final @NotNull Set<String> modules = ConcurrentHashMap.newKeySet();
    private final @NotNull Set<String> steps = ConcurrentHashMap.newKeySet();

    private final @NotNull Set<String> groups = ConcurrentHashMap.newKeySet();
    private final @NotNull AtomicBoolean reloadScheduled = new AtomicBoolean();

    private final @NotNull ExecutorService updates = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("Testin completion values");

    private static void addTo(final @NotNull Set<String> target, final @NotNull String value) {
        if (!value.isBlank()) target.add(value.trim());
    }

    private static void replace(final @NotNull Set<String> live, final @NotNull Set<String> rebuilt) {
        live.addAll(rebuilt);
        live.retainAll(rebuilt);
    }

    public @NotNull Set<String> getDescription() {
        return Collections.unmodifiableSet(descriptions);
    }

    public @NotNull Set<String> getExpectedResults() {
        return Collections.unmodifiableSet(expectedResults);
    }

    public @NotNull Set<String> getModules() {
        return Collections.unmodifiableSet(modules);
    }

    public @NotNull Set<String> getSteps() {
        return Collections.unmodifiableSet(steps);
    }

    public @NotNull Set<String> getGroups() {
        return Collections.unmodifiableSet(groups);
    }

    public void addDescription(final @NotNull String description) {
        addTo(descriptions, description);
    }

    public void addExpectedResult(final @NotNull String expectedResult) {
        addTo(expectedResults, expectedResult);
    }

    public void addModule(final @NotNull String module) {
        addTo(modules, module);
    }

    public void addGroup(final @NotNull String group) {
        addTo(groups, group);
    }

    public void addStep(final @NotNull String step) {
        addTo(steps, step);
    }

    public void load(final @NotNull List<TestCase> testCases) {
        cacheAsync(testCases);
    }

    public void addNewItems(final @NotNull List<TestCase> testCases) {
        cacheAsync(testCases);
    }

    public void reload(final @NotNull Supplier<@NotNull List<TestCase>> source) {
        if (!reloadScheduled.compareAndSet(false, true)) return;

        updates.execute(() -> {
            reloadScheduled.set(false);

            final @NotNull List<TestCase> testCases = source.get();

            final @NotNull Set<String> newDescriptions = ConcurrentHashMap.newKeySet();
            final @NotNull Set<String> newExpectedResults = ConcurrentHashMap.newKeySet();
            final @NotNull Set<String> newModules = ConcurrentHashMap.newKeySet();
            final @NotNull Set<String> newSteps = ConcurrentHashMap.newKeySet();
            final @NotNull Set<String> newGroups = ConcurrentHashMap.newKeySet();

            for (final TestCase testCase : testCases) {
                addTo(newDescriptions, testCase.getDescription());
                addTo(newExpectedResults, testCase.getExpectedResult());
                addTo(newModules, testCase.getModule());
                testCase.getSteps().forEach(step -> addTo(newSteps, step));
                testCase.getGroups().forEach(g -> addTo(newGroups, g));
            }

            replace(descriptions, newDescriptions);
            replace(expectedResults, newExpectedResults);
            replace(modules, newModules);
            replace(steps, newSteps);
            replace(groups, newGroups);
        });
    }

    private void cacheAsync(final @NotNull List<TestCase> testCases) {
        if (testCases.isEmpty()) return;
        updates.execute(() -> testCases.forEach(this::cache));
    }

    private void cache(final @NotNull TestCase testCase) {
        addDescription(testCase.getDescription());
        addExpectedResult(testCase.getExpectedResult());
        addModule(testCase.getModule());
        testCase.getSteps().forEach(this::addStep);
        testCase.getGroups().forEach(this::addGroup);
    }

    @Override
    public void dispose() {
        updates.shutdownNow();

        descriptions.clear();
        expectedResults.clear();
        modules.clear();
        steps.clear();
        groups.clear();
    }
}