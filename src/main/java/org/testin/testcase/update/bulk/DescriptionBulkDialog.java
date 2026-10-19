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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.testcase.DescriptionAsMethod;
import org.testin.testcase.TestSetEditorFields;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DescriptionBulkDialog extends BaseValueBulkDialog {
    private final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);

    public DescriptionBulkDialog(final @NotNull Project project, final @NotNull List<TestCase> selectedTestCases, final @NotNull Consumer<List<TestCase>> updatedTestCases) {
        super(project, TestSetEditorFields.DESCRIPTION, Bundle.message("bulk.title.description"), false, selectedTestCases, updatedTestCases);
    }

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-224, Rule-CODEGEN-079
    @Override
    protected @NotNull Set<Integer> clashing(final @NotNull List<TestCase> testCases, final @NotNull List<EditedValue> newValues) {
        final @NotNull Set<UUID> inThisDialog = testCases.stream().map(TestCase::getUuid).collect(Collectors.toSet());
        final @NotNull Map<Path, Set<String>> taken = new HashMap<>();

        for (int i = 0; i < testCases.size(); i++) {
            final @NotNull TestCase testCase = testCases.get(i);
            final @NotNull Set<String> inItsSet = taken.computeIfAbsent(testCase.getParent().getPath(), path -> keysOutside(path, inThisDialog));

            if (!newValues.get(i).changed())
                DescriptionAsMethod.keyOf(testCase.getDescription()).ifPresent(inItsSet::add);
        }

        final @NotNull Set<Integer> clashing = new LinkedHashSet<>();
        final @NotNull Map<DescriptionAsMethod, String> firstRefused = new EnumMap<>(DescriptionAsMethod.class);

        for (int i = 0; i < testCases.size(); i++) {
            final @NotNull EditedValue edited = newValues.get(i);
            if (!edited.changed()) continue;

            final @NotNull Set<String> inItsSet = Objects.requireNonNull(taken.get(testCases.get(i).getParent().getPath()));
            final @NotNull Optional<DescriptionAsMethod> refused = DescriptionAsMethod.refusalOf(edited.value(), inItsSet);
            if (refused.isPresent()) {
                clashing.add(i);
                firstRefused.putIfAbsent(refused.orElseThrow(), edited.value());
                continue;
            }

            DescriptionAsMethod.keyOf(edited.value()).ifPresent(inItsSet::add);
        }

        firstRefused.forEach((refusal, description) -> refusal.say(project, description));
        return clashing;
    }

    private @NotNull Set<String> keysOutside(final @NotNull Path testSet, final @NotNull Set<UUID> inThisDialog) {
        final @NotNull Set<String> keys = new HashSet<>();

        testCases.getTestCasesForTestSet(testSet).stream()
                .filter(sibling -> !inThisDialog.contains(sibling.getUuid()))
                .forEach(sibling -> DescriptionAsMethod.keyOf(sibling.getDescription()).ifPresent(keys::add));

        return keys;
    }
}
