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

package org.testin.testcase.update;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.UiInterceptors;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.fields.IntegerField;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.filter.SortDirection;
import org.testin.filter.SortField;
import org.testin.filter.SortPopupButton;
import org.testin.filter.Sorting;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.testcase.TestCaseOrder;
import org.testin.testcase.UpdateTestCaseAction;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.ui.framework.ShownDialog;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class OrderByNumberIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestSetNode testSet = new TestSetNode();

    private static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError(file + " could not be read", ex);
        }
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), UpdateTestCaseDialog.class);
        super.tearDown();
    }

    private @NotNull TestSetEditor sixTestCases() {
        testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        EditorFixtures.testCases(getProject(), testSet, 6);
        return EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
    }

    private void select(final @NotNull TestSetEditor editor, final @NotNull String description) {
        for (int i = 0; i < editor.getList().getModel().getSize(); i++)
            if (editor.getList().getModel().getElementAt(i).getDescription().equals(description))
                editor.getList().setSelectedIndex(i);
    }

    private @NotNull IntegerField theOrderBox(final @NotNull TestSetEditor editor) {
        ShownDialog.open(getProject(), UpdateTestCaseDialog.class, () -> UpdateTestCaseAction.openField(getProject(), editor, UpdateTestCaseFields.ORDER));
        return Drawn.components(ShownDialog.content(getProject(), UpdateTestCaseDialog.class)).stream()
                .filter(IntegerField.class::isInstance).map(IntegerField.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError("the order dialog has no box for the number"));
    }

    private @NotNull List<String> storedOrder() {
        return TestCaseOrder.ordered(Services.getInstance(getProject(), TestCases.class).getTestCasesForTestSet(testSet.getPath())).stream().map(TestCase::getDescription).map(description -> description.replace("Test case number ", "")).toList();
    }

    private void moveToThird(final @NotNull TestSetEditor editor, final @NotNull String description) {
        select(editor, description);
        theOrderBox(editor).setValue(3);
        ShownDialog.press(getProject(), UpdateTestCaseDialog.class, Shortcuts.Enter.getKey());
    }

    private @NotNull List<String> everyFileButNumber6() {
        try (final Stream<Path> files = Files.list(testSet.getPath())) {
            return files.filter(file -> file.toString().endsWith(".testcase")).sorted().map(file -> file + "@" + file.toFile().lastModified() + "=" + read(file)).filter(file -> !file.contains("\"Test case number 6\"")).toList();
        } catch (final IOException ex) {
            throw new AssertionError("the test set could not be read", ex);
        }
    }

    // Rule-EDITOR-PANEL-054
    public void testTheNumberIsThePositionInTheWholeTestSet() {
        final @NotNull TestSetEditor editor = sixTestCases();
        editor.getToolBar().getSearchText().setText("number 5");
        Await.until("the search never narrowed the list to one", () -> editor.getList().getModel().getSize() == 1);

        select(editor, "Test case number 5");

        assertEquals("the box does not hold the position in the whole test set, counting from one", Integer.valueOf(5), theOrderBox(editor).getValue());
    }

    // Rule-EDITOR-PANEL-055, Rule-EDITOR-PANEL-056
    public void testTypingThreePutsItThirdAndOnlyItsOwnFileIsWritten() {
        final @NotNull TestSetEditor editor = sixTestCases();
        final @NotNull List<String> others = everyFileButNumber6();

        moveToThird(editor, "Test case number 6");

        Await.until("the test case never moved up to third: " + storedOrder(), () -> storedOrder().equals(List.of("1", "2", "6", "3", "4", "5")));
        assertEquals("a test case around the moved one was written", others, everyFileButNumber6());
    }

    // UC-EDITOR-PANEL-009
    public void testANumberAboveHowManyTestCasesThereAreIsRefusedAndNothingIsSaved() {
        final @NotNull TestSetEditor editor = sixTestCases();
        final @NotNull List<String> before = storedOrder();
        select(editor, "Test case number 6");
        final @NotNull IntegerField box = theOrderBox(editor);
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        box.setText("9");
        ShownDialog.press(getProject(), UpdateTestCaseDialog.class, Shortcuts.Enter.getKey());

        assertFalse("a number past the last test case was not refused in words", said.shown().isEmpty());
        assertTrue("a number past the last test case closed the dialog", ShownDialog.isOpen(getProject(), UpdateTestCaseDialog.class));
        assertEquals("a number past the last test case moved something", before, storedOrder());
    }

    // UC-EDITOR-PANEL-009
    public void testTheOrderKeyOnSeveralSelectedCardsIsRefusedInWords() {
        final @NotNull TestSetEditor editor = sixTestCases();
        editor.getList().setSelectionInterval(0, 1);
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        ShownDialog.press(editor.getList(), UpdateTestCaseFields.ORDER.getShortcut().getKey());

        assertTrue("the order key on two cards was not refused in words: " + said.shown(), said.shown().contains(Bundle.message("update.order.one.at.a.time")));
        assertFalse("the order key on two cards opened the order dialog", ShownDialog.isOpen(getProject(), UpdateTestCaseDialog.class));
    }

    // UC-EDITOR-PANEL-009
    public void testOrderOnTheUpdateMenuOfSeveralTestCasesIsGrayAndSaysWhyInPlaceOfItsKey() {
        final @NotNull TestSetEditor editor = sixTestCases();
        final @NotNull List<JBPopup> shown = new ArrayList<>();
        UiInterceptors.register(new UiInterceptors.UiInterceptor<>(JBPopup.class) {
            @Override
            protected void doIntercept(final @NotNull JBPopup popup) {
                shown.add(popup);
            }
        });

        new UpdateTestCaseMenu(getProject(), List.of(editor.getList().getModel().getElementAt(0), editor.getList().getModel().getElementAt(1)), (_, _) -> {
        }).show();

        final @NotNull JBList<?> offered = Drawn.first(shown.getFirst().getContent(), JBList.class);
        final @NotNull SimpleColoredComponent order = (SimpleColoredComponent) Drawn.row(offered, Arrays.asList(UpdateTestCaseFields.values()).indexOf(UpdateTestCaseFields.ORDER));
        final @NotNull SimpleColoredComponent.ColoredIterator name = order.iterator();
        name.next();
        assertFalse("Order can be chosen for two test cases", order.isEnabled());
        assertEquals("Order's name is not gray for two test cases", SimpleTextAttributes.GRAYED_ATTRIBUTES, name.getTextAttributes());
        assertTrue("Order does not say why in place of its key: " + order.getCharSequence(false), order.getCharSequence(false).toString().endsWith(Bundle.message("update.order.one.at.a.time")));
        shown.getFirst().cancel();
    }

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-009
    public void testClickingTheGrayOrderEntryOpensNothingAndKeepsTheMenu() {
        final @NotNull TestSetEditor editor = sixTestCases();
        final @NotNull List<JBPopup> shown = new ArrayList<>();
        UiInterceptors.register(new UiInterceptors.UiInterceptor<>(JBPopup.class) {
            @Override
            protected void doIntercept(final @NotNull JBPopup popup) {
                shown.add(popup);
            }
        });
        new UpdateTestCaseMenu(getProject(), List.of(editor.getList().getModel().getElementAt(0), editor.getList().getModel().getElementAt(1)), (_, _) -> {
        }).show();
        final @NotNull JBList<?> offered = Drawn.first(shown.getFirst().getContent(), JBList.class);
        final @NotNull Rectangle order = offered.getCellBounds(Arrays.asList(UpdateTestCaseFields.values()).indexOf(UpdateTestCaseFields.ORDER), Arrays.asList(UpdateTestCaseFields.values()).indexOf(UpdateTestCaseFields.ORDER));

        final @NotNull MouseEvent click = new MouseEvent(offered, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, order.x + 2, order.y + order.height / 2, 1, false, MouseEvent.BUTTON1);
        for (final MouseListener listener : offered.getMouseListeners()) listener.mouseClicked(click);

        assertFalse("clicking the gray Order entry closed the menu", shown.getFirst().isDisposed());
        assertFalse("clicking the gray Order entry opened its dialog", ShownDialog.isOpen(getProject(), UpdateTestCaseDialog.class));
        shown.getFirst().cancel();
    }

    // Rule-EDITOR-PANEL-055
    public void testMovingDownCountsWithTheTestCaseTakenOut() {
        final @NotNull TestSetEditor editor = sixTestCases();

        moveToThird(editor, "Test case number 1");

        Await.until("typing three did not put the test case third: " + storedOrder(), () -> storedOrder().equals(List.of("2", "3", "1", "4", "5", "6")));
    }

    // Rule-EDITOR-PANEL-282
    public void testUnderASortTheBoxHoldsThePlaceInTheSetAndThatIsWhatIsWritten() {
        final @NotNull TestSetEditor editor = sixTestCases();
        final @NotNull SortPopupButton sort = editor.getToolBar().getToolbarItem(SortPopupButton.class);
        Sorting.choose(sort, SortField.DESCRIPTION.getLabel());
        Sorting.choose(sort, SortDirection.DESCENDING.getLabel());
        Await.until("the sort never put number 6 first", () -> editor.getList().getModel().getSize() > 0 && editor.getList().getModel().getElementAt(0).getDescription().equals("Test case number 6"));

        select(editor, "Test case number 6");
        assertEquals("under a sort the box does not hold the place in the set", Integer.valueOf(6), theOrderBox(editor).getValue());
        ShownDialog.close(getProject(), UpdateTestCaseDialog.class);

        moveToThird(editor, "Test case number 6");

        Await.until("the place in the set was not what was written: " + storedOrder(), () -> storedOrder().equals(List.of("1", "2", "6", "3", "4", "5")));
    }

    // Rule-EDITOR-PANEL-038, Rule-EDITOR-PANEL-039, Rule-EDITOR-PANEL-223
    public void testOneOrderChangeAmongUnplacedTestCasesIsOneUndoThatPutsTheWholeSetBack() {
        testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Imported");
        for (final String description : List.of("Test case number 1", "Test case number 2", "Test case number 3"))
            EditorFixtures.testCase(getProject(), testSet, description, "");
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());

        final @NotNull String first = storedOrder().getFirst();
        moveToThird(editor, "Test case number " + first);
        Await.until("the test case never moved to third: " + storedOrder(), () -> storedOrder().getLast().equals(first));
        final @NotNull UndoScope scope = UndoScope.of(testSet.getPath());
        Await.until("the Order change never became an undo entry", () -> Services.getInstance(getProject(), UndoHistories.class).canUndo(scope));

        assertTrue("one Ctrl+Z was refused", Services.getInstance(getProject(), UndoHistories.class).undo(scope));

        Await.until("Ctrl+Z left a test case it ranked with its new order", () -> Services.getInstance(getProject(), TestCases.class).getTestCasesForTestSet(testSet.getPath()).stream().allMatch(testCase -> testCase.getOrder().isEmpty()));
        assertFalse("the Order change was more than one undo entry", Services.getInstance(getProject(), UndoHistories.class).canUndo(scope));
    }
}
