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

import com.intellij.ui.components.fields.ExtendableTextField;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class FrameworkComponentsTest {

    private static @NotNull TextFieldWithSelections<Integer> twoSelections() {
        return DialogPart.<Integer>textFieldWithSelections()
                .placeholder("set name..")
                .selection(DialogStyle.NO_ICON, "One", "first row", 1)
                .selection(DialogStyle.NO_ICON, "Two", "second row", 2)
                .build()
                .getComponent();
    }

    private static void pressNavigationKey(final TextFieldWithSelections<?> component, final String actionKey) {
        final JComponent field = component.getFocusComponent();
        field.getActionMap().get(actionKey).actionPerformed(new ActionEvent(field, 0, ""));
    }

    private static @NotNull Optional<JList<?>> findList(final Container container) {
        for (final Component child : container.getComponents()) {
            if (child instanceof JList<?> list) return Optional.of(list);

            if (child instanceof Container inner) {
                final Optional<JList<?>> found = findList(inner);
                if (found.isPresent()) return found;
            }
        }

        return Optional.empty();
    }

    private static int rowCount(final DialogComponent shown) {
        final Container content = (Container) shown.getPanel().getComponent(0);
        return content.getComponentCount();
    }

    @Test
    public void selectionStartsOnTheFirstDeclaredRow() {
        assertEquals(twoSelections().getSelectedValue(), Integer.valueOf(1));
    }

    @Test
    public void arrowNavigationMovesAndClampsAtBothEnds() {
        final TextFieldWithSelections<Integer> component = twoSelections();

        pressNavigationKey(component, "testin.framework.selectionDown");
        assertEquals(component.getSelectedValue(), Integer.valueOf(2), "down moves to the second row");

        pressNavigationKey(component, "testin.framework.selectionDown");
        assertEquals(component.getSelectedValue(), Integer.valueOf(2), "down clamps at the last row");

        pressNavigationKey(component, "testin.framework.selectionUp");
        assertEquals(component.getSelectedValue(), Integer.valueOf(1), "up moves back to the first row");

        pressNavigationKey(component, "testin.framework.selectionUp");
        assertEquals(component.getSelectedValue(), Integer.valueOf(1), "up clamps at the first row");
    }

    @Test
    public void clearedSelectionFallsBackToTheFirstRow() {
        final TextFieldWithSelections<Integer> component = twoSelections();

        final JList<?> list = findList(component.getPanel())
                .orElseThrow(() -> new AssertionError("the selection list must be part of the component panel"));
        list.clearSelection();

        assertEquals(component.getSelectedValue(), Integer.valueOf(1));
    }

    @Test
    public void textInputOpensWithValueAndPlaceholder() {
        final TextInput input = DialogPart.textInput()
                .placeholder("set new name..")
                .value("current name")
                .build()
                .getComponent();

        assertEquals(input.getText(), "current name");
        assertEquals(((ExtendableTextField) input.getFocusComponent()).getEmptyText().getText(), "set new name..");
    }

    @Test
    public void emptyWarningKeepsThePlaceholderText() {
        final TextInput input = DialogPart.textInput().placeholder("set new name..").build().getComponent();

        input.showEmptyWarning();

        assertEquals(((ExtendableTextField) input.getFocusComponent()).getEmptyText().getText(), "set new name..");
    }

    @Test
    public void confirmCardSaysWherePlacesAreGivenAndNothingWhereTheyAreNot() {
        final ConfirmCard asked = DialogPart.confirmCard("Save to testin.yml?", List.of(), List.of()).getComponent();
        final ConfirmCard removing = DialogPart.confirmCard("Remove 'X'?", List.of("demo", "Test Cases"), List.of()).getComponent();
        final ConfirmCard moving = DialogPart.confirmCard("Move 'X' into 'Y'?", List.of("demo", "Test Cases"), List.of("demo", "Test Cases", "Smoke")).getComponent();

        assertEquals(rowCount(asked), 1, "the question alone, with no empty captioned block under it");
        assertEquals(rowCount(removing), 2, "the question and where the node is");
        assertEquals(rowCount(moving), 2, "the question and one row from where to where, not two blocks");
    }

    @Test
    public void messageQuotesAngleBracketsInsteadOfDroppingThem() {
        final DialogMessage message = DialogPart.message("Remove 'Login refuses <empty> password'?\nFor good").getComponent();
        final JLabel label = (JLabel) ((Container) message.getPanel().getComponent(0)).getComponent(0);

        assertTrue(label.getText().contains("Login refuses &lt;empty&gt; password"), label.getText());
        assertTrue(label.getText().contains("<br>"), "a line break is still a line break: " + label.getText());
    }

    @Test
    public void selectionRowKeepsAllDeclaredParts() {
        final Selection<String> row = Selection.of(DialogStyle.NO_ICON, "Test Set", "Holds test cases", "TS");

        assertEquals(row.name(), "Test Set");
        assertEquals(row.hint(), "Holds test cases");
        assertEquals(row.value(), "TS");
    }

    // Rule-INTERNAL-107
    @Test
    public void radioSelectionStartsOnTheDeclaredDefault() {
        final RadioSelection<String> radios = DialogPart.<String>radios("Severity")
                .option("Major", "MAJOR")
                .option("Minor", "MINOR")
                .select("MINOR")
                .build()
                .getComponent();

        assertEquals(radios.getSelected(), "MINOR");
    }

    @Test
    public void radiosNeedOptionsAndAValueToOpenOn() {
        Assert.expectThrows(IllegalStateException.class, () ->
                DialogPart.<String>radios("x").build());

        Assert.expectThrows(IllegalStateException.class, () ->
                DialogPart.<String>radios("x").option("A", "A").build());
    }

    // Rule-TREE-PANEL-121
    @Test
    public void radiosOpenUnansweredWhenTheStoredValueIsNotOffered() {
        final RadioSelection<String> group = DialogPart.<String>radios("x")
                .option("A", "A")
                .select("")
                .build()
                .getComponent();

        assertFalse(group.isAnswered());
        assertEquals(group.getSelected(), "");

        group.select("A");

        assertTrue(group.isAnswered());
        assertEquals(group.getSelected(), "A");
    }

    @Test
    public void textAreaKeepsValueAndClaimsTheSpace() {
        final PlainTextArea area = DialogPart.textArea()
                .placeholder("paste error or exception or screenshot..")
                .value("java.lang.RuntimeException")
                .build()
                .getComponent();

        assertEquals(((JTextArea) area.getFocusComponent()).getText(), "java.lang.RuntimeException");
        assertTrue(area.fillsSpace(), "the area must claim the dialog's remaining space");
        assertFalse(((JTextArea) area.getFocusComponent()).isEditable(), "a text area is for reading, and typing is done in a box of many lines");
    }

    @Test
    public void theScreenshotsRowHoldsThePicturesAndIsNotDrawnWithoutOne() {
        final byte[] screenshot = {1, 2, 3};
        final Screenshots shown = DialogPart.screenshots(Bundle.message("dialog.failure.caption.screenshots")).getComponent();
        shown.putFirst(List.of(screenshot));

        assertEquals(shown.screenshots(), List.of(screenshot), "a picture comes back as it was given, even one that cannot be drawn");
        assertTrue(shown.getPanel().isVisible(), "a row holding a picture is drawn");
        assertFalse(DialogPart.screenshots(Bundle.message("dialog.failure.caption.screenshots")).getComponent().getPanel().isVisible(),
                "a failure with no screenshot draws no Screenshots row at all, and no gap where one would be");
    }

    @Test
    public void aScreenshotIsReadBackAsThePictureItWas() {
        try {
            final ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", png);

            assertEquals(ScreenshotImages.read(png.toByteArray()).map(BufferedImage::getWidth), Optional.of(40));
            assertTrue(ScreenshotImages.read(new byte[]{1, 2, 3}).isEmpty(), "bytes that are not a picture draw nothing, and throw nothing (#50)");
        } catch (final Exception ex) {
            throw new AssertionError("the picture could not be encoded for the test", ex);
        }
    }

    @Test
    public void detailsSkipBlankRowsAndNeverWantFocus() {
        final DialogDetails details = DialogPart.details()
                .row("Description", "shown")
                .row("Expected", "")
                .row("Skipped", "   ")
                .build()
                .getComponent();

        assertEquals(details.getPanel().getComponentCount(), 1, "blank rows are skipped");
        assertFalse(details.wantsFocus(), "context rows never take the focus");
    }

    @Test
    public void aButtonRowNeverTakesTheDialogSpace() {
        final DialogButton button = DialogPart.button("Generate").getComponent();

        assertFalse(button.fillsSpace(), "a button row does not claim the space");
        assertFalse(button.canFillSpace(), "and must not be given it when nothing else claims it");
    }

    @Test
    public void everythingElseCanTakeTheSpaceWhenNothingClaimsIt() {
        final TextInput input = DialogPart.textInput().value("name").build().getComponent();
        final PlainTextArea area = DialogPart.textArea().value("x").build().getComponent();

        assertTrue(input.canFillSpace(), "a field is a reasonable fallback filler");
        assertTrue(area.canFillSpace());
    }

    @Test
    public void builtStatusBarEntryRendersTheKeymapText() {
        final StatusBarShortcut entry = StatusBarShortcut.build(Shortcuts.Enter, "Confirm", () -> {
        });

        assertTrue(entry.isBindable());
        assertEquals(entry.getName(), "Confirm");
        assertFalse(entry.getShortcutText().isBlank(), "the keystroke must render as text");
    }

    // Rule-INTERNAL-085
    @Test
    public void aChoiceBoxGivenANewTextFieldStillLeavesEnterToTheDialog() {
        final ChoiceInput choice = DialogPart.choice("Branch", List.of("main", "release"), "main").getComponent();
        final JComboBox<?> combo = (JComboBox<?>) choice.getFocusComponent();

        combo.setEditor(new BasicComboBoxEditor());

        final JComponent field = (JComponent) combo.getEditor().getEditorComponent();
        final Object key = field.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        assertFalse(field.getActionMap().get(key).isEnabled(),
                "with the list closed, the new field kept Enter from the dialog");
    }
}
