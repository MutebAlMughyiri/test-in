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

package org.testin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.TestActionEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.TestinDataKeys;
import org.testin.model.node.AbstractNode;

import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TreeGesture {

    private static @NotNull AnActionEvent on(final @NotNull Project project, final @NotNull AnAction action, final @NotNull List<AbstractNode> selected) {
        return TestActionEvent.createTestEvent(action, SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, project)
                .add(TestinDataKeys.SELECTED_NODES, selected)
                .build());
    }

    public static @NotNull Presentation updated(final @NotNull Project project, final @NotNull AnAction action, final @NotNull List<AbstractNode> selected) {
        final @NotNull AnActionEvent e = on(project, action, selected);
        ActionUtil.updateAction(action, e);
        return e.getPresentation();
    }

    public static void pressed(final @NotNull Project project, final @NotNull AnAction action, final @NotNull List<AbstractNode> selected) {
        final @NotNull AnActionEvent e = on(project, action, selected);
        ActionUtil.updateAction(action, e);
        if (!e.getPresentation().isEnabled())
            throw new AssertionError(action.getClass().getSimpleName() + " is gray on " + selected + ": " + Objects.requireNonNullElse(e.getPresentation().getDescription(), ""));

        ActionUtil.performAction(action, e);
    }
}
