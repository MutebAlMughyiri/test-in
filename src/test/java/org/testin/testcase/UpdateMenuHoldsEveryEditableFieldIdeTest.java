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


import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.testin.AbstractIdeTest;

// Rule-EDITOR-PANEL-259
public class UpdateMenuHoldsEveryEditableFieldIdeTest extends AbstractIdeTest {

    public void testEveryFieldEditableInTheGridIsOnTheUpdateMenu() {
        final Set<String> onTheMenu = Arrays.stream(UpdateTestCaseFields.values())
                .map(UpdateTestCaseFields::getName)
                .collect(Collectors.toSet());

        final List<String> missing = Arrays.stream(TestSetEditorFields.values())
                .filter(field -> field.can(Can.EDIT))
                .map(TestSetEditorFields::getName)
                .filter(name -> !onTheMenu.contains(name))
                .toList();

        assertEmpty("Editable in the grid but missing from the update menu: " + missing
                + ". Give each one a section, a bulk dialog and an UpdateTestCaseFields entry.", missing);
    }
}
