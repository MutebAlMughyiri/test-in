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

package org.testin.java.navigate;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.testin.config.AutomationLanguage;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.navigate.TestMethodCode;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class JavaTestMethodCode implements TestMethodCode {
    @Override
    public @NotNull AutomationLanguage language() {
        return AutomationLanguage.JAVA;
    }

    // UC-CODEGEN-006, UC-CODEGEN-021, Rule-CODEGEN-001, Rule-CODEGEN-003
    @Override
    public @NotNull Map<UUID, Boolean> methodsFor(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        final @NotNull Map<UUID, Boolean> found = new LinkedHashMap<>();
        TestMethods.ofEach(project, testCases).forEach((testCaseUuid, testMethod) -> found.put(testCaseUuid, GeneratedMethod.holdsAWrittenBody(testMethod)));

        return found;
    }

    // UC-CODEGEN-006, Rule-CODEGEN-001
    @Override
    public void toCode(final @NotNull Project project, final @NotNull TestCase testCase) {
        TestinLogger.trace("navigate to the method of '" + testCase.getDescription() + "'");

        if (DumbService.isDumb(project)) {
            TestinLogger.trace("dumb mode detected, deferring navigation");
            DumbService.getInstance(project).runWhenSmart(() -> toCode(project, testCase));
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {
                    try {
                        final @NotNull Optional<PsiMethod> found = TestMethods.of(project, testCase);

                        if (found.isEmpty()) {
                            ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(project, Notifier.class)
                                    .softRefuse(project, Refused.NO_GENERATED_CODE.getSentence()));
                            return;
                        }

                        final @NotNull Navigatable target = found.orElseThrow();
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (target.canNavigate()) target.navigate(true);
                        });

                    } catch (final IndexNotReadyException ex) {
                        TestinLogger.trace("index not ready, deferring navigation");
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(project, Notifier.class).softShow(project, Bundle.message("navigate.waiting.for.indexing")));
                        DumbService.getInstance(project).runWhenSmart(() -> toCode(project, testCase));
                    }
                })
        );
    }
}
