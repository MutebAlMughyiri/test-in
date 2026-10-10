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

    public static @NotNull TestProjectNode testProject(final @NotNull Project p, final @NotNull Path root) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestProjectNode tp = Services.getInstance(p, NodeMapper.class).setTestProjectNode(root.resolve("NAFATH"));
            Services.getInstance(p, Nodes.class).addTestProject(tp);
            return tp;
        });
    }

    public static @NotNull TestSetNode testSet(final @NotNull Project p, final @NotNull TestProjectNode tp, final @NotNull String name) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestSetNode ts = Services.getInstance(p, NodeMapper.class).getTestSetNode(tp.getTestCasesFolder().getPath().resolve(name), tp.getTestCasesFolder());
            Services.getInstance(p, Nodes.class).addTestSet(ts);
            return ts;
        });
    }

    public static @NotNull TestCaseDto testCase(final @NotNull Project p, final @NotNull TestSetNode ts, final @NotNull String description, final @NotNull String order) {
        final @NotNull TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description(description).order(order).build();
        tc.setParent(ts);
        Services.getInstance(p, TestCases.class).putTestCaseVerbatim(ts.getPath(), tc);
        return tc;
    }

    public static @NotNull List<TestCaseDto> testCases(final @NotNull Project p, final @NotNull TestSetNode ts, final int count) {
        final @NotNull List<TestCaseDto> made = new ArrayList<>();
        for (int i = 0; i < count; i++)
            made.add(testCase(p, ts, "Test case number " + (i + 1), String.format("m%04d", i)));
        return made;
    }

    public static @NotNull TestRunNode testRun(final @NotNull Project p, final @NotNull TestProjectNode tp, final @NotNull List<RunItem> runItems) {
        final @NotNull TestRunNode tr = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunNode made = Services.getInstance(p, NodeMapper.class).setTestRunNode(tp.getTestRunsFolder().getPath().resolve("Cycle-1"), tp.getTestRunsFolder());
            Services.getInstance(p, Nodes.class).addTestRunNode(made);
            return made;
        });

        Services.getInstance(p, TestRuns.class).putRunItems(tr.getPath(), new RunItems().setAll(new ArrayList<>(runItems)));
        return tr;
    }

    public static @NotNull RunItem pending(final @NotNull TestCaseDto tc) {
        return new RunItem().setId(tc.getId());
    }

    public static @NotNull TestSetEditor openTestSetEditor(final @NotNull Project p, final @NotNull TestSetNode ts, final @NotNull Disposable owner) {
        final @NotNull TestSetEditor editor = new TestSetEditor(p, new UnifiedVirtualFile(ts));
        Disposer.register(owner, editor);

        Await.until("the test set editor never loaded", () -> !editor.isLoading());
        return editor;
    }

    public static @NotNull TestRunEditor openTestRunEditor(final @NotNull Project p, final @NotNull TestRunNode tr, final @NotNull Disposable owner) {
        final @NotNull TestRunEditor editor = new TestRunEditor(p, new UnifiedVirtualFile(tr));
        Disposer.register(owner, editor);

        Await.until("the test run editor never loaded", () -> editor.loadedRunItems().isPresent());
        return editor;
    }
}
