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

package org.testin.codegen;

import org.testng.annotations.Test;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestNameTest {

    private static final UUID ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

    @Test
    public void theFirstAttemptIsTheTestCaseId() {
        assertEquals(TestName.testCaseId(ID.toString()), Optional.of(ID));
    }

    // Rule-CODEGEN-110
    @Test
    public void aRetriedAttemptNamesTheSameTestCase() {
        assertEquals(TestName.testCaseId(ID + " (1)"), Optional.of(ID), "the IDE numbers each attempt TestNG retries");
        assertEquals(TestName.testCaseId(ID + " (12)"), Optional.of(ID));
    }

    @Test
    public void aNameThatIsNotAnIdNamesNoTestCase() {
        assertTrue(TestName.testCaseId("logsIn").isEmpty());
        assertTrue(TestName.testCaseId(ID + " (one)").isEmpty());
    }

    // Rule-CODEGEN-112
    @Test
    public void aDataProviderRowNamesTheSameTestCaseAndKeepsItsRow() {
        assertEquals(TestName.testCaseId(ID + "[1234]"), Optional.of(ID));
        assertEquals(TestName.testCaseId(ID + "[0000] (1)"), Optional.of(ID), "a later row is numbered like any other start");
        assertEquals(TestName.parameters(ID + "[0000] (1)"), "[0000]");
        assertEquals(TestName.parameters(ID + " (2)"), "", "a retry or an invocation has no row");
    }

    @Test
    public void anIdInCapitalsIsTheSameTestCase() {
        assertEquals(TestName.testCaseId(ID.toString().toUpperCase(Locale.ROOT)), Optional.of(ID), "the IDE's name is no longer lowercased");
    }
}
