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

package org.testin.clipboard;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.tree.TreeTransferHandler;
import org.testin.explorer.tree.TreeTransferPayload;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import javax.swing.TransferHandler;
import java.awt.datatransfer.Transferable;
import java.util.List;

record PasteNodeWork(@NotNull Project project, @NotNull TreeTransferHandler transferHandler, @NotNull Notifier notifier) {
    PasteNodeWork(final @NotNull Project project, final @NotNull TreeTransferHandler transferHandler) {
        this(project, transferHandler, Services.getInstance(project, Notifier.class));
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-006
    void paste(final @NotNull Transferable contents, final @NotNull AbstractNode target) {
        try {
            final @NotNull TreeTransferPayload payload = (TreeTransferPayload) contents.getTransferData(TreeTransferHandler.NODE_FLAVOR);

            final @NotNull List<AbstractNode> nodes = transferHandler.transferableSources(payload.nodes(), target);

            final boolean collisionsReported = transferHandler.notifyNameCollisions(payload.nodes(), target);

            if (nodes.isEmpty()) {
                if (!collisionsReported) {
                    notifier.softRefuse(project, Bundle.message("paste.select.folder"));
                }

                return;
            }

            transferHandler.confirmPaste(nodes, target, payload.clipboardAction() == TransferHandler.MOVE);

        } catch (final Exception ex) {
            TestinLogger.error("Paste Node failed: " + FailureText.of(ex));
            notifier.softRefuse(project, Bundle.message("clipboard.paste.failed.title"), FailureText.of(ex));
        }
    }
}
