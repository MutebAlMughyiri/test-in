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

import org.testin.codegen.event.NoOpCodeUpdate;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GenTypeActionTest {

    @Test
    public void aDataOnlyAttributeCarriesTheNoOp() {
        for (final GenType<?> type : List.of(
                GenType.UPDATE_TEST_CASE_EXPECTED_RESULT,
                GenType.UPDATE_TEST_CASE_MODULE,
                GenType.UPDATE_TEST_CASE_TEST_DATA,
                GenType.UPDATE_TEST_CASE_PRE_CONDITIONS,
                GenType.UPDATE_TEST_CASE_STEPS,
                GenType.UPDATE_TEST_CASE_PRIORITY,
                GenType.NO_CODE_CHANGE)) {

            assertTrue(type.update() instanceof NoOpCodeUpdate,
                    type + " writes no code, so it must carry the no-op rather than look for a generator");
        }
    }

    @Test
    public void anOperationThatWritesCodeDoesNot() {
        for (final GenType<?> type : List.of(
                GenType.CREATE_TEST_CASE,
                GenType.REMOVE_TEST_CASE,
                GenType.UPDATE_TEST_CASE_DESCRIPTION,
                GenType.UPDATE_TEST_CASE_GROUP,
                GenType.UPDATE_TEST_CASE_ORDER,
                GenType.UPDATE_TEST_CASE_STATUS,
                GenType.CREATE_TEST_SET,
                GenType.RENAME_TEST_SET)) {

            assertFalse(type.update() instanceof NoOpCodeUpdate,
                    type + " writes code, so it must reach a generator");
        }
    }
}
