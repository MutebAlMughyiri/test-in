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

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import java.util.function.BooleanSupplier;

public final class SelectUnselectAllButton extends AbstractIconButton {
    private final @NotNull BooleanSupplier everyShownTicked;

    // Rule-TREE-PANEL-140
    public SelectUnselectAllButton(final @NotNull BooleanSupplier everyShownTicked, final @NotNull Runnable onClicked) {
        super(Bundle.message("selection.tick.all"), AllIcons.Actions.Selectall);
        this.everyShownTicked = everyShownTicked;
        addActionListener(_ -> onClicked.run());
    }

    // Rule-TREE-PANEL-140, Rule-INTERNAL-137
    public void updateState() {
        if (everyShownTicked.getAsBoolean()) show(Bundle.message("selection.untick.all"), AllIcons.Actions.Unselectall);
        else show(Bundle.message("selection.tick.all"), AllIcons.Actions.Selectall);
    }
}
