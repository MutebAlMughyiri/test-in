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

import com.intellij.notification.Notification;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractOpenEditorsIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.Said;
import org.testin.editor.open.TestinEditors;
import org.testin.editor.testset.TestSetEditor;
import org.testin.indexer.TestCases;
import org.testin.indexer.recyclebin.RecycleBin;
import org.testin.model.FileKind;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Done;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.List;

public class RecycleBinRefusesIdeTest extends AbstractOpenEditorsIdeTest {

    @Override
    public void tearDown() {
        ShownDialog.close(getProject(), ConfirmDialog.class);
        super.tearDown();
    }

    // UC-EDITOR-PANEL-011, Rule-INTERNAL-143
    public void testATestCaseTheRecycleBinRefusesStaysInTheOpenEditorAndOnlyTheOnesThatWentAreCounted() {
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestSetNode login = made.testSet(made.testProject(root.resolve("NAFATH")).getTestCasesFolder(), "Login");
        final @NotNull List<TestCase> testCases = List.of(made.testCase(login), made.testCase(login), made.testCase(login));
        opened(login);
        final @NotNull TestSetEditor editor = (TestSetEditor) Services.getInstance(getProject(), TestinEditors.class).editorFor(login).orElseThrow(() -> new AssertionError("the test set has no editor"));
        Await.until("the test set editor never loaded", () -> !editor.isLoading());
        final @NotNull TestCase refused = testCases.getFirst();
        final @NotNull String refusedFile = FileKind.TEST_CASE.fileName(refused.getUuid());
        RecycleBin.standIn(getTestRootDisposable(), file -> !file.getName().equals(refusedFile) && FileUtil.delete(file));
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());
        final @NotNull List<Notification> notifications = said.notifications();

        new RemoveTestCaseWork(getProject(), editor, login, testCases.subList(0, 2)).remove();
        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);

        Await.until("the removal that went was not counted alone: " + said.shown(), () -> said.shown().contains(Done.counted(Done.REMOVED.getOutcome(), 1)));
        assertTrue("the file the recycle bin refused was not named", notifications.stream().anyMatch(notification -> Bundle.message("recycle.bin.refused.title").equals(notification.getTitle()) && notification.getContent().contains(refusedFile)));
        Await.until("the open editor did not read its test set again with the refused test case there", () -> editor.getAllTestCases().stream().anyMatch(testCase -> testCase.getUuid().equals(refused.getUuid())));
        assertTrue("the test case the recycle bin refused left the index", Services.getInstance(getProject(), TestCases.class).findTestCase(refused.getUuid()).isPresent());
    }
}
