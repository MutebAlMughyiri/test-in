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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.util.Bundle;
import org.testin.util.Mapper;
import org.testin.util.TestDataParser;

import java.time.ZonedDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RunItemMerge {
    private static final @NotNull String EXECUTED_AT = "executedAt";

    // UC-SHARE-018, Rule-SHARE-080
    public static @NotNull Merge of(final @NotNull Mapper mapper, final @NotNull String mine, final @NotNull String theirs) {
        final @NotNull ObjectNode mineNode = mapper.readTree(mine);
        final @NotNull ObjectNode theirsNode = mapper.readTree(theirs);

        if (mineNode.equals(theirsNode)) return new Merge(mineNode, List.of(), List.of());

        final boolean takeTheirs = executedAt(theirsNode).isAfter(executedAt(mineNode));

        return new Merge(takeTheirs ? theirsNode : mineNode, List.of(), List.of(Bundle.message("git.merge.run.item.status")));
    }

    private static @NotNull ZonedDateTime executedAt(final @NotNull ObjectNode runItem) {
        return TestDataParser.date(runItem.path(EXECUTED_AT).asText("")).orElse(Config.NOT_EXECUTED);
    }
}
