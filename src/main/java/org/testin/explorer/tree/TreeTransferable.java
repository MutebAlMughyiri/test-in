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

import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

// Rule-TREE-PANEL-105
record TreeTransferable(@NotNull TreeTransferPayload payload) implements Transferable {
    @Override
    public @NotNull DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{TreeTransferHandler.NODE_FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(final @NotNull DataFlavor flavor) {
        return TreeTransferHandler.NODE_FLAVOR.equals(flavor);
    }

    @Override
    public @NotNull Object getTransferData(final @NotNull DataFlavor flavor) throws UnsupportedFlavorException {
        if (TreeTransferHandler.NODE_FLAVOR.equals(flavor)) return payload;
        throw new UnsupportedFlavorException(flavor);
    }
}
