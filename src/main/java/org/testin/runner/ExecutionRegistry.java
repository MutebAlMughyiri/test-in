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

    void starting(final @NotNull UUID id) {
        pending.add(id);
        stopped.remove(id);
    }

    boolean take(final @NotNull UUID id) {
        return pending.remove(id);
    }

    void launched(final @NotNull Collection<UUID> ids, final @NotNull String testRunName) {
        ids.forEach(id -> configOf.put(id, testRunName));
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

    void notStarting(final @NotNull UUID id) {
        pending.remove(id);
        configOf.remove(id);
    }

    boolean isStopped(final @NotNull UUID id) {
        return stopped.contains(id);
    }

    boolean isRunning(final @NotNull UUID id) {
        return pending.contains(id) || configOf.containsKey(id);
    }

    // Rule-EDITOR-PANEL-291
    boolean isExecutingNow(final @NotNull UUID id) {
        return executingNow.contains(id);
    }

    @NotNull ExecutionStatus statusOf(final @NotNull UUID id) {
        return isRunning(id) ? ExecutionStatus.RUNNING : runItemStatus.getOrDefault(id, ExecutionStatus.IDLE);
    }

    // Rule-CODEGEN-110
    @NotNull Map<UUID, Attempts> attempted(final @NotNull UUID id, final @NotNull Attempt attempt) {
        if (attempt.status() == ExecutionStatus.IDLE && held.containsKey(id)) return decide(List.of(id));
        if (!configOf.containsKey(id)) return Map.of(id, Attempts.of(attempt));
        if (attempt.status() == ExecutionStatus.RUNNING) {
            executingNow.add(id);
            return decidedBefore(id, attempt);
        }

        executingNow.remove(id);
        held.merge(id, Attempts.of(attempt), Attempts::then);
        return Map.of();
    }

    // Rule-CODEGEN-111
    @NotNull Optional<Attempts> takeUnwritten(final @NotNull UUID id) {
        final @NotNull Optional<Attempts> live = Optional.ofNullable(held.get(id)).filter(attempts -> attempts.written() < attempts.executionCount());
        live.ifPresent(attempts -> held.put(id, attempts.allWritten()));
        return live;
    }

    private @NotNull Map<UUID, Attempts> decidedBefore(final @NotNull UUID id, final @NotNull Attempt running) {
        final @NotNull String execution = configOf.getOrDefault(id, "");
        final @NotNull Map<UUID, Attempts> decided = decide(held.keySet().stream()
                .filter(other -> !other.equals(id) && execution.equals(configOf.get(other)))
                .toList());

        decided.put(id, Attempts.of(running));
        return decided;
    }

    private @NotNull Map<UUID, Attempts> decide(final @NotNull List<UUID> ids) {
        final @NotNull Map<UUID, Attempts> decided = new LinkedHashMap<>();

        ids.forEach(id -> Optional.ofNullable(held.remove(id)).ifPresent(attempts -> decided.put(id, attempts)));
        return decided;
    }

    // UC-CODEGEN-009, Rule-CODEGEN-038
    void reported(final @NotNull UUID id, final @NotNull ExecutionStatus status) {
        if (status == ExecutionStatus.RUNNING) return;

        pending.remove(id);
        configOf.remove(id);
        executingNow.remove(id);

        if (status == ExecutionStatus.IDLE && !stopped.contains(id)) return;

        runItemStatus.put(id, status);
    }

    // UC-CODEGEN-009, Rule-CODEGEN-037
    @NotNull Stop stopping(final @NotNull List<UUID> asked) {
        final @NotNull List<UUID> running = asked.stream().filter(this::isRunning).toList();
        if (running.isEmpty()) return Stop.NOTHING;

        final @NotNull Set<String> testRuns = running.stream()
                .map(id -> configOf.getOrDefault(id, ""))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());

        final @NotNull List<UUID> testCases = Stream.concat(
                        running.stream(),
                        configOf.entrySet().stream().filter(e -> testRuns.contains(e.getValue())).map(Map.Entry::getKey))
                .distinct()
                .toList();

        testCases.forEach(id -> {
            pending.remove(id);
            configOf.remove(id);
            held.remove(id);
            executingNow.remove(id);
            stopped.add(id);
        });

        return new Stop(testRuns, testCases);
    }

    @NotNull List<UUID> ended(final @NotNull String testRunName) {
        if (!launchedNames.remove(testRunName)) return List.of();

        final @NotNull List<UUID> abandoned = configOf.entrySet().stream()
                .filter(e -> e.getValue().equals(testRunName))
                .map(Map.Entry::getKey)
                .toList();

        abandoned.forEach(id -> {
            pending.remove(id);
            configOf.remove(id);
            executingNow.remove(id);
        });

        return abandoned;
    }
}
