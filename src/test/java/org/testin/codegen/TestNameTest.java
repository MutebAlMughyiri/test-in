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

    private static final UUID TEST_CASE_UUID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

    @Test
    public void theFirstAttemptIsTheTestCaseUuid() {
        assertEquals(TestName.testCaseUuid(TEST_CASE_UUID.toString()), Optional.of(TEST_CASE_UUID));
    }

    // Rule-CODEGEN-110
    @Test
    public void aRetriedAttemptNamesTheSameTestCase() {
        assertEquals(TestName.testCaseUuid(TEST_CASE_UUID + " (1)"), Optional.of(TEST_CASE_UUID), "the IDE numbers each attempt TestNG retries");
        assertEquals(TestName.testCaseUuid(TEST_CASE_UUID + " (12)"), Optional.of(TEST_CASE_UUID));
    }

    @Test
    public void aNameThatIsNotAUuidNamesNoTestCase() {
        assertTrue(TestName.testCaseUuid("logsIn").isEmpty());
        assertTrue(TestName.testCaseUuid(TEST_CASE_UUID + " (one)").isEmpty());
    }

    // Rule-CODEGEN-110
    @Test
    public void aDataProviderRowNamesTheSameTestCase() {
        assertEquals(TestName.testCaseUuid(TEST_CASE_UUID + "[1234]"), Optional.of(TEST_CASE_UUID));
        assertEquals(TestName.testCaseUuid(TEST_CASE_UUID + "[0000] (1)"), Optional.of(TEST_CASE_UUID), "a later row is numbered like any other start");
    }

    @Test
    public void aUuidInCapitalsIsTheSameTestCase() {
        assertEquals(TestName.testCaseUuid(TEST_CASE_UUID.toString().toUpperCase(Locale.ROOT)), Optional.of(TEST_CASE_UUID), "the IDE's name is no longer lowercased");
    }
}
