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

package org.testin.codegen;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;

@AllArgsConstructor
public final class NoCodeChange<T> implements CodeChange<T> {
    private final @NotNull String nodeType;

    // UC-CODEGEN-004
    @Override
    public void execute(final @NotNull Project p, final @NotNull T payload) {
        TestinLogger.debug(nodeType + " generates no Java code");
    }

    @Override
    public boolean generates() {
        return false;
    }
}
