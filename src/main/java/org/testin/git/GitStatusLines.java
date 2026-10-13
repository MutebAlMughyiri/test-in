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
import org.testin.git.change.StatusKind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitStatusLines {
    // UC-SHARE-010, Rule-SHARE-048
    public static @NotNull List<StatusEntry> parseStatus(final @NotNull List<String> statusRecords) {
        final @NotNull List<StatusEntry> entries = new ArrayList<>();

        for (final Map.Entry<String, String> change : changes(statusRecords)) {
            final @NotNull String code = change.getKey().substring(0, 2);
            final @NotNull String from = change.getValue();
            if (code.indexOf('R') >= 0 && !from.isEmpty()) entries.add(new StatusEntry(StatusKind.DELETED, from));

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

    private static @NotNull StatusKind typeOf(final @NotNull String code) {
        if (code.equals("??") || code.indexOf('A') >= 0) return StatusKind.ADDED;
        if (code.indexOf('D') >= 0) return StatusKind.DELETED;

        if (code.indexOf('R') >= 0) return StatusKind.ADDED;

        return StatusKind.MODIFIED;
    }
}