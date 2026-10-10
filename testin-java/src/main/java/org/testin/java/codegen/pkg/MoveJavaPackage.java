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
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.GenType;
import org.testin.codegen.JavaSourceRoot;
import org.testin.codegen.event.Moved;
import org.testin.java.codegen.PackageDeclarations;
import org.testin.logger.Logger;

import java.util.List;
import java.util.Optional;

public class MoveJavaPackage implements GenAction<Moved> {
    // UC-CODEGEN-017, Rule-CODEGEN-057, Rule-CODEGEN-058
    @Override
    public void execute(final @NotNull Project p, final @NotNull Moved moved) {
        final @NotNull List<String> fqcn = Fqcn.ofPackage(moved.from());

        final @NotNull Optional<List<String>> destinationFound = moved.destinationPackage(p);
        if (destinationFound.isEmpty()) {
            moved.reportCodeLeftBehind(p, "package " + String.join(".", fqcn));
            return;
        }

        final @NotNull List<String> destination = destinationFound.get();

        JavaSourceRoot.commandInRoot(p, GenType.MOVE_TEST_SET_PACKAGE.description(), sourceRoot -> {
            final @NotNull Optional<VirtualFile> found = Optional.ofNullable(sourceRoot.findFileByRelativePath(String.join("/", fqcn)))
                    .filter(VirtualFile::isDirectory);

            if (found.isEmpty()) {
                Logger.info("Package not found for move: " + String.join(".", fqcn));
                return;
            }

            final @NotNull VirtualFile folder = found.get();
            final @NotNull Optional<VirtualFile> target = JavaSourceRoot.packageFolder(sourceRoot, destination);

            if (target.isEmpty()
                    || target.get().equals(folder.getParent())
                    || VfsUtil.isAncestor(folder, target.get(), false)) return;

            if (!JavaSourceRoot.move(folder, target.get(), this)) return;

            PackageDeclarations.retarget(p, sourceRoot, folder);

            Logger.info("Moved package " + folder.getName() + " into: " + String.join(".", destination));
        });
    }
}
