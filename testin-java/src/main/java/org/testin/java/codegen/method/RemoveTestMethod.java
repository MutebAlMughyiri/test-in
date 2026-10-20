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
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChange;
import org.testin.codegen.CodeChangeType;
import org.testin.codegen.writer.CodeCommand;
import org.testin.codegen.writer.KeptTestCode;
import org.testin.codegen.writer.RemovedTestCodeFile;
import org.testin.java.codegen.method.update.BaseTestMethodChange;
import org.testin.java.codegen.method.update.UpdateTestMethodOrder;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.services.Services;

import java.util.List;
import java.util.Optional;

public class RemoveTestMethod extends BaseTestMethodChange implements CodeChange<TestCase> {
    // UC-CODEGEN-014, Rule-CODEGEN-049, Rule-CODEGEN-050, Rule-CODEGEN-114, Rule-INTERNAL-143
    @Override
    public void execute(final @NotNull Project project, final @NotNull TestCase testCase) {
        applyToMethod(project, testCase, "Remove Test Method", testMethod -> {
            final @NotNull String name = testMethod.getName();
            final @NotNull String before = testMethod.getPrevSibling() instanceof PsiWhiteSpace space ? space.getText() : "";
            Services.getInstance(project, KeptTestCode.class).keep(testCase, before.substring(before.lastIndexOf('\n') + 1) + testMethod.getText());
            RemovedTestCodeFile.toTheBin(project, Optional.ofNullable(testMethod.getContainingClass()).map(PsiClass::getName).map(className -> className + ".").orElse("") + name + ".java", testMethod.getText());
            testMethod.delete();
            TestinLogger.info("Removed test method: " + name);
        }, detail -> TestinLogger.debug("No generated method for '" + testCase.getDescription() + "': " + detail));
    }

    // UC-CODEGEN-014, Rule-CODEGEN-049, Rule-CODEGEN-014
    @Override
    public void executeAll(final @NotNull Project project, final @NotNull List<? extends TestCase> testCases) {
        if (testCases.isEmpty()) return;

        CodeCommand.inOne(project, CodeChangeType.REMOVE_TEST_CASE.description(), () -> {
            testCases.forEach(testCase -> execute(project, testCase));
            new UpdateTestMethodOrder().executeAll(project, testCases);
        });
    }
}
