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

package org.testin.importexport.exports;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileFormat;
import org.testin.importexport.shared.Destination;
import org.testin.importexport.shared.DestinationForm;
import org.testin.importexport.shared.SheetPreview;
import org.testin.model.TestCase;
import org.testin.testcase.TestSetEditorFields;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogSize;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class ExportDialog extends AbstractFrameworkDialog {
    private final @NotNull DestinationForm form;

    private final @NotNull SheetPreview preview;

    private final @NotNull BiConsumer<@NotNull Destination,
            @NotNull Map<String, List<TestCase>>> onExport;

    public ExportDialog(final @NotNull Project project, final @NotNull List<TestSetEditorFields> exportFields, final @NotNull Map<String, List<TestCase>> sheets, final @NotNull String exportName, final @NotNull BiConsumer<@NotNull Destination, @NotNull Map<String, List<TestCase>>> onExport) {
        super(project);
        this.onExport = onExport;

        title = Bundle.message("dialog.export.title");

        form = DestinationForm.inSection(project,
                Arrays.stream(FileFormat.values()).filter(FileFormat::isExportable).toArray(FileFormat[]::new),
                FileFormat.XLSX,
                exportName,
                Bundle.message("dialog.export.folder.title"),
                Bundle.message("dialog.export.folder.message"),
                Bundle.message("destination.section"));

        preview = new SheetPreview(project, exportFields);
        preview.show(sheets);

        components = List.of(
                DialogPart.of(form),
                DialogPart.of(preview),
                DialogPart.button(ExportAction.NAME));

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, ExportAction.NAME, this::submit),
                StatusBarShortcut.cancel(this::closeCancel));

        size = DialogSize.HALF;
    }

    // UC-SHARE-001
    @Override
    protected void submit() {
        form.resolve().ifPresent(destination -> {
            final @NotNull Map<String, List<TestCase>> selected = preview.selected();
            if (selected.isEmpty()) {
                notifier.softRefuse(project, Bundle.message("notification.export.empty.title"), Bundle.message("notification.export.empty.message"));
                return;
            }

            closeOk();
            onExport.accept(destination, selected);
        });
    }
}
