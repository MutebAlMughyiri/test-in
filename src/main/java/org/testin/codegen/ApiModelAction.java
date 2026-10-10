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

package org.testin.codegen;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.GrayWithReason;
import org.testin.util.Bundle;

import java.util.Arrays;
import java.util.Optional;

public final class ApiModelAction extends AbstractAnyProjectAction {
    // UC-CODEGEN-022, Rule-CODEGEN-098
    private static @NotNull Optional<PsiDirectory> packageOf(final @NotNull AnActionEvent e, final @NotNull ApiModelMaker maker) {
        return Optional.ofNullable(e.getData(LangDataKeys.IDE_VIEW))
                .map(IdeView::getDirectories)
                .filter(directories -> directories.length == 1)
                .flatMap(directories -> Arrays.stream(directories).findFirst())
                .filter(maker::canMakeIn);
    }

    // UC-CODEGEN-022, Rule-CODEGEN-082, Rule-CODEGEN-098
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project p) {
        if (CodeOn.grayedWithReason(this, e, p)) return;

        GrayWithReason.unless(this, e, ApiModelMaker.available().flatMap(maker -> packageOf(e, maker)).isPresent(), Bundle.message("api.model.not.a.package"));
    }

    // UC-CODEGEN-022
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project p) {
        ApiModelMaker.available().ifPresent(maker -> packageOf(e, maker).ifPresent(directory -> maker.open(p, directory)));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
