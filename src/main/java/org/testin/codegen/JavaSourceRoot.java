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

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.testin.help.SetupHint;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.indexer.recyclebin.RecycleBin;
import org.testin.logger.TestinLogger;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.FromContentModule;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JavaSourceRoot {

    // UC-CODEGEN-020, Rule-CODEGEN-064, Rule-CODEGEN-066
    public static @NotNull Optional<VirtualFile> find(final @NotNull Project project) {
        final @NotNull RememberedTestSourceRoot remembered = Services.getInstance(project, RememberedTestSourceRoot.class);

        final @NotNull Optional<VirtualFile> cached = remembered.get();
        if (cached.isPresent()) return cached;

        for (final Module module : ModuleManager.getInstance(project).getModules()) {
            final @NotNull List<VirtualFile> sourceRoots = ModuleRootManager.getInstance(module)
                    .getSourceRoots(JavaSourceRootType.TEST_SOURCE);

            if (!sourceRoots.isEmpty()) {
                TestinLogger.debug("Found test source root: " + sourceRoots.getFirst());
                remembered.set(sourceRoots.getFirst());
                return Optional.of(sourceRoots.getFirst());
            }
        }

        TestinLogger.warn("No Java test source root found in the project.");
        return Optional.empty();
    }

    // UC-CODEGEN-020, Rule-CODEGEN-065, Rule-CODEGEN-072, Rule-INTERNAL-127
    public static @NotNull Optional<VirtualFile> findOrWarn(final @NotNull Project project, final @NotNull String className) {
        final @NotNull Optional<VirtualFile> root = find(project);
        final @NotNull Hints hints = Services.getInstance(project, Hints.class);

        root.ifPresentOrElse(_ -> hints.clear(SetupStep.TEST_SOURCE_FOLDER),
                () -> hints.fire(SetupHint.of(SetupStep.TEST_SOURCE_FOLDER, Bundle.message("codegen.no.source.root.message", className))));
        return root;
    }

    public static @NotNull Optional<VirtualFile> under(final @NotNull VirtualFile root, final @NotNull String relativePath) {
        return Optional.ofNullable(root.findFileByRelativePath(relativePath));
    }

    // UC-CODEGEN-018, Rule-CODEGEN-059, Rule-CODEGEN-060, Rule-INTERNAL-143
    @FromContentModule
    public static void removeUnder(final @NotNull Project project, final @NotNull VirtualFile root, final @NotNull String relativePath) {
        final @NotNull Optional<VirtualFile> found = under(root, relativePath).filter(VirtualFile::exists);
        if (found.isEmpty()) return;

        final @NotNull VirtualFile target = found.orElseThrow();
        if (RecycleBin.took(project, target)) TestinLogger.info("Removed generated code at: " + target.getPath());
    }

    // UC-CODEGEN-001, Rule-CODEGEN-008
    public static @NotNull Optional<VirtualFile> packageFolder(final @NotNull VirtualFile root, final @NotNull List<String> packageSegments) {
        final @NotNull String relative = String.join("/", packageSegments);

        final @NotNull Optional<VirtualFile> folder;
        try {
            folder = Optional.ofNullable(VfsUtil.createDirectoryIfMissing(root, relative));
        } catch (final IOException ex) {
            TestinLogger.error("Could not create the package folder " + relative + ": " + FailureText.of(ex));
            return Optional.empty();
        }

        if (folder.isEmpty()) TestinLogger.error("Could not create the package folder: " + relative);
        return folder;
    }

    // UC-CODEGEN-001, Rule-CODEGEN-009, Rule-CODEGEN-010
    @FromContentModule
    public static @NotNull Optional<VirtualFile> classFile(final @NotNull VirtualFile root, final @NotNull List<String> packageSegments, final @NotNull String className) {
        final @NotNull Optional<VirtualFile> folder = packageFolder(root, packageSegments);
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
            final @NotNull VirtualFile file = folder.get().createChildData(JavaSourceRoot.class, fileName);
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
    @FromContentModule
    public static boolean move(final @NotNull VirtualFile file, final @NotNull VirtualFile target, final @NotNull Object requestor) {
        try {
            file.move(requestor, target);
            return true;
        } catch (final IOException ex) {
            TestinLogger.error("Could not move generated code at " + file.getPath() + " to " + target.getPath() + ": " + FailureText.of(ex));
            return false;
        }
    }

    // UC-CODEGEN-020, Rule-CODEGEN-064
    @FromContentModule
    public static @NotNull Optional<VirtualFile> fileInRootOrWarn(final @NotNull Project project, final @NotNull String className, final @NotNull RootFile work) {
        return findOrWarn(project, className).flatMap(work::from);
    }

    // UC-CODEGEN-020, Rule-CODEGEN-065
    @FromContentModule
    public static void writeInRoot(final @NotNull Project project, final @NotNull RootWork work) {
        WriteAction.run(() -> find(project).ifPresent(work::run));
    }

    @FromContentModule
    public static void commandInRoot(final @NotNull Project project, final @NotNull String title, final @NotNull RootWork work) {
        WriteCommandAction.runWriteCommandAction(project, title, null, () -> find(project).ifPresent(work::run));
    }

    // UC-CODEGEN-020, Rule-CODEGEN-065
    @FromContentModule
    public static void writeInRootOrWarn(final @NotNull Project project, final @NotNull String className, final @NotNull RootWork work) {
        WriteAction.run(() -> findOrWarn(project, className).ifPresent(work::run));
    }

    @FunctionalInterface
    public interface RootWork {
        void run(final @NotNull VirtualFile root);
    }

    @FunctionalInterface
    public interface RootFile {
        @NotNull Optional<VirtualFile> from(final @NotNull VirtualFile root);
    }
}
