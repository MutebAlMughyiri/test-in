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

package org.testin.java.codegen.method.update;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.codegen.CodeChange;
import org.testin.codegen.CodeChangeType;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.model.TestCase;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Rule-CODEGEN-044
public class ReconcileTestMethod extends BaseTestMethodChange implements CodeChange<TestCase> {
    @Override
    public void execute(final @NotNull Project project, final @NotNull TestCase testCase) {
        executeAll(project, List.of(testCase));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-068
    @Override
    public void executeAll(final @NotNull Project project, final @NotNull List<? extends TestCase> testCases) {
        if (testCases.isEmpty()) return;

        final @NotNull Map<Path, List<TestCase>> byClass = new LinkedHashMap<>();
        for (final TestCase testCase : testCases) {
            byClass.computeIfAbsent(testCase.getParent().getPath(), _ -> new ArrayList<>()).add(testCase);
        }

        final @NotNull Runnable inCommand = () ->
                WriteCommandAction.runWriteCommandAction(project, CodeChangeType.RECONCILE_TEST_CASE.description(), null, () -> {
                    byClass.values().forEach(inClass -> rewrite(project, inClass));
                    new UpdateTestMethodOrder().executeAll(project, testCases);
                });

        if (CommandProcessor.getInstance().getCurrentCommand() != null) inCommand.run();
        else ApplicationManager.getApplication().invokeLater(inCommand);
    }

    private void rewrite(final @NotNull Project project, final @NotNull List<TestCase> inClass) {
        final @NotNull Optional<PsiClass> target = classOf(project, inClass.getFirst());
        if (target.isEmpty()) return;

        final @NotNull PsiClass testClass = target.orElseThrow();
        final @NotNull Map<String, PsiMethod> methods = GeneratedMethod.byTestCaseUuid(testClass);

        for (final TestCase testCase : inClass) {
            final @Nullable PsiMethod testMethod = methods.get(testCase.getUuid().toString());
            if (testMethod == null) continue;

            writeDescription(project, testMethod, testCase);
            writeGroups(project, testMethod, testCase);
            writeEnabled(project, testMethod, testCase);
            formatAnnotations(project, testMethod);
        }
    }
}
