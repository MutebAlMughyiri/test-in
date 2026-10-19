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
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.undo.Operation;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;
import org.testin.util.JsonCodec;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record TestCaseSnapshot(@NotNull Path testSetPath, @NotNull List<TestCase> present, @NotNull List<UUID> absent) {
    // UC-EDITOR-PANEL-012, Rule-EDITOR-PANEL-228
    public static @NotNull TestCaseSnapshot of(final @NotNull Project project, final @NotNull Path testSetPath, final @NotNull List<UUID> testCaseUuids) {
        final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);
        final @NotNull List<TestCase> present = new ArrayList<>(testCaseUuids.size());
        final @NotNull List<UUID> absent = new ArrayList<>();

        for (final UUID testCaseUuid : testCaseUuids)
            testCases.findTestCase(testCaseUuid)
                    .filter(testCase -> testCase.getParent().getPath().equals(testSetPath))
                    .ifPresentOrElse(testCase -> present.add(testCase.copy()), () -> absent.add(testCaseUuid));

        return new TestCaseSnapshot(testSetPath, present, absent);
    }

    // UC-EDITOR-PANEL-012, Rule-EDITOR-PANEL-067
    public static @NotNull String describe(final @NotNull String verb, final @NotNull List<TestCase> testCases) {
        return testCases.size() == 1
                ? Bundle.message("snapshot.undo.one", verb, testCases.getFirst().getDescription())
                : Bundle.message("snapshot.undo.many", verb, String.valueOf(testCases.size()));
    }

    public static @NotNull List<UUID> uuidsOf(final @NotNull List<TestCase> testCases) {
        return testCases.stream().map(TestCase::getUuid).toList();
    }

    // UC-EDITOR-PANEL-012, Rule-EDITOR-PANEL-038
    public static void record(final @NotNull Project project, final @NotNull String description, final @NotNull TestCaseSnapshot before, final @NotNull TestCaseSnapshot after) {
        record(project, UndoScope.of(before.testSetPath()), description, List.of(before), List.of(after));
    }

    // UC-EDITOR-PANEL-012, Rule-EDITOR-PANEL-070
    public static void record(final @NotNull Project project, final @NotNull UndoScope scope, final @NotNull String description, final @NotNull List<TestCaseSnapshot> before, final @NotNull List<TestCaseSnapshot> after) {
        if (same(before, after, Services.getInstance(project, JsonCodec.class))) return;

        ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(project, UndoHistories.class).push(scope, new Operation(
                description,
                () -> TestCaseRestore.restore(project, before, after),
                () -> TestCaseRestore.restore(project, after, before),
                () -> {
                })));
    }

    private static boolean same(final @NotNull List<TestCaseSnapshot> before, final @NotNull List<TestCaseSnapshot> after, final @NotNull JsonCodec mapper) {
        if (before.size() != after.size()) return false;

        for (int i = 0; i < before.size(); i++)
            if (!before.get(i).sameAs(after.get(i), mapper)) return false;

        return true;
    }

    // Rule-EDITOR-PANEL-038
    public @NotNull TestCaseSnapshot with(final @NotNull TestCaseSnapshot more) {
        final @NotNull List<TestCase> allPresent = new ArrayList<>(present);
        allPresent.addAll(more.present);
        final @NotNull List<UUID> allAbsent = new ArrayList<>(absent);
        allAbsent.addAll(more.absent);

        return new TestCaseSnapshot(testSetPath, List.copyOf(allPresent), List.copyOf(allAbsent));
    }

    public @NotNull List<UUID> uuids() {
        final @NotNull List<UUID> testCaseUuids = new ArrayList<>(uuidsOf(present));
        testCaseUuids.addAll(absent);
        return testCaseUuids;
    }

    boolean sameAs(final @NotNull TestCaseSnapshot other, final @NotNull JsonCodec mapper) {
        return absent.equals(other.absent) && uuidsOf(present).equals(uuidsOf(other.present)) && asJson(mapper).equals(other.asJson(mapper));
    }

    private @NotNull List<String> asJson(final @NotNull JsonCodec mapper) {
        return present.stream().map(mapper::writeValueAsString).sorted().toList();
    }
}
