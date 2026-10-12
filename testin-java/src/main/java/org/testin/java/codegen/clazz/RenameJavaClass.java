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

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.GenType;
import org.testin.codegen.event.Renamed;
import org.testin.java.codegen.GeneratedClass;
import org.testin.logger.TestinLogger;
import org.testin.util.NameSanitizer;

import java.util.List;
import java.util.Optional;

public class RenameJavaClass implements GenAction<Renamed> {
    // UC-CODEGEN-015, Rule-CODEGEN-052
    @Override
    public void execute(final @NotNull Project p, final @NotNull Renamed renamed) {
        final @NotNull String newName = renamed.newName();
        final @NotNull List<String> fqcn = Fqcn.ofClass(renamed.from());
        if (fqcn.isEmpty()) return;
        final @NotNull String path = String.join(".", fqcn);

        WriteCommandAction.runWriteCommandAction(p, GenType.RENAME_TEST_SET.description(), null, () -> {
            final @NotNull Optional<PsiClass> found = GeneratedClass.byName(p, path);
            if (found.isEmpty()) {
                TestinLogger.warn("RenameJavaClass: class not found: " + path);
                return;
            }

            final @NotNull PsiClass targetClass = found.orElseThrow();
            final @NotNull String newClassName = NameSanitizer.className(newName);
            if (!newClassName.equals(targetClass.getName())) {
                targetClass.setName(newClassName);
            }
            TestinLogger.info("Renamed test class to: " + newClassName);
        });
    }
}
