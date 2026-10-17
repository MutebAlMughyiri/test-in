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

package org.testin.testrun;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.Gestures;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.RunItem;
import org.testin.services.Services;
import org.testin.testrun.failure.FailureDetailDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.view.Drawn;

import javax.swing.JComponent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record TestRunFixture(@NotNull Project project, @NotNull List<TestCase> testCases, @NotNull TestRunNode testRun) {

    public static @NotNull TestRunFixture of(final @NotNull Project project, final @NotNull Path root, final @NotNull List<RunItem> runItems, final @NotNull List<TestCase> testCases) {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(project, root);
        return new TestRunFixture(project, testCases, EditorFixtures.testRun(project, testProject, runItems));
    }

    public static @NotNull List<TestCase> testCasesIn(final @NotNull Project project, final @NotNull Path root, final int count) {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(project, root);
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(project, testProject, "Checkout");
        final @NotNull List<TestCase> made = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final @NotNull TestCase testCase = TestCase.builder().uuid(UUID.randomUUID()).description("Test case number " + (i + 1)).expectedResult("It works " + (i + 1)).order(String.format("m%04d", i)).build();
            testCase.setParent(testSet);
            Services.getInstance(project, TestCases.class).putTestCaseVerbatim(testSet.getPath(), testCase);
            made.add(testCase);
        }
        return made;
    }

    public static @NotNull TestRunFixture pending(final @NotNull Project project, final @NotNull Path root, final int count) {
        final @NotNull List<TestCase> testCases = testCasesIn(project, root, count);
        return of(project, root, testCases.stream().map(EditorFixtures::pending).toList(), testCases);
    }

    public static @NotNull AnAction keyFor(final @NotNull TestRunEditor editor, final @NotNull RunItemStatus runItemStatus) {
        return ActionUtil.getActions(editor.getList()).stream()
                .filter(action -> action instanceof final SetRunItemStatusAction key && key.getRunItemStatus() == runItemStatus)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no key records " + runItemStatus));
    }

    public static @NotNull List<EditorTextField> boxesOf(final @NotNull JComponent dialog) {
        return Drawn.components(dialog).stream().filter(EditorTextField.class::isInstance).map(EditorTextField.class::cast).toList();
    }

    public @NotNull TestRunEditor opened(final @NotNull Disposable owner) {
        return EditorFixtures.openTestRunEditor(project, testRun, owner);
    }

    public @NotNull RunItem runItemOf(final @NotNull TestCase testCase) {
        return Services.getInstance(project, TestRuns.class).getRunItems(testRun.getPath()).runItemOf(testCase.getUuid()).orElseThrow();
    }

    public @NotNull RunItemStatus runItemStatusOf(final @NotNull TestCase testCase) {
        return runItemOf(testCase).getStatus();
    }

    public void press(final @NotNull TestRunEditor editor, final @NotNull RunItemStatus runItemStatus) {
        Gestures.press(project, keyFor(editor, runItemStatus), editor.getList());
    }

    public @NotNull Optional<JComponent> failureDialog() {
        return Optional.of(FailureDetailDialog.class).filter(kind -> ShownDialog.isOpen(project, kind)).map(kind -> ShownDialog.content(project, kind));
    }

    public @NotNull JComponent openFailureDialog() {
        Await.until("the failure dialog never opened", () -> failureDialog().isPresent());
        return failureDialog().orElseThrow();
    }
}
