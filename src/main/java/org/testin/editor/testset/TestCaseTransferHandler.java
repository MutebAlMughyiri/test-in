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

package org.testin.editor.testset;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editor.TestinEditor;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.TestCaseSnapshot;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.TransferHandler;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TestCaseTransferHandler extends TransferHandler {
    private static final @NotNull DataFlavor FLAVOR = new DataFlavor(List.class, "List of TestCase");

    private final @NotNull Project project;

    private final @NotNull TestinEditor editor;

    private final @NotNull Notifier notifier;

    public TestCaseTransferHandler(final @NotNull Project project, final @NotNull TestinEditor editor) {
        this.project = project;
        this.editor = editor;
        this.notifier = Services.getInstance(project, Notifier.class);
    }

    private static int landingIndex(final @NotNull List<TestCase> allTestCases, final @NotNull Optional<TestCase> above, final @NotNull Optional<TestCase> below) {
        return above.map(testCase -> Math.min(indexOfUuid(allTestCases, testCase.getUuid()) + 1, allTestCases.size()))
                .or(() -> below.map(testCase -> indexOfUuid(allTestCases, testCase.getUuid())))
                .orElse(allTestCases.size());
    }

    private static int indexOfUuid(final @NotNull List<TestCase> testCases, final @NotNull UUID testCaseUuid) {
        for (int i = 0; i < testCases.size(); i++) {
            if (testCaseUuid.equals(testCases.get(i).getUuid())) return i;
        }

        return testCases.size();
    }

    // UC-EDITOR-PANEL-010, Rule-EDITOR-PANEL-058
    @Override
    public int getSourceActions(final JComponent component) {
        return MOVE;
    }

    @Override
    protected @Nullable Transferable createTransferable(final JComponent component) {
        if (!(component instanceof JBList<?> rawList)) return null;

        final @NotNull List<TestCase> testCases = rawList.getSelectedValuesList().stream()
                .filter(TestCase.class::isInstance)
                .map(TestCase.class::cast)
                .toList();

        return new Transferable() {
            @Override
            public DataFlavor @NotNull [] getTransferDataFlavors() {
                return new DataFlavor[]{FLAVOR};
            }

            @Override
            public boolean isDataFlavorSupported(final DataFlavor flavor) {
                return FLAVOR.equals(flavor);
            }

            @Override
            public @NotNull Object getTransferData(final @NotNull DataFlavor flavor) throws UnsupportedFlavorException {
                if (!FLAVOR.equals(flavor)) throw new UnsupportedFlavorException(flavor);
                return testCases;
            }
        };
    }

    @Override
    public boolean canImport(final TransferSupport support) {
        return support.isDataFlavorSupported(FLAVOR);
    }

    // UC-EDITOR-PANEL-010, Rule-EDITOR-PANEL-061, Rule-EDITOR-PANEL-282, Rule-EDITOR-PANEL-070
    @Override
    public boolean importData(final TransferSupport support) {
        if (!support.isDataFlavorSupported(FLAVOR)) return false;

        final @NotNull List<TestCase> orderBefore = editor.snapshotOfAll();

        try {
            final @NotNull Object data = support.getTransferable().getTransferData(FLAVOR);
            if (!(data instanceof List<?> rawList)) return false;

            final @NotNull List<TestCase> testCasesToMove = rawList.stream()
                    .filter(TestCase.class::isInstance)
                    .map(TestCase.class::cast)
                    .toList();

            if (testCasesToMove.isEmpty()) return false;

            final @NotNull Set<UUID> movedUuids = testCasesToMove.stream().map(TestCase::getUuid).collect(Collectors.toSet());

            final @NotNull Optional<TestCase> above = anchorAboveDrop(support, movedUuids);
            final @NotNull Optional<TestCase> below = anchorBelowDrop(support, movedUuids);

            final @NotNull List<TestCase> allTestCases = editor.getAllTestCases();
            final @NotNull List<UUID> testCaseUuids;

            synchronized (allTestCases) {
                final @NotNull Set<UUID> here = allTestCases.stream().map(TestCase::getUuid).collect(Collectors.toSet());
                if (!here.containsAll(movedUuids)) return false;

                allTestCases.removeIf(testCase -> movedUuids.contains(testCase.getUuid()));
                allTestCases.addAll(landingIndex(allTestCases, above, below), testCasesToMove);
                testCaseUuids = TestCaseSnapshot.uuidsOf(allTestCases);
            }

            if (testCaseUuids.equals(TestCaseSnapshot.uuidsOf(orderBefore))) return false;

            final @NotNull Path setPath = editor.getParent().getPath();
            final @NotNull TestCaseSnapshot before = TestCaseSnapshot.of(project, setPath, testCaseUuids);

            editor.updateSequenceAndSaveAll(() -> {
                TestCaseSnapshot.record(project, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.reorder"), testCasesToMove), before, TestCaseSnapshot.of(project, setPath, testCaseUuids));
                notifier.softShowCounted(project, Done.RE_SORTED, testCasesToMove.size());
            });

            testCasesToMove.stream().findFirst().ifPresentOrElse(
                    editor::selectTestCase,
                    editor::refreshView
            );

            return true;
        } catch (final Exception ex) {
            putBack(orderBefore);

            TestinLogger.error("Reordering the test cases failed: " + FailureText.of(ex));
            notifier.softRefuse(project, Bundle.message("save.failed"));
            return false;
        }
    }

    // UC-EDITOR-PANEL-015
    private void putBack(final @NotNull List<TestCase> orderBefore) {
        final @NotNull List<TestCase> allTestCases = editor.getAllTestCases();

        synchronized (allTestCases) {
            allTestCases.clear();
            allTestCases.addAll(orderBefore);
        }

        editor.refreshView();
    }

    // UC-EDITOR-PANEL-010, Rule-EDITOR-PANEL-059
    int dropIndex(final @NotNull TransferSupport support) {
        return ((JList.DropLocation) support.getDropLocation()).getIndex();
    }

    private @NotNull Optional<TestCase> anchorBelowDrop(final @NotNull TransferSupport support, final @NotNull Set<UUID> movedUuids) {
        if (!(support.getComponent() instanceof JBList<?> target)) return Optional.empty();

        final @NotNull ListModel<?> rows = target.getModel();

        for (int row = Math.max(0, dropIndex(support)); row < rows.getSize(); row++) {
            if (rows.getElementAt(row) instanceof TestCase testCase && !movedUuids.contains(testCase.getUuid()))
                return Optional.of(testCase);
        }

        return Optional.empty();
    }

    // UC-EDITOR-PANEL-010, Rule-EDITOR-PANEL-059
    private @NotNull Optional<TestCase> anchorAboveDrop(final @NotNull TransferSupport support, final @NotNull Set<UUID> movedUuids) {
        if (!(support.getComponent() instanceof JBList<?> target)) return Optional.empty();

        final @NotNull ListModel<?> rows = target.getModel();
        final int drop = Math.min(dropIndex(support), rows.getSize());

        for (int row = drop - 1; row >= 0; row--) {
            if (rows.getElementAt(row) instanceof TestCase testCase && !movedUuids.contains(testCase.getUuid()))
                return Optional.of(testCase);
        }

        return Optional.empty();
    }
}