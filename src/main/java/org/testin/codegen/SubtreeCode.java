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

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;
import org.testin.services.Services;
import org.testin.util.Bundle;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SubtreeCode {
    // UC-CODEGEN-004, Rule-CODEGEN-022, Rule-CODEGEN-024
    public static void generate(final @NotNull Project project, final @NotNull AbstractNode node) {
        WriteCommandAction.runWriteCommandAction(project, Bundle.message("codegen.generate.command"), null, () -> walk(project, node));
    }

    // UC-CODEGEN-004, Rule-CODEGEN-023
    private static void walk(final @NotNull Project project, final @NotNull AbstractNode node) {
        final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);
        final @NotNull Nodes nodes = Services.getInstance(project, Nodes.class);

        TestinLogger.info("Generating code for " + node.getName());
        NodeCode.of(node.getType()).getCreated().execute(project, node);

        GenType.CREATE_TEST_CASE.executeAllNow(project, testCases.getTestCasesForTestSet(node.getPath()));

        for (final AbstractNode child : nodes.getChildren(node.getPath())) {
            walk(project, child);
        }
    }
}
