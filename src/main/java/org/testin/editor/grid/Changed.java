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

package org.testin.editor.grid;

import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChangeType;
import org.testin.model.TestCaseDto;
import org.testin.testcase.TestCaseSnapshot;

import java.util.Set;

record Changed(@NotNull TestCaseDto testCase, @NotNull TestCaseSnapshot before, @NotNull Set<CodeChangeType<TestCaseDto>> generators) {
}
