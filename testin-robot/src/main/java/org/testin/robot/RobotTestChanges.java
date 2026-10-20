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

package org.testin.robot;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChangeType;
import org.testin.codegen.event.CopiedTestCase;
import org.testin.codegen.event.MovedTestCase;
import org.testin.codegen.writer.CodeCommand;
import org.testin.codegen.writer.KeptTestCode;
import org.testin.codegen.writer.RemovedTestCodeFile;
import org.testin.indexer.TestCases;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.services.Services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RobotTestChanges {
    private static final @NotNull String EXCLUDED = "robot:exclude";

    // Rule-CODEGEN-123, Rule-CODEGEN-125
    static @NotNull List<String> tagsOf(final @NotNull TestCase testCase) {
        final @NotNull Stream<String> excluded = testCase.getStatus().isEnabled() ? Stream.empty() : Stream.of(EXCLUDED);
        return Stream.of(Stream.of(testCase.getUuid().toString()), testCase.getGroups().stream(), excluded).flatMap(tags -> tags).toList();
    }

    // Rule-CODEGEN-126
    private static @NotNull List<UUID> orderOf(final @NotNull Project project, final @NotNull AbstractNode testSet) {
        return Services.getInstance(project, TestCases.class).getTestCasesForTestSet(testSet.getPath()).stream().map(TestCase::getUuid).toList();
    }

    // UC-CODEGEN-006, Rule-CODEGEN-119
    private static @NotNull Optional<VirtualFile> suiteHolding(final @NotNull Project project, final @NotNull TestCase testCase) {
        return Optional.ofNullable(RobotTests.of(project, List.of(testCase.getUuid())).get(testCase.getUuid())).map(RobotTest::suite);
    }

    private static void editTest(final @NotNull VirtualFile suite, final @NotNull TestCase testCase, final @NotNull BiFunction<String, RobotTestBlock, String> change) {
        RobotSuiteFiles.edit(suite, text -> RobotSuite.blockOf(text, testCase.getUuid()).map(test -> change.apply(text, test)).orElse(text));
    }

    // UC-CODEGEN-002, UC-CODEGEN-005, Rule-CODEGEN-002, Rule-CODEGEN-025, Rule-CODEGEN-095, Rule-CODEGEN-114, Rule-CODEGEN-124
    static void create(final @NotNull Project project, final @NotNull TestCase testCase) {
        CodeCommand.inOne(project, CodeChangeType.CREATE_TEST_CASE.description(), () -> written(project, testCase));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-002, Rule-CODEGEN-124
    private static void written(final @NotNull Project project, final @NotNull TestCase testCase) {
        final @NotNull Optional<String> name = RobotNames.testNameOf(testCase.getDescription());
        if (name.isEmpty() || suiteHolding(project, testCase).isPresent()) {
            TestinLogger.debug("No Robot test to write for " + testCase.getUuid() + ": it has no name, or it has a test already");
            return;
        }

        RobotSuiteFiles.suiteOf(project, testCase.getParent()).ifPresent(suite ->
                RobotSuiteFiles.edit(suite, text -> RobotSuiteText.arranged(withTheTest(project, text, testCase, name.orElseThrow()), orderOf(project, testCase.getParent()))));
    }

    // Rule-CODEGEN-095, Rule-CODEGEN-114
    private static @NotNull String withTheTest(final @NotNull Project project, final @NotNull String text, final @NotNull TestCase testCase, final @NotNull String name) {
        return RobotSuite.blocksOf(text).stream()
                .filter(test -> test.testCaseUuid().isEmpty() && test.name().equalsIgnoreCase(name))
                .findFirst()
                .map(handWritten -> RobotSuiteText.retagged(text, handWritten, Stream.concat(Stream.of(testCase.getUuid().toString()), handWritten.tags().stream()).toList()))
                .orElseGet(() -> RobotSuiteText.withTest(text, Services.getInstance(project, KeptTestCode.class).take(testCase).orElseGet(() -> RobotSuiteText.newTest(name, tagsOf(testCase)))));
    }

    // UC-CODEGEN-014, Rule-CODEGEN-049, Rule-CODEGEN-050, Rule-CODEGEN-114, Rule-INTERNAL-143
    static void remove(final @NotNull Project project, final @NotNull TestCase testCase) {
        CodeCommand.inOne(project, CodeChangeType.REMOVE_TEST_CASE.description(), () -> suiteHolding(project, testCase).ifPresent(suite -> editTest(suite, testCase, (text, test) -> {
            final @NotNull String removed = RobotSuiteText.testText(text, test);
            Services.getInstance(project, KeptTestCode.class).keep(testCase, removed);
            RemovedTestCodeFile.toTheBin(project, suite.getNameWithoutExtension() + "." + RobotNames.fileNameOf(test.name()) + "." + RobotSuite.EXTENSION, removed);
            return RobotSuiteText.withoutTest(text, test);
        })));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-077
    static void move(final @NotNull Project project, final @NotNull MovedTestCase moved) {
        CodeCommand.inOne(project, CodeChangeType.MOVE_TEST_CASE.description(), () -> suiteHolding(project, moved.testCase()).ifPresent(source -> carry(project, moved, source)));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-077
    private static void carry(final @NotNull Project project, final @NotNull MovedTestCase moved, final @NotNull VirtualFile source) {
        final @NotNull TestCase testCase = moved.testCase();
        final @NotNull String text = RobotSuiteFiles.textOf(source);
        final @NotNull Optional<RobotTestBlock> test = RobotSuite.blockOf(text, testCase.getUuid());
        final @NotNull Optional<VirtualFile> target = RobotSuiteFiles.suiteOf(project, testCase.getParent())
                .filter(suite -> !suite.equals(source) && RobotSuite.blockOf(RobotSuiteFiles.textOf(suite), testCase.getUuid()).isEmpty());
        if (test.isEmpty() || target.isEmpty()) return;

        RobotSuiteFiles.edit(source, before -> RobotSuiteText.arranged(RobotSuiteText.withoutTest(before, test.orElseThrow()), orderOf(project, moved.from())));
        RobotSuiteFiles.edit(target.orElseThrow(), before -> RobotSuiteText.arranged(RobotSuiteText.withTest(before, RobotSuiteText.testText(text, test.orElseThrow())), orderOf(project, testCase.getParent())));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-078
    static void copy(final @NotNull Project project, final @NotNull CopiedTestCase copied) {
        CodeCommand.inOne(project, CodeChangeType.COPY_TEST_CASE.description(), () -> copied(project, copied));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-078
    private static void copied(final @NotNull Project project, final @NotNull CopiedTestCase copied) {
        final @NotNull TestCase copy = copied.copy();
        final @NotNull Optional<String> name = RobotNames.testNameOf(copy.getDescription());
        if (name.isEmpty()) return;

        final @NotNull String test = suiteHolding(project, copied.original())
                .map(RobotSuiteFiles::textOf)
                .flatMap(text -> RobotSuite.blockOf(text, copied.original().getUuid()).map(original -> RobotSuiteText.copyOf(RobotSuiteText.testText(text, original), name.orElseThrow(), tagsOf(copy))))
                .orElseGet(() -> RobotSuiteText.newTest(name.orElseThrow(), tagsOf(copy)));

        RobotSuiteFiles.suiteOf(project, copy.getParent()).ifPresent(suite ->
                RobotSuiteFiles.edit(suite, text -> RobotSuite.blockOf(text, copy.getUuid()).isPresent() ? text : RobotSuiteText.arranged(RobotSuiteText.withTest(text, test), orderOf(project, copy.getParent()))));
    }

    // UC-CODEGEN-003, UC-CODEGEN-010, Rule-CODEGEN-019, Rule-CODEGEN-021, Rule-CODEGEN-040
    static void describe(final @NotNull Project project, final @NotNull TestCase testCase) {
        CodeCommand.inOne(project, CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION.description(), () -> RobotNames.testNameOf(testCase.getDescription()).ifPresent(name ->
                suiteHolding(project, testCase).ifPresentOrElse(
                        suite -> editTest(suite, testCase, (text, test) -> RobotSuiteText.renamed(text, test, name)),
                        () -> written(project, testCase))));
    }

    // UC-CODEGEN-012, UC-CODEGEN-013, Rule-CODEGEN-123, Rule-CODEGEN-125
    static void retag(final @NotNull Project project, final @NotNull TestCase testCase, final @NotNull CodeChangeType<TestCase> change) {
        CodeCommand.inOne(project, change.description(), () -> suiteHolding(project, testCase).ifPresent(suite ->
                editTest(suite, testCase, (text, test) -> RobotSuiteText.retagged(text, test, tagsOf(testCase)))));
    }

    // UC-CODEGEN-011, Rule-CODEGEN-044, Rule-CODEGEN-126
    static void arrange(final @NotNull Project project, final @NotNull TestCase testCase) {
        CodeCommand.inOne(project, CodeChangeType.UPDATE_TEST_CASE_ORDER.description(), () -> suiteHolding(project, testCase).ifPresent(suite ->
                RobotSuiteFiles.edit(suite, text -> RobotSuiteText.arranged(text, orderOf(project, testCase.getParent())))));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-068
    static void reconcile(final @NotNull Project project, final @NotNull TestCase testCase) {
        CodeCommand.inOne(project, CodeChangeType.RECONCILE_TEST_CASE.description(), () -> suiteHolding(project, testCase).ifPresent(suite ->
                RobotSuiteFiles.edit(suite, text -> RobotSuiteText.arranged(rewritten(text, testCase), orderOf(project, testCase.getParent())))));
    }

    // Rule-CODEGEN-068, Rule-CODEGEN-127
    private static @NotNull String rewritten(final @NotNull String text, final @NotNull TestCase testCase) {
        return RobotSuite.blockOf(text, testCase.getUuid()).map(test -> {
            final @NotNull String retagged = RobotSuiteText.retagged(text, test, tagsOf(testCase));
            return RobotNames.testNameOf(testCase.getDescription()).map(name -> RobotSuiteText.renamed(retagged, test, name)).orElse(retagged);
        }).orElse(text);
    }
}
