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

package org.testin.filter;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public class FilterMembershipTest {

    // UC-EDITOR-PANEL-049, Rule-EDITOR-PANEL-274
    @Test
    public void unchoosingTheSortThatIsOnKeepsItOn() {
        final @NotNull Set<SortField> chosen = new HashSet<>(Set.of(SortField.PRIORITY));

        FilterMembership.<SortField>single().apply(SortField.PRIORITY, chosen, false);

        assertEquals(chosen, Set.of(SortField.PRIORITY), "a sort was left with no field");
    }
}
