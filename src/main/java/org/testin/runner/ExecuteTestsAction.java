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
import org.testin.actions.TestinData;
import org.testin.codegen.CodeOn;
import org.testin.editor.TestinEditor;
import org.testin.editor.open.TestinEditors;
import org.testin.indexer.TestCases;
import org.testin.logger.Logger;
import org.testin.model.TestCaseDto;
import org.testin.model.node.Node;
import org.testin.model.node.TestRunNode;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;

import java.util.List;
import java.util.Optional;

public class ExecuteTestsAction extends AbstractAnyProjectAction {
    // Rule-TREE-PANEL-079
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project p) {
        if (CodeOn.grayedWithReason(this, e, p)) return;
        if (OptionalPlugin.TESTNG.grayedWithReason(this, e.getPresentation())) return;

        e.getPresentation().setEnabled(runnable(e).isPresent() || selectedTestRun(e).isPresent());
    }

    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project p) {
        selectedTestRun(e).ifPresentOrElse(testRun -> openAndRun(p, testRun), () -> runnable(e).ifPresent(dir -> run(p, dir)));
    }

    private @NotNull Optional<Node> runnable(final @NotNull AnActionEvent e) {
        return TestinData.firstSelected(e, Node.class).filter(Node::isTestCaseContainer);
    }

    private @NotNull Optional<TestRunNode> selectedTestRun(final @NotNull AnActionEvent e) {
        return TestinData.firstSelected(e, TestRunNode.class)
                .filter(TestRunNode::isOpen);
    }

    private void openAndRun(final @NotNull Project p, final @NotNull TestRunNode testRun) {
        Services.getInstance(p, TestinEditors.class).openThen(testRun, TestinEditor::runWhenLoaded);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-031
    private void run(final @NotNull Project p, final @NotNull Node dir) {
        final @NotNull List<TestCaseDto> testCases = Services.getInstance(p, TestCases.class).getTestCasesUnder(dir);

        if (testCases.isEmpty()) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Refused.NOTHING_TO_RUN, dir.getName());
            return;
        }

        Logger.info("Running " + dir.getName() + " with " + testCases.size() + " test case(s)");

        ExecuteTestCases.run(p, testCases);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
