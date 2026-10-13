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
import org.testin.importexport.shared.PreviewLoader;
import org.testin.model.TestCaseDto;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.ui.dialogs.CollapsiblePanel;
import org.testin.ui.dialogs.FormRows;
import org.testin.ui.framework.DialogComponent;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class SourceSection implements DialogComponent {
    private static final boolean EXPANDED = true;

    private final @NotNull SourceFileField source;
    private final @NotNull FileTypeHint hint;
    private final @NotNull JComponent panel;

    // UC-SHARE-005, Rule-INTERNAL-099
    public SourceSection(final @NotNull Project project, final @NotNull List<TestSetEditorAttributes> importAttributes, final @NotNull BiFunction<File, FileFormat, Map<String, List<TestCaseDto>>> importLoader, final @NotNull Consumer<@NotNull Map<String, List<TestCaseDto>>> onDataLoaded) {
        source = SourceFileField.of(project);
        hint = FileTypeHint.of(importAttributes);

        final @NotNull PreviewLoader preview = new PreviewLoader(source.field(), project, hint::showStatus, (format, parsedData) -> {
            hint.showFor(format);
            onDataLoaded.accept(parsedData);
        }, importLoader);
        source.field().onTextChanged(preview::pathChanged);

        // UC-SHARE-007, Rule-SHARE-036
        source.field().onTextChanged(() -> onDataLoaded.accept(Map.of()));

        panel = CollapsiblePanel.build(Bundle.message("import.section"), new FormRows()
                .wideRow(source.field().getPanel())
                .wideRow(hint.panel()), EXPANDED);
    }

    // UC-SHARE-005, Rule-SETTING-021
    public void selectSourceFile() {
        source.browse();
    }

    // UC-SHARE-005
    public @NotNull Optional<File> resolve() {
        return source.accepted();
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return source.field().getFocusComponent();
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }
}
