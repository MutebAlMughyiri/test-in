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
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChange;
import org.testin.codegen.event.CopiedTestCase;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.java.codegen.method.update.BaseTestMethodChange;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;

import java.util.List;
import java.util.Optional;

// UC-CODEGEN-002, Rule-CODEGEN-078
public class CopyTestMethod extends BaseTestMethodChange implements CodeChange<CopiedTestCase> {
    @Override
    public void execute(final @NotNull Project project, final @NotNull CopiedTestCase copied) {
        executeAll(project, List.of(copied));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-078
    @Override
    public void executeAll(final @NotNull Project project, final @NotNull List<? extends CopiedTestCase> copies) {
        if (copies.isEmpty()) return;

        new CreateTestMethod().executeAll(project, copies.stream().map(CopiedTestCase::copy).toList());

        final @NotNull Optional<PsiClass> into = classOf(project, copies.getFirst().copy());
        if (into.isEmpty()) return;

        final @NotNull PsiClass target = into.orElseThrow();

        int carried = 0;
        for (final CopiedTestCase copied : copies) {
            if (carryBody(project, target, copied)) carried++;
        }

        if (carried > 0)
            TestinLogger.info("Carried " + carried + " test method body(s) into " + target.getQualifiedName());
    }

    private boolean carryBody(final @NotNull Project project, final @NotNull PsiClass target, final @NotNull CopiedTestCase copied) {
        final @NotNull TestCase original = copied.original();

        if (original.getParent().getPath().toString().isEmpty()) {
            TestinLogger.debug("Nothing to carry into the copy of '" + original.getDescription() + "': its test set is not known");
            return false;
        }

        final @NotNull Optional<PsiClass> from = classOf(project, original);
        if (from.isEmpty()) {
            TestinLogger.debug("Nothing to carry into the copy of '" + original.getDescription() + "': its test set has no class");
            return false;
        }

        final @NotNull Optional<PsiCodeBlock> body = GeneratedMethod.forTestCase(from.orElseThrow(), original)
                .map(PsiMethod::getBody);

        if (body.isEmpty()) {
            TestinLogger.debug("Nothing to carry into the copy of '" + original.getDescription() + "': it has no method with a body");
            return false;
        }

        final @NotNull Optional<PsiCodeBlock> written = GeneratedMethod.forTestCase(target, copied.copy())
                .map(PsiMethod::getBody);

        if (written.isEmpty()) {
            TestinLogger.debug("No method to fill for the copy of '" + original.getDescription() + "'");
            return false;
        }

        written.orElseThrow().replace(body.orElseThrow());
        return true;
    }
}
