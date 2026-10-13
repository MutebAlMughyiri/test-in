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

package org.testin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.editor.open.TestinEditors;
import org.testin.editor.open.UnifiedVirtualFile;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItem;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EditorFixtures {

    public static @NotNull TestProjectNode testProject(final @NotNull Project project, final @NotNull Path root) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestProjectNode testProject = Services.getInstance(project, NodeMapper.class).setTestProjectNode(root.resolve("NAFATH"));
            Services.getInstance(project, Nodes.class).addTestProject(testProject);
            return testProject;
        });
    }

    public static @NotNull TestSetNode testSet(final @NotNull Project project, final @NotNull TestProjectNode testProject, final @NotNull String name) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestSetNode testSet = Services.getInstance(project, NodeMapper.class).getTestSetNode(testProject.getTestCasesFolder().getPath().resolve(name), testProject.getTestCasesFolder());
            Services.getInstance(project, Nodes.class).addTestSet(testSet);
            return testSet;
        });
    }

    public static @NotNull TestCaseDto testCase(final @NotNull Project project, final @NotNull TestSetNode testSet, final @NotNull String description, final @NotNull String order) {
        final @NotNull TestCaseDto testCase = TestCaseDto.builder().id(UUID.randomUUID()).description(description).order(order).build();
        testCase.setParent(testSet);
        Services.getInstance(project, TestCases.class).putTestCaseVerbatim(testSet.getPath(), testCase);
        return testCase;
    }

    public static @NotNull List<TestCaseDto> testCases(final @NotNull Project project, final @NotNull TestSetNode testSet, final int count) {
        final @NotNull List<TestCaseDto> made = new ArrayList<>();
        for (int i = 0; i < count; i++)
            made.add(testCase(project, testSet, "Test case number " + (i + 1), String.format("m%04d", i)));
        return made;
    }

    public static @NotNull TestRunNode testRun(final @NotNull Project project, final @NotNull TestProjectNode testProject, final @NotNull List<RunItem> runItems) {
        final @NotNull TestRunNode testRun = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunNode made = Services.getInstance(project, NodeMapper.class).setTestRunNode(testProject.getTestRunsFolder().getPath().resolve("Cycle-1"), testProject.getTestRunsFolder());
            Services.getInstance(project, Nodes.class).addTestRunNode(made);
            return made;
        });

        Services.getInstance(project, TestRuns.class).putRunItems(testRun.getPath(), new RunItems().setAll(new ArrayList<>(runItems)));
        return testRun;
    }

    public static @NotNull TestRunNode testRunOver(final @NotNull Project project, final @NotNull Path root, final @NotNull List<TestCaseDto> testCases) {
        return testRun(project, testProject(project, root), testCases.stream().map(EditorFixtures::pending).toList());
    }

    public static @NotNull TestRunEditor openTestRunInTheIde(final @NotNull Project project, final @NotNull TestRunNode testRun) {
        final @NotNull TestinEditors editors = Services.getInstance(project, TestinEditors.class);
        editors.open(testRun);
        Await.until("the test run never opened in its editor", () -> editors.testRunEditorFor(testRun).filter(editor -> editor.loadedRunItems().isPresent()).isPresent());
        return editors.testRunEditorFor(testRun).orElseThrow();
    }

    public static @NotNull RunItem pending(final @NotNull TestCaseDto testCase) {
        return new RunItem().setId(testCase.getId());
    }

    public static @NotNull TestSetEditor openTestSetEditor(final @NotNull Project project, final @NotNull TestSetNode testSet, final @NotNull Disposable owner) {
        final @NotNull TestSetEditor editor = new TestSetEditor(project, new UnifiedVirtualFile(testSet));
        Disposer.register(owner, editor);

        Await.until("the test set editor never loaded", () -> !editor.isLoading());
        return editor;
    }

    public static @NotNull TestRunEditor openTestRunEditor(final @NotNull Project project, final @NotNull TestRunNode testRun, final @NotNull Disposable owner) {
        final @NotNull TestRunEditor editor = new TestRunEditor(project, new UnifiedVirtualFile(testRun));
        Disposer.register(owner, editor);

        Await.until("the test run editor never loaded", () -> editor.loadedRunItems().isPresent());
        return editor;
    }
}
