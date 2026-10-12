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

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.git.GitFailed;
import org.testin.git.GitRepositoryService;
import org.testin.git.change.FieldChange;
import org.testin.git.change.TestCaseChangeComparator;
import org.testin.indexer.TestCaseFile;
import org.testin.model.Config;
import org.testin.model.NodeType;
import org.testin.model.FileKind;
import org.testin.model.TestCaseDto;
import org.testin.model.node.AbstractNode;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.Mapper;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCaseHistory {
    static final @NotNull String RECORD = Character.toString(0x1E);
    static final @NotNull String FIELD = Character.toString(0x1F);
    private static final @NotNull String FORMAT = "--format=%x1e%H%x1f%an%x1f%aI%x1f%s";

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-113
    public static @NotNull Optional<TestCaseFile> deletedFile(final @NotNull Project p, final @NotNull UUID testCaseId) {
        final @NotNull GitRepositoryService git = new GitRepositoryService(p);
        return Services.getInstance(p, BoundTestProject.class).get()
                .map(AbstractNode::getPath)
                .filter(testProject -> !git.isNotRepository(testProject))
                .flatMap(testProject -> {
                    try {
                        return git.log(testProject, "-1", "--format=", "--name-only", "--", BugHistory.pathspec(NodeType.TCF, FileKind.TEST_CASE, testCaseId)).lines()
                                .filter(line -> !line.isBlank())
                                .findFirst()
                                .map(path -> new TestCaseFile(testProject, Path.of(path.strip())));
                    } catch (final GitFailed ex) {
                        return Optional.empty();
                    }
                });
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-096, Rule-VIEW-PANEL-099, Rule-VIEW-PANEL-100, Rule-VIEW-PANEL-101, Rule-VIEW-PANEL-113
    public static @NotNull History read(final @NotNull Project p, final @NotNull TestCaseFile file, final @NotNull Optional<TestCaseDto> now) {
        final @NotNull GitRepositoryService git = new GitRepositoryService(p);
        if (git.isNotRepository(file.testProject())) return History.NOT_UNDER_GIT;

        final @NotNull String path = file.inProject().toString().replace('\\', '/');
        try {
            final @NotNull List<HistoryCommit> commits = commits(git.log(file.testProject(), "--follow", FORMAT, "--name-only", "--", path), path);
            final @NotNull Map<String, String> versions = commits.isEmpty()
                    ? Map.of()
                    : git.objects(file.testProject(), commits.stream().map(HistoryCommit::objectName).toList());

            return History.read(entries(Services.getInstance(p, Mapper.class), commits, versions, now));
        } catch (final GitFailed ex) {
            return History.failed(FailureText.of(ex));
        }
    }

    static @NotNull List<HistoryCommit> commits(final @NotNull String log, final @NotNull String path) {
        return Arrays.stream(log.split(RECORD)).filter(record -> !record.isBlank()).map(record -> commit(record, path)).toList();
    }

    // Rule-INTERNAL-132
    static @NotNull ZonedDateTime authoredAt(final @NotNull String isoTime) {
        return ZonedDateTime.parse(isoTime).withZoneSameInstant(ZoneId.systemDefault());
    }

    private static @NotNull HistoryCommit commit(final @NotNull String record, final @NotNull String path) {
        final @NotNull List<String> lines = record.lines().filter(line -> !line.isBlank()).toList();
        final String @NotNull [] fields = lines.getFirst().split(FIELD, -1);

        return new HistoryCommit(fields[0], fields[1], authoredAt(fields[2]), fields.length > 3 ? fields[3] : "", lines.size() > 1 ? lines.getLast().strip() : path);
    }

    // Rule-VIEW-PANEL-096, Rule-VIEW-PANEL-098, Rule-VIEW-PANEL-113
    static @NotNull List<HistoryEntry> entries(final @NotNull Mapper mapper, final @NotNull List<HistoryCommit> commits, final @NotNull Map<String, String> versions, final @NotNull Optional<TestCaseDto> now) {
        final @NotNull List<Optional<TestCaseDto>> read = commits.stream().map(commit -> parsed(mapper, versions.getOrDefault(commit.objectName(), ""))).toList();
        final @NotNull List<HistoryEntry> entries = new ArrayList<>();

        uncommitted(read, now).ifPresent(entries::add);
        for (int at = 0; at < commits.size(); at++) {
            entries.add(entry(commits.get(at), read.get(at), at + 1 < read.size() ? read.get(at + 1) : Optional.empty(), at + 1 == commits.size(), at == 0 && now.isEmpty()));
        }
        return entries;
    }

    private static @NotNull Optional<HistoryEntry> uncommitted(final @NotNull List<Optional<TestCaseDto>> read, final @NotNull Optional<TestCaseDto> present) {
        if (present.isEmpty())
            return read.isEmpty() || read.getFirst().isEmpty() ? Optional.empty() : Optional.of(HistoryEntry.uncommitted(HistoryEntryKind.REMOVED, "", Config.NOT_EXECUTED, List.of(), read.getFirst(), Optional.empty()));

        final @NotNull TestCaseDto now = present.orElseThrow();
        if (read.isEmpty()) {
            return Optional.of(HistoryEntry.uncommitted(HistoryEntryKind.CREATED, now.getCreatedBy(), now.getCreatedAt(), List.of(), Optional.empty(), present));
        }

        return read.getFirst()
                .map(committed -> TestCaseChangeComparator.compare(committed, now))
                .filter(changes -> !changes.isEmpty())
                .map(changes -> HistoryEntry.uncommitted(HistoryEntryKind.UPDATED, lastEditor(now), lastEdited(now), changes, read.getFirst(), present));
    }

    private static @NotNull HistoryEntry entry(final @NotNull HistoryCommit commit, final @NotNull Optional<TestCaseDto> version, final @NotNull Optional<TestCaseDto> before, final boolean oldest, final boolean removedHere) {
        if (version.isEmpty()) {
            return removedHere
                    ? HistoryEntry.of(HistoryEntryKind.REMOVED, commit, "", List.of(), before, version)
                    : HistoryEntry.of(HistoryEntryKind.UPDATED, commit, Bundle.message("view.history.unreadable"), List.of(), before, version);
        }
        if (oldest) return HistoryEntry.of(HistoryEntryKind.CREATED, commit, "", List.of(), Optional.empty(), version);

        return before.map(older -> updated(commit, TestCaseChangeComparator.compare(older, version.orElseThrow()), before, version))
                .orElseGet(() -> HistoryEntry.of(HistoryEntryKind.UPDATED, commit, Bundle.message("view.history.uncompared"), List.of(), before, version));
    }

    // Rule-VIEW-PANEL-096
    private static @NotNull HistoryEntry updated(final @NotNull HistoryCommit commit, final @NotNull List<FieldChange> changes, final @NotNull Optional<TestCaseDto> was, final @NotNull Optional<TestCaseDto> now) {
        return HistoryEntry.of(HistoryEntryKind.UPDATED, commit, changes.isEmpty() ? Bundle.message("git.change.no.field") : "", changes, was, now);
    }

    static @NotNull Optional<TestCaseDto> parsed(final @NotNull Mapper mapper, final @NotNull String json) {
        if (json.isBlank()) return Optional.empty();

        try {
            return Optional.of(mapper.readValue(json, TestCaseDto.class));
        } catch (final UncheckedIOException unreadable) {
            return Optional.empty();
        }
    }

    private static @NotNull String lastEditor(final @NotNull TestCaseDto now) {
        return now.getUpdatedBy().isBlank() ? now.getCreatedBy() : now.getUpdatedBy();
    }

    private static @NotNull ZonedDateTime lastEdited(final @NotNull TestCaseDto now) {
        return now.getUpdatedBy().isBlank() ? now.getCreatedAt() : now.getUpdatedAt();
    }
}
