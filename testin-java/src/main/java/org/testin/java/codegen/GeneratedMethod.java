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

package org.testin.java.codegen;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GeneratedMethod {
    public static final @NotNull String TESTNG_TEST = "org.testng.annotations.Test";

    private static final @NotNull String TEST_NAME = "testName";

    private static final @NotNull String DESCRIPTION = "description";

    private static final @NotNull String TODO = "// TODO: Auto-generated test steps for ";

    // UC-CODEGEN-002, Rule-CODEGEN-003
    public static @NotNull String emptyBody(final @NotNull String methodName) {
        return "{\n    " + TODO + methodName + "\n}";
    }

    // UC-CODEGEN-021, Rule-CODEGEN-003, Rule-EDITOR-PANEL-195
    public static boolean holdsAWrittenBody(final @NotNull PsiMethod testMethod) {
        return Optional.ofNullable(testMethod.getBody()).map(body -> !isOnlyTheTodo(body.getText())).orElse(true);
    }

    private static boolean isOnlyTheTodo(final @NotNull String body) {
        final @NotNull String inside = body.substring(1, body.length() - 1).strip();
        return inside.startsWith(TODO) && inside.lines().count() == 1;
    }

    // Rule-CODEGEN-001
    public static @NotNull Optional<PsiMethod> forTestCase(final @NotNull PsiClass testClass, final @NotNull TestCase testCase) {
        return ofEach(testClass).apply(testCase);
    }

    // Rule-CODEGEN-001
    public static @NotNull Function<TestCase, Optional<PsiMethod>> ofEach(final @NotNull PsiClass testClass) {
        final @NotNull Map<String, PsiMethod> byUuid = byTestCaseUuid(testClass);
        return testCase -> Optional.ofNullable(byUuid.get(testCase.getUuid().toString()));
    }

    // Rule-CODEGEN-001
    public static @NotNull Map<String, PsiMethod> byTestCaseUuid(final @NotNull PsiClass testClass) {
        final @NotNull Map<String, PsiMethod> byUuid = new LinkedHashMap<>();

        for (final PsiMethod testMethod : testClass.getMethods()) {
            testCaseUuidOf(testMethod).ifPresent(testCaseUuid -> byUuid.putIfAbsent(testCaseUuid, testMethod));
        }

        return byUuid;
    }

    // UC-CODEGEN-002, Rule-CODEGEN-013
    public static @NotNull Optional<String> testCaseUuidOf(final @NotNull PsiMethod testMethod) {
        return testAnnotationOf(testMethod)
                .map(annotation -> annotation.findDeclaredAttributeValue(TEST_NAME))
                .filter(PsiLiteralValue.class::isInstance)
                .map(value -> ((PsiLiteralValue) value).getValue())
                .filter(String.class::isInstance)
                .map(String.class::cast);
    }

    // UC-CODEGEN-002, Rule-CODEGEN-001
    public static void adopt(final @NotNull Project project, final @NotNull PsiMethod testMethod, final @NotNull TestCase testCase) {
        if (testCaseUuidOf(testMethod).isPresent()) return;

        testAnnotationOf(testMethod).ifPresent(annotation -> {
            final @NotNull PsiAnnotation written = JavaPsiFacade.getElementFactory(project)
                    .createAnnotationFromText("@Test(" + TEST_NAME + " = " + JavaLiteral.of(testCase.getUuid().toString()) + ")", testMethod);

            Optional.ofNullable(written.findDeclaredAttributeValue(TEST_NAME))
                    .ifPresent(value -> annotation.setDeclaredAttributeValue(TEST_NAME, value));
        });
    }

    // UC-CODEGEN-002, Rule-CODEGEN-095
    public static @NotNull Map<String, PsiMethod> untaggedByDescription(final @NotNull PsiClass testClass) {
        final @NotNull Map<String, PsiMethod> found = new LinkedHashMap<>();
        for (final PsiMethod testMethod : testClass.getMethods()) {
            if (testCaseUuidOf(testMethod).isPresent()) continue;

            testAnnotationOf(testMethod)
                    .map(annotation -> AnnotationUtil.getStringAttributeValue(annotation, DESCRIPTION))
                    .filter(description -> !description.isEmpty())
                    .ifPresent(description -> found.putIfAbsent(description, testMethod));
        }
        return found;
    }

    public static @NotNull Optional<PsiAnnotation> testAnnotationOf(final @NotNull PsiMethod testMethod) {
        return Optional.ofNullable(testMethod.getModifierList().findAnnotation(TESTNG_TEST));
    }

    // UC-CODEGEN-002
    public static void importTest(final @NotNull Project project, final @NotNull PsiFile file) {
        if (!(file instanceof PsiJavaFile javaFile)) return;

        Optional.ofNullable(javaFile.getImportList())
                .filter(imports -> imports.findSingleClassImportStatement(TESTNG_TEST) == null)
                .ifPresent(imports -> Optional.ofNullable(JavaPsiFacade.getInstance(project).findClass(TESTNG_TEST, GlobalSearchScope.allScope(project)))
                        .ifPresent(testClass -> imports.add(JavaPsiFacade.getElementFactory(project).createImportStatement(testClass))));
    }
}
