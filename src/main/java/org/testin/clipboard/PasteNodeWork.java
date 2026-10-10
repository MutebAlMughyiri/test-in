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
import org.testin.logger.Logger;
import org.testin.model.node.Node;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import javax.swing.TransferHandler;
import java.awt.datatransfer.Transferable;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

record PasteNodeWork(@NotNull Project p, @NotNull TreeTransferHandler transferHandler, @NotNull Notifier notifier) {
    PasteNodeWork(final @NotNull Project p, final @NotNull TreeTransferHandler transferHandler) {
        this(p, transferHandler, Services.getInstance(p, Notifier.class));
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-006
    void paste(final @NotNull Transferable contents, final @NotNull Node target) {
        try {
            final @NotNull TreeTransferPayload payload = (TreeTransferPayload) contents.getTransferData(TreeTransferHandler.NODE_FLAVOR);

            final @NotNull List<Node> nodes = payload.nodes().stream()
                    .filter(node -> transferHandler.canTransferInto(node, target))
                    .toList();

            final boolean collisionsReported = transferHandler.notifyNameCollisions(payload.nodes(), target);

            if (nodes.isEmpty()) {
                if (!collisionsReported) {
                    notifier.softRefuse(p, Bundle.message("paste.select.folder"));
                }

                return;
            }

            final boolean move = payload.clipboardAction() == TransferHandler.MOVE;
            final @NotNull String verb = move ? Bundle.message("transfer.move") : Bundle.message("transfer.copy");
            final @NotNull String what = nodes.size() == 1
                    ? "'" + nodes.getFirst().getName() + "'"
                    : Bundle.message("transfer.items", String.valueOf(nodes.size()));
            final @NotNull Path fromPath = nodes.getFirst().getPath().getParent();

            new ConfirmDialog(p, Bundle.message("paste.title"),
                    Bundle.message("transfer.confirm", verb, what, target.getName()),
                    Objects.toString(fromPath, ""),
                    target.getPath().toString(),
                    verb,
                    () -> transferHandler.pasteFromClipboard(target)
            ).show();

        } catch (final Exception ex) {
            Logger.error("Paste Node failed: " + FailureText.of(ex));
            notifier.softRefuse(p, Bundle.message("clipboard.paste.failed.title"), FailureText.of(ex));
        }
    }
}
