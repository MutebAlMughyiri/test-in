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

package org.testin;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OnScreen {

    public static @NotNull JFrame shown(final @NotNull JComponent content, final @NotNull Disposable owner) {
        final @NotNull JFrame frame = new JFrame();
        frame.add(content);
        frame.setSize(900, 700);
        frame.setVisible(true);
        frame.toFront();
        frame.requestFocus();
        Disposer.register(owner, frame::dispose);
        Await.until("the window never took the keyboard", frame::isFocused);
        return frame;
    }

    public static boolean pressKey(final @NotNull JComponent on, final @NotNull KeyStroke key) {
        if (pressKeyBoundTo(on, key)) return true;
        for (final Component child : on.getComponents()) {
            if (child instanceof final JComponent inner && pressKey(inner, key)) return true;
        }
        return false;
    }

    private static boolean pressKeyBoundTo(final @NotNull JComponent on, final @NotNull KeyStroke key) {
        for (final InputMap keys : List.of(on.getInputMap(JComponent.WHEN_FOCUSED), on.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT), on.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW))) {
            final @NotNull Optional<Action> bound = Optional.ofNullable(keys.get(key)).map(name -> on.getActionMap().get(name));
            if (bound.isPresent()) {
                bound.orElseThrow().actionPerformed(new ActionEvent(on, ActionEvent.ACTION_PERFORMED, ""));
                return true;
            }
        }
        return false;
    }
}
