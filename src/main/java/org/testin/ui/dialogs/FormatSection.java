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

import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileFormat;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.RadioSelection;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.util.List;

public record FormatSection(@NotNull RadioSelection<FileFormat> choice) {
    // Rule-INTERNAL-107
    public static @NotNull FormatSection of(final FileFormat @NotNull [] formats, final @NotNull FileFormat defaultFormat) {
        return new FormatSection(DialogPart.<FileFormat>radios(Bundle.message("destination.caption.format"))
                .options(List.of(formats), FileFormat::getLabel)
                .select(defaultFormat)
                .build()
                .getComponent());
    }

    public @NotNull JComponent panel() {
        return choice.getPanel();
    }

    // Rule-INTERNAL-107
    public @NotNull FileFormat chosen() {
        return choice.getSelected();
    }
}
