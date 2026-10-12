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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;
import org.testin.util.JsonCodec;

import java.util.ArrayList;
import java.util.List;

record ThreeWayMerge(@NotNull ObjectNode base, @NotNull ObjectNode mine, @NotNull ObjectNode theirs, @NotNull ObjectNode merged, @NotNull List<Question> questions, @NotNull List<String> settled) {
    // UC-SHARE-018, Rule-SHARE-080
    static @NotNull ThreeWayMerge read(final @NotNull JsonCodec mapper, final @NotNull String base, final @NotNull String mine, final @NotNull String theirs) {
        final @NotNull ObjectNode ours = mapper.readTree(mine);

        return new ThreeWayMerge(mapper.readTree(base), ours, mapper.readTree(theirs), ours.deepCopy(), new ArrayList<>(), new ArrayList<>());
    }

    @NotNull MergeOutcome done() {
        return new MergeOutcome(merged, List.copyOf(questions), List.copyOf(settled));
    }
}
