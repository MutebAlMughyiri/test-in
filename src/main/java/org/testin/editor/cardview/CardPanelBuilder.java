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

package org.testin.editor.cardview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.AbstractEditorContextMenu;
import org.testin.editor.TestinEditor;
import org.testin.model.TestCaseDto;
import org.testin.model.node.AbstractNode;
import org.testin.ui.FontSync;
import org.testin.util.Bundle;

import javax.swing.BorderFactory;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CardPanelBuilder {
    private static final int RESIZE_SETTLES_MILLIS = 100;

    public static @NotNull CardView build(final @NotNull Project p, final @NotNull Disposable fontSyncDisposable, final @NotNull TestinEditor editor) {
        final @NotNull CollectionListModel<TestCaseDto> model = new CollectionListModel<>(new ArrayList<>());

        // UC-EDITOR-PANEL-001, UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-003
        final @NotNull JBList<TestCaseDto> list = new CardList(model, editor);
        list.setBackground(UIUtil.getPanelBackground());
        list.setOpaque(true);
        list.setPaintBusy(true);
        list.getEmptyText().setText(Bundle.message("editor.loading"));
        // Rule-INTERNAL-122
        list.getAccessibleContext().setAccessibleName(Bundle.message("editor.test.cases"));
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setExpandableItemsEnabled(false);

        FontSync.syncWithNativeEditor(p, list, fontSyncDisposable, _ -> list.updateUI());

        final @NotNull Timer remeasure = new Timer(RESIZE_SETTLES_MILLIS, _ -> model.allContentsChanged());
        remeasure.setRepeats(false);

        list.addComponentListener(new ComponentAdapter() {
            private int lastWidth = -1;

            @Override
            public void componentResized(final @NotNull ComponentEvent e) {
                if (list.getWidth() == lastWidth) return;

                lastWidth = list.getWidth();
                remeasure.restart();
            }
        });

        final @NotNull JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(UIUtil.getPanelBackground());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        return new CardView(model, list, scrollPane);
    }

    public static void wireCommonListeners(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull CardView view, final @NotNull AbstractNode dir, final @NotNull AbstractEditorContextMenu contextMenu, final @NotNull Supplier<Optional<JBTable>> gridTableSupplier, final @NotNull ToIntFunction<TestCaseDto> rowOnGrid, final @NotNull BooleanSupplier gridActiveSupplier) {
        final @NotNull JBList<TestCaseDto> list = view.list();

        final @NotNull CardMouseListener mouseListener = new CardMouseListener(p, editor, list, view.model(), dir, contextMenu);
        list.addMouseListener(mouseListener);
        list.addMouseWheelListener(mouseListener);
        list.addMouseMotionListener(mouseListener);

        contextMenu.registerShortcuts(list);

        list.addListSelectionListener(new CardSelectionListener(p, list, editor, dir.getPath2()));
        list.addListSelectionListener(new CardGridSelectionSynchronizer(list, gridTableSupplier, gridActiveSupplier, rowOnGrid));
    }
}
