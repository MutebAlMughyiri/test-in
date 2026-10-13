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
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Done;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;

import java.util.List;

public class RemoveConfirmsOnceIdeTest extends AbstractTempRootIdeTest {

    // Rule-PRODUCT-014
    public void testRemovingSeveralTestCasesConfirmsOnceInThePastTenseWithACount() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode login = EditorFixtures.testSet(getProject(), testProject, "Login");
        final @NotNull List<TestCaseDto> testCases = EditorFixtures.testCases(getProject(), login, 4);
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), login, getTestRootDisposable());
        final @NotNull List<TestCaseDto> chosen = testCases.subList(0, 3);
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());
        new RemoveTestCaseWork(getProject(), editor, login, chosen).remove();
        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);
        Await.until("the three test cases were never removed", () -> chosen.stream().allMatch(testCase -> Services.getInstance(getProject(), TestCases.class).findTestCase(testCase.getId()).isEmpty()));
        Await.until("removing three test cases said nothing", () -> balloons.shown().stream().anyMatch(said -> said.startsWith(Done.REMOVED.getOutcome())));

        final @NotNull List<String> confirmations = balloons.shown().stream().filter(said -> said.startsWith(Done.REMOVED.getOutcome())).toList();
        assertEquals("removing three test cases did not confirm once, in the past tense, with the count", List.of(Done.counted(Done.REMOVED.getOutcome(), 3)), confirmations);
    }
}
