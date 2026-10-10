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
import org.testin.model.node.Node;
import org.testin.model.node.TestSetNode;
import org.testng.annotations.Test;

import javax.swing.TransferHandler;
import java.awt.datatransfer.DataFlavor;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class NodesTransferableTest {

    private static @NotNull NodesTransferable aDraggedTestSet() {
        final @NotNull Node testSet = new TestSetNode();
        testSet.setPath(Path.of("root", "NAFATH", "Test Cases", "Login"));

        return new NodesTransferable(new TreeTransferPayload(List.of(testSet), TransferHandler.MOVE));
    }

    // Rule-TREE-PANEL-105
    @Test
    public void aNodeDraggedOutOfTheIdeCarriesNothingAFileManagerReads() {
        final @NotNull NodesTransferable dragged = aDraggedTestSet();

        assertEquals(List.of(dragged.getTransferDataFlavors()), List.of(TreeTransferHandler.NODE_FLAVOR),
                "a dragged node offers something other than Testin's own flavor");

        for (final DataFlavor outside : List.of(DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor, DataFlavor.getTextPlainUnicodeFlavor())) {
            assertFalse(dragged.isDataFlavorSupported(outside), "a dragged node can be dropped as " + outside.getHumanPresentableName());
        }
    }
}
