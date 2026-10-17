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

package org.testin.importexport;

import com.google.errorprone.annotations.Immutable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItems;

@FunctionalInterface
@Immutable
public interface ReportHandler {
    ReportHandler UNSUPPORTED = (_, _, _) -> {
        throw new IllegalStateException("This format has no report generator");
    };

    byte @NotNull [] execute(final @NotNull Project project, final @NotNull TestRunNode testRunNode, final @NotNull RunItems runItems);
}
