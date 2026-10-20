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

package org.testin.robot;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.TestName;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Rule-CODEGEN-119, Rule-CODEGEN-127
record RobotTestBlock(int start, int end, @NotNull String name, @NotNull Optional<TextRange> tagsLine, @NotNull List<String> tags) {
    // Rule-CODEGEN-119
    @NotNull Optional<UUID> testCaseUuid() {
        return tags.stream().findFirst().flatMap(TestName::testCaseUuid);
    }

    int nameEnd() {
        return start + name.length();
    }
}
