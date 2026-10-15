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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diff.DiffColors;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.JBSplitter;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.DialogComponent;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Json;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

// UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-292
final class BulkJsonEditors implements DialogComponent {
    private static final int TYPING_SETTLES_MILLIS = 150;

    private final @NotNull Project project;

    private final @NotNull Document leftDocument;

    private final @NotNull Document rightDocument;

    private final @NotNull Editor leftEditor;

    private final @NotNull Editor rightEditor;

    private final @NotNull JBSplitter splitter;

    private final @NotNull List<RangeMarker> markers = new ArrayList<>();

    private final @NotNull List<RangeMarker> guardBlocks = new ArrayList<>();

    private final @NotNull List<RangeHighlighter> leftLineHighlighters = new ArrayList<>();

    private final @NotNull TextAttributes leftLineAttributes = new TextAttributes();

    private final @NotNull Disposable documentListenerDisposable = Disposer.newDisposable();

    private @NotNull IntFunction<String> originalTextAt = _ -> "";

    // Rule-EDITOR-PANEL-292
    BulkJsonEditors(final @NotNull Project project) {
        this.project = project;

        leftDocument = EditorFactory.getInstance().createDocument("");
        leftEditor = EditorFactory.getInstance().createEditor(leftDocument, project, Json.fileType(), true);
        setupEditorAppearance(leftEditor);
        leftEditor.getContentComponent().setFocusable(false);
        leftEditor.getSettings().setCaretRowShown(false);
        leftEditor.addEditorMouseListener(new EditorMouseListener() {
            @Override
            public void mousePressed(final @NotNull EditorMouseEvent event) {
                event.consume();
            }
        });

        rightDocument = EditorFactory.getInstance().createDocument("");
        EditorActionManager.getInstance().setReadonlyFragmentModificationHandler(rightDocument, _ -> {
        });
        rightEditor = EditorFactory.getInstance().createEditor(rightDocument, project, Json.fileType(), false);
        setupEditorAppearance(rightEditor);

        final @NotNull Color caretRowColor = Optional
                .ofNullable(rightEditor.getColorsScheme().getColor(EditorColors.CARET_ROW_COLOR))
                .orElseGet(rightEditor.getColorsScheme()::getDefaultBackground);
        leftLineAttributes.setBackgroundColor(caretRowColor);

        splitter = new JBSplitter(false, 0.5f);
        splitter.setFirstComponent(leftEditor.getComponent());
        splitter.setSecondComponent(rightEditor.getComponent());

        installCaretSnapping();
        installDiffHighlighting();
        installScrollSync();
        installMultiCaretClick();
    }

