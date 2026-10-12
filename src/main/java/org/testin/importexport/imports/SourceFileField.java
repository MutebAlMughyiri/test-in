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

package org.testin.importexport.imports;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileFormat;
import org.testin.ui.dialogs.DownloadFolder;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;

import java.io.File;
import java.util.Optional;

public record SourceFileField(@NotNull TextInput field) {
    // Rule-INTERNAL-095, Rule-INTERNAL-096, Rule-INTERNAL-118, Rule-SETTING-021
    public static @NotNull SourceFileField of(final @NotNull Project p) {
        final @NotNull FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withExtensionFilter("", FileFormat.importableExtensionsForChooser())
                .withTitle(Bundle.message("import.file.title"))
                .withDescription(Bundle.message("import.file.description"));

        return new SourceFileField(ComponentDialogBase.textField()
                .caption(Bundle.message("import.caption.source"))
                .placeholder(Bundle.message("import.choose.file"))
                .value(DownloadFolder.of(p))
                .browse(p, descriptor)
                .build().getComponent());
    }

    // UC-SHARE-005, Rule-SETTING-021
    public void browse() {
        ApplicationManager.getApplication().invokeLater(field::browseNow);
    }

    // UC-SHARE-005, Rule-INTERNAL-067
    public @NotNull Optional<File> accepted() {
        final @NotNull String path = field.accepted();
        return path.isEmpty() ? Optional.empty() : Optional.of(new File(path));
    }
}
