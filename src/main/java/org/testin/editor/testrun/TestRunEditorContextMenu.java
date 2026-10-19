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

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.actions.TestinEscapeAction;
import org.testin.editor.AbstractEditorContextMenu;
import org.testin.editor.TestinEditor;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.open.OpenContextMenuAction;
import org.testin.report.GenerateReportAction;
import org.testin.testrun.SetRunItemStatusGroup;

public class TestRunEditorContextMenu extends AbstractEditorContextMenu {
    private final @NotNull Project project;

    public TestRunEditorContextMenu(final @NotNull Project project, final @NotNull TestinEditor editor, final @NotNull AbstractNode node, final @NotNull JBList<TestCase> list) {
        super();
        this.project = project;

        add(Declared.forMenu("Testin.SetRunItemStatus"));
        addSeparator();
        add(Declared.forMenu("Testin.UpdateRunItem"));
        addSeparator();
        add(Declared.forMenu("Testin.ViewDetails"));
        add(Declared.forMenu("Testin.NavigateToTestCase"));
        addSeparator();

        add(actions(project, node, list));

        addSeparator();
        add(Declared.forMenu("Testin.RunTestMethod"));
        add(Declared.forMenu("Testin.NavigateToTestMethod"));

        addSeparator();
        add(new GenerateReportAction(project, editor, list));
    }

    // UC-EDITOR-PANEL-026, UC-EDITOR-PANEL-029, Rule-PRODUCT-024
    @Override
    public void registerShortcuts(final @NotNull JBList<TestCase> list) {
        new TestinEscapeAction(project, list);
        new OpenContextMenuAction(list, this);

        SetRunItemStatusGroup.bindLettersTo(list);
    }
}