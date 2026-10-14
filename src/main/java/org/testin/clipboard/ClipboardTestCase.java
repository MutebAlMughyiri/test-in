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

package org.testin.clipboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;

import java.util.UUID;

// UC-EDITOR-PANEL-017, Rule-INTERNAL-012
record ClipboardTestCase(@JsonProperty(required = true) @NotNull UUID testCaseId, @JsonProperty(required = true) @NotNull TestCase testCase) {
    static @NotNull ClipboardTestCase of(final @NotNull TestCase testCase) {
        return new ClipboardTestCase(testCase.getId(), testCase);
    }

    @NotNull TestCase withItsId() {
        return testCase.setId(testCaseId);
    }
}
