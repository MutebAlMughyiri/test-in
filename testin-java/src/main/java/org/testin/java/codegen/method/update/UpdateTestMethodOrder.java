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

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.DocumentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.codegen.CodeChange;
import org.testin.codegen.CodeChangeType;
import org.testin.codegen.writer.CodeCommand;
import org.testin.indexer.TestCases;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.model.TestCase;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

// Rule-CODEGEN-014
public class UpdateTestMethodOrder extends BaseTestMethodChange implements CodeChange<TestCase> {
    private static final @NotNull String PRIORITY = "priority";

    // UC-CODEGEN-011, Rule-CODEGEN-067
    private static @NotNull PsiElement place(final @NotNull PsiClass testClass, final @NotNull PsiMethod testMethod, final @NotNull Optional<PsiElement> after) {
        return after.map(previous -> placeAfter(testClass, testMethod, previous)).orElseGet(() -> placeFirst(testClass, testMethod));
    }

    // UC-CODEGEN-011, Rule-CODEGEN-067
    private static @NotNull PsiElement placeFirst(final @NotNull PsiClass testClass, final @NotNull PsiMethod testMethod) {
        final @NotNull Optional<PsiMethod> first = firstGenerated(testClass).filter(found -> !found.equals(testMethod));
        if (first.isEmpty()) return testMethod;

        final @NotNull PsiElement moved = testClass.addBefore(testMethod, first.get());
        testMethod.delete();
        return moved;
    }

    // UC-CODEGEN-011, Rule-CODEGEN-067
    private static @NotNull PsiElement placeAfter(final @NotNull PsiClass testClass, final @NotNull PsiMethod testMethod, final @NotNull PsiElement after) {
        if (nextAfter(after).filter(next -> next.equals(testMethod)).isPresent()) return testMethod;

        final @NotNull PsiElement moved = testClass.addAfter(testMethod, after);
        testMethod.delete();
        return moved;
    }

    private static @NotNull Optional<PsiMethod> firstGenerated(final @NotNull PsiClass testClass) {
        return Arrays.stream(testClass.getMethods()).filter(method -> GeneratedMethod.testCaseUuidOf(method).isPresent()).findFirst();
    }

    private static @NotNull Optional<PsiElement> nextAfter(final @NotNull PsiElement element) {
        @Nullable PsiElement next = element.getNextSibling();
        while (next instanceof PsiWhiteSpace) next = next.getNextSibling();

        return Optional.ofNullable(next);
    }

    @Override
    public void execute(final @NotNull Project project, final @NotNull TestCase testCase) {
        executeAll(project, List.of(testCase));
    }

    // UC-CODEGEN-011, Rule-CODEGEN-042, Rule-CODEGEN-067
    @Override
    public void executeAll(final @NotNull Project project, final @NotNull List<? extends TestCase> testCases) {
        arrangeSets(project, testCases.stream().map(testCase -> testCase.getParent().getPath()).toList());
    }

    // UC-CODEGEN-011, Rule-CODEGEN-014, Rule-CODEGEN-042, Rule-CODEGEN-067
    public void arrangeSets(final @NotNull Project project, final @NotNull List<Path> testSetPaths) {
        final @NotNull List<List<TestCase>> ordered = testSetPaths.stream().distinct().map(Services.getInstance(project, TestCases.class)::getTestCasesForTestSet).toList();
        if (ordered.isEmpty()) return;

        CodeCommand.inOne(project, CodeChangeType.UPDATE_TEST_CASE_ORDER.description(), () -> ordered.forEach(inSet -> arrange(project, inSet)));
    }

    // UC-CODEGEN-011, Rule-CODEGEN-043, Rule-CODEGEN-067, Rule-CODEGEN-044
    private void arrange(final @NotNull Project project, final @NotNull List<TestCase> inSet) {
        if (inSet.isEmpty()) return;

        final @NotNull Optional<PsiClass> target = classOf(project, inSet.getFirst());
        if (target.isEmpty()) return;

        final @NotNull PsiClass testClass = target.get();

        final @NotNull Function<TestCase, Optional<PsiMethod>> methodOf = GeneratedMethod.ofEach(testClass);

        @NotNull Optional<PsiElement> after = Optional.empty();

        for (final TestCase testCase : inSet) {
            final @NotNull Optional<PsiMethod> testMethod = methodOf.apply(testCase);
            if (testMethod.isEmpty()) continue;

            after = Optional.of(place(testClass, testMethod.orElseThrow(), after));
        }

        number(project, testClass, inSet);
    }

    // UC-CODEGEN-011, Rule-CODEGEN-014
    private void number(final @NotNull Project project, final @NotNull PsiClass testClass, final @NotNull List<TestCase> inSet) {
        final @NotNull Function<TestCase, Optional<PsiMethod>> methodOf = GeneratedMethod.ofEach(testClass);
        final @NotNull Map<PsiMethod, String> renumbered = new LinkedHashMap<>();
        int position = 0;

        for (final TestCase testCase : inSet) {
            final @NotNull String number = String.valueOf(++position);

            methodOf.apply(testCase).ifPresent(testMethod -> {
                final @NotNull Optional<PsiAnnotationMemberValue> priority = priorityOf(testMethod);

                if (priority.isEmpty()) {
                    if (updateTestAnnotationAttribute(project, testMethod, PRIORITY, number))
                        formatAnnotations(project, testMethod);
                } else if (!priority.orElseThrow().getText().equals(number)) renumbered.put(testMethod, number);
            });
        }

        writeAsText(project, testClass.getContainingFile(), renumbered);
    }

    // UC-CODEGEN-011, Rule-CODEGEN-014
    private void writeAsText(final @NotNull Project project, final @NotNull PsiFile file, final @NotNull Map<PsiMethod, String> renumbered) {
        if (renumbered.isEmpty()) return;

        final @NotNull PsiDocumentManager documents = PsiDocumentManager.getInstance(project);
        final @NotNull Optional<Document> document = Optional.ofNullable(documents.getDocument(file));

        if (document.isEmpty()) {
            renumbered.forEach((testMethod, number) -> updateTestAnnotationAttribute(project, testMethod, PRIORITY, number));
            return;
        }

        documents.doPostponedOperationsAndUnblockDocument(document.orElseThrow());

        final @NotNull List<Map.Entry<TextRange, String>> edits = renumbered.entrySet().stream()
                .flatMap(entry -> priorityOf(entry.getKey()).map(priority -> Map.entry(priority.getTextRange(), entry.getValue())).stream())
                .sorted(Comparator.comparingInt((Map.Entry<TextRange, String> edit) -> edit.getKey().getStartOffset()).reversed())
                .toList();

        DocumentUtil.executeInBulk(document.orElseThrow(), () -> edits.forEach(edit -> document.orElseThrow().replaceString(edit.getKey().getStartOffset(), edit.getKey().getEndOffset(), edit.getValue())));
        documents.commitDocument(document.orElseThrow());
    }

    private @NotNull Optional<PsiAnnotationMemberValue> priorityOf(final @NotNull PsiMethod testMethod) {
        return getTestAnnotation(testMethod).flatMap(annotation -> Optional.ofNullable(annotation.findDeclaredAttributeValue(PRIORITY)));
    }
}
