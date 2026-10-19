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

package org.testin.git.conflict;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.model.testrun.TestRunResultAnalysis;
import org.testin.testcase.TestSetEditorFields;

import java.util.Locale;

// UC-SHARE-018, Rule-SHARE-081
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FieldName {
    static @NotNull String of(final @NotNull String jsonField) {
        final int dot = jsonField.indexOf('.');
        if (dot < 0) return ofTestCaseField(jsonField);

        return ofTestRunKey(jsonField.substring(dot + 1), jsonField);
    }

    private static @NotNull String ofTestCaseField(final @NotNull String jsonField) {
        for (final TestSetEditorFields field : TestSetEditorFields.values()) {
            if (field.getStoredKey().equals(jsonField)) return field.getName();
        }

        return jsonField;
    }

    private static @NotNull String ofTestRunKey(final @NotNull String key, final @NotNull String jsonField) {
        final @NotNull String constant = constantOf(key);

        for (final TestRunConfiguration question : TestRunConfiguration.values()) {
            if (question.name().equals(constant)) return question.getDisplayName();
        }
        for (final TestRunResultAnalysis heading : TestRunResultAnalysis.values()) {
            if (heading.name().equals(key)) return heading.getRunItemStatus().getLabel();
        }

        return jsonField;
    }

    private static @NotNull String constantOf(final @NotNull String jsonKey) {
        return jsonKey.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }
}
