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

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.JavaSourceRoot;
import org.testin.codegen.event.Renamed;
import org.testin.java.codegen.PackageDeclarations;
import org.testin.logger.TestinLogger;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class RenameJavaPackage implements GenAction<Renamed> {
    // UC-CODEGEN-017, Rule-CODEGEN-056
    @Override
    public void execute(final @NotNull Project p, final @NotNull Renamed renamed) {
        final @NotNull List<String> fqcn = Fqcn.ofPackage(renamed.from());

        JavaSourceRoot.find(p).ifPresentOrElse(
                testSourceRoot -> renameUnder(p, testSourceRoot, fqcn, renamed.newPackage()),
                () -> TestinLogger.info("Could not find Test Source Root in the project modules."));
    }

    private void renameUnder(final @NotNull Project p, final @NotNull VirtualFile testSourceRoot, final @NotNull List<String> fqcn, final @NotNull String newTop) {
        final @NotNull Optional<VirtualFile> found = JavaSourceRoot.under(testSourceRoot, String.join("/", fqcn))
                .filter(VirtualFile::isDirectory);
        if (found.isEmpty()) {
            TestinLogger.info("Package not found for rename: " + String.join(".", fqcn));
            return;
        }

        final @NotNull VirtualFile pkgDir = found.orElseThrow();

        WriteCommandAction.runWriteCommandAction(p, Bundle.message("codegen.rename.package"), null, () -> {
            try {
                pkgDir.rename(this, newTop);

                PackageDeclarations.retarget(p, testSourceRoot, pkgDir);
                TestinLogger.info("Package renamed to: " + newTop);
            } catch (final IOException ex) {
                TestinLogger.info("Error renaming package: " + FailureText.of(ex));
            }
        });
    }
}
