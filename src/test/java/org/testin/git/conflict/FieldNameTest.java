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

package org.testin.git.conflict;

import org.testin.model.testrun.TestRunConfiguration;
import org.testin.model.testrun.TestRunResultAnalysis;
import org.testin.testcase.TestSetEditorFields;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class FieldNameTest {

    // Rule-SHARE-081
    @Test
    public void everyStoredTestCaseFieldIsNamedAsTheTesterKnowsIt() {
        for (final TestSetEditorFields field : TestSetEditorFields.values()) {
            if (field.getStoredKey().isEmpty()) continue;

            assertEquals(FieldName.of(field.getStoredKey()), field.getName(), "the key " + field.getStoredKey() + " is not named " + field.getName());
        }
    }

    @Test
    public void aTestRunsConfigurationKeyIsNamedByTheQuestionTheTesterAnswered() {
        final TestRunConfiguration question = TestRunConfiguration.TEST_TYPE;

        assertEquals(FieldName.of("configuration.testType"), question.getDisplayName(),
                "the key is named as the test run creation dialog names it");
    }

    @Test
    public void aResultAnalysisKeyIsNamedByItsHeading() {
        final TestRunResultAnalysis heading = TestRunResultAnalysis.PASSED;

        assertEquals(FieldName.of("resultAnalysis." + heading.name()), heading.getRunItemStatus().getLabel(),
                "the key is named as the analysis dialog heads it");
    }

    @Test
    public void aFieldNoEnumCarriesKeepsItsOwnName() {
        assertEquals(FieldName.of("somethingOnlyTheFileHas"), "somethingOnlyTheFileHas",
                "the merge works on the file, which may hold more than the model does");
        assertEquals(FieldName.of("configuration.SOMETHING_ELSE"), "configuration.SOMETHING_ELSE",
                "and a key inside an object keeps the whole name, so it is still findable");
    }
}
