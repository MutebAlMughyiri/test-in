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

package org.testin.runner;

import org.jetbrains.annotations.NotNull;
import org.testin.model.status.ExecutionStatus;
import org.testin.util.Bundle;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class ExecutionRegistry {
    private final @NotNull Set<String> launchedNames = ConcurrentHashMap.newKeySet();

    private final @NotNull Set<UUID> pending = ConcurrentHashMap.newKeySet();

    private final @NotNull Map<UUID, String> configOf = new ConcurrentHashMap<>();

    private final @NotNull Set<UUID> stopped = ConcurrentHashMap.newKeySet();

    private final @NotNull Map<UUID, ExecutionStatus> runItemStatus = new ConcurrentHashMap<>();

    private final @NotNull Map<UUID, Attempts> held = new ConcurrentHashMap<>();

    private final @NotNull Set<UUID> executingNow = ConcurrentHashMap.newKeySet();

    void starting(final @NotNull UUID testCaseId) {
        pending.add(testCaseId);
        stopped.remove(testCaseId);
    }

    boolean take(final @NotNull UUID testCaseId) {
        return pending.remove(testCaseId);
    }

    void launched(final @NotNull Collection<UUID> testCaseIds, final @NotNull String testRunName) {
        testCaseIds.forEach(testCaseId -> configOf.put(testCaseId, testRunName));
        launchedNames.add(testRunName);
    }

    boolean launchedHere(final @NotNull String testRunName) {
        return launchedNames.contains(testRunName);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-076, Rule-CODEGEN-094
    @NotNull String freeName(final @NotNull String wanted) {
        final @NotNull String own = Bundle.message("runner.execution.name", wanted);
        if (!launchedNames.contains(own)) return own;

        return IntStream.iterate(2, next -> next + 1)
                .mapToObj(next -> own + " (" + next + ")")
                .filter(candidate -> !launchedNames.contains(candidate))
                .findFirst()
                .orElseThrow();
    }

    void notStarting(final @NotNull UUID testCaseId) {
        pending.remove(testCaseId);
        configOf.remove(testCaseId);
    }

    boolean isStopped(final @NotNull UUID testCaseId) {
        return stopped.contains(testCaseId);
    }

    boolean isRunning(final @NotNull UUID testCaseId) {
        return pending.contains(testCaseId) || configOf.containsKey(testCaseId);
    }

    // Rule-EDITOR-PANEL-291
    boolean isExecutingNow(final @NotNull UUID testCaseId) {
        return executingNow.contains(testCaseId);
    }

    @NotNull ExecutionStatus executionStatusOf(final @NotNull UUID testCaseId) {
        return isRunning(testCaseId) ? ExecutionStatus.RUNNING : runItemStatus.getOrDefault(testCaseId, ExecutionStatus.IDLE);
    }

    // Rule-CODEGEN-110
    @NotNull Map<UUID, Attempts> attempted(final @NotNull UUID testCaseId, final @NotNull Attempt attempt) {
        if (attempt.executionStatus() == ExecutionStatus.IDLE && held.containsKey(testCaseId))
            return decide(List.of(testCaseId));
        if (!configOf.containsKey(testCaseId)) return Map.of(testCaseId, Attempts.of(attempt));
        if (attempt.executionStatus() == ExecutionStatus.RUNNING) {
            executingNow.add(testCaseId);
            return decidedBefore(testCaseId, attempt);
        }

        executingNow.remove(testCaseId);
        held.merge(testCaseId, Attempts.of(attempt), Attempts::then);
        return Map.of();
    }

    // Rule-CODEGEN-111
    @NotNull Optional<Attempts> takeUnwritten(final @NotNull UUID testCaseId) {
        final @NotNull Optional<Attempts> live = Optional.ofNullable(held.get(testCaseId)).filter(attempts -> attempts.written() < attempts.executionCount());
        live.ifPresent(attempts -> held.put(testCaseId, attempts.allWritten()));
        return live;
    }

    private @NotNull Map<UUID, Attempts> decidedBefore(final @NotNull UUID testCaseId, final @NotNull Attempt running) {
        final @NotNull String execution = configOf.getOrDefault(testCaseId, "");
        final @NotNull Map<UUID, Attempts> decided = decide(held.keySet().stream()
                .filter(other -> !other.equals(testCaseId) && execution.equals(configOf.get(other)))
                .toList());

        decided.put(testCaseId, Attempts.of(running));
        return decided;
    }

    private @NotNull Map<UUID, Attempts> decide(final @NotNull List<UUID> testCaseIds) {
        final @NotNull Map<UUID, Attempts> decided = new LinkedHashMap<>();

        testCaseIds.forEach(testCaseId -> Optional.ofNullable(held.remove(testCaseId)).ifPresent(attempts -> decided.put(testCaseId, attempts)));
        return decided;
    }

    // UC-CODEGEN-009, Rule-CODEGEN-038
    void reported(final @NotNull UUID testCaseId, final @NotNull ExecutionStatus executionStatus) {
        if (executionStatus == ExecutionStatus.RUNNING) return;

        pending.remove(testCaseId);
        configOf.remove(testCaseId);
        executingNow.remove(testCaseId);

        if (executionStatus == ExecutionStatus.IDLE && !stopped.contains(testCaseId)) return;

        runItemStatus.put(testCaseId, executionStatus);
    }

    // UC-CODEGEN-009, Rule-CODEGEN-037
    @NotNull Stop stopping(final @NotNull List<UUID> asked) {
        final @NotNull List<UUID> running = asked.stream().filter(this::isRunning).toList();
        if (running.isEmpty()) return Stop.NOTHING;

        final @NotNull Set<String> testRuns = running.stream()
                .map(testCaseId -> configOf.getOrDefault(testCaseId, ""))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());

        final @NotNull List<UUID> testCases = Stream.concat(
                        running.stream(),
                        configOf.entrySet().stream().filter(e -> testRuns.contains(e.getValue())).map(Map.Entry::getKey))
                .distinct()
                .toList();

        testCases.forEach(testCaseId -> {
            pending.remove(testCaseId);
            configOf.remove(testCaseId);
            held.remove(testCaseId);
            executingNow.remove(testCaseId);
            stopped.add(testCaseId);
        });

        return new Stop(testRuns, testCases);
    }

    @NotNull List<UUID> ended(final @NotNull String testRunName) {
        if (!launchedNames.remove(testRunName)) return List.of();

        final @NotNull List<UUID> abandoned = configOf.entrySet().stream()
                .filter(e -> e.getValue().equals(testRunName))
                .map(Map.Entry::getKey)
                .toList();

        abandoned.forEach(testCaseId -> {
            pending.remove(testCaseId);
            configOf.remove(testCaseId);
            executingNow.remove(testCaseId);
        });

        return abandoned;
    }
}
