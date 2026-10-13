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

package org.testin.search;

import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.util.Icons;

import javax.swing.Icon;
import java.util.Optional;

public record Hit(@NotNull Icon icon, @NotNull String name, @NotNull String where, @NotNull AbstractNode node, @NotNull Optional<TestCase> testCase) {
    public static @NotNull Hit of(final @NotNull TestCase testCase) {
        return new Hit(Icons.TEST_CASE, testCase.getDescription(), where(testCase.getParent()),
                testCase.getParent(), Optional.of(testCase));
    }

    // UC-INTERNAL-001, Rule-INTERNAL-072, Rule-INTERNAL-098
    public static @NotNull Hit of(final @NotNull TestCase testCase, final @NotNull AbstractNode testRun) {
        return new Hit(testRun.iconShownInTree(), testCase.getDescription(), where(testRun), testRun, Optional.of(testCase));
    }

    // UC-INTERNAL-001, Rule-INTERNAL-072
    public static @NotNull Hit of(final @NotNull AbstractNode node) {
        return new Hit(node.iconShownInTree(), node.getName(), where(node), node, Optional.empty());
    }

    private static @NotNull String where(final @NotNull AbstractNode node) {
        return String.join(" > ", node.treePath());
    }
}
