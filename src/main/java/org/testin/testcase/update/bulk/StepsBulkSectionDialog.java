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
import org.testin.model.TestCaseDto;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class StepsBulkSectionDialog extends JsonArraySplitBulkSectionDialog {
    public StepsBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems, final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p, selectedItems, updatedItems);
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return Bundle.message("bulk.title.steps");
    }

    @Override
    protected @NotNull String getArrayFieldName() {
        return "steps";
    }

    @Override
    protected @NotNull List<List<String>> extractOriginalValues(final @NotNull List<TestCaseDto> items) {
        final @NotNull List<List<String>> originalSteps = new ArrayList<>();

        for (final TestCaseDto tc : items) {
            originalSteps.add(new ArrayList<>(tc.getSteps()));
        }

        return originalSteps;
    }

    @Override
    protected @NotNull List<TestCaseDto> applyValues(final @NotNull List<TestCaseDto> items, final @NotNull List<List<String>> newValues) {
        final @NotNull List<TestCaseDto> written = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            final @NotNull List<String> cleanSteps = new ArrayList<>();

            for (final String step : newValues.get(i)) {
                final @NotNull String cleanStr = Objects.toString(step, "").trim();

                if (!cleanStr.isEmpty()) {
                    cleanSteps.add(cleanStr);
                }
            }

            written.add(items.get(i).edit().steps(cleanSteps).build());
        }

        return written;
    }
}