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

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.ComponentWithEmptyText;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EmptyWarning {
    public static void show(final @NotNull JComponent field, final @NotNull String whatIsMissing) {
        if (field instanceof ComponentWithEmptyText withEmptyText) {
            withEmptyText.getEmptyText().clear();
            withEmptyText.getEmptyText().appendText(whatIsMissing, SimpleTextAttributes.ERROR_ATTRIBUTES);
            field.repaint();
        }

        field.requestFocusInWindow();
    }
}
