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

package org.testin.testrun;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;
import org.testin.model.status.RunItemStatus;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import java.util.Arrays;
import java.util.List;

public class SetRunItemStatusGroup extends DefaultActionGroup {
    // UC-EDITOR-PANEL-032
    public SetRunItemStatusGroup() {
        super(runItemStatuses());
    }

    // UC-EDITOR-PANEL-032
    public static void bindLettersTo(final @NotNull JComponent list) {
        runItemStatuses().forEach(action -> action.registerCustomShortcutSet(action.getShortcutSet(), list));
    }

    // UC-EDITOR-PANEL-032, Rule-EDITOR-PANEL-213
    private static @NotNull List<SetRunItemStatusAction> runItemStatuses() {
        return Arrays.stream(RunItemStatus.values())
                .filter(RunItemStatus::isRunItemStatus)
                .map(runItemStatus -> {
                    final @NotNull SetRunItemStatusAction action = new SetRunItemStatusAction(runItemStatus);

                    action.registerCustomShortcutSet(Shortcuts.customShortcut(runItemStatus.getMenuEntry().shortcut()), null);
                    return action;
                })
                .toList();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
