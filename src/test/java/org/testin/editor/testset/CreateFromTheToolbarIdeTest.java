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

package org.testin.editor.testset;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.model.node.TestSetNode;
import org.testin.testcase.create.CreateTestCaseDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;

import java.util.List;

public class CreateFromTheToolbarIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestSetEditor anOpenTestSet() {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        EditorFixtures.testCases(getProject(), testSet, 2);
        return EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), CreateTestCaseDialog.class);
        super.tearDown();
    }

    // UC-EDITOR-PANEL-005
    public void testCreateWhileTheTestSetIsStillBeingReadIsRefusedAndSaysWhy() {
        final @NotNull TestSetEditor editor = anOpenTestSet();
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        editor.reloadData();
        editor.onToolBarCreateTestCaseClicked();

        assertEquals("Create while the test set was being read was not refused with its reason", List.of(Bundle.message("create.test.case.still.loading")), said.shown());
        assertFalse("Create opened its dialog over a test set still being read", ShownDialog.isOpen(getProject(), CreateTestCaseDialog.class));
        Await.until("the test set was never read again", () -> !editor.isLoading());
    }

    // UC-EDITOR-PANEL-005
    public void testCreateOnATestSetThatHasBeenReadOpensTheDialog() {
        final @NotNull TestSetEditor editor = anOpenTestSet();

        editor.onToolBarCreateTestCaseClicked();

        assertTrue("Create on a test set that has been read did not open its dialog", ShownDialog.isOpen(getProject(), CreateTestCaseDialog.class));
    }

    // UC-EDITOR-PANEL-001
    public void testAnEmptyTestSetPutInOrderStillSaysItHoldsNothing() {
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Empty"), getTestRootDisposable());

        editor.refreshOrdered();

        assertTrue("an empty test set put in order no longer says it holds nothing: " + editor.getList().getEmptyText().getText(), editor.getList().getEmptyText().getText().startsWith(Bundle.message("editor.test.empty")));
    }
}
