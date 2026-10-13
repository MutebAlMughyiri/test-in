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

package org.testin.view.details;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.AutomationState;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.editor.card.HoverButton;
import org.testin.editor.card.Offered;
import org.testin.model.Automated;
import org.testin.model.TestCaseDto;
import org.testin.model.node.AbstractNode;
import org.testin.services.Services;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.JComponent;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCaseHoverButtons {
    // UC-VIEW-PANEL-009, UC-VIEW-PANEL-012, UC-VIEW-PANEL-014, Rule-VIEW-PANEL-050, Rule-VIEW-PANEL-056, Rule-VIEW-PANEL-057, Rule-VIEW-PANEL-063, Rule-EDITOR-PANEL-237
    public static @NotNull List<JComponent> of(final @NotNull Project p, final @NotNull TestCaseDto shownTestCase, final @NotNull Optional<AbstractNode> shownIn) {
        final @NotNull Offered navigate = TestCaseQuickAction.NAVIGATE_TO_TEST_METHOD.offer(p, shownIn, shownTestCase);
        final @NotNull Offered testRun = TestCaseQuickAction.RUN_TEST_METHOD.offer(p, shownIn, shownTestCase);
        final @NotNull Offered testCase = TestCaseQuickAction.NAVIGATE_TO_TEST_CASE.offer(p, shownIn, shownTestCase);

        final @NotNull AutomationState automation = Services.getInstance(p, AutomationState.class);
        automation.read(p, List.of(shownTestCase), () -> ViewToolWindowFactory.panel(p).ifPresent(ViewPanel::refreshCurrentView));

        final @NotNull Automated state = automation.of(shownTestCase.getId());

        return List.of(
                HoverButton.of(p, navigate, state.getIcon(), navigate.action().getTooltip(), () -> navigate.action().execute(p, shownTestCase)),
                HoverButton.of(p, testRun, testRun.action().getIcon(), testRun.action().getTooltip(), () -> testRun.action().execute(p, shownTestCase)),
                HoverButton.of(p, testCase, testCase.action().getIcon(), testCase.action().getTooltip(), () -> testCase.action().execute(p, shownTestCase)));
    }
}
