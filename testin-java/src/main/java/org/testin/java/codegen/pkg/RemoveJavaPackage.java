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

package org.testin.java.codegen.pkg;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChange;
import org.testin.codegen.Fqcn;
import org.testin.codegen.TestSourceRoot;
import org.testin.java.codegen.GeneratedSourceFiles;
import org.testin.model.node.AbstractNode;

public class RemoveJavaPackage implements CodeChange<AbstractNode> {
    // UC-CODEGEN-018, Rule-CODEGEN-059
    @Override
    public void execute(final @NotNull Project project, final @NotNull AbstractNode node) {
        Fqcn.packageFolderOf(node).ifPresent(packageFolder -> TestSourceRoot.writeInRoot(project, testSourceRoot ->
                GeneratedSourceFiles.removeUnder(project, testSourceRoot, packageFolder)));
    }
}
