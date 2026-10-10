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

package org.testin.testcase.update.bulk;

import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BulkJsonEditor {
    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-044
    static @NotNull String escapeJson(final @NotNull String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-044
    static @NotNull String unescapeJson(final @NotNull String str) {
        final @NotNull StringBuilder out = new StringBuilder(str.length());

        for (int i = 0; i < str.length(); i++) {
            final char current = str.charAt(i);

            if (current != '\\' || i == str.length() - 1) {
                out.append(current);
                continue;
            }

            final char escaped = str.charAt(++i);

            switch (escaped) {
                case 'n' -> out.append('\n');
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                default -> out.append(current).append(escaped);
            }
        }

        return out.toString();
    }

    static void setupEditorAppearance(final @NotNull Editor editor, final @NotNull Project p) {
        final @NotNull FileType jsonFileType = FileTypeManager.getInstance().getFileTypeByExtension("json");
        final @NotNull EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(p, jsonFileType);

        if (editor instanceof EditorEx editorEx)
            editorEx.setHighlighter(highlighter);

        final @NotNull EditorColorsScheme scheme = editor.getColorsScheme();
        scheme.setEditorFontSize(15f);
        scheme.setLineSpacing(1.4f);

        final @NotNull EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setLineMarkerAreaShown(false);
        settings.setFoldingOutlineShown(true);
        settings.setVirtualSpace(false);
        settings.setUseSoftWraps(false);
        settings.setAdditionalLinesCount(1);
    }

    static int nearestValidOffset(final int offset, final @NotNull List<RangeMarker> markers) {
        int minDistance = Integer.MAX_VALUE;
        int nearestOffset = offset;
        for (final RangeMarker m : markers) {
            if (!m.isValid()) continue;

            if (Math.abs(offset - m.getStartOffset()) < minDistance) {
                minDistance = Math.abs(offset - m.getStartOffset());
                nearestOffset = m.getStartOffset();
            }
            if (Math.abs(offset - m.getEndOffset()) < minDistance) {
                minDistance = Math.abs(offset - m.getEndOffset());
                nearestOffset = m.getEndOffset();
            }
        }
        return nearestOffset;
    }

    // UC-EDITOR-PANEL-007
    static void placeCaretOnAll(final @NotNull Editor editor, final @NotNull List<RangeMarker> markers) {
        final @NotNull CaretModel caretModel = editor.getCaretModel();
        caretModel.removeSecondaryCarets();

        boolean isFirst = true;
        for (final RangeMarker marker : markers) {
            if (!marker.isValid()) continue;

            final @NotNull LogicalPosition logPos = editor.offsetToLogicalPosition(marker.getEndOffset());
            final @NotNull VisualPosition visPos = editor.logicalToVisualPosition(logPos);

            if (isFirst) {
                caretModel.moveToVisualPosition(visPos);
                isFirst = false;
            } else {
                caretModel.addCaret(visPos, true);
            }
        }
    }
}
