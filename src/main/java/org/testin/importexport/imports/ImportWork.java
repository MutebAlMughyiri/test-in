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

package org.testin.importexport.imports;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.ThreadingAssertions;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeOn;
import org.testin.codegen.CodeChangeType;
import org.testin.codegen.NodeCode;
import org.testin.creator.CreateTestSet;
import org.testin.editor.open.TestinEditors;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.logger.TestinLogger;
import org.testin.model.NodeType;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.testcase.Can;
import org.testin.testcase.Rank;
import org.testin.testcase.TestSetEditorFields;
import org.testin.testcase.TestCaseOrder;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.NameSanitizer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

record ImportWork(@NotNull Project project, @NotNull Notifier notifier, @NotNull TestinEditors editors, @NotNull Nodes nodes, @NotNull TestCases indexedTestCases) {
    private static final int METHODS_PER_COMMAND = 200;

    ImportWork(final @NotNull Project project) {
        this(project, Services.getInstance(project, Notifier.class), Services.getInstance(project, TestinEditors.class), Services.getInstance(project, Nodes.class), Services.getInstance(project, TestCases.class));
    }

    private static void report(final int testCases, final long startedAt, final long readyAt) {
        final long finishedAt = System.currentTimeMillis();
        TestinLogger.info("Import: " + testCases + " test cases in " + (finishedAt - startedAt) + "ms"
                + " (waiting for the index " + (readyAt - startedAt) + "ms,"
                + " writing and generating " + (finishedAt - readyAt) + "ms)");
    }

    private static void onEdt(final @NotNull Runnable work) {
        ThreadingAssertions.assertBackgroundThread();
        ApplicationManager.getApplication().invokeAndWait(work, ModalityState.nonModal());
    }

    private static <T> @NotNull T onEdtCompute(final @NotNull Supplier<T> work) {
        final @NotNull List<T> answer = new ArrayList<>(1);
        onEdt(() -> answer.add(work.get()));
        return answer.getFirst();
    }

    // UC-SHARE-006, Rule-SHARE-031, Rule-SHARE-128
    private static @NotNull String testSetNameOf(final @NotNull String sheetName) {
        final @NotNull String name = NameSanitizer.removeSpecialChars(sheetName).trim();
        return name.isEmpty() ? Bundle.message("import.sheet.no.name") : name;
    }

    void openImportDialog(final @NotNull AbstractNode node) {
        new ImportDialog(project, TestSetEditorFields.all(Can.IMPORT),
                (file, format) -> format.importToFile(project, file),
                selectedTestCasesBySheet -> executeImportWriteAction(node, selectedTestCasesBySheet))
                .show();
    }

    // UC-SHARE-005, UC-SHARE-006
    private void executeImportWriteAction(final @NotNull AbstractNode selectedNode, final @NotNull Map<String, List<TestCase>> selectedTestCasesBySheet) {
        final boolean generateCode = CodeOn.isOnOrHinted(project);

        final int total = selectedTestCasesBySheet.values().stream().mapToInt(List::size).sum();

        BackgroundWork.run(project, Bundle.message("import.task.importing", String.valueOf(total), selectedNode.getName()),
                Bundle.message("import.failed.title"), indicator -> importInBackground(selectedNode, selectedTestCasesBySheet, generateCode, total, indicator));
    }

    // UC-SHARE-005, UC-SHARE-006
    void importInBackground(final @NotNull AbstractNode selectedNode, final @NotNull Map<String, List<TestCase>> selectedTestCasesBySheet, final boolean generateCode, final int total, final @NotNull ProgressIndicator indicator) {
        final @NotNull Path targetPath = selectedNode.getPath();

        indicator.setIndeterminate(false);
        final long startedAt = System.currentTimeMillis();

        if (generateCode) {
            indicator.setText2(Bundle.message("import.progress.indexing"));
            DumbService.getInstance(project).waitForSmartMode();
        }
        final long readyAt = System.currentTimeMillis();

        int imported = 0;

        final @NotNull Set<String> stillEmpty = new LinkedHashSet<>();

        try {
            final @NotNull Map<TestSetNode, List<TestCase>> targets =
                    targetSets(selectedNode, targetPath, selectedTestCasesBySheet);
            targets.keySet().forEach(made -> stillEmpty.add(made.getName()));

            for (final Map.Entry<TestSetNode, List<TestCase>> set : targets.entrySet()) {
                final @NotNull TestSetNode into = set.getKey();
                final @NotNull List<TestCase> testCases = set.getValue();
                final @NotNull Path setPath = into.getPath();

                final @NotNull List<TestCase> written = linkAndSaveTestCases(into, testCases, rankOfTail(setPath), indicator, imported, total);
                if (!written.isEmpty()) stillEmpty.remove(into.getName());

                if (generateCode) generateTestMethods(written, into.getName(), indicator);

                imported += written.size();

                // UC-SHARE-007, Rule-SHARE-037
                if (indicator.isCanceled()) break;
            }
        } catch (final Exception ex) {
            // UC-SHARE-007, Rule-SHARE-037
            TestinLogger.error("Import failed after at least " + imported + " of " + total + ": " + FailureText.of(ex));

            notifier.error(project, Bundle.message("import.failed.title"),
                    Bundle.message("import.failed.partial", String.valueOf(imported), String.valueOf(total), FailureText.of(ex)));

            refreshTarget(targetPath);
            return;
        }

        if (selectedNode.holdsTestCases()) {
            onEdt(() -> editors.closeThenOpen(selectedNode));
        }

        notifier.softShowCounted(project, Done.IMPORTED, imported);

        reportEmptySets(List.copyOf(stillEmpty));

        report(total, startedAt, readyAt);

        refreshTarget(targetPath);
    }

