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
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.model.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.awt.datatransfer.StringSelection;
import java.util.List;

public class CopiedTestCasesIdeTest extends BasePlatformTestCase {

    // Rule-EDITOR-PANEL-262
    public void testOnlyTestCasesOnTheClipboardAreWaitingToBePasted() {
        CopyPasteManager.getInstance().setContents(new StringSelection("a line copied from a bug report"));
        assertTrue("text the tester copied elsewhere was read as test cases waiting to be pasted", CopiedTestCases.onTheClipboard(getProject()).isEmpty());

        CopyPasteManager.getInstance().setContents(new StringSelection(Services.getInstance(getProject(), Mapper.class).writeValueAsString(List.of(TestCaseDto.builder().description("Log in").build()))));
        assertEquals("a copied test case was not seen waiting on the clipboard", List.of("Log in"), CopiedTestCases.onTheClipboard(getProject()).stream().map(TestCaseDto::getDescription).toList());
    }
}
