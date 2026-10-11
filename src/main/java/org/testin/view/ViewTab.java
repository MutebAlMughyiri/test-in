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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.view.details.DetailsTab;
import org.testin.view.history.HistoryTab;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum ViewTab {
    DETAILS(
            Bundle.message("view.tab.details"),
            ViewPanel::getDetailsScrollPane,
            ViewPanel::getDetailsTab,
            panel -> new DetailsTab().load(panel.getP(), panel.getDetailsTab(), panel.shownTestCase(), panel.shownRunItem(), panel.getPage().getCurrentPath(), panel.shownLine(), panel::showExecution)
    ),

    HISTORY(
            Bundle.message("view.tab.history"),
            ViewPanel::getHistoryScrollPane,
            ViewPanel::getHistoryTab,
            panel -> new HistoryTab().load(panel.getP(), panel.getHistoryTab(), panel.liveTestCase())
    );

    private final @NotNull String displayName;

    private final @NotNull Function<ViewPanel, JScrollPane> pane;

    private final @NotNull Function<ViewPanel, JComponent> keyboardTarget;

    @Getter(AccessLevel.NONE)
    private final @NotNull Consumer<ViewPanel> loader;

    public @NotNull JScrollPane paneOf(final @NotNull ViewPanel panel) {
        return pane.apply(panel);
    }

    // UC-VIEW-PANEL-017, Rule-VIEW-PANEL-080
    public @NotNull JComponent keyboardTargetOf(final @NotNull ViewPanel panel) {
        return keyboardTarget.apply(panel);
    }

    // Rule-VIEW-PANEL-008
    public void load(final @NotNull ViewPanel panel) {
        loader.accept(panel);
    }
}
