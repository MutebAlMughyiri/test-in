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

package org.testin.java.codegen.clazz;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChange;
import org.testin.codegen.Fqcn;
import org.testin.codegen.TestSourceRoot;
import org.testin.java.codegen.GeneratedSourceFiles;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;

import java.util.List;

public class CreateJavaClass implements CodeChange<AbstractNode> {
    // UC-CODEGEN-001, UC-CODEGEN-020, Rule-CODEGEN-065
    @Override
    public void execute(final @NotNull Project project, final @NotNull AbstractNode node) {
        final @NotNull List<String> fqcn = Fqcn.ofClass(node);
        if (fqcn.isEmpty()) return;

        final @NotNull List<String> packageSegments = fqcn.subList(0, fqcn.size() - 1);
        final @NotNull String className = fqcn.getLast();

        TestinLogger.info("Ready to generate Test Class: " + className + " in package: " + fqcn);

        WriteAction.run(() -> TestSourceRoot.findOrWarn(project, className + ".java").ifPresent(root -> GeneratedSourceFiles.classFile(root, packageSegments, className)));
    }
}