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
import org.testin.codegen.GenType;
import org.testin.codegen.event.CopiedTestCase;
import org.testin.codegen.event.MovedTestCase;
import org.testin.editor.TestinEditor;
import org.testin.editor.testset.TestSetEditor;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.TestCaseDto.TestCaseDtoBuilder;
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

record PasteTestCaseWork(@NotNull Project p, @NotNull TestinEditor editor, @NotNull CutState cutState, @NotNull TestCases testCases, @NotNull Notifier notifier, @NotNull AppSettingsState settings) {
    PasteTestCaseWork(final @NotNull Project p, final @NotNull TestinEditor editor) {
        this(p, editor, Services.getInstance(p, CutState.class), Services.getInstance(p, TestCases.class), Services.getInstance(p, Notifier.class), Services.getInstance(p, AppSettingsState.class));
    }

    void paste() {
        final @NotNull List<TestCaseDto> pastedTestCases = CopiedTestCases.onTheClipboard(p);
        if (pastedTestCases.isEmpty()) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            if (editor instanceof TestSetEditor destUI) pasteInto(destUI, pastedTestCases);
        });
    }

    private void pasteInto(final @NotNull TestSetEditor destUI, final @NotNull List<TestCaseDto> pastedTestCases) {
        final boolean isCut = cutState.isCutOf(pastedTestCases);

        final @NotNull Optional<AbstractNode> cutFromSet =
                isCut ? cutState.source().map(TestinEditor::getParent) : Optional.empty();

        if (!isCut) cutState.clear();

        final @NotNull List<TestCaseDto> cutTestCases = cutState.source()
                .map(sourceUI -> sourceUI.getAllTestCases().stream().filter(testCase -> cutState.isPending(testCase.getId())).toList())
                .orElseGet(List::of);
        final @NotNull Optional<TestCaseSnapshot> cutFrom = cutState.source()
                .map(sourceUI -> TestCaseSnapshot.of(p, sourceUI.getParent().getPath(), TestCaseSnapshot.idsOf(cutTestCases)));

        // Rule-EDITOR-PANEL-083
        final @NotNull List<String> ranks = ranksUnderTheSelection(destUI, isCut ? Set.copyOf(TestCaseSnapshot.idsOf(pastedTestCases)) : Set.of(), pastedTestCases.size());

        final @NotNull List<TestCaseDto> pastedHere = new ArrayList<>(clonesInto(destUI, pastedTestCases, isCut, ranks));

        // Rule-CODEGEN-078
        final @NotNull List<CopiedTestCase> copied = isCut ? List.of() : copiedFrom(pastedTestCases, pastedHere);

        final @NotNull Path destPath = destUI.getParent().getPath();
        final @NotNull List<UUID> pastedIds = TestCaseSnapshot.idsOf(pastedHere);
        final @NotNull List<TestCaseSnapshot> before = new ArrayList<>();
        cutFrom.ifPresent(before::add);
        before.add(TestCaseSnapshot.of(p, destPath, pastedIds));

        // Rule-EDITOR-PANEL-082, Rule-INTERNAL-035
        cutState.source().ifPresent(sourceUI -> moveCut(sourceUI, destUI, cutTestCases, pastedHere));

        if (pastedHere.isEmpty()) return;

        final int pasted = pastedHere.size();

        destUI.reorderAndPersist(() -> {
            final @NotNull List<TestCaseSnapshot> after = new ArrayList<>();
            cutFrom.ifPresent(taken -> after.add(TestCaseSnapshot.of(p, taken.testSetPath(), taken.ids())));
            after.add(TestCaseSnapshot.of(p, destPath, pastedIds));

            TestCaseSnapshot.record(p, UndoScope.of(destPath), TestCaseSnapshot.describe(isCut ? Bundle.message("snapshot.verb.move") : Bundle.message("snapshot.verb.paste"), pastedHere), before, after);

            // UC-EDITOR-PANEL-017, UC-CODEGEN-002, Rule-CODEGEN-078
            if (!isCut) GenType.COPY_TEST_CASE.executeAll(p, copied);

            else cutFromSet.ifPresent(source -> GenType.MOVE_TEST_CASE.executeAll(p,
                    pastedHere.stream().map(moved -> new MovedTestCase(moved, source)).toList()));

            notifier.softShowCounted(p, Done.PASTED, pasted);
        });

        if (isCut) cutState.clear();
    }

    private @NotNull List<TestCaseDto> clonesInto(final @NotNull TestSetEditor destUI, final @NotNull List<TestCaseDto> pastedTestCases, final boolean isCut, final @NotNull List<String> ranks) {
        final @NotNull List<TestCaseDto> clones = new ArrayList<>(pastedTestCases.size());
        for (int i = 0; i < pastedTestCases.size(); i++) {
            final @NotNull TestCaseDto clonedTestCase = cloneForPasting(pastedTestCases.get(i), isCut, destUI.getParent(), ranks.get(i));

            destUI.getAllTestCases().add(clonedTestCase);
            clones.add(clonedTestCase);
        }

        return clones;
    }

    // Rule-CODEGEN-078
    private @NotNull List<CopiedTestCase> copiedFrom(final @NotNull List<TestCaseDto> originals, final @NotNull List<TestCaseDto> copies) {
        final @NotNull List<CopiedTestCase> copied = new ArrayList<>(originals.size());
        for (int i = 0; i < originals.size(); i++) {
            final @NotNull TestCaseDto original = originals.get(i);
            copied.add(new CopiedTestCase(copies.get(i), testCases.findTestCase(original.getId()).orElse(original)));
        }

        return copied;
    }

    // UC-EDITOR-PANEL-017, Rule-INTERNAL-035
    private void moveCut(final @NotNull TestinEditor sourceUI, final @NotNull TestSetEditor destUI, final @NotNull List<TestCaseDto> cutTestCases, final @NotNull List<TestCaseDto> pastedHere) {
        final @NotNull Path from = sourceUI.getParent().getPath();
        final @NotNull Path to = destUI.getParent().getPath();

        final @NotNull List<TestCaseDto> stayed = new ArrayList<>();
        for (final TestCaseDto moved : pastedHere) {
            if (!testCases.moveTestCase(from, to, moved)) stayed.add(moved);
        }

        pastedHere.removeAll(stayed);
        destUI.getAllTestCases().removeAll(stayed);

        final @NotNull Set<UUID> arrived = new HashSet<>(TestCaseSnapshot.idsOf(pastedHere));
        sourceUI.getAllTestCases().removeAll(cutTestCases.stream().filter(testCase -> arrived.contains(testCase.getId())).toList());
        if (!sourceUI.equals(destUI)) sourceUI.reorderAndPersist();
    }

    boolean holdsTestCases(final @NotNull Transferable contents) {
        return !CopiedTestCases.in(p, contents).isEmpty();
    }

    // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-083
    private @NotNull List<String> ranksUnderTheSelection(final @NotNull TestSetEditor destUI, final @NotNull Set<UUID> leaving, final int count) {
        final @NotNull List<TestCaseDto> ordered = TestCaseOrder.ordered(destUI.getAllTestCases().stream().filter(testCase -> !leaving.contains(testCase.getId())).toList());
        final @NotNull Optional<TestCaseDto> anchor = destUI.getSelectedTestCases().stream()
                .filter(selected -> !selected.getOrder().isEmpty())
                .reduce((_, last) -> last);

        final int under = anchor.map(selected -> TestCaseOrder.positionOf(ordered, selected)).orElse(ordered.size());

        @NotNull String previous = under > 0 && under <= ordered.size() ? ordered.get(under - 1).getOrder() : "";
        final @NotNull String upperBound = under < ordered.size() ? ordered.get(under).getOrder() : "";

        final @NotNull List<String> ranks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            previous = Rank.between(previous, upperBound);
            ranks.add(previous);
        }

        return ranks;
    }

    // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-081, Rule-EDITOR-PANEL-082
    private @NotNull TestCaseDto cloneForPasting(final @NotNull TestCaseDto original, final boolean isCut, final @NotNull TestSetNode parent, final @NotNull String rank) {
        final @NotNull TestCaseDtoBuilder draft = original.edit().parent(parent).order(rank);

        if (isCut) {
            final @NotNull TestCaseDto moved = draft.build();
            moved.touch(settings.testerName);
            return moved;
        }

        final @NotNull ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);

        return draft.id(UUID.randomUUID())
                .description(Bundle.message("paste.copy.suffix", original.getDescription()))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
