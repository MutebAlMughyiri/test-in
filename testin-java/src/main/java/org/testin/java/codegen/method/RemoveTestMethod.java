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
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChange;
import org.testin.java.codegen.method.update.BaseTestMethodChange;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;

public class RemoveTestMethod extends BaseTestMethodChange implements CodeChange<TestCase> {
    // UC-CODEGEN-014, Rule-CODEGEN-049, Rule-CODEGEN-050
    @Override
    public void execute(final @NotNull Project project, final @NotNull TestCase testCase) {
        applyToMethod(project, testCase, "Remove Test Method", testMethod -> {
            final @NotNull String name = testMethod.getName();
            testMethod.delete();
            TestinLogger.info("Removed test method: " + name);
        }, detail -> TestinLogger.debug("No generated method for '" + testCase.getDescription() + "': " + detail));
    }
}
