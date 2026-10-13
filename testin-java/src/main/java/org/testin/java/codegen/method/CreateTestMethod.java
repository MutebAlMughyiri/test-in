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

package org.testin.java.codegen.method;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.ExecutionPosition;
import org.testin.codegen.Fqcn;
import org.testin.codegen.CodeChange;
import org.testin.codegen.GenType;
import org.testin.codegen.JavaSourceRoot;
import org.testin.java.codegen.GeneratedClass;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.java.codegen.JavaLiteral;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.NameSanitizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class CreateTestMethod implements CodeChange<TestCaseDto> {

    static @NotNull Optional<Target> parse(final @NotNull List<String> fqcn) {
        if (fqcn.size() < 2) return Optional.empty();

        return Optional.of(new Target(
                String.join(".", fqcn.subList(0, fqcn.size() - 1)),
                fqcn.subList(0, fqcn.size() - 2),
                fqcn.get(fqcn.size() - 2),
                fqcn.getLast()));
    }

    private static @NotNull String noMethodTitle(final @NotNull List<TestCaseDto> without) {
        return without.size() == 1
                ? Bundle.message("codegen.no.method.one")
                : Bundle.message("codegen.no.method.many", String.valueOf(without.size()));
    }

    private static @NotNull String named(final @NotNull List<TestCaseDto> testCases) {
        final @NotNull String names = testCases.stream().limit(3).map(TestCaseDto::getDescription).collect(Collectors.joining("\", \"", "\"", "\""));

        return testCases.size() > 3 ? Bundle.message("codegen.named.and.more", names, String.valueOf(testCases.size() - 3)) : names;
    }


    private static @NotNull String testMethods(final int howMany) {
        return howMany + " test method" + (howMany == 1 ? "" : "s");
    }

    // UC-CODEGEN-002, Rule-CODEGEN-014, Rule-CODEGEN-046
    private static @NotNull String methodText(final @NotNull String methodName, final @NotNull TestCaseDto testCase, final int position) {
        final @NotNull StringBuilder attributes = new StringBuilder();

        if (!testCase.getGroups().isEmpty()) {
            final @NotNull List<String> quoted = testCase.getGroups().stream().map(JavaLiteral::of).toList();

            attributes.append(", groups = {").append(String.join(", ", quoted)).append("}");
        }

        attributes.append(", priority = ").append(position);

        final @NotNull String annotation = String.format("@Test(description = %s, testName = \"%s\"%s)",
                JavaLiteral.of(testCase.getDescription()),
                testCase.getId(),
                attributes);

        return annotation + "\npublic void " + methodName + "() " + GeneratedMethod.emptyBody(methodName);
    }

    // Rule-CODEGEN-095
    private static @NotNull Optional<PsiMethod> theTestersOwn(final @NotNull Optional<String> owner, final @NotNull Optional<PsiMethod> sameName, final @NotNull Map<String, PsiMethod> describedByHand, final @NotNull TestCaseDto testCase) {
        if (owner.isPresent())
            return sameName.filter(pm -> owner.orElseThrow().isEmpty() && GeneratedMethod.testAnnotationOf(pm).isPresent());

        return Optional.ofNullable(describedByHand.remove(testCase.getDescription()));
    }

    // UC-CODEGEN-002
    @Override
    public void execute(final @NotNull Project p, final @NotNull TestCaseDto testCase) {
        executeAll(p, List.of(testCase));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-018
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<? extends TestCaseDto> testCases) {
        final @NotNull Map<String, List<TestCaseDto>> byClass = new LinkedHashMap<>();

        for (final TestCaseDto testCase : testCases) {
            final @NotNull List<String> fqcn = Fqcn.ofMethod(testCase);
            parse(fqcn).ifPresentOrElse(
                    target -> byClass.computeIfAbsent(target.path(), _ -> new ArrayList<>()).add(testCase),
                    () -> noMethodFor(testCase, fqcn));
        }

        WriteCommandAction.runWriteCommandAction(p, GenType.CREATE_TEST_CASE.description(), null,
                () -> byClass.values().forEach(group -> createMethods(p, group)));
    }

    private void noMethodFor(final @NotNull TestCaseDto testCase, final @NotNull List<String> fqcn) {
        if (fqcn.isEmpty()) TestinLogger.debug("No description yet, so no method to write for " + testCase.getId());
        else TestinLogger.error("FQCN list is too short to generate a method: " + fqcn);
    }

    private void createMethods(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        final @NotNull Optional<Target> first = parse(Fqcn.ofMethod(testCases.getFirst()));
        if (first.isEmpty()) return;

        final @NotNull Target target = first.orElseThrow();
        TestinLogger.info("Creating " + testMethods(testCases.size()) + " in " + target.path());

        GeneratedClass.findOrWrite(p, target.packageList(), target.className()).ifPresentOrElse(
                targetClass -> injectAsText(p, targetClass, testCases),
                () -> testCases.forEach(testCase -> retryInjectPhysically(p, target.packageList(), target.className(),
                        Fqcn.methodNameOf(testCase), testCase)));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-016
    private void injectAsText(final @NotNull Project p, final @NotNull PsiClass targetClass, final @NotNull List<TestCaseDto> testCases) {
        final @NotNull PsiFile file = targetClass.getContainingFile();
        final @NotNull PsiDocumentManager documents = PsiDocumentManager.getInstance(p);
        final @NotNull Optional<Document> document = Optional.ofNullable(documents.getDocument(file));

        if (document.isEmpty()) {
            oneAtATime(p, targetClass, testCases, "it has no document to edit");
            return;
        }

        final @NotNull Map<String, String> owners = new HashMap<>();
        final @NotNull Map<String, PsiMethod> byKey = new HashMap<>();
        for (final PsiMethod pm : targetClass.getMethods()) {
            final @NotNull String methodKey = NameSanitizer.methodKey(pm.getName());

            owners.putIfAbsent(methodKey, GeneratedMethod.testCaseIdOf(pm).orElse(""));
            byKey.putIfAbsent(methodKey, pm);
        }

        final @NotNull Map<String, PsiMethod> generated = GeneratedMethod.byTestCaseId(targetClass);
        final @NotNull Map<String, PsiMethod> describedByHand = GeneratedMethod.untaggedByDescription(targetClass);
        final @NotNull ToIntFunction<TestCaseDto> positions = ExecutionPosition.ofEach(p);

        final @NotNull List<TestCaseDto> missing = testCases.stream().filter(testCase -> !generated.containsKey(testCase.getId().toString())).toList();
        final @NotNull List<TestCaseDto> cannotBeNamed = missing.stream().filter(testCase -> NameSanitizer.cannotMakeMethodName(testCase.getDescription())).toList();
        final @NotNull List<TestCaseDto> nameable = missing.stream().filter(testCase -> !NameSanitizer.cannotMakeMethodName(testCase.getDescription())).toList();
        final int alreadyThere = testCases.size() - missing.size();

        final @NotNull StringBuilder methods = new StringBuilder();
        final @NotNull List<TestCaseDto> lostTheName = new ArrayList<>();
        int adopted = 0;

        for (final TestCaseDto testCase : nameable) {
            final @NotNull String testCaseId = testCase.getId().toString();
            final @NotNull String methodName = Fqcn.methodNameOf(testCase);
            final @NotNull String key = NameSanitizer.methodKey(methodName);
            final @NotNull Optional<String> owner = Optional.ofNullable(owners.get(key));
            final @NotNull Optional<PsiMethod> theTestersOwn = theTestersOwn(owner, Optional.ofNullable(byKey.get(key)), describedByHand, testCase);

            if (theTestersOwn.isPresent()) {
                GeneratedMethod.adopt(p, theTestersOwn.orElseThrow(), testCase);
                owners.put(NameSanitizer.methodKey(theTestersOwn.orElseThrow().getName()), testCaseId);
                adopted++;
                continue;
            }

            if (owner.isPresent()) {
                lostTheName.add(testCase);
                continue;
            }

            owners.put(key, testCaseId);
            methods.append('\n').append(methodText(methodName, testCase, positions.applyAsInt(testCase))).append('\n');
        }

        if (alreadyThere > 0) {
            TestinLogger.info(alreadyThere + " of " + testMethods(testCases.size())
                    + " already in " + targetClass.getQualifiedName());
        }

        if (adopted > 0) {
            TestinLogger.info("Linked " + adopted + " hand-written method(s) in "
                    + targetClass.getQualifiedName() + " to the test cases that describe them");
        }

        reportLostTheName(p, targetClass, lostTheName);
        reportCannotBeNamed(p, targetClass, cannotBeNamed);

        if (!methods.isEmpty()) insertBeforeClosingBrace(p, targetClass, testCases, document.orElseThrow(), methods);
    }

    // UC-CODEGEN-002, Rule-CODEGEN-016
    private void insertBeforeClosingBrace(final @NotNull Project p, final @NotNull PsiClass targetClass, final @NotNull List<TestCaseDto> testCases, final @NotNull Document document, final @NotNull CharSequence methods) {
        final @NotNull PsiFile file = targetClass.getContainingFile();
        final @NotNull PsiDocumentManager documents = PsiDocumentManager.getInstance(p);

        GeneratedMethod.importTest(p, file);

        documents.doPostponedOperationsAndUnblockDocument(document);

        final @NotNull Optional<PsiElement> closingBrace = Optional.ofNullable(targetClass.getRBrace());
        if (closingBrace.isEmpty()) {
            oneAtATime(p, targetClass, testCases, "it has no closing brace to write before");
            return;
        }

        final int insertAt = closingBrace.orElseThrow().getTextRange().getStartOffset();
        document.insertString(insertAt, methods);
        documents.commitDocument(document);

        CodeStyleManager.getInstance(p).reformatText(file, insertAt, insertAt + methods.length());
    }

    // UC-CODEGEN-002, Rule-CODEGEN-011
    private void reportCannotBeNamed(final @NotNull Project p, final @NotNull PsiClass targetClass, final @NotNull List<TestCaseDto> cannotBeNamed) {
        if (cannotBeNamed.isEmpty()) return;

        TestinLogger.warn("No method for " + cannotBeNamed.size() + " test case(s) in " + targetClass.getQualifiedName()
                + ": the description cannot name a Java method");

        Services.getInstance(p, Notifier.class).warn(p, noMethodTitle(cannotBeNamed),
                Bundle.message("codegen.cannot.name.message", named(cannotBeNamed)));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-001
    private void reportLostTheName(final @NotNull Project p, final @NotNull PsiClass targetClass, final @NotNull List<TestCaseDto> lost) {
        if (lost.isEmpty()) return;

        TestinLogger.warn("No method for " + lost.size() + " test case(s) in " + targetClass.getQualifiedName()
                + ": the name is already taken by another method");

        Services.getInstance(p, Notifier.class).warn(p, noMethodTitle(lost),
                Bundle.message("codegen.name.taken.message", named(lost)));
    }

    private void oneAtATime(final @NotNull Project p, final @NotNull PsiClass targetClass, final @NotNull List<TestCaseDto> testCases, final @NotNull String reason) {
        TestinLogger.warn("Writing " + testCases.size() + " methods one at a time into "
                + targetClass.getQualifiedName() + ": " + reason);

        int written = 0;
        for (final TestCaseDto testCase : testCases) {
            if (injectMethod(p, targetClass, Fqcn.methodNameOf(testCase), testCase).isPresent()) written++;
        }

        if (written > 0) CodeStyleManager.getInstance(p).reformat(targetClass);
    }

    private void retryInjectPhysically(final @NotNull Project p, final @NotNull List<String> packageList, final @NotNull String className, final @NotNull String methodName, final @NotNull TestCaseDto testCase) {
        JavaSourceRoot.find(p).ifPresentOrElse(
                sourceRoot -> injectIntoFile(p, sourceRoot, packageList, className, methodName, testCase),
                () -> TestinLogger.error("retryInjectPhysically: no Java test source root, cannot inject method '"
                        + methodName + "'"));
    }

    private void injectIntoFile(final @NotNull Project p, final @NotNull VirtualFile sourceRoot, final @NotNull List<String> packageList, final @NotNull String className, final @NotNull String methodName, final @NotNull TestCaseDto testCase) {
        try {
            final @NotNull String relativePath = String.join("/", packageList) + "/" + className + ".java";
            final @NotNull Optional<VirtualFile> found = JavaSourceRoot.under(sourceRoot, relativePath);
            if (found.isEmpty()) {
                TestinLogger.error("retryInjectPhysically: file not found at " + relativePath + " for method '" + methodName + "'");
                return;
            }

            if (!(PsiManager.getInstance(p).findFile(found.orElseThrow()) instanceof PsiJavaFile javaPsiFile)) {
                TestinLogger.error("retryInjectPhysically: file " + className + ".java is not a valid Java file for method '" + methodName + "'");
                return;
            }

            final PsiClass @NotNull [] classes = javaPsiFile.getClasses();
            if (classes.length == 0) {
                TestinLogger.error("retryInjectPhysically: no classes found in " + className + ".java for method '" + methodName + "'");
                return;
            }

            injectMethod(p, classes[0], methodName, testCase)
                    .ifPresent(added -> CodeStyleManager.getInstance(p).reformat(added));

        } catch (final Exception ex) {
            TestinLogger.error("retryInjectPhysically failed for method '" + methodName + "': " + FailureText.of(ex));
        }
    }

    // UC-CODEGEN-002, Rule-CODEGEN-016
    private @NotNull Optional<PsiElement> injectMethod(final @NotNull Project p, final @NotNull PsiClass targetClass, final @NotNull String methodName, final @NotNull TestCaseDto testCase) {
        try {
            final @NotNull PsiElementFactory factory = JavaPsiFacade.getElementFactory(p);
            final @NotNull PsiFile file = targetClass.getContainingFile();

            GeneratedMethod.importTest(p, file);

            if (GeneratedMethod.forTestCase(targetClass, testCase).isPresent()) {
                TestinLogger.info("Method already exists: " + methodName);
                return Optional.empty();
            }

            final @NotNull String key = NameSanitizer.methodKey(methodName);
            final @NotNull Optional<PsiMethod> sameName = Arrays.stream(targetClass.getMethods())
                    .filter(pm -> key.equals(NameSanitizer.methodKey(pm.getName())))
                    .findFirst();

            if (sameName.isPresent()) {
                if (GeneratedMethod.testCaseIdOf(sameName.orElseThrow()).isPresent() || GeneratedMethod.testAnnotationOf(sameName.orElseThrow()).isEmpty())
                    reportLostTheName(p, targetClass, List.of(testCase));
                else
                    TestinLogger.info("Method already exists: " + methodName);

                return Optional.empty();
            }

            // Rule-CODEGEN-095
            final @NotNull Optional<PsiMethod> describedByTheTester = Optional.ofNullable(GeneratedMethod.untaggedByDescription(targetClass).get(testCase.getDescription()));
            if (describedByTheTester.isPresent()) {
                GeneratedMethod.adopt(p, describedByTheTester.orElseThrow(), testCase);
                return Optional.empty();
            }

            final @NotNull PsiMethod newMethod = factory.createMethodFromText(methodText(methodName, testCase, ExecutionPosition.of(p, testCase)), targetClass);
            final @NotNull PsiElement addedElement = targetClass.add(newMethod);

            TestinLogger.info("Injected method: " + methodName + " with Priority: " + testCase.getPriority().getLabel());
            return Optional.of(addedElement);

        } catch (final Exception ex) {
            TestinLogger.error("injectMethod failed for '" + methodName + "': " + FailureText.of(ex));
            return Optional.empty();
        }
    }
}