    private void refreshTarget(final @NotNull Path targetPath) {
        nodes.refreshDirectory(targetPath);
    }

    // UC-SHARE-007, Rule-SHARE-037
    private void reportEmptySets(final @NotNull List<String> empty) {
        if (empty.isEmpty()) return;

        final @NotNull String named = empty.stream().limit(5).collect(Collectors.joining(", "));
        final @NotNull String rest = empty.size() > 5
                ? Bundle.message("indexer.more", String.valueOf(empty.size() - 5))
                : "";
        final @NotNull String count = empty.size() == 1
                ? Bundle.message("import.empty.one")
                : Bundle.message("import.empty.many", String.valueOf(empty.size()));

        notifier.warn(project, Bundle.message("import.empty.title"),
                Bundle.message("import.empty.message", count, named, rest));
    }

    // UC-SHARE-006, Rule-SHARE-031
    private @NotNull Map<TestSetNode, List<TestCase>> targetSets(final @NotNull AbstractNode selectedNode, final @NotNull Path targetPath, final @NotNull Map<String, List<TestCase>> testCasesBySheet) {
        if (selectedNode instanceof TestSetNode testSet) {
            final @NotNull List<TestCase> everything = new ArrayList<>();
            testCasesBySheet.values().forEach(everything::addAll);

            return Map.of(testSet, everything);
        }

        final @NotNull Map<TestSetNode, List<TestCase>> sets = new LinkedHashMap<>();
        testCasesBySheet.forEach((sheetName, testCases) -> {
            final @NotNull String name = testSetNameOf(sheetName);
            final @NotNull Path path = targetPath.resolve(name);

            sets.put(onEdtCompute(() -> {
                final @NotNull TestSetNode made = (TestSetNode) new CreateTestSet(project)
                        .execute(name, selectedNode, path)
                        .orElseThrow(() -> new IllegalStateException(Bundle.message("import.set.not.made", name)));

                try {
                    NodeCode.of(NodeType.TEST_SET).getCreated().execute(project, made);
                } catch (final Exception ex) {
                    TestinLogger.error("Failed to create Java class: " + FailureText.of(ex));
                }

                return made;
            }), testCases);
        });

        return sets;
    }

    void generateTestMethods(final @NotNull List<TestCase> testCases, final @NotNull String targetName, final @NotNull ProgressIndicator indicator) {
        TestinLogger.info("Import: generating test methods for '" + targetName + "' with " + testCases.size() + " test cases");
        final long startedAt = System.currentTimeMillis();
        // Rule-CODEGEN-018
        final @NotNull String oneUndo = UUID.randomUUID().toString();

        for (int from = 0; from < testCases.size(); from += METHODS_PER_COMMAND) {
            final @NotNull List<TestCase> batch =
                    testCases.subList(from, Math.min(from + METHODS_PER_COMMAND, testCases.size()));
            final int written = from + batch.size();

            indicator.setText2(Bundle.message("import.progress.generating", String.valueOf(written), String.valueOf(testCases.size())));
            onEdt(() -> CodeChangeType.CREATE_TEST_CASE.executeAllNow(project, batch, oneUndo));
        }

        TestinLogger.info("Import: generated " + testCases.size() + " test methods in "
                + (System.currentTimeMillis() - startedAt) + "ms");
    }

    // UC-SHARE-005, Rule-SHARE-025, Rule-SHARE-037
    private @NotNull List<TestCase> linkAndSaveTestCases(final @NotNull TestSetNode into, final @NotNull List<TestCase> testCases, final @NotNull String tailRank, final @NotNull ProgressIndicator indicator, final int done, final int total) {
        String rank = tailRank;

        final @NotNull List<TestCase> placed = new ArrayList<>(testCases.size());
        for (final TestCase currentTestCase : testCases) {
            rank = Rank.after(rank);
            placed.add(currentTestCase.edit().order(rank).parent(into).build());
        }

        final @NotNull List<TestCase> written = new ArrayList<>(testCases.size());
        int tried = 0;
        for (final TestCase testCase : placed) {
            if (indicator.isCanceled()) break;

            if (indexedTestCases.putTestCaseVerbatim(into.getPath(), testCase)) written.add(testCase);

            tried++;
            indicator.setFraction((done + tried) / (double) total);
            indicator.setText2(testCase.getDescription());
        }

        return written;
    }

    private @NotNull String rankOfTail(final @NotNull Path directory) {
        return findExistingTail(directory).map(TestCase::getOrder).orElse("");
    }

    private @NotNull Optional<TestCase> findExistingTail(final @NotNull Path directory) {
        final @NotNull List<TestCase> existing =
                TestCaseOrder.ordered(indexedTestCases.getTestCasesForTestSet(directory));

        return existing.isEmpty() ? Optional.empty() : Optional.of(existing.getLast());
    }
}
