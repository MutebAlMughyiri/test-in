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

package org.testin.util;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SeparatedValues {
    private static final String BYTE_ORDER_MARK = "﻿";

    private final @NotNull String text;
    private final char separator;
    private final @NotNull List<List<String>> records = new ArrayList<>();
    private final @NotNull StringBuilder current = new StringBuilder();
    private @NotNull List<String> fields = new ArrayList<>();

    // Rule-EDITOR-PANEL-254, Rule-SHARE-124
    public static @NotNull List<List<String>> split(final @NotNull String text, final char separator) {
        return new SeparatedValues(text, separator).records();
    }

    // UC-EDITOR-PANEL-018, UC-SHARE-002, Rule-EDITOR-PANEL-087
    public static @NotNull String field(final @NotNull String value, final char separator) {
        if (value.indexOf(separator) < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private @NotNull List<List<String>> records() {
        int next = text.startsWith(BYTE_ORDER_MARK) ? 1 : 0;
        while (next < text.length()) {
            next = text.charAt(next) == '"' && current.isEmpty() ? readQuoted(next + 1) : readPlain(next);
        }

        if (!current.isEmpty() || !fields.isEmpty()) endRecord();

        return records;
    }

    // Rule-SHARE-124
    private int readQuoted(final int from) {
        int at = from;
        while (at < text.length()) {
            final char c = text.charAt(at);
            if (c != '"') {
                current.append(c);
                at++;
            } else if (charAt(at + 1) == '"') {
                current.append('"');
                at += 2;
            } else {
                return at + 1;
            }
        }

        return at;
    }

    private int readPlain(final int at) {
        final char c = text.charAt(at);
        if (c == separator) {
            endField();
        } else if (c == '\r' && charAt(at + 1) == '\n') {
            endRecord();
            return at + 2;
        } else if (c == '\n' || c == '\r') {
            endRecord();
        } else {
            current.append(c);
        }

        return at + 1;
    }

    private char charAt(final int at) {
        return at < text.length() ? text.charAt(at) : 0;
    }

    private void endField() {
        fields.add(current.toString());
        current.setLength(0);
    }

    private void endRecord() {
        endField();
        records.add(fields);
        fields = new ArrayList<>();
    }
}
