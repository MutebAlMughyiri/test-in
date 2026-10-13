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

package org.testin;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.FileEditorManagerTestCase;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.open.TestinEditors;
import org.testin.editor.open.UnifiedVirtualFile;
import org.testin.indexer.TestRuns;
import org.testin.model.node.AbstractNode;
import org.testin.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractOpenEditorsIdeTest extends FileEditorManagerTestCase {

    protected Path root;

    @Override
    public void setUp() {
        try {
            root = Files.createTempDirectory("testin-" + getClass().getSimpleName()).toRealPath();
            super.setUp();
        } catch (final Exception ex) {
            throw new AssertionError("Could not set up " + getName() + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public void tearDown() {
        try {
            Services.getInstance(getProject(), TestinEditors.class).closeAll();
            Services.getInstance(getProject(), TestRuns.class).awaitWrites();
            TempTree.delete(root);
            super.tearDown();
        } catch (final Exception ex) {
            throw new AssertionError("Could not tear down " + getName() + ": " + ex.getMessage(), ex);
        }
    }

    protected @NotNull List<VirtualFile> openOn(final @NotNull AbstractNode node) {
        return Arrays.stream(FileEditorManager.getInstance(getProject()).getOpenFiles())
                .filter(open -> open instanceof UnifiedVirtualFile testin && testin.getNode().getPath().equals(node.getPath()))
                .toList();
    }

    protected void opened(final @NotNull AbstractNode node) {
        Services.getInstance(getProject(), TestinEditors.class).open(node);
        Await.until(node.getName() + " never opened in its editor", () -> openOn(node).size() == 1);
    }
}
