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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.testin.navigate.AbstractTestCaseGutter;
import org.testin.navigate.TestCaseIdentity;
import org.testin.util.Bundle;

import java.util.List;

public class TestMethodGutter extends AbstractTestCaseGutter {
    // UC-CODEGEN-007, Rule-CODEGEN-028, Rule-CODEGEN-029, Rule-CODEGEN-118
    @Override
    protected @NotNull List<TestCaseIdentity> testCasesIn(final @NotNull PsiElement element) {
        if (element.getFirstChild() != null || !RobotSuite.mayHoldTestCases(element.getContainingFile().getName(), element.getText()))
            return List.of();

        return RobotSuite.testCasesIn(element.getContainingFile().getText(), element.getTextRange());
    }

    // UC-CODEGEN-007, Rule-CODEGEN-069
    @Override
    protected @NotNull String codeName(final @NotNull PsiFile file, final int offset) {
        return RobotSuite.testNameAbove(file.getText(), offset).orElseGet(() -> Bundle.message("gutter.this.test"));
    }
}
