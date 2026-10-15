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

package org.testin.creator;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeType;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NodeCreators {
    // UC-TREE-PANEL-002
    public static @NotNull NodeCreator of(final @NotNull Project project, final @NotNull NodeType type) {
        return switch (type) {
            case TEST_PROJECT, TEST_CASES_FOLDER, TEST_RUNS_FOLDER -> new NotCreatableFromTree(type.getDescription());
            case TEST_SET_PACKAGE -> new CreateTestSetPackage(project);
            case TEST_RUN_PACKAGE -> new CreateTestRunPackage(project);
            case TEST_SET -> new CreateTestSet(project);
            case TEST_RUN -> new CreateTestRun(project);
        };
    }
}
