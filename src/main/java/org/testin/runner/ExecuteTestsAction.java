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

package org.testin.runner;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.TestinDataKeys;
import org.testin.codegen.CodeOn;
import org.testin.editor.TestinEditor;
import org.testin.editor.open.TestinEditors;
import org.testin.editor.testrun.ExecutionMode;
import org.testin.indexer.TestCases;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestRunNode;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;

import java.util.List;
import java.util.Optional;

public class ExecuteTestsAction extends AbstractAnyProjectAction {
    // Rule-TREE-PANEL-079, Rule-TREE-PANEL-141, Rule-TREE-PANEL-080
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project project) {
        if (CodeOn.grayedWithReason(this, e, project)) return;
        if (OptionalPlugin.TESTNG.grayedWithReason(this, e.getPresentation())) return;

        final @NotNull Optional<TestRunNode> testRun = selectedTestRun(e);
        final @NotNull ExecutionMode mode = testRun.map(selected -> modeOf(project, selected)).orElse(ExecutionMode.IDLE);
        if (mode != ExecutionMode.IDLE) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setDescription(mode.getRefusal());
            return;
        }

        e.getPresentation().setEnabled(runnable(e).isPresent() || testRun.isPresent());
    }

    // UC-TREE-PANEL-023, Rule-TREE-PANEL-141
    private @NotNull ExecutionMode modeOf(final @NotNull Project project, final @NotNull TestRunNode testRun) {
        return Services.getInstance(project, TestinEditors.class).testRunEditorFor(testRun)
                .map(editor -> editor.getWalk().mode())
                .orElse(ExecutionMode.IDLE);
    }

    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project project) {
        selectedTestRun(e).ifPresentOrElse(testRun -> openAndRun(project, testRun), () -> runnable(e).ifPresent(node -> run(project, node)));
    }

    private @NotNull Optional<AbstractNode> runnable(final @NotNull AnActionEvent e) {
        return TestinDataKeys.firstSelected(e, AbstractNode.class).filter(AbstractNode::isTestCaseContainer);
    }

    private @NotNull Optional<TestRunNode> selectedTestRun(final @NotNull AnActionEvent e) {
        return TestinDataKeys.firstSelected(e, TestRunNode.class)
                .filter(TestRunNode::isOpen);
    }

    private void openAndRun(final @NotNull Project project, final @NotNull TestRunNode testRun) {
        Services.getInstance(project, TestinEditors.class).openThen(testRun, TestinEditor::runWhenLoaded);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-031
    private void run(final @NotNull Project project, final @NotNull AbstractNode node) {
        final @NotNull List<TestCase> testCases = Services.getInstance(project, TestCases.class).getTestCasesUnder(node);

        if (testCases.isEmpty()) {
            Services.getInstance(project, Notifier.class).softRefuse(project, Refused.NOTHING_TO_RUN, node.getName());
            return;
        }

        TestinLogger.info("Running " + node.getName() + " with " + testCases.size() + " test case(s)");

        ExecuteTestCases.run(project, testCases);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
