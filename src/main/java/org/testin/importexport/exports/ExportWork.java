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

package org.testin.importexport.exports;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.testcase.Can;
import org.testin.testcase.TestSetEditorFields;
import org.testin.ui.dialogs.Destination;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

record ExportWork(@NotNull Project project, @NotNull Notifier notifier, @NotNull TestCases testCases, @NotNull Nodes nodes) {
    ExportWork(final @NotNull Project project) {
        this(project, Services.getInstance(project, Notifier.class), Services.getInstance(project, TestCases.class), Services.getInstance(project, Nodes.class));
    }

    // UC-SHARE-002, Rule-SHARE-001
    private static @NotNull String unreadableWarning(final @NotNull List<String> unreadable) {
        final @NotNull String named = String.join(", ", unreadable.subList(0, Math.min(5, unreadable.size())));
        final @NotNull String rest = unreadable.size() > 5
                ? Bundle.message("export.unreadable.more", String.valueOf(unreadable.size() - 5))
                : "";
        final @NotNull String count = unreadable.size() == 1
                ? Bundle.message("export.unreadable.one")
                : Bundle.message("export.unreadable.many", String.valueOf(unreadable.size()));

        return Bundle.message("export.unreadable.message", count, named, rest);
    }

    private static @NotNull String uniqueKey(final @NotNull Map<String, ?> taken, final @NotNull List<String> treePath) {
        for (int from = treePath.size() - 1; from >= 0; from--) {
            final @NotNull String key = String.join(" - ", treePath.subList(from, treePath.size()));
            if (!taken.containsKey(key)) return key;
        }

        return String.join(" - ", treePath) + " (" + (taken.size() + 1) + ")";
    }

    // UC-SHARE-001, Rule-SHARE-015
    void exportFrom(final @NotNull AbstractNode node) {

        BackgroundWork.run(project, Bundle.message("export.task.reading", node.getName()), Bundle.message("export.failed.title"), _ -> {
            final @NotNull GatheredSheets gathered = gather(node);
            final @NotNull Map<String, List<TestCase>> sheets = gathered.sheets();
            if (sheets.isEmpty()) {
                ApplicationManager.getApplication().invokeLater(() ->
                        notifier.softRefuse(project, Bundle.message("notification.export.empty.title"), Bundle.message("export.none.found")));
                return;
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                if (gathered.unreadable().isEmpty()) {
                    chooseWhatToExport(sheets, node.getName());
                    return;
                }

                new ConfirmDialog(project, Bundle.message("export.unreadable.title"), unreadableWarning(gathered.unreadable()),
                        "", "", Bundle.message("export.anyway"), () -> chooseWhatToExport(sheets, node.getName())).show();
            });
        });
    }

    private void chooseWhatToExport(final @NotNull Map<String, List<TestCase>> sheets, final @NotNull String exportName) {
        new ExportDialog(project, TestSetEditorFields.all(Can.EXPORT), sheets, exportName, this::writeExport).show();
    }

    // UC-SHARE-001, Rule-SHARE-005
    private void writeExport(final @NotNull Destination destination, final @NotNull Map<String, List<TestCase>> selected) {
        final int testCases = selected.values().stream().mapToInt(List::size).sum();

        BackgroundWork.run(project, Bundle.message("export.task.writing", String.valueOf(testCases), destination.file().getName()),
                Bundle.message("export.failed.title"), _ -> {
                    destination.format().exportToFile(project, destination.file(), selected);

                    ExportNotice.show(project, destination.file(), testCases);
                });
    }

    // UC-SHARE-002, Rule-SHARE-001, Rule-SHARE-013
    private @NotNull GatheredSheets gather(final @NotNull AbstractNode node) {
        final @NotNull List<Sheet> found = new ArrayList<>();
        final @NotNull List<String> unreadable = new ArrayList<>();

        walk(node, List.of(node.getName()), found, unreadable);

        final @NotNull Map<String, List<TestCase>> sheets = new LinkedHashMap<>();
        for (final Sheet sheet : found) sheets.put(uniqueKey(sheets, sheet.treePath()), sheet.testCases());

        return new GatheredSheets(sheets, unreadable);
    }

    // UC-SHARE-003, Rule-SHARE-020, Rule-SHARE-011
    private @NotNull List<TestCase> detached(final @NotNull List<TestCase> testCases) {
        return testCases.stream()
                .map(TestCase::copy)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // Rule-SHARE-007, Rule-SHARE-014
    private void walk(final @NotNull AbstractNode node, final @NotNull List<String> treePath, final @NotNull List<Sheet> found, final @NotNull List<String> unreadable) {
        final @NotNull List<TestCase> here = testCases.getTestCasesForTestSet(node.getPath());
        if (!here.isEmpty()) found.add(new Sheet(treePath, detached(here)));

        unreadable.addAll(testCases.unreadableTestCasesIn(node.getPath()).stream().sorted().toList());

        for (final AbstractNode child : nodes.getChildren(node.getPath())) {
            final @NotNull List<String> under = new ArrayList<>(treePath);
            under.add(child.getName());
            walk(child, under, found, unreadable);
        }
    }
}
