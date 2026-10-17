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

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChange;
import org.testin.codegen.CodeChangeType;
import org.testin.codegen.Fqcn;
import org.testin.codegen.event.MovedTestCase;
import org.testin.java.codegen.GeneratedClass;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.java.codegen.method.update.BaseTestMethodChange;
import org.testin.java.codegen.method.update.UpdateTestMethodOrder;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// UC-CODEGEN-002, Rule-CODEGEN-077
public class MoveTestMethod extends BaseTestMethodChange implements CodeChange<MovedTestCase> {

    // UC-CODEGEN-002, Rule-CODEGEN-077
    @Override
    public void execute(final @NotNull Project project, final @NotNull MovedTestCase moved) {
        executeAll(project, List.of(moved));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-077, Rule-CODEGEN-014
    @Override
    public void executeAll(final @NotNull Project project, final @NotNull List<? extends MovedTestCase> moves) {
        if (moves.isEmpty()) return;

        asOneCommand(project, CodeChangeType.MOVE_TEST_CASE.description(), () -> {
            moves.forEach(moved -> move(project, moved));
            new UpdateTestMethodOrder().arrangeSets(project, moves.stream().flatMap(moved -> Stream.of(moved.testCase().getParent().getPath(), moved.from().getPath())).toList());
        });
    }

    private void move(final @NotNull Project project, final @NotNull MovedTestCase moved) {
        final @NotNull TestCase testCase = moved.testCase();

        final @NotNull Optional<PsiClass> from = GeneratedClass.find(project, Fqcn.ofClass(moved.from()));
        if (from.isEmpty()) {
            TestinLogger.debug("Nothing to move for '" + testCase.getDescription() + "': the test set it came from has no class");
            return;
        }

        final @NotNull Optional<PsiMethod> method = GeneratedMethod.forTestCase(from.orElseThrow(), testCase);
        if (method.isEmpty()) {
            TestinLogger.debug("Nothing to move for '" + testCase.getDescription() + "': no method with testName=" + testCase.getUuid());
            return;
        }

        final @NotNull List<String> destination = Fqcn.ofClass(testCase.getParent());

        final @NotNull Optional<PsiClass> into = destination.isEmpty()
                ? Optional.empty()
                : GeneratedClass.findOrWrite(project, destination.subList(0, destination.size() - 1), destination.getLast());

        if (into.isEmpty()) {
            TestinLogger.warn("Left the method for '" + testCase.getDescription() + "' where it was: "
                    + "the test set it moved into has no class to put it in");
            return;
        }

        final @NotNull PsiClass target = into.orElseThrow();

        if (target.equals(from.orElseThrow())) {
            TestinLogger.debug("The method for '" + testCase.getDescription() + "' is already in the right class");
            return;
        }

        if (GeneratedMethod.forTestCase(target, testCase).isPresent()) {
            TestinLogger.warn("Left the method for '" + testCase.getDescription() + "' in " + from.orElseThrow().getQualifiedName()
                    + ": " + target.getQualifiedName() + " already has a method for this test case");
            return;
        }

        GeneratedMethod.importTest(project, target.getContainingFile());

        target.add(method.orElseThrow());
        method.orElseThrow().delete();

        TestinLogger.info("Moved the method for '" + testCase.getDescription() + "' into " + target.getName());
    }

}
