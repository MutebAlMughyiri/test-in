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

package org.testin.apimodel;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.ApiModelMaker;

public final class JavaApiModelMaker implements ApiModelMaker {
    // Rule-CODEGEN-098
    @Override
    public boolean canMakeIn(final @NotNull PsiDirectory directory) {
        return JavaDirectoryService.getInstance().getPackage(directory) != null;
    }

    // UC-CODEGEN-022
    @Override
    public void open(final @NotNull Project project, final @NotNull PsiDirectory directory) {
        new ApiModelDialog(project, directory).show();
    }
}
