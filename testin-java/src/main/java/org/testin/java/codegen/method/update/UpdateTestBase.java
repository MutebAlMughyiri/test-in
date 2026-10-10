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
import org.testin.logger.Logger;
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

public class UpdateTestBase {
    private static boolean anotherMethodIsCalled(final @NotNull PsiMethod pm, final @NotNull String methodName) {
        final @NotNull String key = NameSanitizer.methodKey(methodName);

        return Optional.ofNullable(pm.getContainingClass()).stream()
                .flatMap(pc -> Arrays.stream(pc.getMethods()))
                .anyMatch(other -> !other.equals(pm) && key.equals(NameSanitizer.methodKey(other.getName())));
    }

    private static void keptItsName(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull String why) {
        Logger.warn("Kept the name of " + pm.getName() + ": " + why);

        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("codegen.rename.kept.title"), why);
    }

    // UC-CODEGEN-002, Rule-CODEGEN-014, Rule-CODEGEN-021
    protected static @NotNull Optional<PsiClass> classOf(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        return GeneratedClass.find(p, Fqcn.ofClass(tc.getParent()));
    }

    protected @NotNull Optional<PsiMethod> findMethodByTestName(final @NotNull PsiClass pc, final @NotNull TestCaseDto tc) {
        return GeneratedMethod.forTestCase(pc, tc);
    }

    protected @NotNull Optional<PsiAnnotation> getTestAnnotation(final @NotNull PsiMethod pm) {
        return GeneratedMethod.testAnnotationOf(pm);
    }

    protected void updateAnnotationAttribute(final @NotNull PsiElementFactory pf, final @NotNull PsiAnnotation pa, final @NotNull String attrName, final @NotNull String newValue) {
        final @NotNull PsiAnnotation parsed = pf.createAnnotationFromText("@A(v = " + newValue + ")", pa);

        final PsiAnnotationMemberValue value = parsed.findDeclaredAttributeValue("v");
        if (value == null) {
            Logger.warn("Could not read '" + newValue + "' as a value for " + attrName);
            return;
        }

        pa.setDeclaredAttributeValue(attrName, value);
    }

    // UC-CODEGEN-002, Rule-CODEGEN-012, Rule-CODEGEN-040
    protected void writeDescription(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull TestCaseDto tc) {
        updateTestAnnotationAttribute(p, pm, "description", JavaLiteral.of(tc.getDescription()));

        final @NotNull String newMethodName = NameSanitizer.methodName(tc.getDescription());
        if (newMethodName.isEmpty() || pm.getName().equals(newMethodName)) return;

        // Rule-CODEGEN-079
        if (NameSanitizer.cannotMakeMethodName(tc.getDescription())) {
            keptItsName(p, pm, Bundle.message("codegen.rename.not.a.method", pm.getName(), newMethodName));
            return;
        }

        if (anotherMethodIsCalled(pm, newMethodName)) {
            keptItsName(p, pm, Bundle.message("codegen.rename.taken", pm.getName(), newMethodName));
            return;
        }

        pm.setName(newMethodName);
    }

    // UC-CODEGEN-002, UC-CODEGEN-012, Rule-CODEGEN-046
    protected void writeGroups(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull TestCaseDto tc) {
        final @NotNull List<String> quoted = tc.getGroups().stream().map(JavaLiteral::of).toList();

        if (quoted.isEmpty()) removeTestAnnotationAttribute(pm, "groups");
        else updateTestAnnotationAttribute(p, pm, "groups", "{" + String.join(", ", quoted) + "}");
    }

    // UC-CODEGEN-002, Rule-CODEGEN-047, Rule-CODEGEN-048
    protected void writeEnabled(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull TestCaseDto tc) {
        if (tc.getStatus().isEnabled()) removeTestAnnotationAttribute(pm, "enabled");
        else updateTestAnnotationAttribute(p, pm, "enabled", "false");
    }

    protected void updateTestAnnotationAttribute(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull String attrName, final @NotNull String newValue) {
        getTestAnnotation(pm).ifPresentOrElse(testAnnotation ->
                        updateAnnotationAttribute(JavaPsiFacade.getElementFactory(p), testAnnotation, attrName, newValue),
                () -> Logger.warn("Update: method has no @Test annotation"));
    }

    protected void reformat(final @NotNull Project p, final @NotNull PsiElement element) {
        CodeStyleManager.getInstance(p).reformat(element);
    }

    protected void removeTestAnnotationAttribute(final @NotNull PsiMethod pm, final @NotNull String attrName) {
        getTestAnnotation(pm).ifPresentOrElse(testAnnotation -> testAnnotation.setDeclaredAttributeValue(attrName, null),
                () -> Logger.warn("Update: method has no @Test annotation"));
    }

    private void noCodeToUpdate(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String detail) {
        Logger.warn("Update: " + detail);

        Services.getInstance(p, Notifier.class).softRefuse(p, Refused.NO_GENERATED_CODE, tc.getDescription());
    }

    protected void applyUpdate(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String title, final @NotNull Consumer<PsiMethod> updater) {
        applyToMethod(p, tc, title, updater, detail -> noCodeToUpdate(p, tc, detail));
    }

    // UC-CODEGEN-003, Rule-CODEGEN-019
    protected void applyOrCreate(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String title, final @NotNull Consumer<PsiMethod> updater) {
        applyToMethod(p, tc, title, updater, detail -> {
            Logger.info("Writing the method for '" + tc.getDescription() + "' now that it has a name: " + detail);
            GenType.CREATE_TEST_CASE.execute(p, tc);
        });
    }

    // UC-CODEGEN-012, Rule-CODEGEN-045
    protected void applyToEach(final @NotNull Project p, final @NotNull List<? extends TestCaseDto> items, final @NotNull String title, final @NotNull BiConsumer<PsiMethod, TestCaseDto> updater) {
        final @NotNull Map<String, List<TestCaseDto>> byClass = Fqcn.byClassOfMethod(items);
        if (byClass.isEmpty()) return;

        final @NotNull Runnable inCommand = () ->
                WriteCommandAction.runWriteCommandAction(p, title, null,
                        () -> byClass.forEach((path, testCases) -> writeAll(p, path, testCases, updater)));

        if (CommandProcessor.getInstance().getCurrentCommand() != null) inCommand.run();
        else ApplicationManager.getApplication().invokeLater(inCommand);
    }

    private void writeAll(final @NotNull Project p, final @NotNull String path, final @NotNull List<TestCaseDto> testCases, final @NotNull BiConsumer<PsiMethod, TestCaseDto> updater) {
        final @NotNull Optional<PsiClass> target = GeneratedClass.byName(p, path);

        if (target.isEmpty()) {
            Logger.warn("Update: class not found: " + path);
            return;
        }

        final @NotNull PsiClass pc = target.orElseThrow();

        final @NotNull Map<String, PsiMethod> methods = GeneratedMethod.byTestCaseId(pc);

        int written = 0;
        for (final TestCaseDto tc : testCases) {
            final @Nullable PsiMethod pm = methods.get(tc.getId().toString());
            if (pm == null) continue;

            updater.accept(pm, tc);
            written++;
        }

        if (written > 0) reformat(p, pc);
    }

    // Rule-CODEGEN-021
    protected void applyToMethod(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String title, final @NotNull Consumer<PsiMethod> updater, final @NotNull Consumer<String> onMissing) {
        final @NotNull List<String> path = Fqcn.ofClass(tc.getParent());
        if (path.isEmpty()) return;

        final @NotNull Runnable inCommand = () ->
                WriteCommandAction.runWriteCommandAction(p, title, null, () ->
                        GeneratedClass.find(p, path)
                                .ifPresentOrElse(
                                        targetClass -> findMethodByTestName(targetClass, tc).ifPresentOrElse(updater,
                                                () -> onMissing.accept("no method with testName=" + tc.getId())),
                                        () -> onMissing.accept("class not found: " + String.join(".", path))));

        if (CommandProcessor.getInstance().getCurrentCommand() != null) inCommand.run();
        else ApplicationManager.getApplication().invokeLater(inCommand);
    }
}
