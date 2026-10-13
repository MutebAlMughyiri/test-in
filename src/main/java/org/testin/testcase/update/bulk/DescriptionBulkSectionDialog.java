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
import org.testin.model.TestCaseDto;
import org.testin.services.Services;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.util.Bundle;
import org.testin.util.NameSanitizer;

import java.nio.file.Path;
import java.util.ArrayList;
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

public class DescriptionBulkSectionDialog extends AbstractJsonSplitBulkSectionDialog {
    private final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);

    public DescriptionBulkSectionDialog(final @NotNull Project project, final @NotNull List<TestCaseDto> selectedTestCases, final @NotNull Consumer<List<TestCaseDto>> updatedTestCases) {
        super(project, selectedTestCases, updatedTestCases);
    }

    private static @NotNull Optional<String> keyOf(final @NotNull String description) {
        return Optional.of(NameSanitizer.methodName(description))
                .filter(name -> !name.isEmpty())
                .map(NameSanitizer::methodKey);
    }

    @Override
    protected @NotNull TestSetEditorAttributes attribute() {
        return TestSetEditorAttributes.DESCRIPTION;
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return Bundle.message("bulk.title.description");
    }

    @Override
    protected @NotNull String getJsonFieldName() {
        return "description";
    }

    @Override
    protected boolean showsDescriptionContext() {
        return false;
    }

    @Override
    protected boolean acceptsBlank() {
        return false;
    }

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-224
    @Override
    protected @NotNull Set<Integer> clashing(final @NotNull List<TestCaseDto> testCases, final @NotNull List<EditedValue> newValues) {
        final @NotNull Set<UUID> inThisDialog = testCases.stream().map(TestCaseDto::getId).collect(Collectors.toSet());
        final @NotNull Map<Path, Set<String>> taken = new HashMap<>();

        for (int i = 0; i < testCases.size(); i++) {
            final @NotNull TestCaseDto testCase = testCases.get(i);
            final @NotNull Set<String> inItsSet = taken.computeIfAbsent(testCase.getParent().getPath(), path -> keysOutside(path, inThisDialog));

            if (!newValues.get(i).changed()) keyOf(testCase.getDescription()).ifPresent(inItsSet::add);
        }

        final @NotNull Set<Integer> clashing = new LinkedHashSet<>();
        final @NotNull List<String> notANameYet = new ArrayList<>();
        final @NotNull List<String> takenYet = new ArrayList<>();

        for (int i = 0; i < testCases.size(); i++) {
            final @NotNull EditedValue edited = newValues.get(i);
            if (!edited.changed() || edited.value().isEmpty()) continue;

            final @NotNull String methodName = NameSanitizer.methodName(edited.value());

            if (NameSanitizer.cannotMakeMethodName(edited.value())) {
                clashing.add(i);
                notANameYet.add(methodName);
                continue;
            }

            if (!Objects.requireNonNull(taken.get(testCases.get(i).getParent().getPath())).add(NameSanitizer.methodKey(methodName))) {
                clashing.add(i);
                takenYet.add(methodName);
            }
        }

        if (!notANameYet.isEmpty()) {
            notifier.softRefuse(project, Bundle.message("description.not.a.method.title"), Bundle.message("description.not.a.method.message", notANameYet.getFirst()));
        }
        if (!takenYet.isEmpty()) {
            notifier.softRefuse(project, Bundle.message("description.taken.title"), Bundle.message("description.taken.message", takenYet.getFirst()));
        }

        return clashing;
    }

    private @NotNull Set<String> keysOutside(final @NotNull Path testSet, final @NotNull Set<UUID> inThisDialog) {
        final @NotNull Set<String> keys = new HashSet<>();

        testCases.getTestCasesForTestSet(testSet).stream()
                .filter(sibling -> !inThisDialog.contains(sibling.getId()))
                .forEach(sibling -> keyOf(sibling.getDescription()).ifPresent(keys::add));

        return keys;
    }
}
