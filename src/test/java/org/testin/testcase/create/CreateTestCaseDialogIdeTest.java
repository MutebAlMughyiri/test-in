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

package org.testin.testcase.create;

import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.clipboard.CopyChoice;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.indexer.TestCases;
import org.testin.model.Priority;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Done;
import org.testin.services.Services;
import org.testin.testcase.CreateTestCaseAction;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.TestSetEditorFields;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;
import org.testin.view.PopupsBuilt;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateTestCaseDialogIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull List<TestCase> saved = new ArrayList<>();

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), CreateTestCaseDialog.class);
        super.tearDown();
    }

    private @NotNull CreateTestCaseDialog shown() {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        EditorFixtures.testCases(getProject(), testSet, 3);

        final @NotNull CreateTestCaseDialog dialog = new CreateTestCaseDialog(getProject(), testSet, saved::add);
        ShownDialog.open(getProject(), CreateTestCaseDialog.class, dialog::show);
        return dialog;
    }

    private void press(final @NotNull CreateTestCaseFields field) {
        ShownDialog.press(getProject(), CreateTestCaseDialog.class, field.getShortcut().getKey());
    }

    private void save() {
        ShownDialog.press(getProject(), CreateTestCaseDialog.class, Shortcuts.Enter.getKey());
    }

    private @NotNull TestCase theOneSaved() {
        assertEquals("the dialog did not save exactly one test case", 1, saved.size());
        return saved.getFirst();
    }

    // Rule-EDITOR-PANEL-028
    public void testTheDialogOpensOnTheDescriptionAloneAndAFieldAppearsOnItsKey() {
        final @NotNull CreateTestCaseDialog dialog = shown();

        assertTrue("the description is not shown", dialog.getDescriptionSection().isShown());
        for (final TestCaseSection section : dialog.getAllSections())
            if (!section.equals(dialog.getDescriptionSection()))
                assertFalse(section.getClass().getSimpleName() + " is shown before its key was pressed", section.isShown());

        press(CreateTestCaseFields.EXPECTED_RESULT);

        assertTrue("the expected result did not appear on its key", dialog.getExpectedResultSection().isShown());
        assertFalse("another field appeared on the expected result's key", dialog.getModuleSection().isShown());
    }

    // Rule-EDITOR-PANEL-271
    public void testReferenceOpensOnCtrlRAndCarriesTheLetterTheUpdateAndCopyMenusGiveIt() {
        final @NotNull CreateTestCaseDialog dialog = shown();

        assertEquals("Reference is not Ctrl+R, Cmd+R on a Mac", KeyStroke.getKeyStroke(KeyEvent.VK_R, Shortcuts.menuMask()), CreateTestCaseFields.REFERENCE.getShortcut().getKey());
        press(CreateTestCaseFields.REFERENCE);

        assertTrue("Reference did not open on its key", dialog.getReferenceSection().isShown());
        assertEquals("R", TestSetEditorFields.REFERENCE.getLetter());
        assertSame("the update menu gives Reference another icon", CreateTestCaseFields.REFERENCE.getIcon(), UpdateTestCaseFields.REFERENCE.getIcon());
        assertSame("the copy menu gives Reference another icon", CreateTestCaseFields.REFERENCE.getIcon(), CopyChoice.REFERENCE.getIcon());
    }

    // Rule-EDITOR-PANEL-029, Rule-EDITOR-PANEL-031
    public void testAFieldLeftUnopenedWritesNothingAndTheTestCaseStartsAtTheLowestPriority() {
        final @NotNull CreateTestCaseDialog dialog = shown();
        dialog.getDescriptionSection().field.setText("Pay with a saved card");
        dialog.getModuleSection().field.setText("Payments");

        save();

        assertEquals("a field the tester never opened was written", "", theOneSaved().getModule());
        assertEquals("a new test case did not start at the lowest priority", Priority.LOW, theOneSaved().getPriority());
        assertEquals("the priority field does not start at the lowest priority", Priority.LOW, dialog.getPrioritySection().applyTo(TestCase.builder().priority(Priority.HIGH).build()).getPriority());
    }

    // Rule-EDITOR-PANEL-032, Rule-EDITOR-PANEL-033
    public void testEveryTypedFieldIsTrimmedAndABlankStepIsDropped() {
        final @NotNull CreateTestCaseDialog dialog = shown();
        press(CreateTestCaseFields.EXPECTED_RESULT);
        press(CreateTestCaseFields.MODULE);
        press(CreateTestCaseFields.PRE_CONDITIONS);
        press(CreateTestCaseFields.TEST_DATA);
        press(CreateTestCaseFields.STEPS);
        press(CreateTestCaseFields.GROUP);
        press(CreateTestCaseFields.REFERENCE);
        dialog.addBoxAt(dialog.getStepsSection().getFields().getFirst());
        dialog.addBoxAt(dialog.getStepsSection().getFields().getFirst());

        dialog.getDescriptionSection().field.setText("  Pay with a saved card  ");
        dialog.getExpectedResultSection().field.setText("  The order is placed  ");
        dialog.getModuleSection().field.setText("  Payments  ");
        dialog.getPreConditionsSection().field.setText("  A card is saved  ");
        dialog.getTestDataSection().field.setText("  4111 1111 1111 1111  ");
        dialog.getGroupSection().getFields().getFirst().setText("  smoke  ");
        dialog.getReferenceSection().field.setText("  PAY-12  ");
        final @NotNull List<EditorTextField> steps = dialog.getStepsSection().getFields();
        assertEquals("Ctrl+S and two Ctrl+= did not give three steps", 3, steps.size());
        steps.getFirst().setText("  Open the cart  ");
        steps.get(1).setText("   ");
        steps.get(2).setText("  Pay  ");

        save();

        final @NotNull TestCase testCase = theOneSaved();
        assertEquals("Pay with a saved card", testCase.getDescription());
        assertEquals("The order is placed", testCase.getExpectedResult());
        assertEquals("Payments", testCase.getModule());
        assertEquals("A card is saved", testCase.getPreConditions());
        assertEquals("4111 1111 1111 1111", testCase.getTestData());
        assertEquals(List.of("smoke"), testCase.getGroups());
        assertEquals("PAY-12", testCase.getReference());
        assertEquals("a blank step was kept, or a step was not trimmed", List.of("Open the cart", "Pay"), testCase.getSteps());
    }

    // UC-EDITOR-PANEL-005
    public void testAnEmptyDescriptionKeepsTheDialogOpenSavesNothingAndSaysNothing() {
        shown();
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        save();

        assertTrue("a test case with no description was saved", saved.isEmpty());
        assertTrue("an empty description closed the dialog", ShownDialog.isOpen(getProject(), CreateTestCaseDialog.class));
        assertEquals("an empty description raised a message", List.of(), said.shown());
    }

    // Rule-EDITOR-PANEL-034
    public void testTheDialogStaysOpenWhenTheTesterClicksOutsideOrTheIdeLosesTheFocus() {
        final @NotNull PopupsBuilt popups = PopupsBuilt.recording(getTestRootDisposable());
        new CreateTestCaseDialog(getProject(), EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout"), saved::add).show();

        assertEquals("a click outside closes the dialog", List.of(false), popups.last().asked("setCancelOnClickOutside"));
        assertEquals("the IDE losing the focus closes the dialog", List.of(false), popups.last().asked("setCancelOnWindowDeactivation"));
    }

    // Rule-EDITOR-PANEL-030, Rule-EDITOR-PANEL-008
    public void testANewTestCaseSortsLastAndConfirmsItselfOnce() {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        EditorFixtures.testCases(getProject(), testSet, 3);
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());

        ShownDialog.open(getProject(), CreateTestCaseDialog.class, () -> CreateTestCaseAction.openCreateDialog(getProject(), editor, testSet));
        Drawn.components(ShownDialog.content(getProject(), CreateTestCaseDialog.class)).stream().filter(EditorTextField.class::isInstance).map(EditorTextField.class::cast).findFirst().orElseThrow().setText("Pay with a saved card");
        save();

        final @NotNull TestCases testCases = Services.getInstance(getProject(), TestCases.class);
        Await.until("the new test case never reached the editor", () -> editor.getAllTestCases().size() == 4);
        final @NotNull TestCase created = editor.getAllTestCases().getLast();
        assertEquals("the new test case is not last", "Pay with a saved card", created.getDescription());
        Await.until("the new test case was never given a place after the others", () -> testCases.findTestCase(created.getUuid()).map(TestCase::getOrder).filter(order -> order.compareTo("m0002") > 0).isPresent());
        Await.until("the creation never confirmed itself", () -> balloons.shown().contains(Done.CREATED.getOutcome()));
        assertEquals("the creation confirmed itself more than once", 1, Collections.frequency(balloons.shown(), Done.CREATED.getOutcome()));
    }
}
