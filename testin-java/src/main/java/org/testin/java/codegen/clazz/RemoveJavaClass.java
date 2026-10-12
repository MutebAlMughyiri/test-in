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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.JavaSourceRoot;
import org.testin.model.node.AbstractNode;

import java.util.List;

public class RemoveJavaClass implements GenAction<AbstractNode> {
    // UC-CODEGEN-018, Rule-CODEGEN-059
    @Override
    public void execute(final @NotNull Project p, final @NotNull AbstractNode dir) {
        final @NotNull List<String> fqcn = Fqcn.ofClass(dir);
        if (fqcn.isEmpty()) return;

        final @NotNull String packagePath = String.join("/", fqcn.subList(0, fqcn.size() - 1));
        final @NotNull String className = fqcn.getLast();
        final @NotNull String fileName = className + ".java";

        JavaSourceRoot.writeInRoot(p, testSourceRoot ->
                JavaSourceRoot.deleteUnder(testSourceRoot, packagePath + "/" + fileName, this));
    }
}
