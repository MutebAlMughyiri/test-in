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
import org.testin.codegen.GenAction;
import org.testin.codegen.GenType;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.model.TestCaseDto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Rule-CODEGEN-044
public class ReconcileTestMethod extends UpdateTestBase implements GenAction<TestCaseDto> {
    @Override
    public void execute(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        executeAll(p, List.of(tc));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-068
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<? extends TestCaseDto> testCases) {
        if (testCases.isEmpty()) return;

        final @NotNull Map<Path, List<TestCaseDto>> byClass = new LinkedHashMap<>();
        for (final TestCaseDto tc : testCases) {
            byClass.computeIfAbsent(tc.getParent().getPath(), _ -> new ArrayList<>()).add(tc);
        }

        final @NotNull Runnable inCommand = () ->
                WriteCommandAction.runWriteCommandAction(p, GenType.RECONCILE_TEST_CASE.description(), null, () -> {
                    byClass.values().forEach(inClass -> rewrite(p, inClass));
                    new UpdateTestMethodOrder().executeAll(p, testCases);
                });

        if (CommandProcessor.getInstance().getCurrentCommand() != null) inCommand.run();
        else ApplicationManager.getApplication().invokeLater(inCommand);
    }

    private void rewrite(final @NotNull Project p, final @NotNull List<TestCaseDto> inClass) {
        final @NotNull Optional<PsiClass> target = classOf(p, inClass.getFirst());
        if (target.isEmpty()) return;

        final @NotNull PsiClass pc = target.orElseThrow();
        final @NotNull Map<String, PsiMethod> methods = GeneratedMethod.byTestCaseId(pc);

        int written = 0;
        for (final TestCaseDto tc : inClass) {
            final @Nullable PsiMethod pm = methods.get(tc.getId().toString());
            if (pm == null) continue;

            writeDescription(p, pm, tc);
            writeGroups(p, pm, tc);
            writeEnabled(p, pm, tc);
            written++;
        }

        if (written > 0) reformat(p, pc);
    }
}
