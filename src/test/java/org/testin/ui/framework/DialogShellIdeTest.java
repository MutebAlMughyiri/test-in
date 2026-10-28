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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.TitlePanel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.model.node.TestSetNode;
import org.testin.search.GlobalSearchDialog;
import org.testin.testrun.form.TestCaseSelectionRenderer;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;
import org.testin.view.marker.NodeDetailsDialog;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class DialogShellIdeTest extends AbstractIdeTest {

    private final @NotNull List<AbstractFrameworkDialog> opened = new ArrayList<>();

    private static @NotNull AbstractPopup popupOf(final @NotNull AbstractFrameworkDialog dialog) {
        return (AbstractPopup) dialog.getPopup();
    }

    private static @NotNull Object popupField(final @NotNull JBPopup popup, final @NotNull String name) {
        try {
            final @NotNull Field field = AbstractPopup.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(popup);
        } catch (final ReflectiveOperationException ex) {
            throw new LinkageError("the popup no longer has " + name, ex);
        }
    }

    private static void laidOut(final @NotNull Container container) {
        container.doLayout();
        for (final Component child : container.getComponents()) {
            if (child instanceof final Container inner) laidOut(inner);
        }
    }

    private static @NotNull JComponent realized(final @NotNull JComponent root) {
        if (!root.isDisplayable()) root.addNotify();
        root.setSize(root.getPreferredSize());
        laidOut(root);
        return root;
    }

    private static @NotNull List<Component> tabStops(final @NotNull JComponent root) {
        final @NotNull TabStop tabStop = new TabStop();
        return Drawn.components(root).stream().filter(tabStop::accept).toList();
    }

    private static int yOf(final @NotNull Component component, final @NotNull Component within) {
        return SwingUtilities.convertPoint(component.getParent(), component.getLocation(), within).y;
    }

    private static void pressed(final @NotNull Component on, final @NotNull Shortcuts shortcut, final int keyCode, @MagicConstant(flags = {InputEvent.SHIFT_DOWN_MASK, InputEvent.CTRL_DOWN_MASK, InputEvent.META_DOWN_MASK, InputEvent.ALT_DOWN_MASK}) final int modifiers) {
        final @NotNull KeyEvent key = new KeyEvent(on, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
        assertTrue("the test does not press " + shortcut.getShortcutText(), shortcut.matches(key));
        SwingUtilities.processKeyBindings(key);
    }

    private static @NotNull Optional<AbstractIconButton> maximizeButtonOf(final @NotNull AbstractFrameworkDialog dialog) {
        return Drawn.components(popupOf(dialog).getTitle()).stream().filter(AbstractIconButton.class::isInstance).map(AbstractIconButton.class::cast)
                .filter(button -> Bundle.message("dialog.maximize").equals(button.getAccessibleContext().getAccessibleName())).findFirst();
    }

    private static @NotNull DialogPart<TextInput> aField(final @NotNull String caption) {
        return DialogPart.textInput().caption(caption).placeholder("set " + caption.toLowerCase(Locale.ROOT) + "..").build();
    }

    @Override
    protected void setUp() {
        try {
            super.setUp();
        } catch (final Exception ex) {
            throw new AssertionError("could not set up " + getName(), ex);
        }
        PopupsShowAtContentSize.during(getTestRootDisposable());
    }

    @Override
    protected void tearDown() {
        try {
            opened.stream().map(AbstractFrameworkDialog::getPopup).filter(popup -> !popup.isDisposed()).forEach(JBPopup::cancel);
            super.tearDown();
        } catch (final Exception ex) {
            throw new AssertionError("could not tear down " + getName(), ex);
        }
    }

    private <D extends AbstractFrameworkDialog> @NotNull D shown(final @NotNull D dialog) {
        if (dialog.show()) opened.add(dialog);
        return dialog;
    }

    // UC-INTERNAL-007, Rule-INTERNAL-053
    public void testEveryDialogHasATitleItsFieldsStackedDownTheMiddleAndAStripOfKeysAlongTheBottom() {
        final @NotNull DialogPart<TextInput> name = aField("Name");
        final @NotNull DialogPart<TextInput> path = aField("Path");
        final @NotNull Form form = shown(new Form(getProject(), List.of(name, path)));

        assertEquals("the dialog does not carry its title at the top", "Every field", ((TitlePanel) popupOf(form).getTitle()).getLabel().getText());

        final @NotNull JComponent content = realized(form.root());
        final @NotNull Component strip = ((BorderLayout) content.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        assertTrue("the strip does not name the keys that work: " + Drawn.words((Container) strip), Drawn.words((Container) strip).containsAll(List.of(Shortcuts.Enter.getShortcutText(), "Save", Bundle.message("shortcut.cancel"))));
        assertTrue("the fields are not stacked in the order they were declared", yOf(name.getComponent().getFocusComponent(), content) < yOf(path.getComponent().getFocusComponent(), content));
        assertTrue("the strip is not along the bottom, below every field", yOf(path.getComponent().getFocusComponent(), content) < yOf(strip, content));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-055
    public void testAKeyOnTheStripWorksWhereverTheCursorIsInsideTheDialog() {
        final @NotNull Form form = shown(new Form(getProject(), List.of(
                aField("Name"),
                DialogPart.<String>textFieldWithSelections().placeholder("set kind..").selection(DialogStyle.NO_ICON, "Test Set", "Holds test cases", "ts").selection(DialogStyle.NO_ICON, "Test Set Package", "Groups test sets", "tsp").build(),
                DialogPart.<String>radios("Format").option("PDF", "pdf").option("HTML", "html").select("pdf").build(),
                DialogPart.choice("Branch", List.of("main", "release"), "main"))));

        final @NotNull JRootPane window = new JRootPane();
        window.getContentPane().add(popupOf(form).getComponent());
        new JPanel().add(window);
        realized(window);
        final @NotNull List<Component> stops = tabStops(form.root());
        assertTrue("the dialog has fewer places for the cursor than it has fields: " + stops, stops.size() >= 4);

        for (final Component stop : stops) {
            final int before = form.submitted.get();
            pressed(stop, Shortcuts.Enter, KeyEvent.VK_ENTER, 0);
            assertEquals("Enter on the strip did nothing with the cursor in a " + stop.getClass().getName(), before + 1, form.submitted.get());

            final int searched = form.searched.get();
            pressed(stop, Shortcuts.FocusSearch, KeyEvent.VK_F, SystemInfo.isMac ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK);
            assertEquals(Shortcuts.FocusSearch.getShortcutText() + " on the strip did nothing with the cursor in a " + stop.getClass().getName(), searched + 1, form.searched.get());
        }
    }

    // UC-INTERNAL-007, Rule-INTERNAL-057
    public void testTheFirstFieldThatCanTakeTheCursorHasItWhenTheDialogOpens() {
        final @NotNull DialogPart<TextInput> first = aField("Name");
        final @NotNull Form form = shown(new Form(getProject(), List.of(DialogPart.details().row("Where", "NAFATH › Login").build(), first, aField("Path"))));

        assertSame("the cursor is not in the first field that can take it", first.getComponent().getFocusComponent(), popupField(popupOf(form), "myPreferredFocusedComponent"));
        assertTrue("the dialog does not move the cursor into it when it opens", popupOf(form).shouldRequestFocus());
    }

    // UC-INTERNAL-007, Rule-INTERNAL-058
    public void testTabMovesToTheNextFieldInTheOrderTheyAreDrawnAndShiftTabBack() {
        final @NotNull DialogPart<TextInput> name = aField("Name");
        final @NotNull DialogPart<TextInput> path = aField("Path");
        final @NotNull DialogPart<TextInput> owner = aField("Owner");
        final @NotNull Form form = shown(new Form(getProject(), List.of(name, path, owner)));
        final @NotNull JComponent content = realized(form.root());

        final @NotNull JTextField first = (JTextField) name.getComponent().getFocusComponent();
        final @NotNull JTextField second = (JTextField) path.getComponent().getFocusComponent();
        final @NotNull JTextField third = (JTextField) owner.getComponent().getFocusComponent();

        assertTrue("the dialog's fields are not one Tab cycle of their own", content.isFocusCycleRoot());
        assertTrue("Tab does not follow the order the fields are drawn in", content.getFocusTraversalPolicy() instanceof LayoutFocusTraversalPolicy);
        assertTrue("the field Tab reaches second is not drawn below the first", yOf(first, content) < yOf(second, content));
        assertTrue("the field Tab reaches third is not drawn below the second", yOf(second, content) < yOf(third, content));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-059
    public void testEscapeClosesTheDialogAtOnceAndSavesNothing() {
        final @NotNull DialogPart<TextInput> name = aField("Name");
        final @NotNull Form form = shown(new Form(getProject(), List.of(name)));
        ((JTextField) name.getComponent().getFocusComponent()).setText("Log in with a valid user");

        ShownDialog.press(getProject(), Form.class, Shortcuts.Escape);

        assertFalse("Escape did not close the dialog", ShownDialog.isOpen(getProject(), Form.class));
        assertEquals("Escape saved what was typed", 0, form.submitted.get());
    }

    // UC-INTERNAL-007, Rule-INTERNAL-060
    public void testPressingTheDialogsButtonOrClickingARowIsTheSameAsEnter() {
        final @NotNull DialogPart<DialogButton> button = DialogPart.button("Generate");
        final @NotNull Form withAButton = shown(new Form(getProject(), List.of(aField("Name"), button)));

        ((JButton) button.getComponent().getFocusComponent()).doClick();
        assertEquals("pressing the dialog's button did not do what Enter does", 1, withAButton.submitted.get());
        withAButton.closeCancel();

        final @NotNull DialogPart<TextFieldWithSelections<String>> list = DialogPart.<String>textFieldWithSelections().placeholder("set kind..").selection(DialogStyle.NO_ICON, "Test Set", "Holds test cases", "ts").build();
        final @NotNull Form withAList = shown(new Form(getProject(), List.of(list)));
        final @NotNull JBList<?> rows = Drawn.components(realized(withAList.root())).stream().filter(JBList.class::isInstance).map(JBList.class::cast).findFirst().orElseThrow();
        final @NotNull Rectangle firstRow = rows.getCellBounds(0, 0);

        rows.dispatchEvent(new MouseEvent(rows, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, firstRow.x + 1, firstRow.y + 1, 1, false, MouseEvent.BUTTON1));
        assertEquals("clicking a row did not do what Enter does", 1, withAList.submitted.get());
    }

    // UC-INTERNAL-007, Rule-INTERNAL-061
    public void testOnlyADialogThatAsksForASizeOrSaysItIsResizableCanBeMovedOrResized() {
        final @NotNull Form fitted = shown(new Form(getProject(), List.of(aField("Name"))));
        assertEquals("a dialog sized by its contents can be resized", false, popupField(popupOf(fitted), "myResizable"));
        assertEquals("a dialog sized by its contents can be moved", false, popupField(popupOf(fitted), "myMovable"));
        assertFalse("a dialog sized by its contents was given a size of its own", fitted.root().isPreferredSizeSet());
        fitted.closeCancel();

        final @NotNull Form tall = shown(new Form(getProject(), List.of(aField("Name")), false, DialogSize.TALL));
        assertEquals("a dialog that asks for a size cannot be resized", true, popupField(popupOf(tall), "myResizable"));
        assertEquals("a dialog that asks for a size cannot be moved", true, popupField(popupOf(tall), "myMovable"));
        tall.closeCancel();

        final @NotNull Form growing = shown(new Form(getProject(), List.of(aField("Name")), true, DialogSize.CONTENT));
        assertEquals("a dialog that says it is resizable cannot be resized", true, popupField(popupOf(growing), "myResizable"));
        assertEquals("a dialog that says it is resizable cannot be moved", true, popupField(popupOf(growing), "myMovable"));
        assertFalse("a resizable dialog that names no size does not keep the size its contents give", growing.root().isPreferredSizeSet());
    }

    // UC-INTERNAL-007, Rule-INTERNAL-075
    public void testADialogOfAKindAlreadyOnScreenIsBroughtForwardRatherThanOpenedAgain() {
        final @NotNull Form first = shown(new Form(getProject(), List.of(aField("Name"))));
        final @NotNull Form second = new Form(getProject(), List.of(aField("Name")));

        assertFalse("a second dialog of a kind already on screen was opened", second.show());
        assertFalse("the dialog already on screen was closed", first.getPopup().isDisposed());
        assertSame("the dialog on screen is not the first one", first.getPopup(), ShownDialog.popup(getProject(), Form.class));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-075
    public void testADialogHoldingNothingTheTesterTypedIsReplacedByTheNewerOne() {
        final @NotNull AtomicInteger olderAnswered = new AtomicInteger();
        final @NotNull ConfirmDialog older = shown(new ConfirmDialog(getProject(), "Remove", "Remove Login?", "", "", "Remove", olderAnswered::incrementAndGet));
        final @NotNull ConfirmDialog newer = shown(new ConfirmDialog(getProject(), "Remove", "Remove Payment?", "", "", "Remove", () -> {
        }));

        assertTrue("the older question is still open beside the newer one", older.getPopup().isDisposed());
        assertSame(newer.getPopup(), ShownDialog.popup(getProject(), ConfirmDialog.class));

        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);
        assertEquals("the key meant for the newer question answered the older one", 0, olderAnswered.get());
    }

    // UC-INTERNAL-007, Rule-INTERNAL-076
    public void testOnlyTheSearchAndTheDetailsCloseWhenTheTesterClicksAway() {
        final @NotNull Form form = shown(new Form(getProject(), List.of(aField("Name"))));
        final @NotNull ConfirmDialog confirm = shown(new ConfirmDialog(getProject(), "Remove", "Remove Login?", "", "", "Remove", () -> {
        }));
        final @NotNull GlobalSearchDialog search = shown(new GlobalSearchDialog(getProject()));

        assertFalse("a dialog holding what the tester typed closes on a stray click", form.dismissOnClickOutside);
        assertFalse("a confirmation closes on a stray click", confirm.dismissOnClickOutside);
        assertTrue("the search does not close when the tester clicks away", search.dismissOnClickOutside);
        assertTrue("the Details dialog does not close when the tester clicks away", shown(new NodeDetailsDialog(getProject(), new TestSetNode())).dismissOnClickOutside);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-101
    public void testADialogWhoseSizeTheTesterCanChangeCanBeMaximizedAndPutBack() {
        final @NotNull Form fitted = shown(new Form(getProject(), List.of(aField("Name"))));
        assertTrue("a dialog the tester cannot resize offers to maximize", maximizeButtonOf(fitted).isEmpty());
        fitted.closeCancel();

        final @NotNull DialogPart<TextInput> name = aField("Name");
        final @NotNull Form resizable = shown(new Form(getProject(), List.of(name), true, DialogSize.CONTENT));
        final @NotNull AbstractIconButton maximize = maximizeButtonOf(resizable).orElseThrow(() -> new AssertionError("a resizable dialog has no maximize button in its title bar"));
        final @NotNull Dimension openedAt = resizable.getPopup().getSize();
        ((JTextField) name.getComponent().getFocusComponent()).setText("Log in with a valid user");

        maximize.doClick();
        assertEquals("maximize did not fill the IDE frame", DialogSize.frameOn(getProject()).getSize(), resizable.getPopup().getSize());

        resizable.refit();
        assertEquals("a maximized dialog grew or shrank on its own", DialogSize.frameOn(getProject()).getSize(), resizable.getPopup().getSize());

        maximize.doClick();
        assertEquals("pressing maximize again did not put the dialog back at the size it opened at", openedAt, resizable.getPopup().getSize());
        assertEquals("what was typed did not survive maximizing", "Log in with a valid user", ((JTextField) name.getComponent().getFocusComponent()).getText());
        assertEquals("a dialog offers more than maximize in its title bar, such as minimize", 1, Drawn.components(popupOf(resizable).getTitle()).stream().filter(AbstractIconButton.class::isInstance).count());
    }

    // UC-INTERNAL-007, Rule-INTERNAL-102
    public void testOnlyADialogWhoseSizeTheTesterOwnsScrollsAndATreeAsksForEightRows() {
        final @NotNull Form fitted = shown(new Form(getProject(), List.of(aField("Name"))));
        final @NotNull Component fittedCenter = ((BorderLayout) fitted.root().getLayout()).getLayoutComponent(BorderLayout.CENTER);
        assertFalse("a dialog that cannot be made smaller than its content has a scrollbar", fittedCenter instanceof JScrollPane);
        fitted.closeCancel();

        final @NotNull SelectionTree tree = new SelectionTree("Test cases", new CheckedTreeNode("all"), TestCaseSelectionRenderer.create(), Optional.empty());
        final @NotNull Form resizable = shown(new Form(getProject(), List.of(aField("Name"), DialogPart.of(tree)), true, DialogSize.CONTENT));
        final @NotNull Component center = ((BorderLayout) resizable.root().getLayout()).getLayoutComponent(BorderLayout.CENTER);

        assertTrue("a dialog the tester can make smaller than its content clips it instead of scrolling", center instanceof JScrollPane);
        assertEquals("the scrollbar is not there only when it is needed", ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ((JScrollPane) center).getVerticalScrollBarPolicy());
        assertEquals("the strip moved into what scrolls", BorderLayout.SOUTH, ((BorderLayout) resizable.root().getLayout()).getConstraints(Stream.of(resizable.root().getComponents()).filter(child -> !child.equals(center)).findFirst().orElseThrow()));

        final @NotNull Tree drawnTree = Drawn.components(tree.getPanel()).stream().filter(Tree.class::isInstance).map(Tree.class::cast).findFirst().orElseThrow();
        assertEquals("a tree does not ask for eight rows", 8, drawnTree.getVisibleRowCount());
    }

    // UC-INTERNAL-007, Rule-INTERNAL-102
    public void testATableAsksForEightRows() {
        final @NotNull JBTable table = Drawn.components(DialogPart.table().column("Test project", 40).build().getComponent().getPanel()).stream()
                .filter(JBTable.class::isInstance).map(JBTable.class::cast).findFirst().orElseThrow();

        assertEquals("a table does not ask for eight rows", 8 * table.getRowHeight(), table.getPreferredScrollableViewportSize().height);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-144
    public void testATableColumnIsACountOfCharactersOfTheTablesOwnFont() {
        final @NotNull JBTable table = Drawn.components(DialogPart.table().column("Test project", 26).column("Status", 10).build().getComponent().getPanel()).stream()
                .filter(JBTable.class::isInstance).map(JBTable.class::cast).findFirst().orElseThrow();
        final int character = table.getFontMetrics(table.getFont()).charWidth('m');

        assertEquals("the first column is not 26 characters of the table's font wide", 26 * character, table.getColumnModel().getColumn(0).getPreferredWidth());
        assertEquals("the second column is not 10 characters of the table's font wide", 10 * character, table.getColumnModel().getColumn(1).getPreferredWidth());
    }

    // UC-INTERNAL-007, Rule-INTERNAL-100
    public void testAResizableDialogIsSixTenthsOfTheFrameWideClampedBetweenItsContentAndTheFrame() {
        final @NotNull Rectangle frame = DialogSize.frameOn(getProject());
        final int margin = JBUI.scale(32);

        final @NotNull Form growing = shown(new Form(getProject(), List.of(aField("Name")), true, DialogSize.CONTENT));
        assertEquals("a resizable dialog is not six tenths of the frame wide", (int) (frame.width * 0.6), growing.getPopup().getSize().width);
        assertFalse("a dialog that names no height was given one", growing.root().isPreferredSizeSet());

        final @NotNull JPanel small = new JPanel();
        small.setPreferredSize(new Dimension(100, 100));
        DialogSize.TALL.applyTo(getProject(), small);
        assertEquals("a dialog that names its height is not six tenths of the frame wide", (int) (frame.width * 0.6), small.getPreferredSize().width);

        final @NotNull JPanel wide = new JPanel();
        wide.setPreferredSize(new Dimension((int) (frame.width * 0.8), 100));
        DialogSize.TALL.applyTo(getProject(), wide);
        assertEquals("a dialog was made narrower than its content needs", (int) (frame.width * 0.8), wide.getPreferredSize().width);

        final @NotNull JPanel huge = new JPanel();
        huge.setPreferredSize(new Dimension(frame.width * 3, frame.height * 3));
        DialogSize.TALL.applyTo(getProject(), huge);
        assertEquals("a dialog larger than the frame is not the frame's size less a margin", new Dimension(frame.width - margin, frame.height - margin), huge.getPreferredSize());
    }

    // UC-INTERNAL-007, Rule-INTERNAL-100
    public void testADialogThatNamesNoHeightGrowsAsItsContentDoes() {
        final @NotNull Form growing = shown(new Form(getProject(), List.of(aField("Name")), true, DialogSize.CONTENT));
        final int openedAt = growing.getPopup().getSize().height;
        final @NotNull Component center = ((BorderLayout) growing.root().getLayout()).getLayoutComponent(BorderLayout.CENTER);
        center.setPreferredSize(new Dimension(center.getPreferredSize().width, center.getPreferredSize().height + openedAt));

        growing.refit();
        final int grown = growing.getPopup().getSize().height;
        assertTrue("a dialog that names no height did not grow as its content did: " + openedAt + " then " + grown, grown > openedAt);

        growing.refit();
        assertEquals("refitting a dialog whose content did not change again changed its height", grown, growing.getPopup().getSize().height);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-100
    public void testTheHeightADialogNamesIsHalfTheFrameOrSevenTenthsOfIt() {
        final @NotNull Rectangle frame = DialogSize.frameOn(getProject());
        final @NotNull List<Integer> named = List.of((int) (frame.height * 0.5), (int) (frame.height * 0.7));

        for (final DialogSize size : List.of(DialogSize.HALF, DialogSize.TALL)) {
            final @NotNull JPanel content = new JPanel();
            content.setPreferredSize(new Dimension(100, 100));
            size.applyTo(getProject(), content);
            assertTrue("a dialog can name a height of " + size.heightPart() + " of the frame, which is neither half nor seven tenths", named.contains(content.getPreferredSize().height));
        }
    }

    private static final class TabStop extends LayoutFocusTraversalPolicy {
        @Override
        public boolean accept(final @NotNull Component component) {
            return super.accept(component);
        }
    }

    private static final class Form extends AbstractFrameworkDialog {
        private final @NotNull AtomicInteger submitted = new AtomicInteger();

        private final @NotNull AtomicInteger searched = new AtomicInteger();

        private Form(final @NotNull Project project, final @NotNull List<? extends DialogPart<?>> fields) {
            this(project, fields, false, DialogSize.CONTENT);
        }

        private Form(final @NotNull Project project, final @NotNull List<? extends DialogPart<?>> fields, final boolean canResize, final @NotNull DialogSize named) {
            super(project);
            title = "Every field";
            components = fields;
            resizable = canResize;
            size = named;
            shortcuts = List.of(
                    StatusBarShortcut.build(Shortcuts.Enter, "Save", this::submit),
                    StatusBarShortcut.build(Shortcuts.FocusSearch, "Search", searched::incrementAndGet),
                    StatusBarShortcut.cancel(this::closeCancel));
        }

        @Override
        protected void submit() {
            submitted.incrementAndGet();
        }
    }
}
