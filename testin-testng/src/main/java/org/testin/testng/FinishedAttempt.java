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

package org.testin.testng;

import org.jetbrains.annotations.NotNull;
import org.testin.model.status.TestNGExecutionStatus;
import org.testin.model.testrun.Failure;

// Rule-CODEGEN-075, Rule-CODEGEN-110
record FinishedAttempt(@NotNull TestNGExecutionStatus testNGExecutionStatus, @NotNull Failure failure) {
}
