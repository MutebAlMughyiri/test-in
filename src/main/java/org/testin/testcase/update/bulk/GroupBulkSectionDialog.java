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

package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Groups;
import org.testin.model.TestCaseDto;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GroupBulkSectionDialog extends JsonArraySplitBulkSectionDialog {
    public GroupBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems, final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p, selectedItems, updatedItems);
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return Bundle.message("bulk.title.group");
    }

    @Override
    protected @NotNull String getArrayFieldName() {
        return "group";
    }

    @Override
    protected @NotNull List<List<String>> extractOriginalValues(final @NotNull List<TestCaseDto> items) {
        return items.stream().map(tc -> (List<String>) new ArrayList<>(tc.getGroups())).toList();
    }

    // UC-EDITOR-PANEL-007
    @Override
    protected @NotNull List<TestCaseDto> applyValues(final @NotNull List<TestCaseDto> items, final @NotNull List<List<String>> newValues) {
        final @NotNull List<TestCaseDto> written = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            written.add(items.get(i).edit().groups(Groups.read(String.join(",", newValues.get(i)))).build());
        }

        return written;
    }
}