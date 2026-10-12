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
import org.testin.editor.EditorFixtures;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.testcase.update.UpdateTestCaseDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;

import javax.swing.KeyStroke;
import java.util.List;

public class AddKeyIdeTest extends AbstractTempRootIdeTest {

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), CreateTestCaseDialog.class);
        ShownDialog.close(getProject(), UpdateTestCaseDialog.class);
        super.tearDown();
    }

    private @NotNull TestSetNode checkout() {
        return EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
    }

    private @NotNull CreateTestCaseDialog createDialog() {
        final @NotNull CreateTestCaseDialog dialog = new CreateTestCaseDialog(getProject(), checkout(), _ -> {
        });
        ShownDialog.open(getProject(), CreateTestCaseDialog.class, dialog::show);
        return dialog;
    }

    private void press(final @NotNull CreateTestCaseFields field) {
        ShownDialog.press(getProject(), CreateTestCaseDialog.class, field.getShortcut().getKey());
    }

    private static @NotNull List<EditorTextField> steps(final @NotNull AbstractTestCaseDialog dialog) {
        return dialog.getStepsSection().getFields();
    }

    private static @NotNull List<EditorTextField> groups(final @NotNull AbstractTestCaseDialog dialog) {
        return dialog.getGroupSection().getFields();
    }

    // Rule-EDITOR-PANEL-293
    public void testCtrlEqualsAndCtrlPlusAreBothOnTheDialog() {
        createDialog();

        for (final KeyStroke stroke : Shortcuts.AddItem.getKeys())
            ShownDialog.press(getProject(), CreateTestCaseDialog.class, stroke);
    }

    // Rule-EDITOR-PANEL-293
    public void testInStepsItAddsAStepBox() {
        final @NotNull CreateTestCaseDialog dialog = createDialog();
        press(CreateTestCaseFields.STEPS);

        dialog.addBoxAt(steps(dialog).getFirst());

        assertEquals("the add key in Steps did not add one step box", 2, steps(dialog).size());
        assertSame("the new step box is not the one the cursor goes to", steps(dialog).getLast(), dialog.getStepsSection().getFocusComponent());
    }

    // Rule-EDITOR-PANEL-293
    public void testInGroupItAddsAGroupBox() {
        final @NotNull CreateTestCaseDialog dialog = createDialog();
        press(CreateTestCaseFields.GROUP);

        dialog.addBoxAt(groups(dialog).getFirst());

        assertEquals("the add key in Group did not add one group box", 2, groups(dialog).size());
        assertFalse("the add key in Group opened Steps", dialog.getStepsSection().isShown());
    }

    // Rule-EDITOR-PANEL-293
    public void testAFieldKeyOpensItOnceAndAddsNothing() {
        final @NotNull CreateTestCaseDialog dialog = createDialog();

        press(CreateTestCaseFields.STEPS);
        press(CreateTestCaseFields.STEPS);
        press(CreateTestCaseFields.GROUP);
        press(CreateTestCaseFields.GROUP);

        assertEquals("pressing Ctrl+S again added a step box", 1, steps(dialog).size());
        assertEquals("pressing Ctrl+G again added a group box", 1, groups(dialog).size());
    }

    // Rule-EDITOR-PANEL-293
    public void testElsewhereItAddsNothing() {
        final @NotNull CreateTestCaseDialog dialog = createDialog();
        press(CreateTestCaseFields.STEPS);

        dialog.addBoxAt(dialog.getDescriptionSection().field);

        assertEquals("the add key in the description added a step box", 1, steps(dialog).size());
        assertFalse("the add key in the description opened Group", dialog.getGroupSection().isShown());
    }

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-293
    public void testTheUpdateDialogAddsToTheFieldBeingUpdated() {
        final @NotNull TestCaseDto tc = EditorFixtures.testCase(getProject(), checkout(), "Pay with a saved card", "1")
                .edit().steps(List.of("Open the cart")).build();
        final @NotNull AbstractTestCaseDialog dialog = new UpdateTestCaseDialog(getProject(), tc, UpdateTestCaseFields.STEPS, _ -> {
        });
        ShownDialog.open(getProject(), UpdateTestCaseDialog.class, dialog::show);

        dialog.addBoxAt(steps(dialog).getFirst());

        assertEquals("the add key did not add a step box while updating the steps", 2, steps(dialog).size());
    }
}
