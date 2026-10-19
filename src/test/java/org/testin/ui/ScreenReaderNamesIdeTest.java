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
package org.testin.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.StandIn;
import org.testin.editor.TestinEditor;
import org.testin.editor.card.AbstractCard;
import org.testin.editor.card.HoverButton;
import org.testin.editor.card.Offered;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.editor.cardview.CardPanelBuilder;
import org.testin.editor.cardview.CardView;
import org.testin.editor.grid.GridTables;
import org.testin.editor.statusbar.EditorStatusBar;
import org.testin.editor.statusbar.PageStep;
import org.testin.editor.testrun.RunItemCard;
import org.testin.explorer.tree.TestProjectTree;
import org.testin.model.TestCase;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.RunItemExecution;
import org.testin.testcase.TestSetEditorFields;
import org.testin.testrun.TestRunEditorFields;
import org.testin.testrun.form.TestCaseSelectionRenderer;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogStyle;
import org.testin.ui.framework.SelectionTree;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.view.Drawn;
import org.testin.view.ViewPanel;
import org.testin.view.ViewTab;

import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.LayoutFocusTraversalPolicy;
import java.awt.Component;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public class ScreenReaderNamesIdeTest extends AbstractIdeTest {

    private static @NotNull String spoken(final @NotNull AccessibleContext context) {
        return Objects.requireNonNullElse(context.getAccessibleName(), "").trim();
    }

    private static @NotNull List<Component> assertEveryStopIsNamed(final @NotNull String surface, final @NotNull JComponent drawn) {
        final boolean attached = !drawn.isDisplayable();
        if (attached) drawn.addNotify();
        try {
            final @NotNull TabStop tabStop = new TabStop();
            final @NotNull List<Component> stops = Stream.concat(Stream.of(drawn), Drawn.components(drawn).stream()).filter(tabStop::accept).toList();
            assertFalse(surface + ": the keyboard reaches nothing on it", stops.isEmpty());

            for (final Component stop : stops) {
                assertFalse(surface + ": the keyboard lands on a " + stop.getClass().getName() + " a screen reader has no name for",
                        spoken(stop.getAccessibleContext()).isEmpty());
            }
            return stops;
        } finally {
            if (attached) drawn.removeNotify();
        }
    }

    // Rule-INTERNAL-122
    public void testEveryFieldADialogCanHoldIsNamedFromItsCaptionOrItsHint() {
        final @NotNull EveryField dialog = new EveryField(getProject());
        dialog.show();
        try {
            final @NotNull List<Component> stops = assertEveryStopIsNamed("a dialog", dialog.root());
            final @NotNull List<String> names = stops.stream().map(stop -> spoken(stop.getAccessibleContext())).toList();

            assertTrue("a captioned field is not named by its caption as written: " + names, names.contains("Name"));
            assertTrue("a field without a caption is not named by its hint: " + names, names.contains("set path.."));
            final @NotNull List<Component> drawn = Drawn.components(dialog.root());
            final @NotNull Component steps = drawn.stream().filter(EditorTextField.class::isInstance).findFirst().orElseThrow();
            final @NotNull List<String> stepsCaptions = drawn.stream().filter(JLabel.class::isInstance).map(JLabel.class::cast).filter(label -> steps.equals(label.getLabelFor())).map(label -> spoken(label.getAccessibleContext())).toList();
            assertEquals("a box of many lines does not hand its caption to the editor it opens", List.of("Steps"), stepsCaptions);
            assertTrue("a tree is not named by its caption: " + names, names.contains("Test cases"));
        } finally {
            dialog.close();
        }
    }

    // Rule-INTERNAL-122
    public void testACardSaysItsTitleThenTheRunItemStatusItShows() {
        final @NotNull TestCase testCase = TestCase.builder().uuid(UUID.randomUUID()).description("Log in with a valid user").build();
        final @NotNull RunItem runItem = new RunItem().setUuid(testCase.getUuid()).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build())).showing(Optional.of(testCase), Optional.empty(), Optional.empty(), false);
        final @NotNull RunItemCard card = new RunItemCard(getProject());

        final @NotNull CardView view = CardPanelBuilder.build(getProject(), getTestRootDisposable(), StandIn.of(TestinEditor.class));
        view.model().add(testCase);
        final @NotNull JBList<TestCase> list = view.list();
        list.setCellRenderer((_, shown, index, _, _) -> {
            card.updateData(index, Set.of(TestRunEditorFields.RUN_STATUS), runItem, AbstractCard.titleText(index + 1, true, shown.getDescription()));
            return card;
        });

        assertEveryStopIsNamed("the card list", list);

        final @NotNull AccessibleContext first = list.getAccessibleContext().getAccessibleChild(0).getAccessibleContext();
        assertEquals("1. Log in with a valid user", spoken(first));
        assertTrue("the card does not say the run item status it shows: " + first.getAccessibleDescription(),
                Objects.requireNonNullElse(first.getAccessibleDescription(), "").contains(RunItemStatus.FAILED.getLabel()));
    }

    // Rule-INTERNAL-122
    public void testAGridCellSaysTheValueItShows() {
        final String @NotNull [] row = new String[TestSetEditorFields.values().length];
        Arrays.fill(row, "");
        row[TestSetEditorFields.DESCRIPTION.column()] = "Log in with a valid user";

        final @NotNull JBTable table = new GridTables().buildTestTable(List.<String[]>of(row), Set.of(TestSetEditorFields.DESCRIPTION));

        assertEveryStopIsNamed("the grid", table);
        assertEquals("Log in with a valid user", spoken(table.getAccessibleContext().getAccessibleTable().getAccessibleAt(0, 0).getAccessibleContext()));
    }

    // Rule-INTERNAL-122
    public void testTheStatusBarNamesItsFieldAndItsIconButtons() {
        final @NotNull EditorStatusBar bar = new EditorStatusBar();

        assertEveryStopIsNamed("the status bar", bar);
        for (final PageStep step : PageStep.values()) {
            assertEquals(step.getTooltip(), spoken(bar.button(step).getAccessibleContext()));
        }
    }

    // Rule-INTERNAL-122
    public void testAHoverButtonIsNamedAndSaysWhyItIsGray() {
        final @NotNull TestCaseQuickAction action = TestCaseQuickAction.NAVIGATE_TO_TEST_METHOD;

        final @NotNull JComponent works = HoverButton.of(getProject(), new Offered(action, Optional.empty()), AllIcons.Actions.Execute, "Run test case", () -> {
        });
        assertEquals("Run test case", spoken(works.getAccessibleContext()));

        final @NotNull JComponent gray = HoverButton.of(getProject(), new Offered(action, Optional.of("No generated code yet")), AllIcons.Actions.Execute, "Run test case", () -> {
        });
        assertEquals("a gray hover button does not say why", "No generated code yet", gray.getAccessibleContext().getAccessibleDescription());
    }

    // Rule-INTERNAL-122
    public void testTheExplorerTreeIsNamed() {
        final @NotNull TestProjectTree tree = new TestProjectTree(getProject());
        Disposer.register(getTestRootDisposable(), tree);

        assertEveryStopIsNamed("the explorer tree", tree.getMainTree());
    }

    // Rule-INTERNAL-122
    public void testEveryViewPanelTabIsNamedByItsTitle() {
        final @NotNull ViewPanel panel = new ViewPanel(getProject());

        for (final ViewTab tab : ViewTab.values()) {
            assertEquals(tab.getDisplayName(), spoken(tab.keyboardTargetOf(panel).getAccessibleContext()));
        }
    }

    private static final class TabStop extends LayoutFocusTraversalPolicy {
        @Override
        public boolean accept(final @NotNull Component component) {
            return super.accept(component);
        }
    }

    private static final class EveryField extends AbstractFrameworkDialog {
        private EveryField(final @NotNull Project project) {
            super(project);
            title = "Every field";
            components = List.of(
                    DialogPart.textInput().caption("Name").placeholder("set name..").build(),
                    DialogPart.textInput().placeholder("set path..").build(),
                    DialogPart.multiLineField(project, "Steps", "write the steps..", ""),
                    DialogPart.choice("Branch", List.of("main", "release"), "main"),
                    DialogPart.<String>radios("Format").option("PDF", "pdf").option("HTML", "html").select("pdf").build(),
                    DialogPart.<Integer>textFieldWithSelections().placeholder("set kind..").selection(DialogStyle.NO_ICON, "Test Set", "Holds test cases", 1).build(),
                    DialogPart.of(new SelectionTree("Test cases", new CheckedTreeNode("all"), TestCaseSelectionRenderer.create(), Optional.empty())),
                    DialogPart.table().column("Test project", 40).build(),
                    DialogPart.textArea().placeholder("paste the failure..").build(),
                    DialogPart.splitButton("Commit", "Commit and Push"),
                    DialogPart.button("Generate"));
            shortcuts = List.of(StatusBarShortcut.cancel(this::closeCancel));
        }

        @Override
        protected void submit() {
        }

        private void close() {
            closeCancel();
        }
    }
}
