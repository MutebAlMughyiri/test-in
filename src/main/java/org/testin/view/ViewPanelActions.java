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

package org.testin.view;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.card.ShownTestCaseAction;
import org.testin.editor.card.TestCaseQuickAction;

import javax.swing.JComponent;
import java.util.List;

public class ViewPanelActions {
    // UC-VIEW-PANEL-003, UC-VIEW-PANEL-012, UC-VIEW-PANEL-014, Rule-VIEW-PANEL-019
    public @NotNull List<AnAction> create(final @NotNull ViewPanel panel, final @NotNull JComponent component) {
        final @NotNull Project project = panel.getProject();

        ShownTestCaseAction.bind(project, TestCaseQuickAction.NAVIGATE_TO_TEST_METHOD, panel::getCurrentTestCase, (action, testCase) -> action.execute(project, testCase), component);
        ShownTestCaseAction.bind(project, TestCaseQuickAction.RUN_TEST_METHOD, panel::getCurrentTestCase, (action, testCase) -> action.execute(project, testCase), component);

        return List.of(
                new PreviousTestCaseAction(panel.getPage(), component),
                new NextTestCaseAction(panel.getPage(), component)
        );
    }
}
