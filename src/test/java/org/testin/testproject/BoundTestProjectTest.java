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

package org.testin.testproject;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class BoundTestProjectTest {

    @Test
    public void withNoChoiceTheFileNamesIt() {
        assertEquals(BoundTestProject.resolve("Checkout", List.of()), "Checkout");
        assertEquals(BoundTestProject.resolve("", List.of()), "", "nothing names a project");
    }

    @Test
    public void aChoiceWithNoFileNamesIt() {
        assertEquals(BoundTestProject.resolve("", List.of("Nafath", "")), "Nafath");
    }

    // Rule-TREE-PANEL-106
    @Test
    public void aChoiceWinsOverTheNameItWasMadeOver() {
        assertEquals(BoundTestProject.resolve("Checkout", List.of("Nafath", "Checkout")), "Nafath",
                "picking over what the file says is what picking is for");
    }

    // Rule-TREE-PANEL-106
    @Test
    public void theFileWinsOnceItNamesSomethingElse() {
        assertEquals(BoundTestProject.resolve("Payments", List.of("Nafath", "Checkout")), "Payments",
                "a colleague changed the file, and that change arrives");
        assertEquals(BoundTestProject.resolve("Checkout", List.of("Nafath", "")), "Checkout",
                "a file that now names a project, where it named none when the choice was made");
    }
}
