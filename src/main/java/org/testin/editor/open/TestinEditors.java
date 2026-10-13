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
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.model.node.AbstractNode;
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
    private final @NotNull Project project;

    private @NotNull Optional<VirtualFile> openFileAt(final @NotNull Path path) {
        return Arrays.stream(FileEditorManager.getInstance(project).getOpenFiles())
                .filter(open -> open instanceof UnifiedVirtualFile testinFile && testinFile.getNode().getPath().equals(path))
                .findFirst();
    }

    public void reloadOpen(final @NotNull Path path) {
        editorAt(path).ifPresent(TestinEditor::reloadData);
    }

    // UC-TREE-PANEL-011, UC-TREE-PANEL-012, Rule-TREE-PANEL-111, Rule-TREE-PANEL-116
    public void close(final @NotNull AbstractNode node) {
        openFilesUnder(node.getPath()).forEach(FileEditorManager.getInstance(project)::closeFile);
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-111
    public boolean busyUnder(final @NotNull AbstractNode node) {
        return editorsOf(openFilesUnder(node.getPath()).stream()).anyMatch(TestinEditor::isBusy);
    }

    private @NotNull List<VirtualFile> openFilesUnder(final @NotNull Path path) {
        return Arrays.stream(FileEditorManager.getInstance(project).getOpenFiles())
                .filter(open -> open instanceof UnifiedVirtualFile testinFile && testinFile.getNode().getPath().startsWith(path))
                .toList();
    }

    // Rule-EDITOR-PANEL-015
    public void closeAll() {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(project);

        for (final VirtualFile open : fed.getOpenFiles()) {
            if (open instanceof UnifiedVirtualFile) fed.closeFile(open);
        }
    }

    // UC-TREE-PANEL-025, Rule-TREE-PANEL-082
    public void refreshOpen() {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(project);

        for (final VirtualFile open : fed.getOpenFiles()) {
            if (!(open instanceof UnifiedVirtualFile testinFile)) continue;

            if (!isIndexed(testinFile)) {
                TestinLogger.info("Closing the editor for a node that is no longer indexed: " + testinFile.getName());
                fed.closeFile(testinFile);
                continue;
            }

            for (final FileEditor tab : fed.getAllEditors(testinFile)) {
                if (!(tab instanceof UnifiedFileEditor unified)) continue;

                final @NotNull TestinEditor editor = unified.getEditor();
                if (editor.isBusy()) {
                    TestinLogger.info("Leaving a busy editor as it is rather than reloading under the tester: "
                            + testinFile.getName());
                    editor.followTheIndex();
                    continue;
                }
                editor.reloadData();
            }
        }
    }

    private boolean isIndexed(final @NotNull UnifiedVirtualFile file) {
        return Services.getInstance(project, Nodes.class).nodeExists(file.getNode().getPath());
    }

    public void openThen(final @NotNull AbstractNode node, final @NotNull Consumer<TestinEditor> tell) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!openNow(node, true)) return;

            editorFor(node).ifPresent(tell);
        });
    }

    // UC-EDITOR-PANEL-048, Rule-EDITOR-PANEL-233, Rule-VIEW-PANEL-016
    public void openAndSelect(final @NotNull AbstractNode node, final @NotNull TestCaseDto testCase) {
        openThen(node, editor -> editor.selectWhenLoaded(testCase.getId()));
    }

    public @NotNull Optional<TestRunEditor> testRunEditorFor(final @NotNull TestRunNode testRun) {
        return editorFor(testRun).filter(TestRunEditor.class::isInstance).map(TestRunEditor.class::cast);
    }

    public @NotNull Optional<TestinEditor> editorFor(final @NotNull AbstractNode node) {
        return editorAt(node.getPath());
    }

    private @NotNull Optional<TestinEditor> editorAt(final @NotNull Path path) {
        return editorsOf(openFileAt(path).stream()).findFirst();
    }

    public void closeThenOpen(final @NotNull AbstractNode node) {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(project);

        ApplicationManager.getApplication().invokeLater(() -> openFileAt(node.getPath()).ifPresentOrElse(open -> {
            fed.closeFile(open);
            fed.openFile(open, true);
        }, () -> open(node)));
    }

    public void open(final @NotNull AbstractNode node) {
        open(node, true);
    }

    public void open(final @NotNull AbstractNode node, final boolean focus) {
        ApplicationManager.getApplication().invokeLater(() -> openNow(node, focus));
    }

    private boolean openNow(final @NotNull AbstractNode node, final boolean focus) {
        ThreadingAssertions.assertEventDispatchThread();
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(project);

        final @NotNull Optional<VirtualFile> already = openFileAt(node.getPath());
        if (already.isPresent()) {
            TestinLogger.info("Editor already open, focusing: " + node.getName());
            fed.openFile(already.orElseThrow(), focus);
            return true;
        }

        if (!node.isOpenableInEditor()) {
            TestinLogger.info("Nothing to open for " + node.getName() + " - it holds nodes rather than test cases");
            return false;
        }

        TestinLogger.info("Opening Editor: " + node.getPath());
        fed.openFile(new UnifiedVirtualFile(node), focus);

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
        return editorsOf(Arrays.stream(FileEditorManager.getInstance(project).getOpenFiles()));
    }

    private @NotNull Stream<TestinEditor> editorsOf(final @NotNull Stream<VirtualFile> files) {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(project);

        return files.flatMap(open -> Arrays.stream(fed.getAllEditors(open)))
                .filter(UnifiedFileEditor.class::isInstance)
                .map(tab -> ((UnifiedFileEditor) tab).getEditor());
    }

    public @NotNull List<Path> openNodePaths() {
        final @NotNull List<Path> paths = new ArrayList<>();

        for (final VirtualFile vf : FileEditorManager.getInstance(project).getOpenFiles()) {
            if (vf instanceof UnifiedVirtualFile uvf) paths.add(uvf.getNode().getPath().toAbsolutePath());
        }

        return paths;
    }
}