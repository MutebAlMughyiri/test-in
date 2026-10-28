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

package org.testin.testcase;

import org.testin.model.TestCase;
import org.testin.util.JsonCodec;
import org.testin.util.RealJsonCodec;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestCaseSnapshotTest {

    private static final Path LOGIN = Path.of("NAFATH", "Test Cases", "Login");

    @Test
    public void aSnapshotIsTheSameOnlyWithTheSameTestCasesAsTheyWereAndTheSameOnesGone() {
        final JsonCodec mapper = RealJsonCodec.build();
        final TestCase logIn = TestCase.builder().uuid(UUID.randomUUID()).description("Log in").order("m").build();
        final TestCase logOut = TestCase.builder().uuid(UUID.randomUUID()).description("Log out").order("n").build();
        final TestCaseSnapshot before = new TestCaseSnapshot(LOGIN, List.of(logIn), List.of());

        assertTrue(before.sameAs(new TestCaseSnapshot(LOGIN, List.of(logIn.copy()), List.of()), mapper), "the same test case as it was is not the same");
        assertFalse(before.sameAs(new TestCaseSnapshot(LOGIN, List.of(logIn), List.of(logOut.getUuid())), mapper), "a test case gone since is the same");
        assertFalse(before.sameAs(new TestCaseSnapshot(LOGIN, List.of(logOut), List.of()), mapper), "another test case is the same");
        assertFalse(before.sameAs(new TestCaseSnapshot(LOGIN, List.of(logIn.edit().description("Sign in").build()), List.of()), mapper), "a test case changed since is the same");
    }
}
