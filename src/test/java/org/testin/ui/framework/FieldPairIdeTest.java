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

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.json.JsonLanguage;
import com.intellij.ui.ActiveComponent;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FieldPairIdeTest extends AbstractIdeTest {
    private static final @NotNull String REMEMBERED_AS = "testin.test.fieldPair.sideBySide";

    @Override
    protected void tearDown() {
        try {
            PropertiesComponent.getInstance().unsetValue(REMEMBERED_AS);
            super.tearDown();
        } catch (final Exception ex) {
            throw new AssertionError("could not tear down " + getName(), ex);
        }
    }

    private @NotNull MultiLineField box() {
        return DialogPart.multiLineField(getProject(), "", "", 3, MultiLineField.NO_LIMIT, JsonLanguage.INSTANCE, false).getComponent();
    }

    private @NotNull FieldPair pair(final @NotNull JBLabel first, final @NotNull JBLabel second) {
        return DialogPart.fieldPair(getProject(), REMEMBERED_AS, first, box(), second, box()).getComponent();
    }

    private @NotNull FieldPair pair() {
        return pair(new JBLabel("Request"), new JBLabel("Response"));
    }

    // Rule-INTERNAL-137
    public void testAPairStartsOneAboveTheOtherInSectionsThatOpenAndClose() {
        final @NotNull FieldPair pair = pair();

        assertFalse("a pair did not start one above the other", pair.isSideBySide());
        assertTrue("one above the other is not laid out top to bottom", pair.getPanel().getLayout() instanceof BoxLayout);
        assertEquals("the pair is not two sections", 2, pair.sections().size());
        assertTrue("a section cannot be closed: " + Drawn.words(pair.getPanel()), Drawn.holds(Drawn.words(pair.getPanel()), "Collapse"));
        assertTrue("the pair does not take the dialog's spare height when it is maximized", pair.fillsSpace());
    }

    // Rule-INTERNAL-137
    public void testTheSwitchPutsThemSideBySideAndTheNextPairOpensThatWay() {
        final @NotNull FieldPair first = pair();
        assertEquals("stacked boxes do not offer side by side", Bundle.message("dialog.pair.side.by.side"), first.layoutButton().getAccessibleContext().getAccessibleName());
        first.switchLayout();

        final @NotNull FieldPair next = pair();
        assertTrue("the switch was not remembered for the next time", next.isSideBySide());
        assertTrue("side by side is not laid out in one row", next.getPanel().getLayout() instanceof GridLayout);
        assertEquals("boxes side by side do not offer stacked", Bundle.message("dialog.pair.stacked"), next.layoutButton().getAccessibleContext().getAccessibleName());

        next.switchLayout();
        assertFalse("switching back was not remembered", pair().isSideBySide());
    }

    // Rule-INTERNAL-133, Rule-INTERNAL-137
    public void testASectionsTitleIsTheLabelTheDialogKeepsSoItCanChangeIt() {
        final @NotNull JBLabel request = new JBLabel("Request");
        final @NotNull FieldPair pair = pair(request, new JBLabel("Response"));

        request.setText(String.join("   ", "Request", "UserRequest.java"));

        assertTrue("the section does not show the label the dialog changed", Drawn.holds(Drawn.words(pair.getPanel()), "UserRequest.java"));
    }

    // Rule-INTERNAL-138
    public void testADialogNamingNoTitleButtonsKeepsMaximizeAlone() {
        final @NotNull ActiveComponent maximize = Maximized.button(() -> {
        });

        assertSame("a dialog with no title buttons did not keep maximize as it was", maximize, TitleButtons.beside(List.of(), maximize));
    }

    // Rule-INTERNAL-139
    public void testEachSectionFormatsItsBoxInItsLanguageChangingOnlyWhitespace() {
        final @NotNull MultiLineField request = box();
        final @NotNull FieldPair pair = DialogPart.fieldPair(getProject(), REMEMBERED_AS, new JBLabel("Request"), request, new JBLabel("Response"), box()).getComponent();
        final @NotNull String pasted = IntStream.range(0, 2000).mapToObj(i -> "\"key" + i + "\":[{\"id\":" + i + "}]").collect(Collectors.joining(",", "{", "}"));
        final @NotNull EditorTextField drawn = (EditorTextField) request.getFocusComponent();
        drawn.addNotify();
        try {
            request.setText(pasted);

            final @NotNull List<JButton> format = Drawn.components(pair.getPanel()).stream()
                    .filter(JButton.class::isInstance)
                    .map(JButton.class::cast)
                    .filter(button -> Bundle.message("dialog.box.format", "JSON").equals(button.getAccessibleContext().getAccessibleName()))
                    .toList();
            assertEquals("each section does not carry its own Format JSON button", 2, format.size());

            format.getFirst().doClick();

            assertTrue("the body was not laid out over lines: " + request.getText(), request.getText().lines().count() > 1);
            assertEquals("formatting changed more than whitespace", pasted, request.getText().replaceAll("\\s", ""));
        } finally {
            drawn.removeNotify();
        }
    }

    // Rule-INTERNAL-138
    public void testATitleButtonSitsBesideMaximize() {
        final @NotNull ActiveComponent maximize = Maximized.button(() -> {
        });
        final @NotNull JComponent layout = pair().layoutButton();

        final @NotNull List<Component> row = Drawn.components(TitleButtons.beside(List.of(layout), maximize).getComponent());

        assertTrue("the title button is not in the title bar", row.contains(layout));
        assertTrue("maximize is not beside it", row.contains(maximize.getComponent()));
        assertTrue("the layout switch is not a button", layout instanceof JButton);
    }

    // Rule-INTERNAL-137
    public void testThePasteKeyIsOnTheStrip() {
        final @NotNull StatusBarShortcut paste = StatusBarShortcut.paste();

        assertEquals(Bundle.message("shortcut.paste"), paste.name());
    }
}
