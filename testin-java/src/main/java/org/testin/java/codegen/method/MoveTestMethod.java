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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.GenType;
import org.testin.codegen.event.MovedTestCase;
import org.testin.java.codegen.GeneratedClass;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.java.codegen.method.update.UpdateTestBase;
import org.testin.java.codegen.method.update.UpdateTestOrder;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// UC-CODEGEN-002, Rule-CODEGEN-077
public class MoveTestMethod extends UpdateTestBase implements GenAction<MovedTestCase> {

    // UC-CODEGEN-002, Rule-CODEGEN-077
    @Override
    public void execute(final @NotNull Project p, final @NotNull MovedTestCase moved) {
        executeAll(p, List.of(moved));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-077, Rule-CODEGEN-014
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<? extends MovedTestCase> moves) {
        if (moves.isEmpty()) return;

        final @NotNull Runnable inCommand = () ->
                WriteCommandAction.runWriteCommandAction(p, GenType.MOVE_TEST_CASE.description(), null, () -> {
                    final @NotNull Set<PsiClass> left = new LinkedHashSet<>();
                    moves.forEach(moved -> move(p, moved).ifPresent(left::add));
                    left.forEach(pc -> reformat(p, pc));

                    new UpdateTestOrder().executeAll(p, moves.stream().map(MovedTestCase::tc).toList());
                });

        if (CommandProcessor.getInstance().getCurrentCommand() != null) inCommand.run();
        else ApplicationManager.getApplication().invokeLater(inCommand);
    }

    private @NotNull Optional<PsiClass> move(final @NotNull Project p, final @NotNull MovedTestCase moved) {
        final @NotNull TestCaseDto tc = moved.tc();

        final @NotNull Optional<PsiClass> from = GeneratedClass.find(p, Fqcn.ofClass(moved.from()));
        if (from.isEmpty()) {
            TestinLogger.debug("Nothing to move for '" + tc.getDescription() + "': the test set it came from has no class");
            return Optional.empty();
        }

        final @NotNull Optional<PsiMethod> method = GeneratedMethod.forTestCase(from.orElseThrow(), tc);
        if (method.isEmpty()) {
            TestinLogger.debug("Nothing to move for '" + tc.getDescription() + "': no method with testName=" + tc.getId());
            return Optional.empty();
        }

        final @NotNull List<String> destination = Fqcn.ofClass(tc.getParent());

        final @NotNull Optional<PsiClass> into = destination.isEmpty()
                ? Optional.empty()
                : GeneratedClass.findOrWrite(p, destination.subList(0, destination.size() - 1), destination.getLast());

        if (into.isEmpty()) {
            TestinLogger.warn("Left the method for '" + tc.getDescription() + "' where it was: "
                    + "the test set it moved into has no class to put it in");
            return Optional.empty();
        }

        final @NotNull PsiClass target = into.orElseThrow();

        if (target.equals(from.orElseThrow())) {
            TestinLogger.debug("The method for '" + tc.getDescription() + "' is already in the right class");
            return Optional.empty();
        }

        if (GeneratedMethod.forTestCase(target, tc).isPresent()) {
            TestinLogger.warn("Left the method for '" + tc.getDescription() + "' in " + from.orElseThrow().getQualifiedName()
                    + ": " + target.getQualifiedName() + " already has a method for this test case");
            return Optional.empty();
        }

        GeneratedMethod.importTest(p, target.getContainingFile());

        final @NotNull PsiMethod carried = JavaPsiFacade.getElementFactory(p)
                .createMethodFromText(method.orElseThrow().getText(), target);

        target.add(carried);
        method.orElseThrow().delete();

        TestinLogger.info("Moved test method " + carried.getName() + " into " + target.getName());
        return from;
    }

}
