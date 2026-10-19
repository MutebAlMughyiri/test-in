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

package org.testin.java.codegen.method;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.recyclebin.RecycleBin;
import org.testin.logger.TestinLogger;
import org.testin.util.FailureText;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RemovedTestMethodFile {
    // UC-CODEGEN-014, Rule-INTERNAL-143
    static void toTheBin(final @NotNull Project project, final @NotNull PsiMethod testMethod) {
        final @NotNull String fileName = Optional.ofNullable(testMethod.getContainingClass()).map(PsiClass::getName).map(className -> className + ".").orElse("") + testMethod.getName() + ".java";
        final @NotNull String text = testMethod.getText();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final @NotNull Path folder = Files.createTempDirectory("testin-removed-method");
                if (RecycleBin.took(project, Files.writeString(folder.resolve(fileName), text)))
                    Files.deleteIfExists(folder);
            } catch (final IOException ex) {
                TestinLogger.error("Could not keep the removed method " + fileName + " for the recycle bin: " + FailureText.of(ex));
            }
        });
    }
}
