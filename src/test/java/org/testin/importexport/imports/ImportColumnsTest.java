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

package org.testin.importexport.imports;

import org.testin.testcase.Can;
import org.testin.testcase.TestSetEditorFields;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ImportColumnsTest {

    // Rule-SHARE-034
    @Test
    public void theImportTableHoldsThirteenColumnsAndTheExportTableSeventeen() {
        assertEquals(TestSetEditorFields.all(Can.IMPORT).size(), 13, "the columns an import can read");
        assertEquals(TestSetEditorFields.all(Can.EXPORT).size(), 17, "the columns an export can write");
    }

    // Rule-SHARE-110
    @Test
    public void aHeadingIsFoundByItsShownNameOrItsNameInTheCode() {
        assertTrue(TestSetEditorFields.CREATED_AT.isColumn("Created At"), "the name Testin shows");
        assertTrue(TestSetEditorFields.CREATED_AT.isColumn("created at"), "letter case does not matter");
        assertTrue(TestSetEditorFields.CREATED_AT.isColumn("CREATED_AT"), "the name in the code");
        assertTrue(TestSetEditorFields.CREATED_AT.isColumn("created_at"), "the name in the code, in any case");
        assertTrue(TestSetEditorFields.PRE_CONDITIONS.isColumn("  pre conditions "), "the space around a heading is not part of it");
    }

    // Rule-SHARE-110
    @Test
    public void aHeadingNamesOneColumnAndNoOther() {
        assertFalse(TestSetEditorFields.CREATED_AT.isColumn("Updated At"));
        assertFalse(TestSetEditorFields.CREATED_AT.isColumn("Created"));
        assertFalse(TestSetEditorFields.CREATED_AT.isColumn("CreatedAt"));
    }
}
