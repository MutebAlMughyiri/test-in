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

import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI;
import com.intellij.ui.components.JBOptionButton;
import org.testng.annotations.Test;

import javax.swing.Action;
import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class DialogButtonTest {

    @Test
    public void aButtonWithChoicesIsDrawnAsTheButtonThatConfirms() {
        final JComponent withChoices = DialogPart.splitButton("Commit & Push", "Commit").getComponent().getFocusComponent();
        final JComponent plain = DialogPart.button("Generate").getComponent().getFocusComponent();

        assertEquals(withChoices.getClientProperty(DarculaButtonUI.DEFAULT_STYLE_KEY), true, "a button with choices is not drawn as the one that confirms");
        assertEquals(withChoices.getClientProperty(DarculaButtonUI.DEFAULT_STYLE_KEY), plain.getClientProperty(DarculaButtonUI.DEFAULT_STYLE_KEY), "a button with choices is drawn unlike a button without");
    }

    @Test
    public void anAlternativePressedOnceDoesNotBecomeWhatEnterRuns() {
        final DialogButton split = DialogPart.splitButton("Commit & Push", "Commit").getComponent();
        final List<String> readBySubmit = new ArrayList<>();
        split.onSubmitRequest(() -> readBySubmit.add(split.getChosen()));

        final JBOptionButton button = (JBOptionButton) split.getFocusComponent();
        final Action alternative = button.getOptions()[0];
        alternative.actionPerformed(new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "Commit"));

        assertEquals(readBySubmit, List.of("Commit"), "the submit reads the label that was pressed");
        assertEquals(split.getChosen(), "Commit & Push", "Enter afterward runs the first label");
    }
}
