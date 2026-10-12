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
import org.testin.clipboard.CopyChoice;
import org.testin.git.change.FieldChange;
import org.testin.model.TestCaseDto;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public record TestCaseHistoryCard(@NotNull TestCaseHistoryKind kind, @NotNull String hash, @NotNull String who, @NotNull ZonedDateTime when, @NotNull String message, @NotNull String note, @NotNull List<FieldChange> changes, @NotNull String was, @NotNull String now) implements HistoryCard {
    static @NotNull TestCaseHistoryCard of(final @NotNull TestCaseHistoryKind kind, final @NotNull HistoryCommit commit, final @NotNull String note, final @NotNull List<FieldChange> changes, final @NotNull Optional<TestCaseDto> was, final @NotNull Optional<TestCaseDto> now) {
        return new TestCaseHistoryCard(kind, commit.hash(), commit.who(), commit.when(), commit.message(), note, changes, asText(was), asText(now));
    }

    // Rule-VIEW-PANEL-098
    static @NotNull TestCaseHistoryCard uncommitted(final @NotNull TestCaseHistoryKind kind, final @NotNull String who, final @NotNull ZonedDateTime when, final @NotNull List<FieldChange> changes, final @NotNull Optional<TestCaseDto> was, final @NotNull Optional<TestCaseDto> now) {
        return new TestCaseHistoryCard(kind, "", who, when, "", "", changes, asText(was), asText(now));
    }

    // Rule-VIEW-PANEL-117
    private static @NotNull String asText(final @NotNull Optional<TestCaseDto> version) {
        return version.map(CopyChoice.ALL_DETAILS::from).orElse("");
    }
}
