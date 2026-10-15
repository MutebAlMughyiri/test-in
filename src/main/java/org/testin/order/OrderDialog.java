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

package org.testin.order;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.Marker;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.IntConsumer;

final class OrderDialog extends AbstractFrameworkDialog {
    private final @NotNull TextInput orderInput;

    private final @NotNull IntConsumer onSubmit;

    // UC-TREE-PANEL-015, Rule-TREE-PANEL-055
    OrderDialog(final @NotNull Project project, final int current, final @NotNull IntConsumer onSubmit) {
        super(project);
        this.onSubmit = onSubmit;

        title = Bundle.message("dialog.order.title");

        final @NotNull DialogPart<TextInput> built = DialogPart.textInput()
                .icon(AllIcons.Actions.Edit)
                .placeholder(Bundle.message("dialog.order.placeholder"))
                .value(shown(current))
                .accepting("[1-9][0-9]*")
                .build();
        orderInput = built.getComponent();
        components = List.of(built);

        shortcuts = List.of(
                StatusBarShortcut.confirm(this::submit),
                StatusBarShortcut.cancel(this::closeCancel));
    }

    private static @NotNull String shown(final int order) {
        return order == Marker.NOT_ORDERED ? "" : String.valueOf(order);
    }

    private static @NotNull OptionalInt typed(final @NotNull String text) {
        if (text.isEmpty()) return OptionalInt.of(Marker.NOT_ORDERED);

        try {
            final int number = Integer.parseInt(text);

            return number < Marker.NOT_ORDERED ? OptionalInt.of(number) : OptionalInt.empty();
        } catch (final NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }

    // UC-TREE-PANEL-015, Rule-TREE-PANEL-055, Rule-TREE-PANEL-054
    @Override
    protected void submit() {
        final @NotNull String text = orderInput.getText().trim();
        final @NotNull OptionalInt number = typed(text);

        if (number.isEmpty()) {
            notifier.softRefuse(project, Bundle.message("dialog.order.refused.title"),
                    Bundle.message("dialog.order.refused.message", Marker.NOT_ORDERED));
            return;
        }

        onSubmit.accept(number.getAsInt());
        closeOk();
    }
}
