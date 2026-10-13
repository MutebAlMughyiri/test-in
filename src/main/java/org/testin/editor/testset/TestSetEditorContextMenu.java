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

package org.testin.editor.testset;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.actions.TestinEscapeAction;
import org.testin.editor.AbstractEditorContextMenu;
import org.testin.editor.TestinEditor;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.open.OpenContextMenuAction;
import org.testin.testcase.UpdateTestCaseAction;
import org.testin.testcase.UpdateTestCaseFields;

public class TestSetEditorContextMenu extends AbstractEditorContextMenu {
    private final @NotNull Project project;

    private final @NotNull TestinEditor ui;

    public TestSetEditorContextMenu(final @NotNull Project project, final @NotNull TestinEditor ui, final @NotNull TestSetNode dir, final @NotNull JBList<TestCaseDto> list) {
        super();
        this.project = project;
        this.ui = ui;

        add(Declared.forMenu("Testin.CreateTestCase"));
        add(Declared.forMenu("Testin.ViewDetails"));

        addSeparator();

        add(Declared.forMenu("Testin.UpdateTestCase"));

        add(actions(project, dir, list));

        addSeparator();

        add(Declared.forMenu("Testin.AutomateTestCase"));
        add(Declared.forMenu("Testin.RunTestMethod"));
        add(Declared.forMenu("Testin.NavigateToTestMethod"));
    }

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194
    @Override
    public void registerShortcuts(final @NotNull JBList<TestCaseDto> list) {
        new TestinEscapeAction(project, list);
        new OpenContextMenuAction(list, this);

        for (final UpdateTestCaseFields field : UpdateTestCaseFields.values()) {
            field.bindShortcut(list, () -> UpdateTestCaseAction.openField(project, ui, field));
        }
    }
}