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
import org.testin.model.status.AutomationExecutionStatus;
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

final class AutomationExecutionRegistry {
    private final @NotNull Set<String> launchedNames = ConcurrentHashMap.newKeySet();

    private final @NotNull Set<UUID> pending = ConcurrentHashMap.newKeySet();

    private final @NotNull Map<UUID, String> executionNameOf = new ConcurrentHashMap<>();

    private final @NotNull Set<UUID> stopped = ConcurrentHashMap.newKeySet();

    private final @NotNull Map<UUID, AutomationExecutionStatus> automationExecutionStatuses = new ConcurrentHashMap<>();

    private final @NotNull Map<UUID, Attempts> held = new ConcurrentHashMap<>();

    private final @NotNull Set<UUID> executingNow = ConcurrentHashMap.newKeySet();

    // Rule-CODEGEN-034
    void starting(final @NotNull UUID testCaseUuid) {
        pending.add(testCaseUuid);
        stopped.remove(testCaseUuid);
    }

    boolean take(final @NotNull UUID testCaseUuid) {
        return pending.remove(testCaseUuid);
    }

    void launched(final @NotNull Collection<UUID> testCaseUuids, final @NotNull String executionName) {
        testCaseUuids.forEach(testCaseUuid -> executionNameOf.put(testCaseUuid, executionName));
        launchedNames.add(executionName);
    }

    boolean launchedHere(final @NotNull String executionName) {
        return launchedNames.contains(executionName);
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

    void notStarting(final @NotNull UUID testCaseUuid) {
        pending.remove(testCaseUuid);
        executionNameOf.remove(testCaseUuid);
    }

    boolean isStopped(final @NotNull UUID testCaseUuid) {
        return stopped.contains(testCaseUuid);
    }

    boolean isRunning(final @NotNull UUID testCaseUuid) {
        return pending.contains(testCaseUuid) || executionNameOf.containsKey(testCaseUuid);
    }

    // Rule-EDITOR-PANEL-291
    boolean isExecutingNow(final @NotNull UUID testCaseUuid) {
        return executingNow.contains(testCaseUuid);
    }

    @NotNull AutomationExecutionStatus automationExecutionStatusOf(final @NotNull UUID testCaseUuid) {
        return isRunning(testCaseUuid) ? AutomationExecutionStatus.RUNNING : automationExecutionStatuses.getOrDefault(testCaseUuid, AutomationExecutionStatus.IDLE);
    }

    // Rule-CODEGEN-110
    @NotNull Map<UUID, Attempts> attempted(final @NotNull UUID testCaseUuid, final @NotNull Attempt attempt) {
        if (attempt.automationExecutionStatus() == AutomationExecutionStatus.IDLE && held.containsKey(testCaseUuid))
            return decide(List.of(testCaseUuid));
        if (!executionNameOf.containsKey(testCaseUuid)) return Map.of(testCaseUuid, Attempts.of(attempt));
        if (attempt.automationExecutionStatus() == AutomationExecutionStatus.RUNNING) {
            executingNow.add(testCaseUuid);
            return decidedBefore(testCaseUuid, attempt);
        }

        executingNow.remove(testCaseUuid);
        held.merge(testCaseUuid, Attempts.of(attempt), Attempts::then);
        return Map.of();
    }

    // Rule-CODEGEN-111
    @NotNull Optional<Attempts> takeUnwritten(final @NotNull UUID testCaseUuid) {
        final @NotNull Optional<Attempts> live = Optional.ofNullable(held.get(testCaseUuid)).filter(attempts -> attempts.written() < attempts.executionCount());
        live.ifPresent(attempts -> held.put(testCaseUuid, attempts.allWritten()));
        return live;
    }

    private @NotNull Map<UUID, Attempts> decidedBefore(final @NotNull UUID testCaseUuid, final @NotNull Attempt running) {
        final @NotNull String executionName = executionNameOf.getOrDefault(testCaseUuid, "");
        final @NotNull Map<UUID, Attempts> decided = decide(held.keySet().stream()
                .filter(other -> !other.equals(testCaseUuid) && executionName.equals(executionNameOf.get(other)))
                .toList());

        decided.put(testCaseUuid, Attempts.of(running));
        return decided;
    }

    private @NotNull Map<UUID, Attempts> decide(final @NotNull List<UUID> testCaseUuids) {
        final @NotNull Map<UUID, Attempts> decided = new LinkedHashMap<>();

        testCaseUuids.forEach(testCaseUuid -> Optional.ofNullable(held.remove(testCaseUuid)).ifPresent(attempts -> decided.put(testCaseUuid, attempts)));
        return decided;
    }

    // UC-CODEGEN-009, Rule-CODEGEN-038
    void reported(final @NotNull UUID testCaseUuid, final @NotNull AutomationExecutionStatus automationExecutionStatus) {
        if (automationExecutionStatus == AutomationExecutionStatus.RUNNING) return;

        pending.remove(testCaseUuid);
        executionNameOf.remove(testCaseUuid);
        executingNow.remove(testCaseUuid);

        if (automationExecutionStatus == AutomationExecutionStatus.IDLE && !stopped.contains(testCaseUuid)) return;

        automationExecutionStatuses.put(testCaseUuid, automationExecutionStatus);
    }

    // UC-CODEGEN-009, Rule-CODEGEN-037, Rule-VIEW-PANEL-054
    @NotNull Stop stopping(final @NotNull List<UUID> asked) {
        final @NotNull List<UUID> running = asked.stream().filter(this::isRunning).toList();
        if (running.isEmpty()) return Stop.NOTHING;

        final @NotNull Set<String> executionNames = running.stream()
                .map(testCaseUuid -> executionNameOf.getOrDefault(testCaseUuid, ""))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());

        final @NotNull List<UUID> testCaseUuids = Stream.concat(
                        running.stream(),
                        executionNameOf.entrySet().stream().filter(entry -> executionNames.contains(entry.getValue())).map(Map.Entry::getKey))
                .distinct()
                .toList();

        testCaseUuids.forEach(testCaseUuid -> {
            pending.remove(testCaseUuid);
            executionNameOf.remove(testCaseUuid);
            held.remove(testCaseUuid);
            executingNow.remove(testCaseUuid);
            stopped.add(testCaseUuid);
        });

        return new Stop(executionNames, testCaseUuids);
    }

    // Rule-CODEGEN-092
    @NotNull List<UUID> ended(final @NotNull String executionName) {
        if (!launchedNames.remove(executionName)) return List.of();

        final @NotNull List<UUID> abandoned = executionNameOf.entrySet().stream()
                .filter(entry -> entry.getValue().equals(executionName))
                .map(Map.Entry::getKey)
                .toList();

        abandoned.forEach(testCaseUuid -> {
            pending.remove(testCaseUuid);
            executionNameOf.remove(testCaseUuid);
            executingNow.remove(testCaseUuid);
        });

        return abandoned;
    }
}
