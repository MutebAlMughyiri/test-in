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

package org.testin.ui.dialogs;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileFormat;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogComponent;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.io.File;
import java.util.Arrays;
import java.util.Optional;

public final class DestinationForm implements DialogComponent {
    private static final boolean EXPANDED = true;

    private final @NotNull TextInput folder;

    private final @NotNull TextInput fileName;

    private final @NotNull FormatSection format;

    private final @NotNull JComponent panel;

    private DestinationForm(final @NotNull Project project, final FileFormat @NotNull [] formats, final @NotNull FileFormat defaultFormat, final @NotNull String suggestedName, final @NotNull String chooserTitle, final @NotNull String chooserDescription, final @NotNull String section) {
        folder = DialogPart.textInput()
                .caption(Bundle.message("destination.caption.folder"))
                .placeholder(Bundle.message("destination.choose.folder"))
                .value(DownloadFolder.of(project))
                .browse(project, FileChooserDescriptorFactory.singleDir().withTitle(chooserTitle).withDescription(chooserDescription))
                .build().getComponent();

        fileName = DialogPart.textInput()
                .caption(Bundle.message("destination.caption.file"))
                .placeholder(Bundle.message("destination.name.the.file"))
                .value(suggestedName)
                .build().getComponent();
        format = FormatSection.of(formats, defaultFormat);

        final boolean roomy = !section.isEmpty();

        final @NotNull FormRows rows = roomy
                ? new FormRows().pair(folder.getPanel(), fileName.getPanel()).wideRow(format.panel())
                : new FormRows().wideRow(folder.getPanel()).wideRow(fileName.getPanel()).wideRow(format.panel());

        panel = roomy ? CollapsiblePanel.build(section, rows, EXPANDED) : DialogStyle.asSection(rows);
    }

    // UC-REPORT-001, Rule-INTERNAL-099
    public static @NotNull DestinationForm stacked(final @NotNull Project project, final FileFormat @NotNull [] formats, final @NotNull FileFormat defaultFormat, final @NotNull String suggestedName, final @NotNull String chooserTitle, final @NotNull String chooserDescription) {
        return new DestinationForm(project, formats, defaultFormat, suggestedName, chooserTitle, chooserDescription, "");
    }

    // UC-SHARE-001, Rule-INTERNAL-099
    public static @NotNull DestinationForm inSection(final @NotNull Project project, final FileFormat @NotNull [] formats, final @NotNull FileFormat defaultFormat, final @NotNull String suggestedName, final @NotNull String chooserTitle, final @NotNull String chooserDescription, final @NotNull String section) {
        return new DestinationForm(project, formats, defaultFormat, suggestedName, chooserTitle, chooserDescription, section);
    }

    static @NotNull String withExtension(final @NotNull String fileName, final @NotNull String extension) {
        if (fileName.endsWith(extension)) return fileName;

        final int dot = fileName.lastIndexOf('.');
        if (dot < 0) return fileName + extension;

        final @NotNull String tail = fileName.substring(dot);
        final boolean tailIsAnExtension = Arrays.stream(FileFormat.values())
                .anyMatch(type -> type.getExtension().equalsIgnoreCase(tail));

        return tailIsAnExtension ? fileName.substring(0, dot) + extension : fileName + extension;
    }

    // UC-SHARE-001, Rule-INTERNAL-067
    public @NotNull Optional<Destination> resolve() {
        final @NotNull String named = fileName.accepted();
        if (named.isEmpty()) return Optional.empty();

        final @NotNull String chosenFolder = folder.accepted();
        if (chosenFolder.isEmpty()) return Optional.empty();

        final @NotNull FileFormat chosen = format.chosen();

        return Optional.of(new Destination(new File(chosenFolder, withExtension(named, chosen.getExtension())), chosen));
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return fileName.getFocusComponent();
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }
}
