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
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.codegen.Fqcn;
import org.testin.codegen.CodeChangeType;
import org.testin.java.codegen.GeneratedClass;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.java.codegen.JavaLiteral;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.NameSanitizer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BaseTestMethodChange {
    private static boolean anotherMethodIsCalled(final @NotNull PsiMethod testMethod, final @NotNull String methodName) {
        final @NotNull String key = NameSanitizer.methodKey(methodName);

        return Optional.ofNullable(testMethod.getContainingClass()).stream()
                .flatMap(testClass -> Arrays.stream(testClass.getMethods()))
                .anyMatch(other -> !other.equals(testMethod) && key.equals(NameSanitizer.methodKey(other.getName())));
    }

    private static void keptItsName(final @NotNull Project project, final @NotNull PsiMethod testMethod, final @NotNull String why) {
        TestinLogger.warn("Kept the name of " + testMethod.getName() + ": " + why);

        Services.getInstance(project, Notifier.class).softRefuse(project, Bundle.message("codegen.rename.kept.title"), why);
    }

    // UC-CODEGEN-002, Rule-CODEGEN-014, Rule-CODEGEN-021
    protected static @NotNull Optional<PsiClass> classOf(final @NotNull Project project, final @NotNull TestCase testCase) {
        return GeneratedClass.find(project, Fqcn.ofClass(testCase.getParent()));
    }

    protected @NotNull Optional<PsiMethod> findMethodByTestName(final @NotNull PsiClass testClass, final @NotNull TestCase testCase) {
        return GeneratedMethod.forTestCase(testClass, testCase);
    }

    protected @NotNull Optional<PsiAnnotation> getTestAnnotation(final @NotNull PsiMethod testMethod) {
        return GeneratedMethod.testAnnotationOf(testMethod);
    }

    protected boolean updateAnnotationAttribute(final @NotNull PsiElementFactory factory, final @NotNull PsiAnnotation annotation, final @NotNull String attributeName, final @NotNull String newValue) {
        final @NotNull PsiAnnotation parsed = factory.createAnnotationFromText("@A(v = " + newValue + ")", annotation);

        final PsiAnnotationMemberValue value = parsed.findDeclaredAttributeValue("v");
        if (value == null) {
            TestinLogger.warn("Could not read '" + newValue + "' as a value for " + attributeName);
            return false;
        }

        if (Optional.ofNullable(annotation.findDeclaredAttributeValue(attributeName)).map(PsiElement::getText).filter(value.getText()::equals).isPresent())
            return false;

        annotation.setDeclaredAttributeValue(attributeName, value);
        return true;
    }

    // UC-CODEGEN-002, Rule-CODEGEN-012, Rule-CODEGEN-040
    protected void writeDescription(final @NotNull Project project, final @NotNull PsiMethod testMethod, final @NotNull TestCase testCase) {
        updateTestAnnotationAttribute(project, testMethod, "description", JavaLiteral.of(testCase.getDescription()));

        final @NotNull String newMethodName = NameSanitizer.methodName(testCase.getDescription());
        if (newMethodName.isEmpty() || testMethod.getName().equals(newMethodName)) return;

        // Rule-CODEGEN-079
        if (NameSanitizer.cannotMakeMethodName(testCase.getDescription())) {
            keptItsName(project, testMethod, Bundle.message("codegen.rename.not.a.method", testMethod.getName(), newMethodName));
            return;
        }

        if (anotherMethodIsCalled(testMethod, newMethodName)) {
            keptItsName(project, testMethod, Bundle.message("codegen.rename.taken", testMethod.getName(), newMethodName));
            return;
        }

        testMethod.setName(newMethodName);
    }

    // UC-CODEGEN-002, UC-CODEGEN-012, Rule-CODEGEN-046
    protected void writeGroups(final @NotNull Project project, final @NotNull PsiMethod testMethod, final @NotNull TestCase testCase) {
        final @NotNull List<String> quoted = testCase.getGroups().stream().map(JavaLiteral::of).toList();

        if (quoted.isEmpty()) removeTestAnnotationAttribute(testMethod, "groups");
        else updateTestAnnotationAttribute(project, testMethod, "groups", "{" + String.join(", ", quoted) + "}");
    }

    // UC-CODEGEN-002, Rule-CODEGEN-047, Rule-CODEGEN-048
    protected void writeEnabled(final @NotNull Project project, final @NotNull PsiMethod testMethod, final @NotNull TestCase testCase) {
        if (testCase.getStatus().isEnabled()) removeTestAnnotationAttribute(testMethod, "enabled");
        else updateTestAnnotationAttribute(project, testMethod, "enabled", "false");
    }

    protected boolean updateTestAnnotationAttribute(final @NotNull Project project, final @NotNull PsiMethod testMethod, final @NotNull String attributeName, final @NotNull String newValue) {
        final @NotNull Optional<PsiAnnotation> testAnnotation = getTestAnnotation(testMethod);
        if (testAnnotation.isEmpty()) {
            TestinLogger.warn("Update: method has no @Test annotation");
            return false;
        }

        return updateAnnotationAttribute(JavaPsiFacade.getElementFactory(project), testAnnotation.orElseThrow(), attributeName, newValue);
    }

    // Rule-CODEGEN-003
    protected void formatAnnotations(final @NotNull Project project, final @NotNull PsiMethod testMethod) {
        CodeStyleManager.getInstance(project).reformat(testMethod.getModifierList());
    }

    protected void removeTestAnnotationAttribute(final @NotNull PsiMethod testMethod, final @NotNull String attributeName) {
        getTestAnnotation(testMethod).ifPresentOrElse(testAnnotation -> testAnnotation.setDeclaredAttributeValue(attributeName, null),
                () -> TestinLogger.warn("Update: method has no @Test annotation"));
    }

    private void noCodeToUpdate(final @NotNull Project project, final @NotNull TestCase testCase, final @NotNull String detail) {
        TestinLogger.warn("Update: " + detail);

        Services.getInstance(project, Notifier.class).softRefuse(project, Refused.NO_GENERATED_CODE, testCase.getDescription());
    }

    protected void applyUpdate(final @NotNull Project project, final @NotNull TestCase testCase, final @NotNull String title, final @NotNull Consumer<PsiMethod> updater) {
        applyToMethod(project, testCase, title, updater.andThen(testMethod -> formatAnnotations(project, testMethod)), detail -> noCodeToUpdate(project, testCase, detail));
    }

    // UC-CODEGEN-003, Rule-CODEGEN-019
    protected void applyOrCreate(final @NotNull Project project, final @NotNull TestCase testCase, final @NotNull String title, final @NotNull Consumer<PsiMethod> updater) {
        applyToMethod(project, testCase, title, updater.andThen(testMethod -> formatAnnotations(project, testMethod)), detail -> {
            TestinLogger.info("Writing the method for '" + testCase.getDescription() + "' now that it has a name: " + detail);
            CodeChangeType.CREATE_TEST_CASE.execute(project, testCase);
        });
    }

    // UC-CODEGEN-012, Rule-CODEGEN-045
    protected void applyToEach(final @NotNull Project project, final @NotNull List<? extends TestCase> changedTestCases, final @NotNull String title, final @NotNull BiConsumer<PsiMethod, TestCase> updater) {
        final @NotNull Map<String, List<TestCase>> byClass = Fqcn.byClassOfMethod(changedTestCases);
        if (byClass.isEmpty()) return;

        asOneCommand(project, title, () -> byClass.forEach((path, testCases) -> writeAll(project, path, testCases, updater)));
    }

    private void writeAll(final @NotNull Project project, final @NotNull String path, final @NotNull List<TestCase> testCases, final @NotNull BiConsumer<PsiMethod, TestCase> updater) {
        final @NotNull Optional<PsiClass> target = GeneratedClass.byName(project, path);

        if (target.isEmpty()) {
            TestinLogger.warn("Update: class not found: " + path);
            return;
        }

        final @NotNull PsiClass testClass = target.orElseThrow();

        final @NotNull Map<String, PsiMethod> methods = GeneratedMethod.byTestCaseUuid(testClass);

        for (final TestCase testCase : testCases) {
            final @Nullable PsiMethod testMethod = methods.get(testCase.getUuid().toString());
            if (testMethod == null) continue;

            updater.accept(testMethod, testCase);
            formatAnnotations(project, testMethod);
        }
    }

    // Rule-CODEGEN-021
    protected void applyToMethod(final @NotNull Project project, final @NotNull TestCase testCase, final @NotNull String title, final @NotNull Consumer<PsiMethod> updater, final @NotNull Consumer<String> onMissing) {
        final @NotNull List<String> classSegments = Fqcn.ofClass(testCase.getParent());
        if (classSegments.isEmpty()) return;

        asOneCommand(project, title, () -> GeneratedClass.find(project, classSegments)
                .ifPresentOrElse(
                        targetClass -> findMethodByTestName(targetClass, testCase).ifPresentOrElse(updater,
                                () -> onMissing.accept("no method with testName=" + testCase.getUuid())),
                        () -> onMissing.accept("class not found: " + String.join(".", classSegments))));
    }

    protected static void asOneCommand(final @NotNull Project project, final @NotNull String title, final @NotNull Runnable change) {
        final @NotNull Runnable inCommand = () -> WriteCommandAction.runWriteCommandAction(project, title, null, change);

        if (CommandProcessor.getInstance().getCurrentCommand() != null) inCommand.run();
        else ApplicationManager.getApplication().invokeLater(inCommand);
    }
}
