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
import org.testin.model.Priority;
import org.testin.model.TestCase;
import org.testin.testcase.TestSetEditorFields;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class PriorityBulkSectionDialog extends AbstractJsonSplitBulkSectionDialog {
    public PriorityBulkSectionDialog(final @NotNull Project project, final @NotNull List<TestCase> selectedTestCases, final @NotNull Consumer<List<TestCase>> updatedTestCases) {
        super(project, selectedTestCases, updatedTestCases);
    }

    @Override
    protected @NotNull TestSetEditorFields attribute() {
        return TestSetEditorFields.PRIORITY;
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return Bundle.message("bulk.title.priority");
    }

    @Override
    protected @NotNull String getJsonFieldName() {
        return "priority";
    }

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-206
    @Override
    protected @NotNull Optional<TestCase> withValue(final @NotNull TestCase testCase, final @NotNull String value) {
        return super.withValue(testCase, value.isEmpty() ? Priority.DEFAULT.getLabel() : value);
    }
}
