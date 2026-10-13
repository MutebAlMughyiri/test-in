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

package org.testin.testcase.update.bulk;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

// UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-044
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BulkValueText {
    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-044
    static @NotNull String escape(final @NotNull String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-044
    static @NotNull String unescape(final @NotNull String text) {
        final @NotNull StringBuilder out = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); i++) {
            final char current = text.charAt(i);

            if (current != '\\' || i == text.length() - 1) {
                out.append(current);
                continue;
            }

            final char escaped = text.charAt(++i);

            switch (escaped) {
                case 'n' -> out.append('\n');
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                default -> out.append(current).append(escaped);
            }
        }

        return out.toString();
    }
}
