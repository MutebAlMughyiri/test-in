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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.util.Bundle;
import org.testin.util.Mapper;
import org.testin.util.TestCaseFieldParser;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RunItemMerger {
    private static final @NotNull String EXECUTIONS = "executions";
    private static final @NotNull String EXECUTED_AT = "executedAt";
    private static final @NotNull String DEFAULT = "default";

    // UC-SHARE-018, Rule-SHARE-080, Rule-SHARE-081
    public static @NotNull MergeOutcome of(final @NotNull Mapper mapper, final @NotNull String mine, final @NotNull String theirs) {
        final @NotNull ObjectNode mineNode = mapper.readTree(mine);
        final @NotNull ObjectNode theirsNode = mapper.readTree(theirs);

        if (mineNode.equals(theirsNode)) return new MergeOutcome(mineNode, List.of(), List.of());

        final @NotNull ObjectNode later = lastRecorded(theirsNode).isAfter(lastRecorded(mineNode)) ? theirsNode : mineNode;
        final @NotNull ObjectNode merged = later.deepCopy();
        final @NotNull ArrayNode joined = merged.putArray(EXECUTIONS);

        Stream.concat(executions(mineNode), executions(theirsNode))
                .map(RunItemMerger::unflagged)
                .distinct()
                .sorted(Comparator.comparing(RunItemMerger::executedAt))
                .forEach(joined::add);

        executions(later).filter(execution -> execution.path(DEFAULT).asBoolean()).findFirst().map(RunItemMerger::unflagged)
                .flatMap(chosen -> StreamSupport.stream(joined.spliterator(), false).filter(chosen::equals).findFirst())
                .ifPresent(chosen -> ((ObjectNode) chosen).put(DEFAULT, true));

        return new MergeOutcome(merged, List.of(), List.of(Bundle.message("git.merge.run.item.status")));
    }

    private static @NotNull Stream<JsonNode> executions(final @NotNull ObjectNode runItem) {
        return StreamSupport.stream(runItem.path(EXECUTIONS).spliterator(), false);
    }

    private static @NotNull JsonNode unflagged(final @NotNull JsonNode execution) {
        final @NotNull JsonNode copy = execution.deepCopy();
        if (copy instanceof ObjectNode object) object.remove(DEFAULT);
        return copy;
    }

    private static @NotNull ZonedDateTime executedAt(final @NotNull JsonNode execution) {
        return TestCaseFieldParser.date(execution.path(EXECUTED_AT).asText("")).orElse(Config.NOT_EXECUTED);
    }

    private static @NotNull ZonedDateTime lastRecorded(final @NotNull ObjectNode runItem) {
        return executions(runItem).map(RunItemMerger::executedAt).max(Comparator.naturalOrder()).orElse(Config.NOT_EXECUTED);
    }
}
