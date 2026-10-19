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
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChange;
import org.testin.codegen.CodeChangeType;
import org.testin.codegen.Fqcn;
import org.testin.codegen.JavaSourceRoot;
import org.testin.codegen.event.MovedNode;
import org.testin.java.codegen.GeneratedSourceFiles;
import org.testin.java.codegen.PackageDeclarations;
import org.testin.logger.TestinLogger;

import java.util.List;
import java.util.Optional;

public class MoveJavaClass implements CodeChange<MovedNode> {
    // UC-CODEGEN-016, Rule-CODEGEN-053, Rule-CODEGEN-054
    @Override
    public void execute(final @NotNull Project project, final @NotNull MovedNode moved) {
        final @NotNull List<String> fqcn = Fqcn.ofClass(moved.from());
        if (fqcn.isEmpty()) return;

        final @NotNull Optional<List<String>> found = moved.destinationPackage(project);
        if (found.isEmpty()) {
            moved.reportCodeLeftBehind(project, String.join(".", fqcn));
            return;
        }

        final @NotNull List<String> destination = found.get();
        final @NotNull String fileName = fqcn.getLast() + ".java";

        GeneratedSourceFiles.commandInRoot(project, CodeChangeType.MOVE_TEST_SET.description(), sourceRoot -> {
            final @NotNull Optional<VirtualFile> file =
                    Optional.ofNullable(sourceRoot.findFileByRelativePath(String.join("/", fqcn) + ".java"));

            if (file.isEmpty()) {
                TestinLogger.info("Class not found for move: " + String.join(".", fqcn));
                return;
            }

            final @NotNull Optional<VirtualFile> target = JavaSourceRoot.packageFolder(sourceRoot, destination);

            if (target.isEmpty() || target.get().equals(file.get().getParent())) return;

            if (GeneratedSourceFiles.move(file.get(), target.get(), this)) {
                PackageDeclarations.retarget(project, sourceRoot, file.get(), target.get());
                TestinLogger.info("Moved " + fileName + " to package: " + String.join(".", destination));
            }
        });
    }
}
