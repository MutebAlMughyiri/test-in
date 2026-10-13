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

package org.testin.editor.toolbar;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.filter.FilterPopupButton;
import org.testin.filter.SortPopupButton;

import java.util.List;

public class TestRunToolbar extends AbstractToolbarPanel {
    private final @NotNull Project project;

    private final @NotNull TestRunEditor editor;

    public TestRunToolbar(final @NotNull Project project, final @NotNull TestRunEditor editor) {
        super(editor);
        this.project = project;
        this.editor = editor;
        layoutComponents();
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-129
    @Override
    public @NotNull List<ToolbarItem> getCustomComponents() {
        return List.of(
                new StartExecutionButton(editor, getCallbacks()::onStartExecutionClicked),
                new StopExecutionButton(getCallbacks()::onStopExecutionClicked),
                new LightModeButton(editor),
                new GenerateReportButton(project, editor),
                new RefreshButton(getCallbacks()::onToolBarRefreshButtonClicked),
                new TestRunFieldsPopupButton(getCallbacks()::onToolBarDetailsSelectionChanged),
                new FilterPopupButton(getCallbacks()),
                new SortPopupButton(getCallbacks()),
                new CardViewButton(getCallbacks()::onToolBarSwitchedToCardView),
                new GridViewButton(getCallbacks()::onToolBarSwitchedToGridView)
        );
    }

    @Override
    protected @NotNull List<ToolbarItem> getTrailingComponents() {
        return List.of(new TestRunResultAnalysisButton(editor, getCallbacks()::onToolBarTestRunResultAnalysisClicked));
    }
}