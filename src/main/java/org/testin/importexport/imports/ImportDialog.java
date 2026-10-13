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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileFormat;
import org.testin.importexport.shared.SheetPreview;
import org.testin.model.TestCaseDto;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogSize;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class ImportDialog extends AbstractFrameworkDialog {
    private final @NotNull SourceSection form;

    private final @NotNull Consumer<@NotNull Map<String, List<TestCaseDto>>> onImport;

    private final @NotNull SheetPreview preview;

    public ImportDialog(final @NotNull Project project, final @NotNull List<TestSetEditorAttributes> importAttributes, final @NotNull BiFunction<File, FileFormat, Map<String, List<TestCaseDto>>> importLoader, final @NotNull Consumer<@NotNull Map<String, List<TestCaseDto>>> onImport) {
        super(project);
        this.onImport = onImport;

        title = Bundle.message("dialog.import.title");

        preview = new SheetPreview(project, importAttributes);
        form = new SourceSection(project, importAttributes, importLoader, preview::show);

        components = List.of(
                DialogPart.of(form),
                DialogPart.of(preview),
                DialogPart.button(ImportAction.NAME));

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, ImportAction.NAME, this::submit),
                StatusBarShortcut.cancel(this::closeCancel));

        size = DialogSize.HALF;

        form.selectSourceFile();
    }

    // UC-SHARE-007
    @Override
    protected void submit() {
        if (form.resolve().isEmpty()) return;

        if (preview.isEmpty()) {
            notifier.softRefuse(project, Bundle.message("notification.import.empty.title"), Bundle.message("notification.import.empty.nothing.loaded"));
            return;
        }

        final @NotNull Map<String, List<TestCaseDto>> selected = preview.selected();
        if (selected.isEmpty()) {
            notifier.softRefuse(project, Bundle.message("notification.import.empty.title"), Bundle.message("notification.import.empty.none.selected"));
            return;
        }

        closeOk();
        onImport.accept(selected);
    }
}
