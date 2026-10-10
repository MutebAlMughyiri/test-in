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

package org.testin.actions;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.Component;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ActionSystem {
    public static void perform(final @NotNull Component source, final @NotNull String place, final @NotNull ActionUiKind uiKind, final @NotNull Runnable work) {
        final @NotNull AnAction action = new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                work.run();
            }
        };

        ActionUtil.performAction(action, AnActionEvent.createEvent(action, DataManager.getInstance().getDataContext(source), action.getTemplatePresentation().clone(), place, uiKind, null));
    }
}
