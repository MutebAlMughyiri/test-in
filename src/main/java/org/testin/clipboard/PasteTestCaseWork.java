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

package org.testin.clipboard;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChangeType;
import org.testin.codegen.event.CopiedTestCase;
import org.testin.codegen.event.MovedTestCase;
import org.testin.editor.TestinEditor;
import org.testin.editor.open.TestinEditors;
import org.testin.editor.testset.TestSetEditor;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.TestCase.TestCaseBuilder;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testcase.Rank;
import org.testin.testcase.TestCaseOrder;
import org.testin.testcase.TestCaseSnapshot;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;

import java.awt.datatransfer.Transferable;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

record PasteTestCaseWork(@NotNull Project project, @NotNull TestinEditor editor, @NotNull CutState cutState, @NotNull TestCases testCases, @NotNull TestinEditors editors, @NotNull Notifier notifier, @NotNull AppSettingsState settings) {
    PasteTestCaseWork(final @NotNull Project project, final @NotNull TestinEditor editor) {
        this(project, editor, Services.getInstance(project, CutState.class), Services.getInstance(project, TestCases.class), Services.getInstance(project, TestinEditors.class), Services.getInstance(project, Notifier.class), Services.getInstance(project, AppSettingsState.class));
    }

    void paste() {
        final @NotNull List<TestCase> pastedTestCases = CopiedTestCases.onTheClipboard(project);
        if (pastedTestCases.isEmpty()) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            if (editor instanceof TestSetEditor destinationEditor) pasteInto(destinationEditor, pastedTestCases);
        });
    }

    // Rule-EDITOR-PANEL-084
    private void pasteInto(final @NotNull TestSetEditor destinationEditor, final @NotNull List<TestCase> pastedTestCases) {
        final boolean isCut = cutState.isCutOf(pastedTestCases);
        if (!isCut) cutState.clear();

        final @NotNull Optional<AbstractNode> cutTestSet = cutState.getCutTestSet();

        final @NotNull List<TestCase> cutTestCases = cutTestSet
                .map(source -> testCases.getTestCasesForTestSet(source.getPath()).stream().filter(testCase -> cutState.isPending(testCase.getUuid())).toList())
                .orElseGet(List::of);
        final @NotNull Optional<TestCaseSnapshot> cutFrom = cutTestSet
                .map(source -> TestCaseSnapshot.of(project, source.getPath(), TestCaseSnapshot.uuidsOf(cutTestCases)));

        // Rule-EDITOR-PANEL-083
        final @NotNull List<String> ranks = ranksUnderTheSelection(destinationEditor, isCut ? Set.copyOf(TestCaseSnapshot.uuidsOf(pastedTestCases)) : Set.of(), pastedTestCases.size());

        final @NotNull List<TestCase> pastedHere = new ArrayList<>(clonesInto(destinationEditor, pastedTestCases, isCut, ranks));

        // Rule-CODEGEN-078
        final @NotNull List<CopiedTestCase> copied = isCut ? List.of() : copiedFrom(pastedTestCases, pastedHere);

        final @NotNull Path destinationPath = destinationEditor.getParent().getPath();
        final @NotNull List<UUID> pastedUuids = TestCaseSnapshot.uuidsOf(pastedHere);
        final @NotNull List<TestCaseSnapshot> before = new ArrayList<>();
        cutFrom.ifPresent(before::add);
        before.add(TestCaseSnapshot.of(project, destinationPath, pastedUuids));

        final @NotNull Runnable persistThePaste = () -> {
            if (pastedHere.isEmpty()) return;

            final int pasted = pastedHere.size();

            destinationEditor.reorderAndPersist(() -> {
                final @NotNull List<TestCaseSnapshot> after = new ArrayList<>();
                cutFrom.ifPresent(taken -> after.add(TestCaseSnapshot.of(project, taken.testSetPath(), taken.uuids())));
                after.add(TestCaseSnapshot.of(project, destinationPath, pastedUuids));

                TestCaseSnapshot.record(project, UndoScope.of(destinationPath), TestCaseSnapshot.describe(isCut ? Bundle.message("snapshot.verb.move") : Bundle.message("snapshot.verb.paste"), pastedHere), before, after);

                // UC-EDITOR-PANEL-017, UC-CODEGEN-002, Rule-CODEGEN-078
                if (!isCut) CodeChangeType.COPY_TEST_CASE.executeAll(project, copied);

                else cutTestSet.ifPresent(source -> CodeChangeType.MOVE_TEST_CASE.executeAll(project,
                        pastedHere.stream().map(moved -> new MovedTestCase(moved, source)).toList()));

                notifier.softShowCounted(project, Done.PASTED, pasted);
            });

            if (isCut) cutState.clear();
        };

        cutTestSet.ifPresentOrElse(source -> moveThen(source, destinationEditor, pastedHere, persistThePaste), persistThePaste);
    }

    // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-082, Rule-INTERNAL-035
    private void moveThen(final @NotNull AbstractNode source, final @NotNull TestSetEditor destinationEditor, final @NotNull List<TestCase> pastedHere, final @NotNull Runnable persistThePaste) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull List<TestCase> stayed = movedOnDisk(source.getPath(), destinationEditor.getParent().getPath(), pastedHere);

            ApplicationManager.getApplication().invokeLater(() -> {
                afterTheMove(source, destinationEditor, pastedHere, stayed);
                persistThePaste.run();
            });
        });
    }

    private @NotNull List<TestCase> clonesInto(final @NotNull TestSetEditor destinationEditor, final @NotNull List<TestCase> pastedTestCases, final boolean isCut, final @NotNull List<String> ranks) {
        final @NotNull List<TestCase> clones = new ArrayList<>(pastedTestCases.size());
        for (int i = 0; i < pastedTestCases.size(); i++) {
            final @NotNull TestCase clonedTestCase = cloneForPasting(pastedTestCases.get(i), isCut, destinationEditor.getParent(), ranks.get(i));

            destinationEditor.getAllTestCases().add(clonedTestCase);
            clones.add(clonedTestCase);
        }

        return clones;
    }

    // Rule-CODEGEN-078
    private @NotNull List<CopiedTestCase> copiedFrom(final @NotNull List<TestCase> originals, final @NotNull List<TestCase> copies) {
        final @NotNull List<CopiedTestCase> copied = new ArrayList<>(originals.size());
        for (int i = 0; i < originals.size(); i++) {
            final @NotNull TestCase original = originals.get(i);
            copied.add(new CopiedTestCase(copies.get(i), testCases.findTestCase(original.getUuid()).orElse(original)));
        }

        return copied;
    }

    // UC-EDITOR-PANEL-017, Rule-INTERNAL-035
    private @NotNull List<TestCase> movedOnDisk(final @NotNull Path from, final @NotNull Path to, final @NotNull List<TestCase> pastedHere) {
        final @NotNull List<TestCase> stayed = new ArrayList<>();
        for (final TestCase moved : pastedHere) {
            if (!testCases.moveTestCase(from, to, moved)) stayed.add(moved);
        }

        return stayed;
    }

    // UC-EDITOR-PANEL-017, Rule-INTERNAL-035
    private void afterTheMove(final @NotNull AbstractNode source, final @NotNull TestSetEditor destinationEditor, final @NotNull List<TestCase> pastedHere, final @NotNull List<TestCase> stayed) {
        pastedHere.removeAll(stayed);
        destinationEditor.getAllTestCases().removeAll(stayed);

        final @NotNull Set<UUID> arrived = new HashSet<>(TestCaseSnapshot.uuidsOf(pastedHere));
        editors.editorFor(source).ifPresent(sourceEditor -> {
            sourceEditor.getAllTestCases().removeIf(held -> arrived.contains(held.getUuid()) && !pastedHere.contains(held));
            if (!sourceEditor.equals(destinationEditor)) sourceEditor.reorderAndPersist();
        });
    }

    boolean holdsTestCases(final @NotNull Transferable contents) {
        return !CopiedTestCases.in(project, contents).isEmpty();
    }

    // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-083
    private @NotNull List<String> ranksUnderTheSelection(final @NotNull TestSetEditor destinationEditor, final @NotNull Set<UUID> leaving, final int count) {
        final @NotNull List<TestCase> placed = TestCaseOrder.ordered(destinationEditor.getAllTestCases().stream().filter(testCase -> !testCase.getOrder().isEmpty()).toList());
        final int under = destinationEditor.getSelectedTestCases().stream()
                .filter(selected -> !selected.getOrder().isEmpty())
                .reduce((_, last) -> last)
                .map(selected -> TestCaseOrder.positionOf(placed, selected))
                .orElse(placed.size());

        @NotNull String previous = nearestStaying(placed.subList(0, under).reversed(), leaving);
        final @NotNull String upperBound = nearestStaying(placed.subList(under, placed.size()), leaving);

        final @NotNull List<String> ranks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            previous = Rank.between(previous, upperBound);
            ranks.add(previous);
        }

        return ranks;
    }

    private static @NotNull String nearestStaying(final @NotNull List<TestCase> nearestFirst, final @NotNull Set<UUID> leaving) {
        return nearestFirst.stream().filter(testCase -> !leaving.contains(testCase.getUuid())).findFirst().map(TestCase::getOrder).orElse("");
    }

    // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-081, Rule-EDITOR-PANEL-082
    private @NotNull TestCase cloneForPasting(final @NotNull TestCase original, final boolean isCut, final @NotNull TestSetNode parent, final @NotNull String rank) {
        final @NotNull TestCaseBuilder draft = original.edit().parent(parent).order(rank);

        if (isCut) {
            final @NotNull TestCase moved = draft.build();
            moved.touch(settings.testerName);
            return moved;
        }

        final @NotNull ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);

        return draft.uuid(UUID.randomUUID())
                .description(Bundle.message("paste.copy.suffix", original.getDescription()))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
