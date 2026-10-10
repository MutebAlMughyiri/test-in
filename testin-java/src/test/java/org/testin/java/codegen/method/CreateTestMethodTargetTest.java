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

package org.testin.java.codegen.method;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class CreateTestMethodTargetTest {

    @Test
    public void anEmptyListHasNoClassOrMethod() {
        assertFalse(CreateTestMethod.parse(List.of()).isPresent());
    }

    @Test
    public void aSingleSegmentIsAMethodWithNoClass() {
        assertFalse(CreateTestMethod.parse(List.of("shouldDoSomething")).isPresent());
    }

    @Test
    public void aClassAndMethodAreTheSmallestUsableList() {
        final Target target =
                CreateTestMethod.parse(List.of("LoginTest", "shouldLogIn")).orElseThrow();

        assertEquals(target.className(), "LoginTest");
        assertEquals(target.methodName(), "shouldLogIn");
        assertEquals(target.path(), "LoginTest");
        assertEquals(target.packageList(), List.of(), "no package segments are left over");
    }

    @Test
    public void packageSegmentsAreEverythingBeforeTheClass() {
        final Target target =
                CreateTestMethod.parse(List.of("org", "example", "tests", "LoginTest", "shouldLogIn")).orElseThrow();

        assertEquals(target.className(), "LoginTest");
        assertEquals(target.methodName(), "shouldLogIn");
        assertEquals(target.path(), "org.example.tests.LoginTest");
        assertEquals(target.packageList(), List.of("org", "example", "tests"));
    }
}