    private static void register(final @NotNull Runnable body, final @NotNull CustomShortcutSet keys, final @NotNull JComponent target) {
        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                body.run();
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        }.registerCustomShortcutSet(keys, target);
    }

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-040, Rule-EDITOR-PANEL-045
    void setContent(final @NotNull String leftText, final @NotNull String rightText, final @NotNull List<int[]> editableRanges) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            for (final RangeMarker guard : guardBlocks) rightDocument.removeGuardedBlock(guard);
            guardBlocks.clear();
            markers.clear();

            leftDocument.setReadOnly(false);
            leftDocument.setText(leftText);
            leftDocument.setReadOnly(true);

            rightDocument.setText(rightText);

            int guardStart = 0;
            for (final int[] range : editableRanges) {
                final @NotNull RangeMarker marker = rightDocument.createRangeMarker(range[0], range[1]);
                marker.setGreedyToLeft(true);
                marker.setGreedyToRight(true);
                markers.add(marker);

                if (guardStart < range[0]) guardBlocks.add(rightDocument.createGuardedBlock(guardStart, range[0]));
                guardStart = range[1];
            }
            if (guardStart < rightDocument.getTextLength()) {
                guardBlocks.add(rightDocument.createGuardedBlock(guardStart, rightDocument.getTextLength()));
            }
        });
    }

    void setOriginalTextSource(final @NotNull IntFunction<String> originalTextAt) {
        this.originalTextAt = originalTextAt;
    }

    int valueCount() {
        return markers.size();
    }

    @NotNull Optional<String> valueAt(final int index) {
        final @NotNull RangeMarker marker = markers.get(index);
        if (!marker.isValid()) return Optional.empty();

        return Optional.of(rightDocument.getText(new TextRange(marker.getStartOffset(), marker.getEndOffset())));
    }

    @NotNull List<Integer> indicesUnderCarets() {
        final @NotNull List<Integer> indices = new ArrayList<>();

        for (final Caret caret : rightEditor.getCaretModel().getAllCarets()) {
            final int index = indexAt(caret.getOffset());
            if (index >= 0 && !indices.contains(index)) indices.add(index);
        }

        indices.sort(Comparator.reverseOrder());
        return indices;
    }

    void focusValue(final int index) {
        if (index < 0 || index >= markers.size()) return;

        final @NotNull RangeMarker marker = markers.get(index);
        if (marker.isValid()) rightEditor.getCaretModel().moveToOffset(marker.getEndOffset());
    }

    // UC-EDITOR-PANEL-007
    void navigate(final int direction, final boolean wrap) {
        if (markers.isEmpty()) return;

        rightEditor.getCaretModel().removeSecondaryCarets();

        final int current = Math.max(0, indexAt(rightEditor.getCaretModel().getOffset()));
        final int target = wrap
                ? (current + direction + markers.size()) % markers.size()
                : current + direction;
        if (target < 0 || target >= markers.size()) return;

        focusValue(target);
    }

    // UC-EDITOR-PANEL-007
    void caretOnEveryValue() {
        placeCaretOnAll(rightEditor, liveMarkers());
        refreshRowHighlights();
    }

    void release() {
        Disposer.dispose(documentListenerDisposable);
        if (!leftEditor.isDisposed()) EditorFactory.getInstance().releaseEditor(leftEditor);
        if (!rightEditor.isDisposed()) EditorFactory.getInstance().releaseEditor(rightEditor);
    }

    private void installCaretSnapping() {
        rightEditor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(final @NotNull CaretEvent event) {
                final @NotNull Caret caret = event.getCaret();
                if (indexAt(caret.getOffset()) < 0) {
                    caret.moveToOffset(nearestValidOffset(caret.getOffset(), liveMarkers()));
                }
                refreshRowHighlights();
            }

            @Override
            public void caretAdded(final @NotNull CaretEvent event) {
                refreshRowHighlights();
            }

            @Override
            public void caretRemoved(final @NotNull CaretEvent event) {
                refreshRowHighlights();
            }
        });
    }

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-043
    private void installDiffHighlighting() {
        final @NotNull Timer redraw = new Timer(TYPING_SETTLES_MILLIS, _ -> refreshDiffHighlights());
        redraw.setRepeats(false);

        rightDocument.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(final @NotNull DocumentEvent event) {
                redraw.restart();
            }
        }, documentListenerDisposable);
    }

    private void refreshDiffHighlights() {
        if (rightEditor.isDisposed()) return;

        final @NotNull MarkupModel markup = rightEditor.getMarkupModel();
        for (final RangeHighlighter highlighter : markup.getAllHighlighters()) {
            if (highlighter.getLayer() == HighlighterLayer.SELECTION - 1) markup.removeHighlighter(highlighter);
        }

        for (int i = 0; i < markers.size(); i++) {
            final int index = i;
            valueAt(index)
                    .filter(current -> !current.equals(originalTextAt.apply(index)))
                    .ifPresent(_ -> {
                        final @NotNull RangeMarker marker = markers.get(index);
                        markup.addRangeHighlighter(DiffColors.DIFF_INSERTED, marker.getStartOffset(), marker.getEndOffset(),
                                HighlighterLayer.SELECTION - 1, HighlighterTargetArea.EXACT_RANGE);
                    });
        }
    }

    private void installScrollSync() {
        leftEditor.getScrollingModel().addVisibleAreaListener(e -> {
            final int target = e.getNewRectangle().y;
            if (rightEditor.getScrollingModel().getVerticalScrollOffset() != target) {
                rightEditor.getScrollingModel().scrollVertically(target);
            }
        });
        rightEditor.getScrollingModel().addVisibleAreaListener(e -> {
            final int target = e.getNewRectangle().y;
            if (leftEditor.getScrollingModel().getVerticalScrollOffset() != target) {
                leftEditor.getScrollingModel().scrollVertically(target);
            }
        });
    }

    private void installMultiCaretClick() {
        rightEditor.addEditorMouseListener(new EditorMouseListener() {
            @Override
            public void mousePressed(final @NotNull EditorMouseEvent event) {
                final @NotNull MouseEvent mouse = event.getMouseEvent();
                if (!mouse.isControlDown() && !mouse.isMetaDown()) return;

                VisualPosition position = rightEditor.xyToVisualPosition(mouse.getPoint());
                final int offset = rightEditor.logicalPositionToOffset(rightEditor.visualToLogicalPosition(position));

                if (indexAt(offset) < 0) {
                    final int snapped = nearestValidOffset(offset, liveMarkers());
                    position = rightEditor.logicalToVisualPosition(rightEditor.offsetToLogicalPosition(snapped));
                }

                final @NotNull CaretModel caretModel = rightEditor.getCaretModel();
                final @NotNull VisualPosition clicked = position;
                Optional.ofNullable(caretModel.getCaretAt(clicked)).ifPresentOrElse(
                        existing -> {
                            if (caretModel.getCaretCount() > 1) caretModel.removeCaret(existing);
                        },
                        () -> caretModel.addCaret(clicked, true));
                event.consume();
            }
        });
    }

    private void refreshRowHighlights() {
        if (leftEditor.isDisposed() || rightEditor.isDisposed()) return;

        final @NotNull MarkupModel leftMarkup = leftEditor.getMarkupModel();
        for (final RangeHighlighter highlighter : leftLineHighlighters) leftMarkup.removeHighlighter(highlighter);
        leftLineHighlighters.clear();

        for (final Caret caret : rightEditor.getCaretModel().getAllCarets()) {
            final int line = rightDocument.getLineNumber(caret.getOffset());
            if (line < leftDocument.getLineCount()) {
                leftLineHighlighters.add(leftMarkup.addLineHighlighter(line, HighlighterLayer.CARET_ROW, leftLineAttributes));
            }
        }
    }

    private int indexAt(final int offset) {
        for (int i = 0; i < markers.size(); i++) {
            final @NotNull RangeMarker marker = markers.get(i);
            if (marker.isValid() && offset >= marker.getStartOffset() && offset <= marker.getEndOffset()) return i;
        }
        return -1;
    }

    private @NotNull List<RangeMarker> liveMarkers() {
        return markers.stream().filter(RangeMarker::isValid).toList();
    }

    @Override
    public @NotNull JComponent getPanel() {
        return splitter;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return rightEditor.getContentComponent();
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }

    @Override
    public boolean acceptsDialogKeys() {
        return false;
    }

    // UC-EDITOR-PANEL-007
    void bindKeysToEditor(final @NotNull List<StatusBarShortcut> shortcuts) {
        final @NotNull JComponent target = rightEditor.getContentComponent();

        for (final StatusBarShortcut shortcut : shortcuts) {
            if (shortcut.isBindable()) register(shortcut.action(), shortcut.shortcut().getCustomShortcut(), target);
        }
    }

    void focusFirstValue() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (leftEditor.isDisposed() || rightEditor.isDisposed()) return;

            focusValue(0);
            refreshRowHighlights();
            refreshDiffHighlights();
        });
    }

    // UC-EDITOR-PANEL-007
    private static void setupEditorAppearance(final @NotNull Editor editor) {
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

    // UC-EDITOR-PANEL-007
    private static int nearestValidOffset(final int offset, final @NotNull List<RangeMarker> markers) {
        int minDistance = Integer.MAX_VALUE;
        int nearestOffset = offset;
        for (final RangeMarker marker : markers) {
            if (!marker.isValid()) continue;

            if (Math.abs(offset - marker.getStartOffset()) < minDistance) {
                minDistance = Math.abs(offset - marker.getStartOffset());
                nearestOffset = marker.getStartOffset();
            }
            if (Math.abs(offset - marker.getEndOffset()) < minDistance) {
                minDistance = Math.abs(offset - marker.getEndOffset());
                nearestOffset = marker.getEndOffset();
            }
        }
        return nearestOffset;
    }

    // UC-EDITOR-PANEL-007
    private static void placeCaretOnAll(final @NotNull Editor editor, final @NotNull List<RangeMarker> markers) {
        final @NotNull CaretModel caretModel = editor.getCaretModel();
        caretModel.removeSecondaryCarets();

        boolean isFirst = true;
        for (final RangeMarker marker : markers) {
            if (!marker.isValid()) continue;

            final @NotNull LogicalPosition logicalPosition = editor.offsetToLogicalPosition(marker.getEndOffset());
            final @NotNull VisualPosition visualPosition = editor.logicalToVisualPosition(logicalPosition);

            if (isFirst) {
                caretModel.moveToVisualPosition(visualPosition);
                isFirst = false;
            } else {
                caretModel.addCaret(visualPosition, true);
            }
        }
    }
}
