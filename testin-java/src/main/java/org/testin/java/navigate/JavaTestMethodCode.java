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
import org.testin.model.TestCaseDto;
import org.testin.navigate.TestMethodCode;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class JavaTestMethodCode implements TestMethodCode {
    private static boolean doesSomething(final @NotNull PsiMethod pm) {
        return Optional.ofNullable(pm.getBody())
                .filter(body -> body.getStatements().length > 0)
                .isPresent();
    }

    // UC-CODEGEN-006, Rule-CODEGEN-001
    private @NotNull Optional<PsiMethod> resolve(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        final @NotNull String classFqcn = Fqcn.classOfMethod(tc);
        if (classFqcn.isEmpty()) return Optional.empty();

        final @NotNull Optional<PsiClass> owner = GeneratedClass.byName(p, classFqcn);

        if (owner.isEmpty()) {
            TestinLogger.warn("No generated class " + classFqcn + " for '" + tc.getDescription() + "'");
            return Optional.empty();
        }

        final @NotNull Optional<PsiMethod> method = GeneratedMethod.forTestCase(owner.orElseThrow(), tc);
        if (method.isEmpty())
            TestinLogger.warn("No generated method for '" + tc.getDescription() + "' in " + classFqcn);

        return method;
    }

    // UC-CODEGEN-006, Rule-CODEGEN-001
    @Override
    public @NotNull Map<UUID, Boolean> methodsFor(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        final @NotNull Map<UUID, Boolean> found = new LinkedHashMap<>();
        generatedMethodsOf(p, testCases).forEach((id, pm) -> found.put(id, doesSomething(pm)));

        return found;
    }

    // UC-CODEGEN-006, Rule-CODEGEN-001
    private @NotNull Map<UUID, PsiMethod> generatedMethodsOf(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        final @NotNull Map<UUID, PsiMethod> found = new LinkedHashMap<>();

        for (final Map.Entry<String, List<TestCaseDto>> group : Fqcn.byClassOfMethod(testCases).entrySet()) {
            final @NotNull Optional<PsiClass> owner = GeneratedClass.byName(p, group.getKey());

            if (owner.isEmpty()) continue;

            final @NotNull Map<String, PsiMethod> methods = GeneratedMethod.byTestCaseId(owner.orElseThrow());

            for (final TestCaseDto tc : group.getValue()) {
                Optional.ofNullable(methods.get(tc.getId().toString()))
                        .ifPresent(pm -> found.put(tc.getId(), pm));
            }
        }

        return found;
    }

    // UC-CODEGEN-021, Rule-CODEGEN-003
    @Override
    public @NotNull Set<UUID> withAWrittenBody(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        if (DumbService.isDumb(p)) return Set.of();

        final @NotNull Set<UUID> written = new HashSet<>();
        generatedMethodsOf(p, testCases).forEach((id, pm) -> {
            if (GeneratedMethod.holdsAWrittenBody(pm)) written.add(id);
        });

        return written;
    }

    // UC-CODEGEN-021, Rule-CODEGEN-003, Rule-CODEGEN-089
    @Override
    public @NotNull Optional<String> fillBody(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String statements) {
        return write(p, tc, statements, true);
    }

    // UC-CODEGEN-021, Rule-CODEGEN-003, Rule-CODEGEN-091
    @Override
    public @NotNull Optional<String> replaceBody(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String statements) {
        return write(p, tc, statements, false);
    }

    private @NotNull Optional<String> write(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String statements, final boolean onlyTheTodo) {
        if (DumbService.isDumb(p)) return Optional.empty();

        return WriteCommandAction.writeCommandAction(p)
                .withName(Bundle.message("agent.body.command"))
                .compute(() -> written(p, tc, statements, onlyTheTodo));
    }

    // Rule-CODEGEN-003, Rule-CODEGEN-089
    private @NotNull Optional<String> written(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String statements, final boolean onlyTheTodo) {
        final @NotNull Optional<PsiMethod> method = resolve(p, tc);
        if (method.isEmpty()) return Optional.empty();
        if (onlyTheTodo && GeneratedMethod.holdsAWrittenBody(method.orElseThrow())) return Optional.empty();

        try {
            final @NotNull PsiCodeBlock written = JavaPsiFacade.getElementFactory(p)
                    .createCodeBlockFromText("{\n" + statements + "\n}", method.orElseThrow());

            Optional.ofNullable(method.orElseThrow().getBody()).ifPresent(body -> body.replace(written));
            return Optional.of(statements);
        } catch (final IncorrectOperationException notJava) {
            TestinLogger.warn("The agent's answer for '" + tc.getDescription() + "' is not Java and was dropped: " + FailureText.of(notJava));
            return Optional.empty();
        }
    }

    // UC-CODEGEN-008, Rule-CODEGEN-001
    @Override
    public @NotNull Map<UUID, List<String>> methodFqcnsOf(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        final @NotNull Map<UUID, PsiMethod> methods = generatedMethodsOf(p, testCases);
        final @NotNull Map<UUID, List<String>> named = new LinkedHashMap<>();

        for (final TestCaseDto tc : testCases) {
            Optional.ofNullable(methods.get(tc.getId())).ifPresent(pm -> {
                final @NotNull List<String> fqcn = new ArrayList<>(Fqcn.ofMethod(tc));
                fqcn.set(fqcn.size() - 1, pm.getName());

                named.put(tc.getId(), fqcn);
            });
        }

        return named;
    }

    // UC-CODEGEN-006, Rule-CODEGEN-001
    @Override
    public void toCode(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        TestinLogger.trace("navigate to the method of '" + tc.getDescription() + "'");

        if (DumbService.isDumb(p)) {
            TestinLogger.trace("dumb mode detected, deferring navigation");
            DumbService.getInstance(p).runWhenSmart(() -> toCode(p, tc));
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {
                    try {
                        final @NotNull Optional<PsiMethod> found = resolve(p, tc);

                        if (found.isEmpty()) {
                            ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(p, Notifier.class)
                                    .softRefuse(p, Refused.NO_GENERATED_CODE, tc.getDescription()));
                            return;
                        }

                        final @NotNull Navigatable target = found.orElseThrow();
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (target.canNavigate()) target.navigate(true);
                        });

                    } catch (final IndexNotReadyException ex) {
                        TestinLogger.trace("index not ready, deferring navigation");
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).softShow(p, Bundle.message("navigate.waiting.for.indexing")));
                        DumbService.getInstance(p).runWhenSmart(() -> toCode(p, tc));
                    }
                })
        );
    }
}
