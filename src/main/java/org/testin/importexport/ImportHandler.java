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
import org.testin.model.TestCaseDto;

import java.io.File;
import java.util.List;
import java.util.Map;

@FunctionalInterface
@Immutable
public interface ImportHandler {
    ImportHandler UNSUPPORTED = (_, _) -> {
        throw new IllegalStateException("This format cannot be imported from");
    };

    @NotNull Map<String, List<TestCaseDto>> execute(final @NotNull Project p, final @NotNull File importFile);
}
