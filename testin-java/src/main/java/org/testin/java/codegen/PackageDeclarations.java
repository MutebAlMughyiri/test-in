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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PackageDeclarations {
    // UC-CODEGEN-017, Rule-CODEGEN-056, Rule-CODEGEN-057
    public static void retarget(final @NotNull Project p, final @NotNull VirtualFile sourceRoot, final @NotNull VirtualFile moved) {
        VfsUtilCore.visitChildrenRecursively(moved, new VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(final @NotNull VirtualFile file) {
                if (!file.isDirectory() && "java".equals(file.getExtension()))
                    retarget(p, sourceRoot, file, file.getParent());

                return true;
            }
        });
    }

    // UC-CODEGEN-016, Rule-CODEGEN-053
    public static void retarget(final @NotNull Project p, final @NotNull VirtualFile sourceRoot, final @NotNull VirtualFile file, final @NotNull VirtualFile holder) {
        if (!(PsiManager.getInstance(p).findFile(file) instanceof PsiJavaFile javaFile)) return;

        final @NotNull String declared = packageOf(sourceRoot, holder);
        if (declared.equals(javaFile.getPackageName())) return;

        javaFile.setPackageName(declared);
        Logger.info("Package of " + file.getName() + " is now: " + (declared.isEmpty() ? "<default>" : declared));
    }

    public static @NotNull String packageOf(final @NotNull VirtualFile sourceRoot, final @NotNull VirtualFile dir) {
        final @NotNull String relative = Objects.requireNonNullElse(
                VfsUtil.getRelativePath(dir, sourceRoot, '/'), "");

        return relative.replace('/', '.');
    }
}
