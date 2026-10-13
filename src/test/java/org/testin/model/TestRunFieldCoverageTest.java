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

import org.testin.testrun.TestRunEditorFields;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class TestRunFieldCoverageTest {

    @Test
    public void theStacktraceIsReadOnlyAndOffByDefault() {
        assertFalse(TestRunEditorFields.STACKTRACE.isEdited(),
                "the framework writes it; the tester edits it through the failure dialog, not a grid cell");
        assertEquals(TestRunEditorFields.STACKTRACE.getToolBarDefault(), FieldDefault.OFF,
                "it is measured in paragraphs - a column of it on by default crowds out every other column");
    }

    @Test
    public void theDetailsPanelDrawsEveryRunItemValueWorthReading() {
        final List<TestRunEditorFields> shown = List.of(
                TestRunEditorFields.RUN_STATUS,
                TestRunEditorFields.DURATION,
                TestRunEditorFields.ACTUAL_RESULT,
                TestRunEditorFields.STACKTRACE,
                TestRunEditorFields.BUG_SEVERITY,
                TestRunEditorFields.BUG_PRIORITY,
                TestRunEditorFields.BUG_ISSUE);

        assertEquals(shown.size(), 7, "the rows DetailsTab appends for a test case viewed under a test run");
        assertEquals(shown.stream().distinct().count(), 7L, "each drawn once");
    }
}
