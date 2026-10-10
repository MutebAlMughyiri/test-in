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

package org.testin.testcase;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.services.Services;
import org.testin.undo.Operation;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;
import org.testin.util.Mapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record TestCaseSnapshot(@NotNull Path testSetPath, @NotNull List<TestCaseDto> present, @NotNull List<UUID> absent) {
    // UC-EDITOR-PANEL-012, Rule-EDITOR-PANEL-228
    public static @NotNull TestCaseSnapshot of(final @NotNull Project p, final @NotNull Path testSetPath, final @NotNull List<UUID> ids) {
        final @NotNull TestCases testCases = Services.getInstance(p, TestCases.class);
        final @NotNull List<TestCaseDto> present = new ArrayList<>(ids.size());
        final @NotNull List<UUID> absent = new ArrayList<>();

        for (final UUID id : ids)
            testCases.findTestCase(id)
                    .filter(tc -> tc.getParent().getPath().equals(testSetPath))
                    .ifPresentOrElse(tc -> present.add(tc.copy()), () -> absent.add(id));

        return new TestCaseSnapshot(testSetPath, present, absent);
    }

    // UC-EDITOR-PANEL-012, Rule-EDITOR-PANEL-067
    public static @NotNull String describe(final @NotNull String verb, final @NotNull List<TestCaseDto> testCases) {
        return testCases.size() == 1
                ? Bundle.message("snapshot.undo.one", verb, testCases.getFirst().getDescription())
                : Bundle.message("snapshot.undo.many", verb, String.valueOf(testCases.size()));
    }

    public static @NotNull List<UUID> idsOf(final @NotNull List<TestCaseDto> testCases) {
        return testCases.stream().map(TestCaseDto::getId).toList();
    }

    // UC-EDITOR-PANEL-012, Rule-EDITOR-PANEL-038
    public static void record(final @NotNull Project p, final @NotNull String description, final @NotNull TestCaseSnapshot before, final @NotNull TestCaseSnapshot after) {
        record(p, UndoScope.of(before.testSetPath()), description, List.of(before), List.of(after));
    }

    // UC-EDITOR-PANEL-012, Rule-EDITOR-PANEL-070
    public static void record(final @NotNull Project p, final @NotNull UndoScope scope, final @NotNull String description, final @NotNull List<TestCaseSnapshot> before, final @NotNull List<TestCaseSnapshot> after) {
        if (same(before, after, Services.getInstance(p, Mapper.class))) return;

        ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(p, UndoHistories.class).push(scope, new Operation(
                description,
                () -> TestCaseRestore.restore(p, before, after),
                () -> TestCaseRestore.restore(p, after, before),
                () -> {
                })));
    }

    private static boolean same(final @NotNull List<TestCaseSnapshot> before, final @NotNull List<TestCaseSnapshot> after, final @NotNull Mapper mapper) {
        if (before.size() != after.size()) return false;

        for (int i = 0; i < before.size(); i++)
            if (!before.get(i).sameAs(after.get(i), mapper)) return false;

        return true;
    }

    public @NotNull List<UUID> ids() {
        final @NotNull List<UUID> ids = new ArrayList<>(idsOf(present));
        ids.addAll(absent);
        return ids;
    }

    boolean sameAs(final @NotNull TestCaseSnapshot other, final @NotNull Mapper mapper) {
        return absent.equals(other.absent) && asJson(mapper).equals(other.asJson(mapper));
    }

    private @NotNull List<String> asJson(final @NotNull Mapper mapper) {
        return present.stream().map(mapper::writeValueAsString).sorted().toList();
    }
}
