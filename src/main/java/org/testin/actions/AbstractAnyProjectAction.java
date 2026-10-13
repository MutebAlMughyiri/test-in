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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class AbstractAnyProjectAction extends DumbAwareAction {
    protected AbstractAnyProjectAction() {
        super();
    }

    protected AbstractAnyProjectAction(final @NotNull @Nls String text) {
        super(text);
    }

    @Override
    public final void actionPerformed(final @NotNull AnActionEvent e) {
        Optional.ofNullable(e.getProject()).ifPresent(project -> perform(e, project));
    }

    protected abstract void perform(final @NotNull AnActionEvent e, final @NotNull Project project);

    @Override
    public final void update(final @NotNull AnActionEvent e) {
        Optional.ofNullable(e.getProject()).ifPresentOrElse(project -> update(e, project), () -> e.getPresentation().setEnabled(false));
    }

    protected void update(final @NotNull AnActionEvent e, final @NotNull Project project) {
    }
}
