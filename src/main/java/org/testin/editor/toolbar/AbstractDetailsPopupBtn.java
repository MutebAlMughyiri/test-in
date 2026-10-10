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

package org.testin.editor.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.CheckBoxList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.ShownFields;
import org.testin.model.ToolBarAttribute;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.ui.framework.AbstractIconButton;
import org.testin.util.Bundle;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractDetailsPopupBtn<E extends Enum<E> & ToolBarAttribute> extends AbstractIconButton implements ToolbarItem {
    protected static final @NotNull String FIELDS = Bundle.message("toolbar.fields");

    @Getter
    private final @NotNull Set<E> selectedDetails = new HashSet<>();

    private final @NotNull String propertyKey;
    private final @NotNull List<E> options;

    private final @NotNull AtomicBoolean refreshQueued = new AtomicBoolean();

    protected AbstractDetailsPopupBtn(final @NotNull String tooltip, final @NotNull String propertyKey, final @NotNull Class<E> attributes, final @NotNull Runnable onToolBarDetailsSelectedChanged) {
        super(tooltip, AllIcons.Actions.Selectall);

        this.propertyKey = propertyKey;
        this.options = Arrays.stream(attributes.getEnumConstants()).filter(attribute -> attribute.getToolBarDefault().isListed()).toList();

        selectedDetails.addAll(ShownFields.read(propertyKey, attributes));

        addActionListener(_ -> showDetailsPopup(onToolBarDetailsSelectedChanged));
    }

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-024
    private void showDetailsPopup(final @NotNull Runnable onToolBarDetailsSelectedChanged) {
        final @NotNull CheckBoxList<E> detailsList = fieldList(onToolBarDetailsSelectedChanged);

        JBPopupFactory.getInstance()
                .createComponentPopupBuilder(detailsList, detailsList)
                .setRequestFocus(true)
                .createPopup()
                .showUnderneathOf(this);
    }

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-024
    @NotNull CheckBoxList<E> fieldList(final @NotNull Runnable onToolBarDetailsSelectedChanged) {
        final @NotNull CheckBoxList<E> detailsList = new CheckBoxList<>() {
            // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-023
            @Override
            protected boolean isEnabled(final int index) {
                return Optional.ofNullable(getItemAt(index))
                        .map(field -> field.getToolBarDefault().isSwitchable())
                        .orElse(true);
            }
        };
        DialogStyle.styleContent(detailsList);

        options.forEach(attr -> detailsList.addItem(attr, attr.getName(), selectedDetails.contains(attr)));

        detailsList.setCheckBoxListListener((index, state) -> {
            Optional.ofNullable(detailsList.getItemAt(index)).ifPresent(field -> {
                if (state) selectedDetails.add(field);
                else selectedDetails.remove(field);
            });

            ShownFields.write(propertyKey, selectedDetails);

            if (refreshQueued.compareAndSet(false, true)) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    refreshQueued.set(false);
                    onToolBarDetailsSelectedChanged.run();
                });
            }
        });

        return detailsList;
    }
}
