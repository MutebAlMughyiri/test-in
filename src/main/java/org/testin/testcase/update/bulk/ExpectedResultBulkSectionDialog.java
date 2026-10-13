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
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.util.Bundle;

import java.util.List;
import java.util.function.Consumer;

public class ExpectedResultBulkSectionDialog extends AbstractJsonSplitBulkSectionDialog {
    public ExpectedResultBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedTestCases, final @NotNull Consumer<List<TestCaseDto>> updatedTestCases) {
        super(p, selectedTestCases, updatedTestCases);
    }

    @Override
    protected @NotNull TestSetEditorAttributes attribute() {
        return TestSetEditorAttributes.EXPECTED_RESULT;
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return Bundle.message("bulk.title.expected.result");
    }

    @Override
    protected @NotNull String getJsonFieldName() {
        return "expectedResult";
    }
}
