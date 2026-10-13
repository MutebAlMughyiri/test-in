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

package org.testin.editor;

import com.intellij.ide.util.PropertiesComponent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.open.EditorKind;
import org.testin.logger.TestinLogger;
import org.testin.model.ShowableField;
import org.testin.util.FailureText;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShownFields {
    public static final @NotNull String IN_TEST_SETS = EditorKind.TEST_SET.detailsKey(4);

    public static final @NotNull String IN_TEST_RUNS = EditorKind.TEST_RUN.detailsKey(7);

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-022
    public static <E extends Enum<E> & ShowableField> @NotNull Set<E> read(final @NotNull String propertyKey, final @NotNull Class<E> fieldType) {
        final @NotNull List<E> options = List.of(fieldType.getEnumConstants());
        final @NotNull String defaults = options.stream()
                .filter(field -> field.getToolBarDefault().isSelectedByDefault())
                .map(Enum::name)
                .collect(Collectors.joining(","));

        final @NotNull Set<E> chosen = new HashSet<>();
        for (final String stored : PropertiesComponent.getInstance().getValue(propertyKey, defaults).split(",", -1)) {
            if (stored.isEmpty()) continue;
            try {
                chosen.add(Enum.valueOf(fieldType, stored));
            } catch (final IllegalArgumentException ex) {
                TestinLogger.error("Invalid editor field '" + stored + "' for " + propertyKey + ": " + FailureText.of(ex));
            }
        }

        options.forEach(field -> field.getToolBarDefault().enforceLock(field, chosen));
        return chosen;
    }

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-022
    public static <E extends Enum<E>> void write(final @NotNull String propertyKey, final @NotNull Set<E> chosen) {
        PropertiesComponent.getInstance().setValue(propertyKey, chosen.stream().map(Enum::name).collect(Collectors.joining(",")));
    }
}
