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

import org.jetbrains.annotations.NotNull;
import org.testin.model.node.Node;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class DirectoryIdentityTest {

    private static final Path LOGIN = Path.of("NAFATH", "testCases", "Login");

    // Rule-INTERNAL-116
    @Test
    public void twoReadingsOfOneFolderAreOneNode() {
        final TestSetNode first = TestSetNode.builder().path(LOGIN).name("Login").build();
        final TestSetNode second = TestSetNode.builder().path(LOGIN).name("Log in").build();

        assertEquals(second, first);
        assertEquals(new HashSet<>(List.of(first, second)).size(), 1, "a set held one folder twice");
    }

    // Rule-INTERNAL-116
    @Test
    public void twoKindsAtOnePathAreTwoNodes() {
        final @NotNull Node testSetPackage = TestSetPackageNode.builder().path(LOGIN).build();
        final @NotNull Node testSet = TestSetNode.builder().path(LOGIN).build();

        assertNotEquals(testSetPackage, testSet);
    }

    @Test
    public void twoFoldersAreTwoNodes() {
        assertNotEquals(TestSetNode.builder().path(LOGIN.resolveSibling("Logout")).build(), TestSetNode.builder().path(LOGIN).build());
    }
}
