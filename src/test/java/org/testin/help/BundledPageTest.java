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

package org.testin.help;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class BundledPageTest {

    // Rule-INTERNAL-130
    @Test
    public void aPageIsAMarkdownFileTheGuidesHoldAndAWebAddressIsNot() {
        assertTrue(BundledPage.isPage("treePanel/main.md"), "a page the guides hold is not a page");
        assertFalse(BundledPage.isPage("https://example.com/notes.md"), "a web address was taken for a page the guides hold");
        assertFalse(BundledPage.isPage("treePanel/picture.png"), "a picture was taken for a page");
    }
}
