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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class EditorKindTest {

    // Rule-EDITOR-PANEL-022
    @Test
    public void theDetailsKeysAreTheOnesAlreadyStored() {
        assertEquals(EditorKind.TEST_SET.detailsKey(4), "testin.selectedDetails.test.v4");
        assertEquals(EditorKind.TEST_RUN.detailsKey(7), "testin.selectedDetails.run.v7");
    }

    @Test
    public void theColumnWidthKeysAreTheOnesAlreadyStored() {
        assertEquals(EditorKind.TEST_SET.columnWidthKey("Expected Result"), "testin.grid.colWidth.test.Expected Result");
        assertEquals(EditorKind.TEST_RUN.columnWidthKey("FQCN"), "testin.grid.colWidth.run.FQCN");
    }
}
