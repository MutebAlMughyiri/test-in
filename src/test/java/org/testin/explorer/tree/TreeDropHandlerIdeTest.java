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

package org.testin.explorer.tree;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.FileDropEvent;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.editor.open.TestinEditors;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;

import javax.swing.TransferHandler;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.List;

public class TreeDropHandlerIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull Continuation<Boolean> NOBODY_WAITS = new Continuation<>() {
        @Override
        public @NotNull CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        public void resumeWith(final @NotNull Object result) {
        }
    };

    @Override
    protected void tearDown() {
        Services.getInstance(getProject(), TestinEditors.class).closeAll();
        super.tearDown();
    }

    private boolean taken(final @NotNull Transferable dragged) {
        final @NotNull Editor editor = EditorFactory.getInstance().createEditor(EditorFactory.getInstance().createDocument(""));
        try {
            return Boolean.TRUE.equals(new TreeDropHandler().handleDrop(new FileDropEvent(getProject(), dragged, List.of(), editor), NOBODY_WAITS));
        } finally {
            EditorFactory.getInstance().releaseEditor(editor);
        }
    }

    // UC-TREE-PANEL-005, Rule-TREE-PANEL-022, Rule-TREE-PANEL-146
    public void testATestSetDroppedOnTheEditorsOpensThereAndTheTestProjectBesideItIsLeftOut() {
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode testProject = made.testProject(root.resolve("NAFATH"));
        final @NotNull TestSetNode login = made.testSet(testProject.getTestCasesFolder(), "Login");

        assertTrue("a drop of tree nodes was not taken", taken(new TreeTransferable(new TreeTransferPayload(List.of(testProject, login), TransferHandler.MOVE))));

        final @NotNull TestinEditors editors = Services.getInstance(getProject(), TestinEditors.class);
        Await.until("the dropped test set did not open", () -> editors.openNodePaths().contains(login.getPath()));
        assertEquals("a node that does not open in an editor was opened", List.of(login.getPath()), editors.openNodePaths());
    }

    // Rule-TREE-PANEL-146
    public void testWhatWasNotDraggedFromTheTreeIsLeftToTheIde() {
        assertFalse("a drop of something that is not tree nodes was taken", taken(new StringSelection("notes.txt")));
    }
}
