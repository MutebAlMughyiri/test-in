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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.JavaSourceRoot;

import java.util.List;
import java.util.Optional;

// UC-CODEGEN-001, Rule-CODEGEN-009, Rule-CODEGEN-010
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GeneratedClass {
    public static @NotNull Optional<PsiClass> find(final @NotNull Project project, final @NotNull List<String> classFqcn) {
        if (classFqcn.isEmpty()) return Optional.empty();

        return byName(project, String.join(".", classFqcn));
    }

    // UC-CODEGEN-002, UC-CODEGEN-020, Rule-CODEGEN-010, Rule-CODEGEN-064
    public static @NotNull Optional<PsiClass> findOrWrite(final @NotNull Project project, final @NotNull List<String> packageList, final @NotNull String className) {
        final @NotNull String path = packageList.isEmpty() ? className : String.join(".", packageList) + "." + className;

        final @NotNull Optional<PsiClass> existing = byName(project, path);
        if (existing.isPresent()) return existing;

        JavaSourceRoot.findOrWarn(project, className).flatMap(root -> GeneratedSourceFiles.classFile(root, packageList, className))
                .ifPresent(written -> commit(project, written));

        return byName(project, path);
    }

    public static @NotNull Optional<PsiClass> byName(final @NotNull Project project, final @NotNull String path) {
        return Optional.ofNullable(JavaPsiFacade.getInstance(project).findClass(path, GlobalSearchScope.projectScope(project)));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-011
    private static void commit(final @NotNull Project project, final @NotNull VirtualFile written) {
        final @NotNull PsiDocumentManager documents = PsiDocumentManager.getInstance(project);

        Optional.ofNullable(PsiManager.getInstance(project).findFile(written))
                .map(documents::getDocument)
                .ifPresent(documents::commitDocument);
    }
}
