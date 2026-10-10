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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.codegen.ExecutionPosition;
import org.testin.codegen.GenAction;
import org.testin.codegen.GenType;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.model.TestCaseDto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Rule-CODEGEN-014
public class UpdateTestOrder extends UpdateTestBase implements GenAction<TestCaseDto> {
    // UC-CODEGEN-011, Rule-CODEGEN-067
    private static @NotNull PsiElement place(final @NotNull PsiClass pc, final @NotNull PsiMethod pm, final @NotNull Optional<PsiElement> after) {
        return after.map(previous -> placeAfter(pc, pm, previous)).orElseGet(() -> placeFirst(pc, pm));
    }

    // UC-CODEGEN-011, Rule-CODEGEN-067
    private static @NotNull PsiElement placeFirst(final @NotNull PsiClass pc, final @NotNull PsiMethod pm) {
        final @NotNull Optional<PsiMethod> first = firstGenerated(pc).filter(found -> !found.equals(pm));
        if (first.isEmpty()) return pm;

        final @NotNull PsiElement moved = pc.addBefore(pm, first.get());
        pm.delete();
        return moved;
    }

    // UC-CODEGEN-011, Rule-CODEGEN-067
    private static @NotNull PsiElement placeAfter(final @NotNull PsiClass pc, final @NotNull PsiMethod pm, final @NotNull PsiElement after) {
        if (nextAfter(after).filter(next -> next.equals(pm)).isPresent()) return pm;

        final @NotNull PsiElement moved = pc.addAfter(pm, after);
        pm.delete();
        return moved;
    }

    private static @NotNull Optional<PsiMethod> firstGenerated(final @NotNull PsiClass pc) {
        return Arrays.stream(pc.getMethods()).filter(method -> GeneratedMethod.testCaseIdOf(method).isPresent()).findFirst();
    }

    private static @NotNull Optional<PsiElement> nextAfter(final @NotNull PsiElement element) {
        @Nullable PsiElement next = element.getNextSibling();
        while (next instanceof PsiWhiteSpace) next = next.getNextSibling();

        return Optional.ofNullable(next);
    }

    @Override
    public void execute(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        executeAll(p, List.of(tc));
    }

    // UC-CODEGEN-011, Rule-CODEGEN-042, Rule-CODEGEN-067
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<? extends TestCaseDto> items) {
        final @NotNull Map<Path, List<TestCaseDto>> sets = new LinkedHashMap<>();

        for (final TestCaseDto tc : items) {
            sets.computeIfAbsent(tc.getParent().getPath(), _ -> ExecutionPosition.setOf(p, tc));
        }
        if (sets.isEmpty()) return;

        final @NotNull List<List<TestCaseDto>> ordered = new ArrayList<>(sets.values());

        final @NotNull Runnable inCommand = () ->
                WriteCommandAction.runWriteCommandAction(p, GenType.UPDATE_TEST_CASE_ORDER.description(), null,
                        () -> ordered.forEach(inSet -> arrange(p, inSet)));

        if (CommandProcessor.getInstance().getCurrentCommand() != null) inCommand.run();
        else ApplicationManager.getApplication().invokeLater(inCommand);
    }

    // UC-CODEGEN-011, Rule-CODEGEN-043, Rule-CODEGEN-067, Rule-CODEGEN-044
    private void arrange(final @NotNull Project p, final @NotNull List<TestCaseDto> inSet) {
        if (inSet.isEmpty()) return;

        final @NotNull Optional<PsiClass> target = classOf(p, inSet.getFirst());
        if (target.isEmpty()) return;

        final @NotNull PsiClass pc = target.get();

        final @NotNull Map<String, PsiMethod> methods = GeneratedMethod.byTestCaseId(pc);

        @NotNull Optional<PsiElement> after = Optional.empty();
        int position = 0;
        int written = 0;

        for (final TestCaseDto tc : inSet) {
            position++;

            final @Nullable PsiMethod pm = methods.get(tc.getId().toString());
            if (pm == null) continue;

            updateTestAnnotationAttribute(p, pm, "priority", String.valueOf(position));
            after = Optional.of(place(pc, pm, after));
            written++;
        }

        if (written > 0) reformat(p, pc);
    }
}
