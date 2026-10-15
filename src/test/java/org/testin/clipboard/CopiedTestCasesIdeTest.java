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

import com.intellij.openapi.ide.CopyPasteManager;
import org.testin.AbstractIdeTest;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.util.JsonCodec;

import java.awt.datatransfer.StringSelection;
import java.util.List;

public class CopiedTestCasesIdeTest extends AbstractIdeTest {

    // Rule-EDITOR-PANEL-085
    public void testOnlyTestCasesOnTheClipboardAreWaitingToBePasted() {
        CopyPasteManager.getInstance().setContents(new StringSelection("a line copied from a bug report"));
        assertTrue("text the tester copied elsewhere was read as test cases waiting to be pasted", CopiedTestCases.onTheClipboard(getProject()).isEmpty());

        CopyPasteManager.getInstance().setContents(new StringSelection(CopiedTestCases.json(getProject(), List.of(TestCase.builder().description("Log in").build()))));
        assertEquals("a copied test case was not seen waiting on the clipboard", List.of("Log in"), CopiedTestCases.onTheClipboard(getProject()).stream().map(TestCase::getDescription).toList());
    }

    // Rule-INTERNAL-012, Rule-EDITOR-PANEL-085
    public void testATestCaseOnTheClipboardKeepsItsUuidAndOneWithoutIsNotATestCase() {
        final TestCase copied = TestCase.builder().description("Log in").build();
        CopyPasteManager.getInstance().setContents(new StringSelection(CopiedTestCases.json(getProject(), List.of(copied))));
        assertEquals("the clipboard lost the test case's UUID, so a cut would paste a new test case", List.of(copied.getUuid()), CopiedTestCases.onTheClipboard(getProject()).stream().map(TestCase::getUuid).toList());

        CopyPasteManager.getInstance().setContents(new StringSelection(Services.getInstance(getProject(), JsonCodec.class).writeValueAsString(List.of(copied))));
        assertTrue("a test case's JSON without its UUID was read as one waiting to be pasted", CopiedTestCases.onTheClipboard(getProject()).isEmpty());
    }
}
