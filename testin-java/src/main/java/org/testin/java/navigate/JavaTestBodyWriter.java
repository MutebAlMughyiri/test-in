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

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiMethod;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.agent.TestBodyWriter;
import org.testin.config.AutomationLanguage;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.util.Optional;

public final class JavaTestBodyWriter implements TestBodyWriter {
    private static @NotNull Optional<String> write(final @NotNull Project project, final @NotNull TestCase testCase, final @NotNull String statements, final boolean onlyTheTodo) {
        if (DumbService.isDumb(project)) return Optional.empty();

        return WriteCommandAction.writeCommandAction(project)
                .withName(Bundle.message("agent.body.command"))
                .compute(() -> written(project, testCase, statements, onlyTheTodo));
    }

    // Rule-CODEGEN-003, Rule-CODEGEN-089
    private static @NotNull Optional<String> written(final @NotNull Project project, final @NotNull TestCase testCase, final @NotNull String statements, final boolean onlyTheTodo) {
        final @NotNull Optional<PsiMethod> method = TestMethods.of(project, testCase);
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

    @Override
    public @NotNull AutomationLanguage language() {
        return AutomationLanguage.JAVA;
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
}
