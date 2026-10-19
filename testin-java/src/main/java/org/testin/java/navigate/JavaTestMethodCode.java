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
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiMethod;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.java.codegen.GeneratedClass;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.navigate.TestMethodCode;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public final class JavaTestMethodCode implements TestMethodCode {
    // UC-CODEGEN-006, Rule-CODEGEN-001
    private @NotNull Optional<PsiMethod> resolve(final @NotNull Project project, final @NotNull TestCase testCase) {
        final @NotNull String classFqcn = Fqcn.classOfMethod(testCase);
        if (classFqcn.isEmpty()) return Optional.empty();

        final @NotNull Optional<PsiClass> owner = GeneratedClass.byName(project, classFqcn);

        if (owner.isEmpty()) {
            TestinLogger.warn("No generated class " + classFqcn + " for '" + testCase.getDescription() + "'");
            return Optional.empty();
        }

        final @NotNull Optional<PsiMethod> method = GeneratedMethod.forTestCase(owner.orElseThrow(), testCase);
        if (method.isEmpty())
            TestinLogger.warn("No generated method for '" + testCase.getDescription() + "' in " + classFqcn);

        return method;
    }

    // UC-CODEGEN-006, UC-CODEGEN-021, Rule-CODEGEN-001, Rule-CODEGEN-003
    @Override
    public @NotNull Map<UUID, Boolean> methodsFor(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        final @NotNull Map<UUID, Boolean> found = new LinkedHashMap<>();
        generatedMethodsOf(project, testCases).forEach((testCaseUuid, testMethod) -> found.put(testCaseUuid, GeneratedMethod.holdsAWrittenBody(testMethod)));

        return found;
    }

    // UC-CODEGEN-006, Rule-CODEGEN-001
    private @NotNull Map<UUID, PsiMethod> generatedMethodsOf(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        final @NotNull Map<UUID, PsiMethod> found = new LinkedHashMap<>();

        for (final Map.Entry<String, List<TestCase>> group : Fqcn.byClassOfMethod(testCases).entrySet()) {
            final @NotNull Optional<PsiClass> owner = GeneratedClass.byName(project, group.getKey());

            if (owner.isEmpty()) continue;

            final @NotNull Function<TestCase, Optional<PsiMethod>> methodOf = GeneratedMethod.ofEach(owner.orElseThrow());

            for (final TestCase testCase : group.getValue()) {
                methodOf.apply(testCase).ifPresent(testMethod -> found.put(testCase.getUuid(), testMethod));
            }
        }

        return found;
    }

    // UC-CODEGEN-021, Rule-CODEGEN-003, Rule-CODEGEN-089
    @Override
    public @NotNull Optional<String> fillBody(final @NotNull Project project, final @NotNull TestCase testCase, final @NotNull String statements) {
        return write(project, testCase, statements, true);
    }

    // UC-CODEGEN-021, Rule-CODEGEN-003, Rule-CODEGEN-091
    @Override
    public @NotNull Optional<String> replaceBody(final @NotNull Project project, final @NotNull TestCase testCase, final @NotNull String statements) {
        return write(project, testCase, statements, false);
    }

    private @NotNull Optional<String> write(final @NotNull Project project, final @NotNull TestCase testCase, final @NotNull String statements, final boolean onlyTheTodo) {
        if (DumbService.isDumb(project)) return Optional.empty();

        return WriteCommandAction.writeCommandAction(project)
                .withName(Bundle.message("agent.body.command"))
                .compute(() -> written(project, testCase, statements, onlyTheTodo));
    }

    // Rule-CODEGEN-003, Rule-CODEGEN-089
    private @NotNull Optional<String> written(final @NotNull Project project, final @NotNull TestCase testCase, final @NotNull String statements, final boolean onlyTheTodo) {
        final @NotNull Optional<PsiMethod> method = resolve(project, testCase);
        if (method.isEmpty()) return Optional.empty();
        if (onlyTheTodo && GeneratedMethod.holdsAWrittenBody(method.orElseThrow())) return Optional.empty();

        try {
            final @NotNull PsiCodeBlock written = JavaPsiFacade.getElementFactory(project)
                    .createCodeBlockFromText("{\n" + statements + "\n}", method.orElseThrow());

            Optional.ofNullable(method.orElseThrow().getBody()).ifPresent(body -> body.replace(written));
            return Optional.of(statements);
        } catch (final IncorrectOperationException notJava) {
            TestinLogger.warn("The agent's answer for '" + testCase.getDescription() + "' is not Java and was dropped: " + FailureText.of(notJava));
            return Optional.empty();
        }
    }

    // UC-CODEGEN-008, Rule-CODEGEN-001
    @Override
    public @NotNull Map<UUID, List<String>> methodFqcnsOf(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        final @NotNull Map<UUID, PsiMethod> methods = generatedMethodsOf(project, testCases);
        final @NotNull Map<UUID, List<String>> named = new LinkedHashMap<>();

        for (final TestCase testCase : testCases) {
            Optional.ofNullable(methods.get(testCase.getUuid())).ifPresent(testMethod -> {
                final @NotNull List<String> fqcn = new ArrayList<>(Fqcn.ofMethod(testCase));
                fqcn.set(fqcn.size() - 1, testMethod.getName());

                named.put(testCase.getUuid(), fqcn);
            });
        }

        return named;
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
                        final @NotNull Optional<PsiMethod> found = resolve(project, testCase);

                        if (found.isEmpty()) {
                            ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(project, Notifier.class)
                                    .softRefuse(project, Refused.NO_GENERATED_CODE, testCase.getDescription()));
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
