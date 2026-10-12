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

package org.testin.editor.open;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.ThreadingAssertions;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.indexer.Nodes;
import org.testin.logger.Logger;
import org.testin.model.TestCaseDto;
import org.testin.model.node.Node;
import org.testin.model.node.TestRunNode;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class TestinEditors {
    private final @NotNull Project p;

    private @NotNull Optional<VirtualFile> openFileAt(final @NotNull Path path) {
        return Arrays.stream(FileEditorManager.getInstance(p).getOpenFiles())
                .filter(open -> open instanceof UnifiedVirtualFile testinFile && testinFile.getDir().getPath().equals(path))
                .findFirst();
    }

    public void reloadOpen(final @NotNull Path path) {
        editorAt(path).ifPresent(TestinEditor::reloadData);
    }

    // UC-TREE-PANEL-011, UC-TREE-PANEL-012, Rule-TREE-PANEL-111, Rule-TREE-PANEL-116
    public void close(final @NotNull Node dir) {
        openFilesUnder(dir.getPath()).forEach(FileEditorManager.getInstance(p)::closeFile);
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-111
    public boolean busyUnder(final @NotNull Node dir) {
        return editorsOf(openFilesUnder(dir.getPath()).stream()).anyMatch(TestinEditor::isBusy);
    }

    private @NotNull List<VirtualFile> openFilesUnder(final @NotNull Path path) {
        return Arrays.stream(FileEditorManager.getInstance(p).getOpenFiles())
                .filter(open -> open instanceof UnifiedVirtualFile testinFile && testinFile.getDir().getPath().startsWith(path))
                .toList();
    }

    // Rule-EDITOR-PANEL-015
    public void closeAll() {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(p);

        for (final VirtualFile open : fed.getOpenFiles()) {
            if (open instanceof UnifiedVirtualFile) fed.closeFile(open);
        }
    }

    // UC-TREE-PANEL-025, Rule-TREE-PANEL-082
    public void refreshOpen() {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(p);

        for (final VirtualFile open : fed.getOpenFiles()) {
            if (!(open instanceof UnifiedVirtualFile testinFile)) continue;

            if (!isIndexed(testinFile)) {
                Logger.info("Closing the editor for a node that is no longer indexed: " + testinFile.getName());
                fed.closeFile(testinFile);
                continue;
            }

            for (final FileEditor tab : fed.getAllEditors(testinFile)) {
                if (!(tab instanceof UnifiedFileEditor unified)) continue;

                final @NotNull TestinEditor editor = unified.getEditor();
                if (editor.isBusy()) {
                    Logger.info("Leaving a busy editor as it is rather than reloading under the tester: "
                            + testinFile.getName());
                    editor.followTheIndex();
                    continue;
                }
                editor.reloadData();
            }
        }
    }

    private boolean isIndexed(final @NotNull UnifiedVirtualFile file) {
        return Services.getInstance(p, Nodes.class).nodeExists(file.getDir().getPath());
    }

    public void openThen(final @NotNull Node dir, final @NotNull Consumer<TestinEditor> tell) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!openNow(dir, true)) return;

            editorFor(dir).ifPresent(tell);
        });
    }

    // UC-EDITOR-PANEL-048, Rule-EDITOR-PANEL-233, Rule-VIEW-PANEL-016
    public void openAndSelect(final @NotNull Node dir, final @NotNull TestCaseDto tc) {
        openThen(dir, editor -> editor.selectWhenLoaded(tc.getId()));
    }

    public @NotNull Optional<TestRunEditor> testRunEditorFor(final @NotNull TestRunNode testRun) {
        return editorFor(testRun).filter(TestRunEditor.class::isInstance).map(TestRunEditor.class::cast);
    }

    public @NotNull Optional<TestinEditor> editorFor(final @NotNull Node dir) {
        return editorAt(dir.getPath());
    }

    private @NotNull Optional<TestinEditor> editorAt(final @NotNull Path path) {
        return editorsOf(openFileAt(path).stream()).findFirst();
    }

    public void closeThenOpen(final @NotNull Node dir) {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(p);

        ApplicationManager.getApplication().invokeLater(() -> openFileAt(dir.getPath()).ifPresentOrElse(open -> {
            fed.closeFile(open);
            fed.openFile(open, true);
        }, () -> open(dir)));
    }

    public void open(final @NotNull Node dir) {
        open(dir, true);
    }

    public void open(final @NotNull Node dir, final boolean focus) {
        ApplicationManager.getApplication().invokeLater(() -> openNow(dir, focus));
    }

    private boolean openNow(final @NotNull Node dir, final boolean focus) {
        ThreadingAssertions.assertEventDispatchThread();
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(p);

        final @NotNull Optional<VirtualFile> already = openFileAt(dir.getPath());
        if (already.isPresent()) {
            Logger.info("Editor already open, focusing: " + dir.getName());
            fed.openFile(already.orElseThrow(), focus);
            return true;
        }

        if (!dir.isOpenableInEditor()) {
            Logger.info("Nothing to open for " + dir.getName() + " - it holds nodes rather than test cases");
            return false;
        }

        Logger.info("Opening Editor: " + dir.getPath());
        fed.openFile(new UnifiedVirtualFile(dir), focus);

        return true;
    }

    // Rule-EDITOR-PANEL-291
    public boolean walking(final @NotNull UUID testCaseId) {
        return openEditors().anyMatch(editor -> editor.walksOn(testCaseId));
    }

    // Rule-EDITOR-PANEL-291
    public void repaint(final @NotNull UUID testCaseId) {
        openEditors().forEach(editor -> editor.repaint(testCaseId));
    }

    private @NotNull Stream<TestinEditor> openEditors() {
        return editorsOf(Arrays.stream(FileEditorManager.getInstance(p).getOpenFiles()));
    }

    private @NotNull Stream<TestinEditor> editorsOf(final @NotNull Stream<VirtualFile> files) {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(p);

        return files.flatMap(open -> Arrays.stream(fed.getAllEditors(open)))
                .filter(UnifiedFileEditor.class::isInstance)
                .map(tab -> ((UnifiedFileEditor) tab).getEditor());
    }

    public @NotNull List<Path> openNodePaths() {
        final @NotNull List<Path> paths = new ArrayList<>();

        for (final VirtualFile vf : FileEditorManager.getInstance(p).getOpenFiles()) {
            if (vf instanceof UnifiedVirtualFile uvf) paths.add(uvf.getDir().getPath().toAbsolutePath());
        }

        return paths;
    }
}