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
import org.testin.codegen.GenType;
import org.testin.java.codegen.GeneratedClass;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.java.codegen.JavaLiteral;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
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
    private static boolean anotherMethodIsCalled(final @NotNull PsiMethod pm, final @NotNull String methodName) {
        final @NotNull String key = NameSanitizer.methodKey(methodName);

        return Optional.ofNullable(pm.getContainingClass()).stream()
                .flatMap(pc -> Arrays.stream(pc.getMethods()))
                .anyMatch(other -> !other.equals(pm) && key.equals(NameSanitizer.methodKey(other.getName())));
    }

    private static void keptItsName(final @NotNull Project project, final @NotNull PsiMethod pm, final @NotNull String why) {
        TestinLogger.warn("Kept the name of " + pm.getName() + ": " + why);

        Services.getInstance(project, Notifier.class).softRefuse(project, Bundle.message("codegen.rename.kept.title"), why);
    }

    // UC-CODEGEN-002, Rule-CODEGEN-014, Rule-CODEGEN-021
    protected static @NotNull Optional<PsiClass> classOf(final @NotNull Project project, final @NotNull TestCaseDto testCase) {
        return GeneratedClass.find(project, Fqcn.ofClass(testCase.getParent()));
    }

    protected @NotNull Optional<PsiMethod> findMethodByTestName(final @NotNull PsiClass pc, final @NotNull TestCaseDto testCase) {
        return GeneratedMethod.forTestCase(pc, testCase);
    }

    protected @NotNull Optional<PsiAnnotation> getTestAnnotation(final @NotNull PsiMethod pm) {
        return GeneratedMethod.testAnnotationOf(pm);
    }

    protected void updateAnnotationAttribute(final @NotNull PsiElementFactory pf, final @NotNull PsiAnnotation pa, final @NotNull String attrName, final @NotNull String newValue) {
        final @NotNull PsiAnnotation parsed = pf.createAnnotationFromText("@A(v = " + newValue + ")", pa);

        final PsiAnnotationMemberValue value = parsed.findDeclaredAttributeValue("v");
        if (value == null) {
            TestinLogger.warn("Could not read '" + newValue + "' as a value for " + attrName);
            return;
        }

        pa.setDeclaredAttributeValue(attrName, value);
    }

    // UC-CODEGEN-002, Rule-CODEGEN-012, Rule-CODEGEN-040
    protected void writeDescription(final @NotNull Project project, final @NotNull PsiMethod pm, final @NotNull TestCaseDto testCase) {
        updateTestAnnotationAttribute(project, pm, "description", JavaLiteral.of(testCase.getDescription()));

        final @NotNull String newMethodName = NameSanitizer.methodName(testCase.getDescription());
        if (newMethodName.isEmpty() || pm.getName().equals(newMethodName)) return;

        // Rule-CODEGEN-079
        if (NameSanitizer.cannotMakeMethodName(testCase.getDescription())) {
            keptItsName(project, pm, Bundle.message("codegen.rename.not.a.method", pm.getName(), newMethodName));
            return;
        }

        if (anotherMethodIsCalled(pm, newMethodName)) {
            keptItsName(project, pm, Bundle.message("codegen.rename.taken", pm.getName(), newMethodName));
            return;
        }

        pm.setName(newMethodName);
    }

    // UC-CODEGEN-002, UC-CODEGEN-012, Rule-CODEGEN-046
    protected void writeGroups(final @NotNull Project project, final @NotNull PsiMethod pm, final @NotNull TestCaseDto testCase) {
        final @NotNull List<String> quoted = testCase.getGroups().stream().map(JavaLiteral::of).toList();

        if (quoted.isEmpty()) removeTestAnnotationAttribute(pm, "groups");
        else updateTestAnnotationAttribute(project, pm, "groups", "{" + String.join(", ", quoted) + "}");
    }

    // UC-CODEGEN-002, Rule-CODEGEN-047, Rule-CODEGEN-048
    protected void writeEnabled(final @NotNull Project project, final @NotNull PsiMethod pm, final @NotNull TestCaseDto testCase) {
        if (testCase.getStatus().isEnabled()) removeTestAnnotationAttribute(pm, "enabled");
        else updateTestAnnotationAttribute(project, pm, "enabled", "false");
    }

    protected void updateTestAnnotationAttribute(final @NotNull Project project, final @NotNull PsiMethod pm, final @NotNull String attrName, final @NotNull String newValue) {
        getTestAnnotation(pm).ifPresentOrElse(testAnnotation ->
                        updateAnnotationAttribute(JavaPsiFacade.getElementFactory(project), testAnnotation, attrName, newValue),
                () -> TestinLogger.warn("Update: method has no @Test annotation"));
    }

    protected void reformat(final @NotNull Project project, final @NotNull PsiElement element) {
        CodeStyleManager.getInstance(project).reformat(element);
    }

    protected void removeTestAnnotationAttribute(final @NotNull PsiMethod pm, final @NotNull String attrName) {
        getTestAnnotation(pm).ifPresentOrElse(testAnnotation -> testAnnotation.setDeclaredAttributeValue(attrName, null),
                () -> TestinLogger.warn("Update: method has no @Test annotation"));
    }

    private void noCodeToUpdate(final @NotNull Project project, final @NotNull TestCaseDto testCase, final @NotNull String detail) {
        TestinLogger.warn("Update: " + detail);

        Services.getInstance(project, Notifier.class).softRefuse(project, Refused.NO_GENERATED_CODE, testCase.getDescription());
    }

    protected void applyUpdate(final @NotNull Project project, final @NotNull TestCaseDto testCase, final @NotNull String title, final @NotNull Consumer<PsiMethod> updater) {
        applyToMethod(project, testCase, title, updater, detail -> noCodeToUpdate(project, testCase, detail));
    }

    // UC-CODEGEN-003, Rule-CODEGEN-019
    protected void applyOrCreate(final @NotNull Project project, final @NotNull TestCaseDto testCase, final @NotNull String title, final @NotNull Consumer<PsiMethod> updater) {
        applyToMethod(project, testCase, title, updater, detail -> {
            TestinLogger.info("Writing the method for '" + testCase.getDescription() + "' now that it has a name: " + detail);
            GenType.CREATE_TEST_CASE.execute(project, testCase);
        });
    }

    // UC-CODEGEN-012, Rule-CODEGEN-045
    protected void applyToEach(final @NotNull Project project, final @NotNull List<? extends TestCaseDto> changedTestCases, final @NotNull String title, final @NotNull BiConsumer<PsiMethod, TestCaseDto> updater) {
        final @NotNull Map<String, List<TestCaseDto>> byClass = Fqcn.byClassOfMethod(changedTestCases);
        if (byClass.isEmpty()) return;

        final @NotNull Runnable inCommand = () ->
                WriteCommandAction.runWriteCommandAction(project, title, null,
                        () -> byClass.forEach((path, testCases) -> writeAll(project, path, testCases, updater)));

        if (CommandProcessor.getInstance().getCurrentCommand() != null) inCommand.run();
        else ApplicationManager.getApplication().invokeLater(inCommand);
    }

    private void writeAll(final @NotNull Project project, final @NotNull String path, final @NotNull List<TestCaseDto> testCases, final @NotNull BiConsumer<PsiMethod, TestCaseDto> updater) {
        final @NotNull Optional<PsiClass> target = GeneratedClass.byName(project, path);

        if (target.isEmpty()) {
            TestinLogger.warn("Update: class not found: " + path);
            return;
        }

        final @NotNull PsiClass pc = target.orElseThrow();

        final @NotNull Map<String, PsiMethod> methods = GeneratedMethod.byTestCaseId(pc);

        int written = 0;
        for (final TestCaseDto testCase : testCases) {
            final @Nullable PsiMethod pm = methods.get(testCase.getId().toString());
            if (pm == null) continue;

            updater.accept(pm, testCase);
            written++;
        }

        if (written > 0) reformat(project, pc);
    }

    // Rule-CODEGEN-021
    protected void applyToMethod(final @NotNull Project project, final @NotNull TestCaseDto testCase, final @NotNull String title, final @NotNull Consumer<PsiMethod> updater, final @NotNull Consumer<String> onMissing) {
        final @NotNull List<String> classSegments = Fqcn.ofClass(testCase.getParent());
        if (classSegments.isEmpty()) return;

        final @NotNull Runnable inCommand = () ->
                WriteCommandAction.runWriteCommandAction(project, title, null, () ->
                        GeneratedClass.find(project, classSegments)
                                .ifPresentOrElse(
                                        targetClass -> findMethodByTestName(targetClass, testCase).ifPresentOrElse(updater,
                                                () -> onMissing.accept("no method with testName=" + testCase.getId())),
                                        () -> onMissing.accept("class not found: " + String.join(".", classSegments))));

        if (CommandProcessor.getInstance().getCurrentCommand() != null) inCommand.run();
        else ApplicationManager.getApplication().invokeLater(inCommand);
    }
}
