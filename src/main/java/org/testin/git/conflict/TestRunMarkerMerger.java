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
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.model.status.TestRunStatus;
import org.testin.util.Bundle;
import org.testin.util.Mapper;
import org.testin.util.TestDataParser;

import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TestRunMarkerMerger {
    private static final @NotNull String STARTED = "executionStartedAt";
    private static final @NotNull String ENDED = "executionEndedAt";
    private static final @NotNull String STATUS = "status";
    private static final @NotNull String COMMIT = "commit";
    private static final @NotNull String MODIFIED_AT = "updatedAt";
    private static final @NotNull String MODIFIED_BY = "updatedBy";

    private static final @NotNull List<String> WRITTEN_INTO = List.of("configuration", "resultAnalysis");

    // UC-SHARE-018, Rule-SHARE-080
    static @NotNull MergeOutcome of(final @NotNull Mapper mapper, final @NotNull String base, final @NotNull String mine, final @NotNull String theirs) {
        final @NotNull ThreeWayMerge merging = ThreeWayMerge.read(mapper, base, mine, theirs);

        execution(merging);
        status(merging);
        audit(merging);

        for (final String object : WRITTEN_INTO) {
            written(merging, object);
        }

        return merging.done();
    }

    private static void execution(final @NotNull ThreeWayMerge merging) {
        take(merging, STARTED, true, Bundle.message("git.merge.execution.started"));
        take(merging, ENDED, false, Bundle.message("git.merge.execution.ended"));
    }

    private static void take(final @NotNull ThreeWayMerge merging, final @NotNull String field, final boolean earliest, final @NotNull String said) {
        final @NotNull ZonedDateTime ours = stamp(merging.mine(), field);
        final @NotNull ZonedDateTime yours = stamp(merging.theirs(), field);
        if (ours.equals(yours)) return;

        final boolean takeTheirs = Config.isNotExecuted(ours)
                || (!Config.isNotExecuted(yours) && (earliest ? yours.isBefore(ours) : yours.isAfter(ours)));

        if (takeTheirs) merging.merged().set(field, merging.theirs().path(field).deepCopy());
        merging.settled().add(said);
    }

    private static void status(final @NotNull ThreeWayMerge merging) {
        final @NotNull TestRunStatus ours = statusIn(merging.mine());
        final @NotNull TestRunStatus yours = statusIn(merging.theirs());
        if (ours == yours) return;

        if (yours.isFurtherThan(ours)) {
            merging.merged().set(STATUS, merging.theirs().path(STATUS).deepCopy());
            // Rule-SHARE-130
            if (merging.theirs().has(COMMIT)) merging.merged().set(COMMIT, merging.theirs().path(COMMIT).deepCopy());
        }
        merging.settled().add(Bundle.message("git.merge.status"));
    }

    private static void audit(final @NotNull ThreeWayMerge merging) {
        if (!stamp(merging.theirs(), MODIFIED_AT).isAfter(stamp(merging.mine(), MODIFIED_AT))) return;

        merging.merged().set(MODIFIED_AT, merging.theirs().path(MODIFIED_AT).deepCopy());
        merging.merged().set(MODIFIED_BY, merging.theirs().path(MODIFIED_BY).deepCopy());
    }

    private static void written(final @NotNull ThreeWayMerge merging, final @NotNull String object) {
        final @NotNull Set<String> keys = new LinkedHashSet<>();
        merging.mine().path(object).fieldNames().forEachRemaining(keys::add);
        merging.theirs().path(object).fieldNames().forEachRemaining(keys::add);

        for (final String key : keys) {
            final @NotNull JsonNode was = merging.base().path(object).path(key);
            final @NotNull JsonNode ours = merging.mine().path(object).path(key);
            final @NotNull JsonNode yours = merging.theirs().path(object).path(key);

            if (ours.equals(yours)) continue;

            if (ours.equals(was)) {
                merging.merged().withObjectProperty(object).set(key, yours.deepCopy());
                merging.settled().add(Bundle.message("git.merge.written", FieldName.of(object + "." + key)));
                continue;
            }
            if (yours.equals(was)) continue;

            merging.questions().add(new Question(object + "." + key, ours.asText(""), yours.asText("")));
        }
    }

    private static @NotNull TestRunStatus statusIn(final @NotNull ObjectNode marker) {
        try {
            return TestRunStatus.valueOf(marker.path(STATUS).asText(TestRunStatus.CREATED.name()));
        } catch (final IllegalArgumentException unknown) {
            return TestRunStatus.CREATED;
        }
    }

    private static @NotNull ZonedDateTime stamp(final @NotNull ObjectNode marker, final @NotNull String field) {
        return TestDataParser.date(marker.path(field).asText("")).orElse(Config.NOT_EXECUTED);
    }
}
