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

package org.testin.ui.framework;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.MenuItem;
import org.testin.model.StatusBarItem;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.ListValue;

import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

// UC-INTERNAL-007, Rule-INTERNAL-054, Rule-INTERNAL-067
@RequiredArgsConstructor
public final class ShortcutMenuPopup<T extends MenuItem> {
    private final @NotNull Project p;
    private final @NotNull String title;
    private final T @NotNull [] items;
    private final @NotNull Consumer<T> onSelection;

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-009
    private @NotNull Function<T, Optional<String>> refusal = _ -> Optional.empty();

    public @NotNull ShortcutMenuPopup<T> refusing(final @NotNull Function<T, Optional<String>> whyNot) {
        this.refusal = whyNot;
        return this;
    }

    // UC-INTERNAL-007, Rule-INTERNAL-053, Rule-INTERNAL-067
    public void show() {
        final @NotNull JBList<T> list = new JBList<>(items);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setCellRenderer(new ShortcutMenuRenderer<>(refusal));

        DialogStyle.styleContent(list);

        final @NotNull JBPanel<?> content = new JBPanel<>(new BorderLayout());
        content.setOpaque(false);
        content.add(new JBScrollPane(list), BorderLayout.CENTER);

        final @NotNull JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(content, list)
                .setTitle(title)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .createPopup();

        content.add(declaredKeys(list, popup), BorderLayout.SOUTH);
        bindRows(list, popup);

        popup.showCenteredInCurrentWindow(p);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-054, Rule-INTERNAL-067
    private @NotNull JBPanel<?> declaredKeys(final @NotNull JBList<T> list, final @NotNull JBPopup popup) {
        final @NotNull List<StatusBarShortcut> declared = List.of(
                StatusBarShortcut.confirm(() -> ListValue.selected(list).ifPresent(item -> select(item, popup))),
                StatusBarShortcut.select(),
                StatusBarShortcut.cancel(popup::cancel));

        DialogKeys.install(list, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, declared);

        return new ShortcutStrip(declared.toArray(StatusBarItem[]::new)).getPanel();
    }

    private void bindRows(final @NotNull JBList<T> list, final @NotNull JBPopup popup) {
        for (final T item : items) {
            item.bindShortcut(list, () -> select(item, popup));
        }

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final @NotNull MouseEvent e) {
                final int idx = list.locationToIndex(e.getPoint());
                if (idx < 0) return;

                list.setSelectedIndex(idx);
                ListValue.selected(list).ifPresent(item -> select(item, popup));
            }
        });
    }

    private void select(final @NotNull T item, final @NotNull JBPopup popup) {
        if (refusal.apply(item).isPresent()) return;

        popup.closeOk(null);
        ApplicationManager.getApplication().invokeLater(() -> onSelection.accept(item));
    }
}
