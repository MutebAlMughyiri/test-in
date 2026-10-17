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
package org.testin.editor.testrun;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.editor.EditorFixtures;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.runner.CapturedRunner;

import javax.swing.JList;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

public class SelectionFollowsTheAutomationIdeTest extends AbstractCodegenIdeTest {

    private static void pressOnTheFirstCard(final @NotNull JList<TestCase> list) {
        final @NotNull Rectangle cell = list.getCellBounds(0, 0);
        list.dispatchEvent(new MouseEvent(list, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, cell.x + 2, cell.y + cell.height - 2, 1, false, MouseEvent.BUTTON1));
    }

    @Override
    protected void setUp() {
        super.setUp();
        CapturedRunner.installed(getTestRootDisposable());
    }

    private @NotNull List<TestCase> paymentTestCases() {
        final @NotNull TestSetNode payment = createdTestSet("Payment");
        final @NotNull List<TestCase> made = List.of(
                createdTestCase(payment, "Pay with a saved card", "payWithASavedCard"),
                createdTestCase(payment, "Pay with Apple Pay", "payWithApplePay"),
                createdTestCase(payment, "Refuse an expired card", "refuseAnExpiredCard"));
        settled();
        return made;
    }

    private @NotNull TestRunEditor runningEverything(final @NotNull List<TestCase> testCases) {
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), EditorFixtures.testRunOver(getProject(), root, testCases), getTestRootDisposable());
        editor.getList().setSelectedIndex(0);
        editor.runWhenLoaded();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        return editor;
    }

    private void started(final @NotNull TestCase testCase) {
        CapturedRunner.startedByTestNg(getProject(), testCase);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    // Rule-EDITOR-PANEL-289
    public void testTheSelectionMovesToEachRunItemAsTestNgStartsIt() {
        final @NotNull List<TestCase> testCases = paymentTestCases();
        final @NotNull TestRunEditor editor = runningEverything(testCases);
        try {
            started(testCases.get(2));
            assertEquals("the selection did not move to the run item TestNG started", testCases.get(2), editor.getList().getSelectedValue());

            started(testCases.get(1));
            assertEquals("the selection did not move to the next run item TestNG started", testCases.get(1), editor.getList().getSelectedValue());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-289
    public void testAPressOnACardDoesNotStopTheSelectionFollowing() {
        final @NotNull List<TestCase> testCases = paymentTestCases();
        final @NotNull TestRunEditor editor = runningEverything(testCases);
        try {
            pressOnTheFirstCard(editor.getList());
            editor.getList().setSelectedIndex(0);
            final @NotNull TestCase pressed = editor.getList().getSelectedValue();
            final @NotNull TestCase next = testCases.stream().filter(testCase -> testCase != pressed).findFirst().orElseThrow();

            started(next);

            assertEquals("a press on a card stopped the selection following the automation", next, editor.getList().getSelectedValue());
        } finally {
            Disposer.dispose(editor);
        }
    }
}
