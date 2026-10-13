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
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.AbstractIconButton;
import org.testin.util.Bundle;

public class CardViewButton extends AbstractIconButton implements ToolbarItem {
    // UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-016
    public CardViewButton(final @NotNull Runnable onSwitchToList) {
        super(Bundle.message("toolbar.card.view"), AllIcons.General.LayoutEditorOnly);

        addActionListener(_ -> onSwitchToList.run());
    }
}
