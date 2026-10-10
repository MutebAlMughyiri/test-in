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
import org.testin.codegen.GenAction;
import org.testin.java.codegen.method.update.UpdateTestBase;
import org.testin.logger.Logger;
import org.testin.model.TestCaseDto;

public class RemoveTestMethod extends UpdateTestBase implements GenAction<TestCaseDto> {
    // UC-CODEGEN-014, Rule-CODEGEN-049, Rule-CODEGEN-050
    @Override
    public void execute(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        applyToMethod(p, tc, "Remove Test Method", pm -> {
            final @NotNull String name = pm.getName();
            pm.delete();
            Logger.info("Removed test method: " + name);
        }, detail -> Logger.debug("No generated method for '" + tc.getDescription() + "': " + detail));
    }
}
