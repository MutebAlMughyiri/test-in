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

import com.intellij.testFramework.DumbModeTestUtils;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.List;

public class RemoveWhileIndexingIdeTest extends AbstractCodegenIdeTest {

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), ConfirmDialog.class);
        super.tearDown();
    }

    // UC-EDITOR-PANEL-011, Rule-CODEGEN-096
    public void testATestCaseIsNotRemovedWhileTheIdeIsIndexingAndSaysWhy() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase testCase = createdTestCase(login, "Log in with a valid user", "a");
        settled();
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), login, getTestRootDisposable());
        new RemoveTestCaseWork(getProject(), editor, login, List.of(testCase)).remove();

        final @NotNull List<String> said = Said.during(getProject(), () -> DumbModeTestUtils.runInDumbModeSynchronously(getProject(), () -> ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter)));

        assertEquals("a removal refused while indexing did not say why", List.of(Refused.WHILE_INDEXING.about(Bundle.message("remove.confirm.button"))), said);
        assertTrue("a test case was removed while the IDE indexed", Services.getInstance(getProject(), TestCases.class).findTestCase(testCase.getUuid()).isPresent());
    }
}
