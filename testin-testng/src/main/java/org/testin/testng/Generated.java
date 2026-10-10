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
import org.testin.model.TestCaseDto;

import java.util.List;

record Generated(@NotNull TestCaseDto tc, @NotNull List<String> fqcn) {
    @NotNull String pattern() {
        return String.join(".", fqcn.subList(0, fqcn.size() - 1)) + "," + fqcn.getLast();
    }

    @NotNull String simpleClassName() {
        return fqcn.get(fqcn.size() - 2);
    }
}
