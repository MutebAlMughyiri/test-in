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
import org.testin.model.status.NoStatus;
import org.testin.model.status.NodeStatus;
import org.testin.model.status.PackageStatus;
import org.testin.model.status.ProjectStatus;
import org.testin.model.status.TestSetStatus;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class StatusWorthShowingTest {

    private static void assertActiveIs(final @NotNull NodeStatus @NotNull [] values, final @NotNull NodeStatus expected) {
        final @NotNull List<NodeStatus> active = Arrays.stream(values).filter(NodeStatus::isActive).toList();

        assertEquals(List.of(expected), active, expected.getClass().getSimpleName() + " must have exactly one status meaning \"in current work\","
                + " because the tree draws every other one beside the name. Active: " + active);
    }

    @Test
    public void exactlyOneStatusPerEnumIsTheActiveOne() {
        assertActiveIs(ProjectStatus.values(), ProjectStatus.ACTIVE);
        assertActiveIs(TestSetStatus.values(), TestSetStatus.ACTIVE);
        assertActiveIs(PackageStatus.values(), PackageStatus.ACTIVE);
    }

    @Test
    public void theStatusesATesterSeesAreTheOnesThatAreNotActive() {
        assertFalse(ProjectStatus.INACTIVE.isActive(), "an inactive test project says so beside its name");
        assertFalse(TestSetStatus.DEPRECATED.isActive(), "a deprecated test set says so");
        assertFalse(PackageStatus.ARCHIVED.isActive(), "an archived package says so");
    }

    @Test
    public void noStatusAtAllIsNothingToSay() {
        assertTrue(NoStatus.NONE.isActive(), "a marker with no status has nothing to draw");
        assertTrue(NoStatus.NONE.getLabel().isEmpty(), "and nothing to draw it with");
    }
}
