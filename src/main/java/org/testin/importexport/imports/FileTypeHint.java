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

import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileFormat;
import org.testin.testcase.Can;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Html;

import javax.swing.JComponent;
import java.util.List;
import java.util.stream.Collectors;

public record FileTypeHint(@NotNull JBLabel label, @NotNull List<TestSetEditorAttributes> attributes) {
    public static @NotNull FileTypeHint of(final @NotNull List<TestSetEditorAttributes> attributes) {
        final @NotNull JBLabel label = DialogStyle.hint();
        label.setVisible(false);

        return new FileTypeHint(label, attributes);
    }

    public @NotNull JComponent panel() {
        return label;
    }

    // UC-SHARE-005, Rule-SHARE-107
    public void showStatus(final @NotNull String status) {
        label.setText(status);
        label.setVisible(!status.isBlank());
    }

    // UC-SHARE-005
    public void showFor(final @NotNull FileFormat format) {
        final @NotNull String columns = attributes.stream()
                .filter(attribute -> attribute.can(Can.IMPORT))
                .map(TestSetEditorAttributes::getName)
                .collect(Collectors.joining(", "));

        final @NotNull String hint = format.hintFor(columns);
        if (hint.isBlank()) {
            label.setVisible(false);
            return;
        }

        label.setText("<html>" + Html.ofText(hint) + "</html>");
        label.setVisible(true);
    }
}
