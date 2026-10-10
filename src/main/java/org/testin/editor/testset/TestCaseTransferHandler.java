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
import org.testin.logger.Logger;
import org.testin.model.TestCaseDto;
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
    private final @NotNull Project p;
    private final @NotNull TestinEditor editor;
    private final @NotNull Notifier notifier;

    public TestCaseTransferHandler(final @NotNull Project p, final @NotNull TestinEditor editor) {
        this.p = p;
        this.editor = editor;
        this.notifier = Services.getInstance(p, Notifier.class);
    }

    private static int landingIndex(final @NotNull List<TestCaseDto> allItems, final @NotNull Optional<TestCaseDto> above, final @NotNull Optional<TestCaseDto> below) {
        return above.map(tc -> Math.min(indexOfId(allItems, tc.getId()) + 1, allItems.size()))
                .or(() -> below.map(tc -> indexOfId(allItems, tc.getId())))
                .orElse(allItems.size());
    }

    private static int indexOfId(final @NotNull List<TestCaseDto> items, final @NotNull UUID id) {
        for (int i = 0; i < items.size(); i++) {
            if (id.equals(items.get(i).getId())) return i;
        }

        return items.size();
    }

    // UC-EDITOR-PANEL-010, Rule-EDITOR-PANEL-058
    @Override
    public int getSourceActions(final JComponent c) {
        return MOVE;
    }

    @Override
    protected @Nullable Transferable createTransferable(final JComponent c) {
        if (!(c instanceof JBList<?> rawList)) return null;

        final @NotNull List<TestCaseDto> items = rawList.getSelectedValuesList().stream()
                .filter(TestCaseDto.class::isInstance)
                .map(TestCaseDto.class::cast)
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
                return items;
            }
        };
    }

    @Override
    public boolean canImport(final TransferSupport support) {
        return support.isDataFlavorSupported(FLAVOR);
    }

    // UC-EDITOR-PANEL-010, Rule-EDITOR-PANEL-061, Rule-EDITOR-PANEL-282
    @Override
    public boolean importData(final TransferSupport support) {
        if (!support.isDataFlavorSupported(FLAVOR)) return false;

        final @NotNull List<TestCaseDto> orderBefore = editor.snapshotOfAll();

        try {
            final @NotNull Object data = support.getTransferable().getTransferData(FLAVOR);
            if (!(data instanceof List<?> rawList)) return false;

            final @NotNull List<TestCaseDto> itemsToMove = rawList.stream()
                    .filter(TestCaseDto.class::isInstance)
                    .map(TestCaseDto.class::cast)
                    .toList();

            if (itemsToMove.isEmpty()) return false;

            final @NotNull Set<UUID> movedIds = itemsToMove.stream().map(TestCaseDto::getId).collect(Collectors.toSet());

            final @NotNull Optional<TestCaseDto> above = anchorAboveDrop(support, movedIds);
            final @NotNull Optional<TestCaseDto> below = anchorBelowDrop(support, movedIds);

            final @NotNull List<TestCaseDto> allItems = editor.getAllTestCases();
            final @NotNull List<UUID> ids;

            synchronized (allItems) {
                final @NotNull Set<UUID> here = allItems.stream().map(TestCaseDto::getId).collect(Collectors.toSet());
                if (!here.containsAll(movedIds)) return false;

                allItems.removeIf(tc -> movedIds.contains(tc.getId()));
                allItems.addAll(landingIndex(allItems, above, below), itemsToMove);
                ids = TestCaseSnapshot.idsOf(allItems);
            }

            if (ids.equals(TestCaseSnapshot.idsOf(orderBefore))) return false;

            final @NotNull Path setPath = editor.getParent().getPath();
            final @NotNull TestCaseSnapshot before = TestCaseSnapshot.of(p, setPath, ids);

            editor.updateSequenceAndSaveAll(() -> {
                TestCaseSnapshot.record(p, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.reorder"), itemsToMove), before, TestCaseSnapshot.of(p, setPath, ids));
                notifier.softShowCounted(p, Done.RE_SORTED, itemsToMove.size());
            });

            itemsToMove.stream().findFirst().ifPresentOrElse(
                    editor::selectTestCase,
                    editor::refreshView
            );

            return true;
        } catch (final Exception ex) {
            putBack(orderBefore);

            Logger.error("Reordering the test cases failed: " + FailureText.of(ex));
            notifier.softRefuse(p, Bundle.message("save.failed"));
            return false;
        }
    }

    // UC-EDITOR-PANEL-015
    private void putBack(final @NotNull List<TestCaseDto> orderBefore) {
        final @NotNull List<TestCaseDto> allItems = editor.getAllTestCases();

        synchronized (allItems) {
            allItems.clear();
            allItems.addAll(orderBefore);
        }

        editor.refreshView();
    }

    // UC-EDITOR-PANEL-010, Rule-EDITOR-PANEL-059
    int dropIndex(final @NotNull TransferSupport support) {
        return ((JList.DropLocation) support.getDropLocation()).getIndex();
    }

    private @NotNull Optional<TestCaseDto> anchorBelowDrop(final @NotNull TransferSupport support, final @NotNull Set<UUID> movedIds) {
        if (!(support.getComponent() instanceof JBList<?> target)) return Optional.empty();

        final @NotNull ListModel<?> rows = target.getModel();

        for (int row = Math.max(0, dropIndex(support)); row < rows.getSize(); row++) {
            if (rows.getElementAt(row) instanceof TestCaseDto tc && !movedIds.contains(tc.getId()))
                return Optional.of(tc);
        }

        return Optional.empty();
    }

    // UC-EDITOR-PANEL-010, Rule-EDITOR-PANEL-059
    private @NotNull Optional<TestCaseDto> anchorAboveDrop(final @NotNull TransferSupport support, final @NotNull Set<UUID> movedIds) {
        if (!(support.getComponent() instanceof JBList<?> target)) return Optional.empty();

        final @NotNull ListModel<?> rows = target.getModel();
        final int drop = Math.min(dropIndex(support), rows.getSize());

        for (int row = drop - 1; row >= 0; row--) {
            if (rows.getElementAt(row) instanceof TestCaseDto tc && !movedIds.contains(tc.getId()))
                return Optional.of(tc);
        }

        return Optional.empty();
    }
}