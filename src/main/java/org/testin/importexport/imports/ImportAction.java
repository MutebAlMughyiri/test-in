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

package org.testin.importexport.imports;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.TestinData;
import org.testin.model.node.AbstractNode;
import org.testin.util.Bundle;

public class ImportAction extends AbstractAnyProjectAction {
    public static final @NotNull String NAME = Bundle.message("import.action.name");

    // UC-SHARE-005
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project p) {
        TestinData.firstSelected(e, AbstractNode.class)
                .filter(AbstractNode::isTestCaseContainer)
                .ifPresent(dir -> new ImportWork(p).openImportDialog(dir));
    }

    // UC-SHARE-005
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project p) {
        e.getPresentation().setEnabled(TestinData.singleSelectedNode(e)
                .filter(AbstractNode::isTestCaseContainer)
                .isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
