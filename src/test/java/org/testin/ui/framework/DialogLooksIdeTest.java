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

import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.SeparatorComponent;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.model.StatusBarItem;
import org.testin.testrun.form.TestCaseSelectionRenderer;
import org.testin.ui.Caption;
import org.testin.util.Fonts;
import org.testin.util.Icons;
import org.testin.view.Drawn;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertNotEquals;

public class DialogLooksIdeTest extends AbstractIdeTest {

    private static final float MOST_A_GRAY_MAY_LEAN = 0.25f;

    private static final @NotNull Icon GREEN = new Icon() {
        @Override
        public void paintIcon(final Component component, final Graphics g, final int x, final int y) {
            g.setColor(Icons.GREEN);
            g.fillRect(x, y, getIconWidth(), getIconHeight());
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    };

    private static void laidOut(final @NotNull Container container) {
        container.doLayout();
        for (final Component child : container.getComponents()) {
            if (child instanceof final Container inner) laidOut(inner);
        }
    }

    private static <C extends JComponent> @NotNull C realized(final @NotNull C component) {
        component.setSize(component.getPreferredSize());
        laidOut(component);
        return component;
    }

    private static @NotNull BufferedImage painted(final @NotNull JComponent component) {
        final @NotNull BufferedImage image = new BufferedImage(Math.max(1, component.getWidth()), Math.max(1, component.getHeight()), BufferedImage.TYPE_INT_ARGB);
        final @NotNull Graphics2D g = image.createGraphics();
        try {
            component.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static @NotNull BufferedImage painted(final @NotNull Icon icon) {
        final @NotNull BufferedImage image = new BufferedImage(Math.max(1, icon.getIconWidth()), Math.max(1, icon.getIconHeight()), BufferedImage.TYPE_INT_ARGB);
        final @NotNull Graphics2D g = image.createGraphics();
        try {
            icon.paintIcon(new JLabel(), g, 0, 0);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static float saturationOf(final @NotNull Color color) {
        return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[1];
    }

    private static boolean isGray(final @NotNull Icon icon) {
        final @NotNull BufferedImage image = painted(icon);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                final @NotNull Color pixel = new Color(image.getRGB(x, y), true);
                if (pixel.getAlpha() > 64 && saturationOf(pixel) > MOST_A_GRAY_MAY_LEAN) return false;
            }
        }
        return true;
    }

    private static @NotNull Color at(final @NotNull BufferedImage image, final int x, final int y) {
        return new Color(image.getRGB(x, y), true);
    }

    private static @NotNull Point within(final @NotNull Component component, final @NotNull Component ancestor) {
        return SwingUtilities.convertPoint(component.getParent(), component.getLocation(), ancestor);
    }

    private static <T extends Component> @NotNull List<T> drawn(final @NotNull Container container, final @NotNull Class<T> kind) {
        return Drawn.components(container).stream().filter(kind::isInstance).map(kind::cast).toList();
    }

    private static @NotNull JLabel captionOf(final @NotNull JComponent panel, final @NotNull Component field) {
        return drawn(panel, JLabel.class).stream().filter(label -> field.equals(label.getLabelFor())).findFirst().orElseThrow(() -> new AssertionError("the field has no caption"));
    }

    private static @NotNull List<Icon> extensionIconsOf(final @NotNull Component field) {
        return ((ExtendableTextField) field).getExtensions().stream().map(extension -> extension.getIcon(false)).toList();
    }

    private static @NotNull JComponent everyField() {
        final @NotNull JPanel stack = new JPanel();
        Stream.of(
                        DialogPart.textInput().caption("Name").placeholder("set name..").build(),
                        DialogPart.textArea().placeholder("paste the failure..").build(),
                        DialogPart.choice("Branch", List.of("main", "release"), "main"),
                        DialogPart.<String>radios("Format").option("PDF", "pdf").option("HTML", "html").select("pdf").build(),
                        DialogPart.<Integer>textFieldWithSelections().placeholder("set kind..").selection(DialogStyle.NO_ICON, "Test Set", "Holds test cases", 1).build(),
                        DialogPart.of(new SelectionTree("Test cases", new CheckedTreeNode("all"), TestCaseSelectionRenderer.create(), Optional.empty())),
                        DialogPart.details().row("Where", "NAFATH › Login").build(),
                        DialogPart.confirmCard("Remove Login?", List.of("NAFATH", "Test Cases"), List.of()),
                        DialogPart.button("Generate"))
                .map(built -> built.getComponent().getPanel())
                .forEach(stack::add);
        stack.add(new ShortcutStrip(new StatusBarItem[]{StatusBarShortcut.cancel(() -> {
        })}).getPanel());
        return stack;
    }

    private static @NotNull SimpleColoredComponent rowAt(final @NotNull JBList<?> rows, final int index) {
        final @NotNull SimpleColoredComponent row = (SimpleColoredComponent) rendered(rows, index);
        row.setSize(400, row.getPreferredSize().height);
        return row;
    }

    private static <E> @NotNull Component rendered(final @NotNull JList<E> list, final int index) {
        return list.getCellRenderer().getListCellRendererComponent(list, list.getModel().getElementAt(index), index, index == 0, false);
    }

    private static @NotNull List<Integer> gapsOf(final LayoutManager layout) {
        if (layout instanceof final BorderLayout border) return List.of(border.getHgap(), border.getVgap());
        if (layout instanceof final FlowLayout flow) return List.of(flow.getHgap(), flow.getVgap());
        return List.of();
    }

    // Rule-INTERNAL-133
    public void testACollapseChevronIsDrawnInTheAccentOpenOrClosed() {
        assertNotSame("an open section draws the platform's uncolored arrow", AllIcons.General.ArrowDown, DialogStyle.chevron(true));
        assertNotSame("a closed section draws the platform's uncolored arrow", AllIcons.General.ArrowRight, DialogStyle.chevron(false));
        assertEquals("an open chevron is not colored like every action", DialogStyle.asAction(AllIcons.General.ArrowDown).getClass(), DialogStyle.chevron(true).getClass());
    }

    // UC-INTERNAL-007, Rule-INTERNAL-077
    public void testEveryIconAFrameworkSurfaceDrawsIsGrayEvenWhenTheIconItIsHandedIsColored() {
        assertFalse("the icon this test hands the framework is not colored, so it proves nothing", isGray(GREEN));

        final @NotNull Component field = DialogPart.textInput().icon(GREEN).placeholder("set name..").build().getComponent().getFocusComponent();
        assertFalse("the field drew no icon", extensionIconsOf(field).isEmpty());
        assertTrue("a field drew its icon in color", extensionIconsOf(field).stream().allMatch(DialogLooksIdeTest::isGray));

        final @NotNull TextFieldWithSelections<Integer> kinds = DialogPart.<Integer>textFieldWithSelections().placeholder("set kind..").selection(GREEN, "Test Set", "Holds test cases", 1).build().getComponent();
        final @NotNull SimpleColoredComponent row = rowAt(drawn(kinds.getPanel(), JBList.class).getFirst(), 0);
        assertTrue("a row of a list drew its icon in color", isGray(Objects.requireNonNull(row.getIcon())));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-080
    public void testAButtonTheDialogWillNotActOnIsGrayAndHoveringItSaysWhyWhileTheLineBesideItSaysWhatItWouldActOn() {
        final @NotNull DialogButton button = DialogPart.button("Generate").getComponent();
        final @NotNull JButton drawnButton = (JButton) button.getFocusComponent();
        final @NotNull JLabel line = drawn(button.getPanel(), JLabel.class).getFirst();

        button.tally("5 test cases in 2 test sets");
        assertTrue(drawnButton.isEnabled());
        assertEquals("a ready button does not say what pressing it would act on", "5 test cases in 2 test sets", line.getText());
        assertEquals("hovering over a ready button gives a reason it cannot act", "", Drawn.hovering(drawnButton));

        button.enableUnless(Optional.of("Pick a test case first"));
        assertFalse("a button the dialog will not act on is not drawn disabled", drawnButton.isEnabled());
        assertEquals("hovering over the gray button does not say why", "Pick a test case first", Drawn.hovering(drawnButton));
        assertEquals("the reason is printed beside the gray button instead of only on hover", "5 test cases in 2 test sets", line.getText());

        button.enableUnless(Optional.empty());
        assertTrue(drawnButton.isEnabled());
        assertEquals("the reason stayed on the button once it was ready", "", Drawn.hovering(drawnButton));
        assertEquals("the button has more than one line beside it", 1, drawn(button.getPanel(), JLabel.class).size());
    }

    // UC-INTERNAL-007, Rule-INTERNAL-087
    public void testACaptionSitsAboveItsFieldInCapitalsInTheMutedGrayStartingWhereTheFieldsFrameStarts() {
        final @NotNull TextInput name = DialogPart.textInput().caption("Name").placeholder("set name..").build().getComponent();
        final @NotNull JComponent panel = realized(name.getPanel());
        final @NotNull Component field = name.getFocusComponent();
        final @NotNull JLabel caption = captionOf(panel, field);

        assertEquals("the caption is not in capitals", "NAME", caption.getText());
        assertEquals("the caption is not in the muted caption gray", JBUI.CurrentTheme.ContextHelp.FOREGROUND, caption.getForeground());
        assertEquals("the caption is not in the caption font", Fonts.caption(), caption.getFont());
        assertTrue("the caption is not on its own line above the field", within(caption, panel).y + caption.getHeight() <= within(field, panel).y);
        assertEquals("the caption does not start where the field's frame starts", within(field, panel).x, within(caption, panel).x);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-087
    public void testACardsCaptionCarriesAHairlineFromTheEndOfItsTextToTheCardsFarEdge() {
        final @NotNull JLabel title = Caption.of("Test cases", Fonts.caption());
        final @NotNull JComponent header = Caption.header(title, Optional.empty());
        header.setSize(300, header.getPreferredSize().height);
        laidOut(header);
        final @NotNull SeparatorComponent hairline = drawn(header, SeparatorComponent.class).getFirst();

        assertTrue("a card's caption has no hairline after its text", hairline.getX() >= title.getX() + title.getWidth());
        assertEquals("the hairline does not run to the card's far edge", header.getWidth(), hairline.getX() + hairline.getWidth());
    }

    // UC-INTERNAL-007, Rule-INTERNAL-087
    public void testTheCaptionFontIsJetBrainsMonoTwoPointsBelowTheDialogsLabelFont() {
        final @NotNull Font caption = Fonts.caption();

        assertEquals("the caption is not set in JetBrains Mono", "JetBrains Mono", caption.getFamily());
        assertEquals("the caption is not two points below the dialog's label font", JBUI.Fonts.label().getSize2D() - 2, caption.getSize2D(), 0.01f);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-095
    public void testEveryFontADialogDrawsIsOneOfTheRolesFontsNames() {
        final @NotNull Set<Font> roles = Set.copyOf(List.of(Fonts.title(), Fonts.strong(), Fonts.body(), Fonts.label(), Fonts.badge(), Fonts.panelCaption(), Fonts.field(), Fonts.placeholder(),
                Fonts.value(), Fonts.choice(), Fonts.option(), Fonts.caption(), Fonts.row(), Fonts.small(), Fonts.smallStrong(), Fonts.hint(), Fonts.keycap(), Fonts.figure(), Fonts.iconLetter()));

        final @NotNull List<String> ownFonts = new ArrayList<>();
        for (final Component component : Drawn.components(everyField())) {
            final boolean saysWords = (component instanceof JLabel || component instanceof AbstractButton) && !Drawn.text(component).isEmpty();
            final boolean takesWords = component instanceof JTextComponent || component instanceof JList;
            if ((saysWords || takesWords) && !roles.contains(component.getFont()))
                ownFonts.add(component.getClass().getSimpleName() + " '" + Drawn.text(component) + "' in " + component.getFont());
        }

        assertEquals("a surface in a dialog derived a font of its own instead of asking Fonts for a role", List.of(), ownFonts);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-096
    public void testEveryTypingSurfaceIsInTheFrameAndSizeOfATextField() {
        final @NotNull Component field = DialogPart.textInput().placeholder("set name..").build().getComponent().getFocusComponent();
        final @NotNull PlainTextArea area = DialogPart.textArea().placeholder("paste the failure..").build().getComponent();
        final @NotNull JScrollPane well = drawn(area.getPanel(), JScrollPane.class).getFirst();
        final @NotNull JComponent value = drawn(DialogPart.details().row("Description", "Log in with a valid user").build().getComponent().getPanel(), JLabel.class).stream()
                .filter(label -> label.getText().contains("Log in with a valid user")).findFirst().orElseThrow();

        final @NotNull Border frame = ((JComponent) field).getBorder();
        assertEquals("a text area is not drawn in the frame a text field has", frame.getClass(), well.getBorder().getClass());
        assertEquals("a text area is not set in the size a field is", field.getFont(), area.getFocusComponent().getFont());
        assertEquals("a read-only value is set smaller than the field a tester types in", field.getFont().getSize2D(), value.getFont().getSize2D(), 0.01f);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-099
    public void testASectionIsARoundedCardOfTheContentSurfaceWithAHairlineEdgeOnTheOtherSurface() {
        final boolean wasDark = !JBColor.isBright();
        try {
            for (final boolean dark : new boolean[]{false, true}) {
                JBColor.setDark(dark);
                final @NotNull JComponent card = DialogStyle.section(new JLabel("Test cases"), new JPanel());
                card.setSize(240, 120);
                laidOut(card);
                final @NotNull BufferedImage image = painted(card);
                final @NotNull Color fill = at(image, card.getWidth() - 4, card.getHeight() - 4);
                final @NotNull Color ground = DialogStyle.styleContent(new JPanel()).getBackground();

                assertEquals("the corner of a card is square", 0, at(image, 0, 0).getAlpha());
                assertFalse("the card has no edge of its own", fill.getRGB() == at(image, card.getWidth() / 2, 0).getRGB());
                if (dark)
                    assertTrue("in a dark theme the card is not the darker surface", ColorUtil.getLuminance(fill) <= ColorUtil.getLuminance(ground));
                else
                    assertTrue("in a light theme the card is not the lighter surface", ColorUtil.getLuminance(fill) >= ColorUtil.getLuminance(ground));
                assertTrue("the card and the ground are not the theme's two surfaces", fill.getRGB() == UIUtil.getPanelBackground().getRGB() || ground.getRGB() == UIUtil.getPanelBackground().getRGB());
            }
        } finally {
            JBColor.setDark(wasDark);
        }
    }

    // UC-INTERNAL-007, Rule-INTERNAL-105
    public void testOneAccentGoesOnTheButtonThatConfirmsAndEverySurfaceStaysNeutral() {
        final @NotNull ConfirmDialog confirm = new ConfirmDialog(getProject(), "Remove", "Remove Login?", "", "", "Remove", () -> {
        });
        confirm.show();
        try {
            final @NotNull List<JButton> accented = drawn(confirm.root(), JButton.class).stream().filter(button -> Boolean.TRUE.equals(button.getClientProperty(DarculaButtonUI.DEFAULT_STYLE_KEY))).toList();
            assertEquals("a dialog has more or less than one accented button", 1, accented.size());
            assertEquals("the accent is not on the button that confirms", "Remove", accented.getFirst().getText());

            final @NotNull Color ground = confirm.root().getBackground();
            assertTrue("the dialog's ground is colored: " + ground, saturationOf(ground) < 0.1f);

            final @NotNull JComponent card = DialogStyle.section(new JLabel("Test cases"), new JPanel());
            card.setSize(240, 120);
            laidOut(card);
            final @NotNull Color fill = at(painted(card), 120, 60);
            assertTrue("a card is colored: " + fill, saturationOf(fill) < 0.1f);
        } finally {
            confirm.closeCancel();
        }
    }

    // UC-INTERNAL-007, Rule-INTERNAL-106
    public void testAPickedRowIsARoundedBandInsideItsCardAndWhatARowSaysSitsAgainstItsName() {
        final @NotNull TextFieldWithSelections<Integer> kinds = DialogPart.<Integer>textFieldWithSelections().placeholder("set kind..")
                .selection(DialogStyle.NO_ICON, "Test Set", "Holds test cases", 1)
                .selection(DialogStyle.NO_ICON, "Test Set Package", "Groups test sets", 2).build().getComponent();
        final @NotNull JComponent card = kinds.getPanel();
        card.setSize(420, card.getPreferredSize().height);
        laidOut(card);
        final @NotNull JBList<?> rows = drawn(card, JBList.class).getFirst();

        assertTrue("the rows run to the card's left edge", within(rows, card).x > 0);
        assertTrue("the rows run to the card's right edge", within(rows, card).x + rows.getWidth() < card.getWidth());

        final @NotNull List<Integer> hintStarts = new ArrayList<>();
        for (int index = 0; index < 2; index++) {
            final @NotNull SimpleColoredComponent row = rowAt(rows, index);
            hintStarts.add(IntStream.range(0, row.getWidth()).filter(x -> row.findFragmentAt(x) == 1).findFirst().orElseThrow(() -> new AssertionError("the row says nothing about itself")));

            if (index == 0) {
                final @NotNull BufferedImage band = painted(row);
                assertEquals("the picked row's band has square corners", 0, at(band, 0, 0).getAlpha());
                assertTrue("the picked row is not marked by a band", at(band, row.getWidth() - 3, row.getHeight() / 2).getAlpha() > 0);
            }
        }
        assertTrue("what a row says about itself is lined up into a column instead of sitting against its name", hintStarts.getFirst() < hintStarts.get(1));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-109
    public void testWhereSomethingIsGoingIsOneRowWithOnlyTheSegmentItGainsInTheOrdinaryTextColor() {
        final @NotNull JComponent place = realized(DialogPlace.row(List.of("NAFATH", "Test Cases", "Login"), List.of("NAFATH", "Test Cases", "Accounts", "Login")).orElseThrow());
        final @NotNull List<JLabel> parts = drawn(place, JLabel.class);

        assertEquals("the row does not read: where it is, an arrow, where it lands", List.of("NAFATH › Test Cases › Login", "", "NAFATH › Test Cases › Accounts ›", "Login"), parts.stream().map(Drawn::text).toList());
        assertNotNull("the arrow is not drawn", parts.get(1).getIcon());

        final @NotNull Color gray = JBUI.CurrentTheme.ContextHelp.FOREGROUND;
        assertEquals("where it is, is not gray", gray, parts.getFirst().getForeground());
        assertEquals("the rest of where it lands is not gray", gray, parts.get(2).getForeground());
        assertNotEquals("the segment it gains is gray like the rest", gray, parts.get(3).getForeground());

        final int middle = within(parts.getFirst(), place).y + parts.getFirst().getHeight() / 2;
        for (final JLabel part : parts) {
            assertTrue("the place is not one row", Math.abs(within(part, place).y + part.getHeight() / 2 - middle) <= 2);
        }
        assertTrue(within(parts.getFirst(), place).x < within(parts.get(1), place).x && within(parts.get(1), place).x < within(parts.get(2), place).x);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-119
    public void testAnIconButtonShowsItsIconAloneAtRestAGrayRoundedFillUnderThePointerAndADarkerOneWhileOn() {
        final @NotNull AbstractIconButton button = AbstractIconButton.of("Refresh", AllIcons.Actions.Refresh, () -> {
        });
        button.setSize(button.getPreferredSize());
        final int middle = button.getHeight() / 2;
        assertEquals("an icon button has words beside its icon", "", Objects.toString(button.getText(), ""));
        final @NotNull BufferedImage atRest = painted(button);

        button.dispatchEvent(new MouseEvent(button, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, middle, middle, 0, false));
        final @NotNull BufferedImage hovered = painted(button);
        final int fillStarts = IntStream.range(0, hovered.getWidth()).filter(x -> at(hovered, x, middle).getAlpha() > 0).findFirst().orElseThrow(() -> new AssertionError("the pointer on an icon button draws no fill under it"));
        final int fillTop = IntStream.range(0, hovered.getHeight()).filter(y -> at(hovered, button.getWidth() / 2, y).getAlpha() > 0).findFirst().orElseThrow();
        final @NotNull Color hoverFill = at(hovered, fillStarts + 1, middle);

        assertEquals("an icon button is filled at rest", 0, at(atRest, fillStarts + 1, middle).getAlpha());
        assertTrue("the fill under the pointer is not gray: " + hoverFill, saturationOf(hoverFill) < 0.1f);
        assertTrue("the fill under the pointer has square corners", at(hovered, fillStarts, fillTop).getAlpha() < hoverFill.getAlpha());

        button.dispatchEvent(new MouseEvent(button, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, -1, -1, 0, false));
        button.setOn(true);
        final @NotNull Color onFill = at(painted(button), fillStarts + 1, middle);
        assertTrue("the fill while the button is on is not a shade darker than under the pointer", ColorUtil.getLuminance(onFill) < ColorUtil.getLuminance(hoverFill));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-121
    public void testEveryGapAndPaddingInADialogIsOneOfTheFiveSteps() {
        assertEquals(List.of(4, 6, 8, 10, 12), List.of(Spacing.XS, Spacing.S, Spacing.M, Spacing.L, Spacing.XL));

        final @NotNull Set<Integer> steps = Set.copyOf(List.of(0, JBUI.scale(Spacing.XS), JBUI.scale(Spacing.S), JBUI.scale(Spacing.M), JBUI.scale(Spacing.L), JBUI.scale(Spacing.XL)));
        final @NotNull List<String> offSteps = new ArrayList<>();
        for (final Component component : Drawn.components(everyField())) {
            if (!(component instanceof final JComponent drawn)) continue;
            final boolean ours = drawn.getClass().getName().startsWith("org.testin");
            if (!ours && (drawn.getClass().getName().contains("$") || (!(drawn instanceof JPanel) && !(drawn instanceof JLabel))))
                continue;

            if (drawn.getBorder() instanceof final EmptyBorder padding) {
                final @NotNull Insets insets = padding.getBorderInsets();
                Stream.of(insets.top, insets.left, insets.bottom, insets.right).filter(gap -> !steps.contains(gap))
                        .forEach(gap -> offSteps.add("padding " + gap + " on " + drawn.getClass().getSimpleName() + " '" + Drawn.text(drawn) + "'"));
            }
            gapsOf(drawn.getLayout()).stream().filter(gap -> !steps.contains(gap)).forEach(gap -> offSteps.add("gap " + gap + " in " + drawn.getClass().getSimpleName()));
        }

        assertEquals("a dialog writes a gap or a padding that is not one of the five steps", List.of(), offSteps);
    }
}
