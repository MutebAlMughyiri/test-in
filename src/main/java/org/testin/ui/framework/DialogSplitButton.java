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

import com.intellij.ui.components.JBOptionButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Optional;

public final class DialogSplitButton implements DialogComponent {
    private final @NotNull JBOptionButton button;
    private final @NotNull ButtonFooter footer;
    private final @NotNull String defaultLabel;
    private @NotNull String chosen;
    private @NotNull Runnable submitRequest = () -> {
    };

    DialogSplitButton(final @NotNull List<String> labels) {
        if (labels.isEmpty()) throw new IllegalStateException("A split button needs at least one action");

        defaultLabel = labels.getFirst();
        chosen = defaultLabel;

        final @NotNull Action main = action(labels.getFirst());
        final Action @NotNull [] alternatives = labels.stream().skip(1).map(this::action).toArray(Action[]::new);

        button = new JBOptionButton(main, alternatives.length == 0 ? null : alternatives);

        footer = new ButtonFooter(button);
    }

    public @NotNull String getChosen() {
        return chosen;
    }

    // UC-INTERNAL-007, Rule-INTERNAL-080
    public void enableUnless(final @NotNull Optional<String> reason) {
        footer.enableUnless(reason);
    }

    private @NotNull Action action(final @NotNull String label) {
        return new AbstractAction(label) {
            @Override
            public void actionPerformed(final ActionEvent event) {
                chosen = label;
                submitRequest.run();

                // Rule-INTERNAL-054
                chosen = defaultLabel;
            }
        };
    }

    @Override
    public @NotNull JComponent getPanel() {
        return footer.getPanel();
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return button;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        this.submitRequest = submit;
    }

    @Override
    public boolean canFillSpace() {
        return false;
    }
}
