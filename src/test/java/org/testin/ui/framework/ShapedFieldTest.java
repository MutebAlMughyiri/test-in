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
import org.testng.annotations.Test;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import static org.testng.Assert.assertEquals;

public class ShapedFieldTest {

    private static final @NotNull String A_POSITION = "[1-9][0-9]*";

    private static @NotNull JTextField position() {
        return (JTextField) DialogPart.textInput().accepting(A_POSITION).build().getComponent().getFocusComponent();
    }

    private static void type(final @NotNull JTextField field, final @NotNull String keys) {
        final @NotNull Document document = field.getDocument();
        try {
            document.insertString(document.getLength(), keys, null);
        } catch (final BadLocationException ex) {
            throw new AssertionError("could not type " + keys, ex);
        }
    }

    // UC-INTERNAL-007, Rule-INTERNAL-110
    @Test
    public void aKeystrokeThatWouldBreakTheShapeIsRefused() {
        final @NotNull JTextField field = position();

        type(field, "0");
        assertEquals(field.getText(), "", "a position cannot start at nought, and the field took it");

        type(field, "1");
        type(field, "x");
        type(field, "2");
        assertEquals(field.getText(), "12", "a letter in a position was taken");
    }

    // UC-INTERNAL-007, Rule-INTERNAL-110
    @Test
    public void aPasteThatWouldBreakTheShapeLeavesTheValueAsItWas() {
        final @NotNull JTextField field = position();
        type(field, "7");

        field.setText("seven");

        assertEquals(field.getText(), "7", "pasting over a position put something that is not one in the field");
    }
}
