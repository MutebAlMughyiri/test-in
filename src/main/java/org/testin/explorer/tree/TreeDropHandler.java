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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.FileDropEvent;
import com.intellij.openapi.editor.FileDropHandler;
import com.intellij.openapi.project.Project;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editor.open.TestinEditors;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;
import org.testin.services.Services;
import org.testin.util.FailureText;

import java.awt.datatransfer.Transferable;

public class TreeDropHandler implements FileDropHandler {
    @Override
    public @Nullable Object handleDrop(final @NotNull FileDropEvent event, final @NotNull Continuation<? super Boolean> continuation) {
        final @NotNull Project p = event.getProject();
        final @NotNull Transferable transferable = event.getTransferable();

        if (!transferable.isDataFlavorSupported(TreeTransferHandler.NODE_FLAVOR)) {
            return false;
        }

        try {
            final @NotNull TreeTransferPayload payload = (TreeTransferPayload) transferable.getTransferData(TreeTransferHandler.NODE_FLAVOR);

            ApplicationManager.getApplication().invokeLater(() -> {
                for (final AbstractNode node : payload.nodes()) {
                    if (!node.isOpenableInEditor()) continue;

                    TestinLogger.info("dragged " + node.getType().getMarkerKind() + ": " + node.getName());
                    Services.getInstance(p, TestinEditors.class).open(node);
                }
            });
            return true;

        } catch (final Exception ex) {
            TestinLogger.error("Exception: " + FailureText.of(ex));
            return false;
        }
    }
}
