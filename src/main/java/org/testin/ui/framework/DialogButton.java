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
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class DialogButton implements DialogComponent {
    private final @NotNull String face;

    private final @NotNull JButton button;

    private final @NotNull ButtonFooter footer;

    private @NotNull String chosen;

    private @NotNull Runnable submitRequest = () -> {
    };

    // Rule-INTERNAL-105, Rule-INTERNAL-140
    DialogButton(final @NotNull String text, final @NotNull List<String> choices, final @NotNull List<Alternative> others) {
        this.face = text;
        this.chosen = text;
        button = choices.isEmpty() ? new JButton(action(text)) : new JBOptionButton(action(text), choices.stream().map(this::action).toArray(Action[]::new));

        DialogStyle.asPrimary(button);

        footer = new ButtonFooter(Stream.concat(others.stream().map(DialogButton::other), Stream.of(button)).toArray(JComponent[]::new));
    }

    private static @NotNull JButton other(final @NotNull Alternative alternative) {
        final @NotNull JButton other = new JButton(alternative.name());
        other.addActionListener(_ -> alternative.action().run());
        return other;
    }

    public @NotNull String getChosen() {
        return chosen;
    }

    private @NotNull Action action(final @NotNull String label) {
        return new AbstractAction(label) {
            @Override
            public void actionPerformed(final ActionEvent event) {
                chosen = label;
                submitRequest.run();

                // Rule-INTERNAL-054
                chosen = face;
            }
        };
    }

    // UC-INTERNAL-007, Rule-INTERNAL-080
    public void enableUnless(final @NotNull Optional<String> reason) {
        footer.enableUnless(reason);
    }

    // Rule-INTERNAL-080
    public void tally(final @NotNull String text) {
        footer.showTally(text);
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
