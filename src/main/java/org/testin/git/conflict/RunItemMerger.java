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
import org.testin.model.DateFormats;
import org.testin.model.status.RunItemStatus;
import org.testin.util.Bundle;
import org.testin.util.JsonCodec;
import org.testin.util.TestCaseFieldParser;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RunItemMerger {
    private static final @NotNull String EXECUTIONS = "executions";

    private static final @NotNull String EXECUTED_AT = "executedAt";

    private static final @NotNull String DEFAULT = "default";

    static final @NotNull String STATUS = "status";

    private static final @NotNull String BUG_ISSUE_URL = "bugIssueUrl";

    // UC-SHARE-018, Rule-SHARE-080, Rule-SHARE-081, Rule-SHARE-133, Rule-EDITOR-PANEL-136, Rule-VIEW-PANEL-119
    public static @NotNull MergeOutcome of(final @NotNull JsonCodec mapper, final @NotNull String mine, final @NotNull String theirs) {
        final @NotNull ObjectNode mineNode = mapper.readTree(mine);
        final @NotNull ObjectNode theirsNode = mapper.readTree(theirs);

        if (mineNode.equals(theirsNode)) return new MergeOutcome(mineNode, List.of(), List.of());

        final boolean theirsLater = lastRecorded(theirsNode).isAfter(lastRecorded(mineNode));
        final @NotNull ObjectNode later = theirsLater ? theirsNode : mineNode;
        final @NotNull List<JsonNode> joined = joined(later, theirsLater ? mineNode : theirsNode);
        final @NotNull List<ExecutionIdentity> identities = joined.stream().map(ExecutionIdentity::of).toList();
        final int chosen = executions(later).skip(Math.max(0, later.path(DEFAULT).asInt(1) - 1)).findFirst().map(ExecutionIdentity::of).map(identities::indexOf).orElse(-1);

        final @NotNull ObjectNode merged = later.deepCopy();
        merged.put(DEFAULT, chosen < 0 ? joined.size() : chosen + 1);
        final @NotNull ArrayNode executions = merged.putArray(EXECUTIONS);
        joined.forEach(executions::add);

        return new MergeOutcome(merged, List.of(), List.of(Bundle.message("git.merge.run.item.status")));
    }

    // Rule-EDITOR-PANEL-136, Rule-TREE-PANEL-067, Rule-SHARE-133
    private static @NotNull List<JsonNode> joined(final @NotNull ObjectNode later, final @NotNull ObjectNode earlier) {
        final @NotNull Map<ExecutionIdentity, JsonNode> byIdentity = new LinkedHashMap<>();
        Stream.concat(executions(later), executions(earlier))
                .filter(RunItemMerger::isRecorded)
                .forEach(execution -> byIdentity.merge(ExecutionIdentity.of(execution), execution.deepCopy(), RunItemMerger::keepTheIssue));

        final @NotNull List<JsonNode> recorded = new ArrayList<>(byIdentity.values());
        recorded.sort(Comparator.comparing(RunItemMerger::executedAt));

        return recorded.isEmpty() ? Stream.concat(executions(later), executions(earlier)).max(Comparator.comparing(RunItemMerger::isUntested)).stream().toList() : recorded;
    }

    // Rule-SHARE-133
    private static @NotNull JsonNode keepTheIssue(final @NotNull JsonNode kept, final @NotNull JsonNode other) {
        if (kept.path(BUG_ISSUE_URL).asText("").isEmpty() && other.hasNonNull(BUG_ISSUE_URL) && kept instanceof final ObjectNode keptExecution)
            keptExecution.set(BUG_ISSUE_URL, other.get(BUG_ISSUE_URL));
        return kept;
    }

    private static @NotNull Stream<JsonNode> executions(final @NotNull ObjectNode runItem) {
        return StreamSupport.stream(runItem.path(EXECUTIONS).spliterator(), false);
    }

    private static boolean isRecorded(final @NotNull JsonNode execution) {
        return Arrays.stream(RunItemStatus.values()).filter(runItemExecutionStatus -> runItemExecutionStatus.name().equals(execution.path(STATUS).asText(""))).anyMatch(RunItemStatus::isJudged);
    }

    private static boolean isUntested(final @NotNull JsonNode execution) {
        return RunItemStatus.UNTESTED.name().equals(execution.path(STATUS).asText(""));
    }

    static @NotNull ZonedDateTime executedAt(final @NotNull JsonNode execution) {
        return TestCaseFieldParser.date(execution.path(EXECUTED_AT).asText("")).orElse(DateFormats.NOT_EXECUTED);
    }

    private static @NotNull ZonedDateTime lastRecorded(final @NotNull ObjectNode runItem) {
        return executions(runItem).map(RunItemMerger::executedAt).max(Comparator.naturalOrder()).orElse(DateFormats.NOT_EXECUTED);
    }
}
