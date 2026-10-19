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

package org.testin.java.codegen;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.JavaSourceRoot;
import org.testin.indexer.recyclebin.RecycleBin;
import org.testin.logger.TestinLogger;
import org.testin.util.FailureText;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GeneratedSourceFiles {
    // UC-CODEGEN-018, Rule-CODEGEN-059, Rule-CODEGEN-060, Rule-INTERNAL-143
    public static void removeUnder(final @NotNull Project project, final @NotNull VirtualFile root, final @NotNull String relativePath) {
        final @NotNull Optional<VirtualFile> found = JavaSourceRoot.under(root, relativePath).filter(VirtualFile::exists);
        if (found.isEmpty()) return;

        final @NotNull VirtualFile target = found.orElseThrow();
        if (RecycleBin.took(project, target)) TestinLogger.info("Removed generated code at: " + target.getPath());
    }

    // UC-CODEGEN-001, Rule-CODEGEN-009, Rule-CODEGEN-010
    public static @NotNull Optional<VirtualFile> classFile(final @NotNull VirtualFile root, final @NotNull List<String> packageSegments, final @NotNull String className) {
        final @NotNull Optional<VirtualFile> folder = JavaSourceRoot.packageFolder(root, packageSegments);
        if (folder.isEmpty()) return Optional.empty();

        final @NotNull String fileName = className + ".java";
        final @NotNull Optional<VirtualFile> existing = Optional.ofNullable(folder.get().findChild(fileName));

        if (existing.isPresent()) {
            TestinLogger.info("Test class already exists: " + existing.get().getPath());
            return existing;
        }

        final @NotNull String packageName = String.join(".", packageSegments);
        final @NotNull String declaration = packageName.isEmpty() ? "" : """
                package %s;
                
                """.formatted(packageName);

        try {
            final @NotNull VirtualFile file = folder.get().createChildData(GeneratedSourceFiles.class, fileName);
            VfsUtil.saveText(file, declaration + """
                    public class %s {
                    }
                    """.formatted(className));

            TestinLogger.info("Test class created at: " + file.getPath());
            return Optional.of(file);
        } catch (final IOException ex) {
            TestinLogger.error("Could not create the test class " + fileName + ": " + FailureText.of(ex));
            return Optional.empty();
        }
    }

    // UC-CODEGEN-016, Rule-CODEGEN-053
    public static boolean move(final @NotNull VirtualFile file, final @NotNull VirtualFile target, final @NotNull Object requestor) {
        try {
            file.move(requestor, target);
            return true;
        } catch (final IOException ex) {
            TestinLogger.error("Could not move generated code at " + file.getPath() + " to " + target.getPath() + ": " + FailureText.of(ex));
            return false;
        }
    }

    public static void commandInRoot(final @NotNull Project project, final @NotNull String title, final @NotNull JavaSourceRoot.RootWork work) {
        WriteCommandAction.runWriteCommandAction(project, title, null, () -> JavaSourceRoot.find(project).ifPresent(work::run));
    }
}
