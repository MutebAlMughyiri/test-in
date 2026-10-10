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

package org.testin.git.history;

import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record History(@NotNull List<HistoryEntry> entries, @NotNull List<BugCard> bugs, @NotNull List<String> order, @NotNull String problem) {
    // Rule-VIEW-PANEL-100
    public static final @NotNull History NOT_UNDER_GIT = new History(List.of(), List.of(), List.of(), Bundle.message("view.history.not.under.git"));

    static @NotNull History read(final @NotNull List<HistoryEntry> entries) {
        return new History(List.copyOf(entries), List.of(), List.of(), "");
    }

    public static @NotNull History failed(final @NotNull String reason) {
        return new History(List.of(), List.of(), List.of(), Bundle.message("view.history.failed", reason));
    }

    // Rule-VIEW-PANEL-106
    @NotNull History with(final @NotNull List<BugCard> bugCards, final @NotNull List<String> commitOrder) {
        return new History(entries, List.copyOf(bugCards), List.copyOf(commitOrder), problem);
    }

    // Rule-VIEW-PANEL-106
    @NotNull History withoutBugs(final @NotNull String reason) {
        return new History(entries, List.of(), List.of(), problem.isBlank() ? Bundle.message("view.history.bugs.failed", reason) : problem);
    }

    // Rule-VIEW-PANEL-105, Rule-VIEW-PANEL-107
    public @NotNull List<HistoryCard> cards() {
        final @NotNull Map<String, Integer> position = IntStream.range(0, order.size()).boxed().collect(Collectors.toMap(order::get, Function.identity(), (first, _) -> first));
        return Stream.<HistoryCard>concat(entries.stream(), bugs.stream())
                .sorted(Comparator.comparingInt((HistoryCard card) -> card.isCommitted() ? position.getOrDefault(card.hash(), order.size()) : -1))
                .toList();
    }
}
