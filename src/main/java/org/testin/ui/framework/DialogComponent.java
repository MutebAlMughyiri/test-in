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

import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public interface DialogComponent {
    @NotNull JComponent getPanel();

    @NotNull JComponent getFocusComponent();

    // Rule-INTERNAL-060, Rule-INTERNAL-097
    default void hostedBy(final @NotNull DialogHost host, final @NotNull Runnable submit) {
        onSubmitRequest(submit);
    }

    void onSubmitRequest(@NotNull Runnable submit);

    default boolean wantsFocus() {
        return true;
    }

    default boolean acceptsDialogKeys() {
        return true;
    }

    default boolean fillsSpace() {
        return false;
    }

    default boolean canFillSpace() {
        return true;
    }
}
