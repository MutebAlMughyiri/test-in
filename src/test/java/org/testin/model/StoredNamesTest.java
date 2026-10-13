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

package org.testin.model;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class StoredNamesTest {

    // Rule-INTERNAL-003, Rule-INTERNAL-008, Rule-INTERNAL-010
    @Test
    public void everyMarkerIsNamedInFullWords() {
        assertEquals(Arrays.stream(NodeType.values()).map(NodeType::getMarker).toList(),
                List.of(".testproject", ".testcasesfolder", ".testrunsfolder", ".testsetpackage", ".testrunpackage", ".testset", ".testrun"));
    }

    // Rule-INTERNAL-011
    @Test
    public void aTestCaseAndARunItemAreNamedInFullWords() {
        assertEquals(FileKind.TEST_CASE.getExtension(), ".testcase");
        assertEquals(FileKind.RUN_ITEM.getExtension(), ".runitem");
    }
}
