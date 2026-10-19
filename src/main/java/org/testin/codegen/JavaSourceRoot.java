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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.testin.help.Hints;
import org.testin.help.SetupHint;
import org.testin.help.SetupStep;
import org.testin.logger.TestinLogger;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.FromContentModule;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    @FromContentModule
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


    // UC-CODEGEN-018, Rule-CODEGEN-113
    public static @NotNull Map<String, byte[]> contentUnder(final @NotNull Project project, final @NotNull String relativePath) {
        final @NotNull Map<String, byte[]> files = new LinkedHashMap<>();
        find(project).ifPresent(root -> under(root, relativePath).ifPresent(found -> VfsUtilCore.iterateChildrenRecursively(found, VirtualFileFilter.ALL, file -> {
            if (!file.isDirectory())
                Optional.ofNullable(VfsUtilCore.getRelativePath(file, root, '/')).ifPresent(path -> read(file).ifPresent(content -> files.put(path, content)));
            return true;
        })));
        return files;
    }

    // UC-CODEGEN-018, Rule-CODEGEN-113
    public static void putBack(final @NotNull Project project, final @NotNull Map<String, byte[]> files) {
        if (files.isEmpty()) return;

        writeInRoot(project, root -> files.forEach((relativePath, content) -> putBack(root, relativePath, content)));
    }

    private static void putBack(final @NotNull VirtualFile root, final @NotNull String relativePath, final byte @NotNull [] content) {
        if (under(root, relativePath).isPresent()) {
            TestinLogger.info("Left the generated code at " + relativePath + " as it is: something is there already");
            return;
        }

        final int slash = relativePath.lastIndexOf('/');
        packageFolder(root, slash < 0 ? List.of() : List.of(relativePath.substring(0, slash))).ifPresent(folder -> {
            try {
                folder.createChildData(JavaSourceRoot.class, relativePath.substring(slash + 1)).setBinaryContent(content);
                TestinLogger.info("Put the generated code back at: " + relativePath);
            } catch (final IOException ex) {
                TestinLogger.error("Could not put the generated code back at " + relativePath + ": " + FailureText.of(ex));
            }
        });
    }

    private static @NotNull Optional<byte[]> read(final @NotNull VirtualFile file) {
        try {
            return Optional.of(file.contentsToByteArray());
        } catch (final IOException ex) {
            TestinLogger.warn("Could not keep the generated code at " + file.getPath() + " for Ctrl+Z: " + FailureText.of(ex));
            return Optional.empty();
        }
    }

    // UC-CODEGEN-001, Rule-CODEGEN-008
    public static @NotNull Optional<VirtualFile> packageFolder(final @NotNull VirtualFile root, final @NotNull List<String> packageSegments) {
        final @NotNull String relative = Fqcn.folderOf(packageSegments);

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


    // UC-CODEGEN-020, Rule-CODEGEN-065
    @FromContentModule
    public static void writeInRoot(final @NotNull Project project, final @NotNull RootWork work) {
        WriteAction.run(() -> find(project).ifPresent(work::run));
    }


    @FunctionalInterface
    public interface RootWork {
        void run(final @NotNull VirtualFile root);
    }

}
