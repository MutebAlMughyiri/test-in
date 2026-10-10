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

package org.testin.testcase;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Shortcuts;

import java.util.List;

public class RemovedTestMethodComesBackIdeTest extends AbstractCodegenIdeTest {
    private static final @NotNull String LOGIN_CLASS = "nafath.LoginTest";

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), ConfirmDialog.class);
        super.tearDown();
    }

    // Rule-EDITOR-PANEL-069
    public void testATestCaseComingBackFromARemovalGetsItsTestMethodAgain() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCaseDto tc = createdTestCase(login, "Log in with a valid user", "m");
        settled();
        writtenMethodOf(LOGIN_CLASS, tc);
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), login, getTestRootDisposable());

        ShownDialog.open(getProject(), ConfirmDialog.class, () -> new RemoveTestCaseWork(getProject(), editor, login, List.of(tc)).remove());
        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter.getKey());
        Await.until("the test method never went with the test case", () -> {
            settled();
            return methodOf(LOGIN_CLASS, tc).isEmpty();
        });

        final @NotNull UndoScope scope = UndoScope.of(login.getPath());
        Await.until("the removal never reached the undo history", () -> Services.getInstance(getProject(), UndoHistories.class).canUndo(scope));
        assertTrue("the removal could not be taken back", Services.getInstance(getProject(), UndoHistories.class).undo(scope));

        Await.until("the test case came back without its test method", () -> {
            settled();
            return methodOf(LOGIN_CLASS, tc).isPresent();
        });
    }
}
