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

package org.testin.git;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinYml;
import org.testin.git.change.DiffType;
import org.testin.git.change.PendingChange;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitRefs {
    // UC-TREE-PANEL-001, Rule-TREE-PANEL-117
    private static final @NotNull Pattern CLONE_CHARACTERS = Pattern.compile("^[A-Za-z0-9._~:/?#@%+=-]+$");
    private static final @NotNull Pattern REMOTE_SCHEME = Pattern.compile("^(https?|ssh|git)://");

    // UC-SHARE-010, Rule-SHARE-048
    public static @NotNull List<StatusEntry> parseStatus(final @NotNull List<String> statusRecords) {
        final @NotNull List<StatusEntry> entries = new ArrayList<>();

        for (final Map.Entry<String, String> change : changes(statusRecords)) {
            final @NotNull String code = change.getKey().substring(0, 2);
            final @NotNull String from = change.getValue();
            if (code.indexOf('R') >= 0 && !from.isEmpty()) entries.add(new StatusEntry(DiffType.DELETED, from));

            entries.add(new StatusEntry(typeOf(code), change.getKey().substring(3)));
        }
        return entries;
    }

    // UC-TREE-PANEL-026, Rule-TREE-PANEL-085
    public static int changeCount(final @NotNull List<String> statusRecords) {
        return changes(statusRecords).size();
    }

    private static @NotNull List<Map.Entry<String, String>> changes(final @NotNull List<String> statusRecords) {
        final @NotNull List<Map.Entry<String, String>> changes = new ArrayList<>();
        final @NotNull Iterator<String> records = statusRecords.iterator();

        while (records.hasNext()) {
            final @NotNull String record = records.next();
            if (record.length() < 4) continue;

            final @NotNull String code = record.substring(0, 2);
            final boolean moved = code.indexOf('R') >= 0 || code.indexOf('C') >= 0;
            final @NotNull String from = moved && records.hasNext() ? records.next() : "";
            if (code.charAt(0) != '!') changes.add(Map.entry(record, from));
        }
        return changes;
    }

    static @NotNull List<String> records(final @NotNull String nulSeparated) {
        return Arrays.stream(nulSeparated.split("\0")).filter(record -> !record.isEmpty()).toList();
    }

    // UC-SHARE-017
    public static @NotNull String conflictMessage(final @NotNull List<String> unmergedPaths) {
        if (unmergedPaths.isEmpty()) {
            return Bundle.message("git.conflict.unnamed");
        }

        final int shown = Math.min(unmergedPaths.size(), 3);
        final @NotNull String names = String.join(", ", unmergedPaths.subList(0, shown));
        final int rest = unmergedPaths.size() - shown;

        return rest == 0
                ? Bundle.message("git.conflict.named", names)
                : Bundle.message("git.conflict.named.more", names, String.valueOf(rest));
    }

    private static @NotNull DiffType typeOf(final @NotNull String code) {
        if (code.equals("??") || code.indexOf('A') >= 0) return DiffType.ADDED;
        if (code.indexOf('D') >= 0) return DiffType.DELETED;

        if (code.indexOf('R') >= 0) return DiffType.ADDED;

        return DiffType.MODIFIED;
    }

    // UC-SHARE-016, Rule-SHARE-122
    public static @NotNull String chooseRemote(final @NotNull List<String> remotes) {
        final @NotNull List<String> names = remotes.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
        if (names.contains("origin")) return "origin";
        return names.isEmpty() ? "" : names.getFirst();
    }

    // UC-SHARE-008, Rule-SHARE-108
    public static boolean isEmailAddress(final @NotNull String text) {
        final @NotNull String value = text.trim();

        final int at = value.indexOf('@');
        if (at <= 0 || at != value.lastIndexOf('@')) return false;
        if (value.chars().anyMatch(Character::isWhitespace)) return false;

        final @NotNull String domain = value.substring(at + 1);

        return domain.length() >= 3 && domain.indexOf('.') > 0 && !domain.endsWith(".");
    }

    // Rule-TREE-PANEL-117
    public static boolean isRepositoryUrl(final @NotNull String text) {
        final @NotNull String value = text.trim();
        if (!CLONE_CHARACTERS.matcher(value).matches()) return false;

        return REMOTE_SCHEME.matcher(value).find()
                || value.startsWith(TestinYml.SCP_PREFIX)
                || value.endsWith(".git");
    }

    public static @NotNull String localNameOf(final @NotNull String remoteBranchName) {
        return remoteBranchName.substring(remoteBranchName.indexOf('/') + 1);
    }

    // UC-TREE-PANEL-026, Rule-TREE-PANEL-086, Rule-SHARE-126
    public static boolean isRemoteBranch(final @NotNull String branch, final @NotNull List<String> localBranches, final @NotNull List<String> remotes) {
        return !localBranches.contains(branch)
                && remotes.stream().map(String::trim).filter(remote -> !remote.isEmpty()).anyMatch(remote -> branch.startsWith(remote + "/"));
    }

    public static @NotNull Set<String> ancestorDirectories(final @NotNull Collection<String> relativePaths) {
        final @NotNull Set<String> directories = new LinkedHashSet<>();
        directories.add("");

        for (final String path : relativePaths) {
            for (int slash = path.indexOf('/'); slash >= 0; slash = path.indexOf('/', slash + 1)) {
                directories.add(path.substring(0, slash));
            }
        }
        return directories;
    }

    public static @NotNull Set<String> repoRelativePaths(final @NotNull Collection<PendingChange> changes) {
        return changes.stream()
                .map(PendingChange::relativeFilePath)
                .map(path -> path.toString().replace('\\', '/'))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
