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

package org.testin.util;

import org.testin.model.status.TestCaseStatus;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestCaseStatusParseTest {

    @Test
    public void everyStatusCanBeTypedBackExactlyAsItIsShown() {
        for (final TestCaseStatus status : TestCaseStatus.values()) {
            assertEquals(TestDataParser.testCaseStatus(status.getLabel(), TestCaseStatus.PENDING).orElseThrow(), status,
                    status.getLabel() + " is what the cell prints, so it has to be what the cell accepts");
        }
    }

    @Test
    public void theConstantNameIsAcceptedToo() {
        assertEquals(TestDataParser.testCaseStatus("TO_BE_UPDATED", TestCaseStatus.PENDING).orElseThrow(), TestCaseStatus.TO_BE_UPDATED);
    }

    @Test
    public void caseAndSurroundingSpaceDoNotMatter() {
        assertEquals(TestDataParser.testCaseStatus("  reviewed  ", TestCaseStatus.PENDING).orElseThrow(), TestCaseStatus.REVIEWED);
    }

    // Rule-SHARE-106
    @Test
    public void anythingElseKeepsTheStatusTheTestCaseAlreadyHad() {
        assertTrue(TestDataParser.testCaseStatus("Nonsense", TestCaseStatus.REVIEWED).isEmpty(), "a word Testin cannot read is refused, so the caller can say so");
        assertEquals(TestDataParser.testCaseStatus("", TestCaseStatus.DISABLED).orElseThrow(), TestCaseStatus.DISABLED);
        assertEquals(TestDataParser.testCaseStatus("   ", TestCaseStatus.TO_BE_UPDATED).orElseThrow(), TestCaseStatus.TO_BE_UPDATED);
    }
}
