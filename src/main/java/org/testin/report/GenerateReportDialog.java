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

package org.testin.report;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileFormat;
import org.testin.ui.dialogs.DestinationForm;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public final class GenerateReportDialog extends AbstractFrameworkDialog {
    private final @NotNull DestinationForm form;

    private final @NotNull BiConsumer<@NotNull FileFormat, @NotNull File> onGenerate;

    // UC-REPORT-001
    public GenerateReportDialog(final @NotNull Project project, final @NotNull String suggestedFileName, final @NotNull BiConsumer<@NotNull FileFormat, @NotNull File> onGenerate) {
        super(project);
        this.onGenerate = onGenerate;

        title = Bundle.message("dialog.report.title");

        form = DestinationForm.stacked(project,
                Arrays.stream(FileFormat.values()).filter(FileFormat::isReportable).toArray(FileFormat[]::new),
                FileFormat.PDF,
                suggestedFileName,
                Bundle.message("dialog.report.folder.title"),
                Bundle.message("dialog.report.folder.message"));

        components = List.of(
                DialogPart.of(form),
                DialogPart.button(Bundle.message("dialog.report.button.generate")));

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, Bundle.message("dialog.report.button.generate"), this::submit),
                StatusBarShortcut.cancel(this::closeCancel));
    }

    // UC-REPORT-001, Rule-REPORT-009
    @Override
    protected void submit() {
        form.resolve().ifPresent(destination -> {
            closeOk();
            onGenerate.accept(destination.format(), destination.file());
        });
    }
}
