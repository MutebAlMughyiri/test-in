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
import org.testin.codegen.CodeChange;
import org.testin.codegen.CodeChangeType;
import org.testin.codegen.ExecutionPosition;
import org.testin.codegen.Fqcn;
import org.testin.codegen.TestSourceRoot;
import org.testin.codegen.writer.KeptTestCode;
import org.testin.java.codegen.GeneratedClass;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.java.codegen.JavaLiteral;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.NameSanitizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CreateTestMethod implements CodeChange<TestCase> {

    static @NotNull Optional<Target> parse(final @NotNull List<String> fqcn) {
        if (fqcn.size() < 2) return Optional.empty();

        return Optional.of(new Target(
                String.join(".", fqcn.subList(0, fqcn.size() - 1)),
                fqcn.subList(0, fqcn.size() - 2),
                fqcn.get(fqcn.size() - 2),
                fqcn.getLast()));
    }

    private static @NotNull String noMethodTitle(final @NotNull List<TestCase> without) {
        return without.size() == 1
                ? Bundle.message("codegen.no.method.one")
                : Bundle.message("codegen.no.method.many", String.valueOf(without.size()));
    }

    private static @NotNull String named(final @NotNull List<TestCase> testCases) {
        final @NotNull String names = testCases.stream().limit(3).map(TestCase::getDescription).collect(Collectors.joining("\", \"", "\"", "\""));

        return testCases.size() > 3 ? Bundle.message("codegen.named.and.more", names, String.valueOf(testCases.size() - 3)) : names;
    }


    private static @NotNull String testMethods(final int howMany) {
        return howMany + " test method" + (howMany == 1 ? "" : "s");
    }

    // UC-CODEGEN-002, Rule-CODEGEN-014, Rule-CODEGEN-045, Rule-CODEGEN-046, Rule-CODEGEN-013, Rule-CODEGEN-047
    private static @NotNull String methodText(final @NotNull String methodName, final @NotNull TestCase testCase, final int position) {
        final @NotNull StringBuilder attributes = new StringBuilder();

        if (!testCase.getGroups().isEmpty()) {
            final @NotNull List<String> quoted = testCase.getGroups().stream().map(JavaLiteral::of).toList();

            attributes.append(", groups = {").append(String.join(", ", quoted)).append("}");
        }

        attributes.append(", priority = ").append(position);

        if (!testCase.getStatus().isEnabled()) attributes.append(", enabled = false");

        final @NotNull String annotation = String.format("@Test(description = %s, testName = \"%s\"%s)",
                JavaLiteral.of(testCase.getDescription()),
                testCase.getUuid(),
                attributes);

        return annotation + "\npublic void " + methodName + "() " + GeneratedMethod.emptyBody(methodName);
    }

    // Rule-CODEGEN-095
    private static @NotNull Optional<PsiMethod> theTestersOwn(final @NotNull Optional<String> owner, final @NotNull Optional<PsiMethod> sameName, final @NotNull Map<String, PsiMethod> describedByHand, final @NotNull TestCase testCase) {
        if (owner.isPresent())
            return sameName.filter(testMethod -> owner.orElseThrow().isEmpty() && GeneratedMethod.testAnnotationOf(testMethod).isPresent());

        return Optional.ofNullable(describedByHand.remove(testCase.getDescription()));
    }

    // UC-CODEGEN-002
    @Override
    public void execute(final @NotNull Project project, final @NotNull TestCase testCase) {
        executeAll(project, List.of(testCase));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-018
    @Override
    public void executeAll(final @NotNull Project project, final @NotNull List<? extends TestCase> testCases) {
        final @NotNull Map<String, List<TestCase>> byClass = new LinkedHashMap<>();

        for (final TestCase testCase : testCases) {
            final @NotNull List<String> fqcn = Fqcn.ofMethod(testCase);
            parse(fqcn).ifPresentOrElse(
                    target -> byClass.computeIfAbsent(target.path(), _ -> new ArrayList<>()).add(testCase),
                    () -> noMethodFor(testCase, fqcn));
        }

        WriteCommandAction.runWriteCommandAction(project, CodeChangeType.CREATE_TEST_CASE.description(), null,
                () -> byClass.values().forEach(group -> createMethods(project, group)));
    }

    private void noMethodFor(final @NotNull TestCase testCase, final @NotNull List<String> fqcn) {
        if (fqcn.isEmpty()) TestinLogger.debug("No description yet, so no method to write for " + testCase.getUuid());
        else TestinLogger.error("FQCN list is too short to generate a method: " + fqcn);
    }

    private void createMethods(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        final @NotNull Optional<Target> first = parse(Fqcn.ofMethod(testCases.getFirst()));
        if (first.isEmpty()) return;

        final @NotNull Target target = first.orElseThrow();
        TestinLogger.info("Creating " + testMethods(testCases.size()) + " in " + target.path());

        GeneratedClass.findOrWrite(project, target.packageList(), target.className()).ifPresentOrElse(
                targetClass -> injectAsText(project, targetClass, testCases),
                () -> retryInjectPhysically(project, target.packageList(), target.className(), testCases));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-016, Rule-CODEGEN-114
    private void injectAsText(final @NotNull Project project, final @NotNull PsiClass targetClass, final @NotNull List<TestCase> testCases) {
        final @NotNull PsiFile file = targetClass.getContainingFile();
        final @NotNull PsiDocumentManager documents = PsiDocumentManager.getInstance(project);
        final @NotNull Optional<Document> document = Optional.ofNullable(documents.getDocument(file));

        if (document.isEmpty()) {
            oneAtATime(project, targetClass, decide(project, targetClass, testCases));
            return;
        }

        final @NotNull List<TestCase> toWrite = decide(project, targetClass, testCases);
        if (!toWrite.isEmpty()) insertAtClassEnd(project, targetClass, toWrite, document.orElseThrow());
    }

    // UC-CODEGEN-002, Rule-CODEGEN-001, Rule-CODEGEN-079, Rule-CODEGEN-095
    private @NotNull List<TestCase> decide(final @NotNull Project project, final @NotNull PsiClass targetClass, final @NotNull List<TestCase> testCases) {
        final @NotNull Map<String, String> owners = new HashMap<>();
        final @NotNull Map<String, PsiMethod> byKey = new HashMap<>();
        for (final PsiMethod testMethod : targetClass.getMethods()) {
            final @NotNull String methodKey = NameSanitizer.methodKey(testMethod.getName());

            owners.putIfAbsent(methodKey, GeneratedMethod.testCaseUuidOf(testMethod).orElse(""));
            byKey.putIfAbsent(methodKey, testMethod);
        }

        final @NotNull Map<String, PsiMethod> generated = GeneratedMethod.byTestCaseUuid(targetClass);
        final @NotNull Map<String, PsiMethod> describedByHand = GeneratedMethod.untaggedByDescription(targetClass);

        final @NotNull List<TestCase> missing = testCases.stream().filter(testCase -> !generated.containsKey(testCase.getUuid().toString())).toList();
        final @NotNull List<TestCase> cannotBeNamed = missing.stream().filter(testCase -> NameSanitizer.cannotMakeMethodName(testCase.getDescription())).toList();
        final @NotNull List<TestCase> nameable = missing.stream().filter(testCase -> !NameSanitizer.cannotMakeMethodName(testCase.getDescription())).toList();
        final int alreadyThere = testCases.size() - missing.size();

        final @NotNull List<TestCase> toWrite = new ArrayList<>();
        final @NotNull List<TestCase> lostTheName = new ArrayList<>();
        int adopted = 0;

        for (final TestCase testCase : nameable) {
            final @NotNull String testCaseUuid = testCase.getUuid().toString();
            final @NotNull String methodName = Fqcn.methodNameOf(testCase);
            final @NotNull String key = NameSanitizer.methodKey(methodName);
            final @NotNull Optional<String> owner = Optional.ofNullable(owners.get(key));
            final @NotNull Optional<PsiMethod> theTestersOwn = theTestersOwn(owner, Optional.ofNullable(byKey.get(key)), describedByHand, testCase);

            if (theTestersOwn.isPresent()) {
                GeneratedMethod.adopt(project, theTestersOwn.orElseThrow(), testCase);
                owners.put(NameSanitizer.methodKey(theTestersOwn.orElseThrow().getName()), testCaseUuid);
                adopted++;
                continue;
            }

            if (owner.isPresent()) {
                lostTheName.add(testCase);
                continue;
            }

            owners.put(key, testCaseUuid);
            toWrite.add(testCase);
        }

        if (alreadyThere > 0) {
            TestinLogger.info(alreadyThere + " of " + testMethods(testCases.size())
                    + " already in " + targetClass.getQualifiedName());
        }

        if (adopted > 0) {
            TestinLogger.info("Linked " + adopted + " hand-written method(s) in "
                    + targetClass.getQualifiedName() + " to the test cases that describe them");
        }

        reportLostTheName(project, targetClass, lostTheName);
        reportCannotBeNamed(project, targetClass, cannotBeNamed);

        return toWrite;
    }

    // UC-CODEGEN-002, Rule-CODEGEN-016, Rule-CODEGEN-114, Rule-CODEGEN-003
    private void insertAtClassEnd(final @NotNull Project project, final @NotNull PsiClass targetClass, final @NotNull List<TestCase> toWrite, final @NotNull Document document) {
        final @NotNull PsiFile file = targetClass.getContainingFile();
        final @NotNull PsiDocumentManager documents = PsiDocumentManager.getInstance(project);

        GeneratedMethod.importTest(project, file);

        documents.doPostponedOperationsAndUnblockDocument(document);

        final @NotNull ToIntFunction<TestCase> positions = ExecutionPosition.ofEach(project);
        final @NotNull StringBuilder methods = new StringBuilder();
        final @NotNull StringBuilder keptMethods = new StringBuilder();

        for (final TestCase testCase : toWrite) {
            Services.getInstance(project, KeptTestCode.class).take(testCase).ifPresentOrElse(
                    keptMethod -> keptMethods.append('\n').append(keptMethod).append('\n'),
                    () -> methods.append('\n').append(methodText(Fqcn.methodNameOf(testCase), testCase, positions.applyAsInt(testCase))).append('\n'));
        }

        final int insertAt = Optional.ofNullable(targetClass.getRBrace()).map(brace -> brace.getTextRange().getStartOffset()).orElseGet(() -> targetClass.getTextRange().getEndOffset());
        document.insertString(insertAt, methods.toString() + keptMethods);
        documents.commitDocument(document);

        CodeStyleManager.getInstance(project).reformatText(file, insertAt, insertAt + methods.length());
    }

    // UC-CODEGEN-002, Rule-CODEGEN-011
    private void reportCannotBeNamed(final @NotNull Project project, final @NotNull PsiClass targetClass, final @NotNull List<TestCase> cannotBeNamed) {
        if (cannotBeNamed.isEmpty()) return;

        TestinLogger.warn("No method for " + cannotBeNamed.size() + " test case(s) in " + targetClass.getQualifiedName()
                + ": the description cannot name a Java method");

        Services.getInstance(project, Notifier.class).warn(project, noMethodTitle(cannotBeNamed),
                Bundle.message("codegen.cannot.name.message", named(cannotBeNamed)));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-001
    private void reportLostTheName(final @NotNull Project project, final @NotNull PsiClass targetClass, final @NotNull List<TestCase> lost) {
        if (lost.isEmpty()) return;

        TestinLogger.warn("No method for " + lost.size() + " test case(s) in " + targetClass.getQualifiedName()
                + ": the name is already taken by another method");

        Services.getInstance(project, Notifier.class).warn(project, noMethodTitle(lost),
                Bundle.message("codegen.name.taken.message", named(lost)));
    }

    private void oneAtATime(final @NotNull Project project, final @NotNull PsiClass targetClass, final @NotNull List<TestCase> toWrite) {
        if (toWrite.isEmpty()) return;

        TestinLogger.warn("Writing " + toWrite.size() + " methods one at a time into "
                + targetClass.getQualifiedName() + ": it has no document to edit");

        for (final TestCase testCase : toWrite) {
            injectMethod(project, targetClass, testCase).ifPresent(added -> CodeStyleManager.getInstance(project).reformat(added));
        }
    }

    private void retryInjectPhysically(final @NotNull Project project, final @NotNull List<String> packageList, final @NotNull String className, final @NotNull List<TestCase> testCases) {
        TestSourceRoot.find(project).ifPresentOrElse(
                sourceRoot -> injectIntoFile(project, sourceRoot, packageList, className, testCases),
                () -> TestinLogger.error("retryInjectPhysically: no Java test source root, cannot inject " + testMethods(testCases.size())));
    }

    private void injectIntoFile(final @NotNull Project project, final @NotNull VirtualFile sourceRoot, final @NotNull List<String> packageList, final @NotNull String className, final @NotNull List<TestCase> testCases) {
        try {
            final @NotNull String relativePath = Fqcn.classFileOf(Stream.concat(packageList.stream(), Stream.of(className)).toList());
            final @NotNull Optional<VirtualFile> found = TestSourceRoot.under(sourceRoot, relativePath);
            if (found.isEmpty()) {
                TestinLogger.error("retryInjectPhysically: file not found at " + relativePath + " for " + testMethods(testCases.size()));
                return;
            }

            if (!(PsiManager.getInstance(project).findFile(found.orElseThrow()) instanceof PsiJavaFile javaPsiFile)) {
                TestinLogger.error("retryInjectPhysically: file " + className + ".java is not a valid Java file");
                return;
            }

            final PsiClass @NotNull [] classes = javaPsiFile.getClasses();
            if (classes.length == 0) {
                TestinLogger.error("retryInjectPhysically: no classes found in " + className + ".java");
                return;
            }

            TestinLogger.warn(className + " could not be found by name, so its file was read instead");
            injectAsText(project, classes[0], testCases);

        } catch (final Exception ex) {
            TestinLogger.error("retryInjectPhysically failed for " + className + ".java: " + FailureText.of(ex));
        }
    }

    // UC-CODEGEN-002, Rule-CODEGEN-016
    private @NotNull Optional<PsiElement> injectMethod(final @NotNull Project project, final @NotNull PsiClass targetClass, final @NotNull TestCase testCase) {
        final @NotNull String methodName = Fqcn.methodNameOf(testCase);
        try {
            final @NotNull PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
            GeneratedMethod.importTest(project, targetClass.getContainingFile());

            // Rule-CODEGEN-114
            final @NotNull String text = Services.getInstance(project, KeptTestCode.class).take(testCase).map(String::strip).orElseGet(() -> methodText(methodName, testCase, ExecutionPosition.of(project, testCase)));
            final @NotNull PsiMethod newMethod = factory.createMethodFromText(text, targetClass);
            final @NotNull PsiElement addedElement = targetClass.add(newMethod);

            TestinLogger.info("Injected method: " + methodName + " with Priority: " + testCase.getPriority().getLabel());
            return Optional.of(addedElement);

        } catch (final Exception ex) {
            TestinLogger.error("injectMethod failed for '" + methodName + "': " + FailureText.of(ex));
            return Optional.empty();
        }
    }
}