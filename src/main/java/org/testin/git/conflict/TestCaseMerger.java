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
import org.testin.model.DateFormats;
import org.testin.model.FileKind;
import org.testin.model.NodeType;
import org.testin.util.Bundle;
import org.testin.util.JsonCodec;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCaseMerger {
    private static final @NotNull String UPDATED_AT = "updatedAt";

    private static final @NotNull String UPDATED_BY = "updatedBy";

    private static final @NotNull Set<String> ORDER = Set.of("order");

    private static final @NotNull Set<String> SETTLED = Set.of("createdAt", "createdBy");

    public static boolean isTestCase(final @NotNull String relativePath) {
        final @NotNull String slashed = relativePath.replace('\\', '/');

        return FileKind.of(Path.of(relativePath)) == FileKind.TEST_CASE && slashed.contains(NodeType.TEST_CASES_FOLDER.getFolderName() + "/");
    }

    // UC-SHARE-018, Rule-SHARE-080, Rule-SHARE-109, Rule-SHARE-078, Rule-SHARE-109, Rule-SHARE-078, Rule-SHARE-081
    public static @NotNull MergeOutcome of(final @NotNull JsonCodec mapper, final @NotNull String base, final @NotNull String mine, final @NotNull String theirs) {
        final @NotNull ThreeWayMerge merging = ThreeWayMerge.read(mapper, base, mine, theirs);
        final @NotNull ObjectNode merged = merging.merged();
        final @NotNull List<String> settled = merging.settled();

        for (final String field : fields(merging.mine(), merging.theirs())) {
            final @NotNull JsonNode was = merging.base().path(field);
            final @NotNull JsonNode ours = merging.mine().path(field);
            final @NotNull JsonNode yours = merging.theirs().path(field);

            if (same(ours, yours)) continue;

            if (same(was, ours)) {
                set(merged, field, yours);
                continue;
            }
            if (same(was, yours)) {
                set(merged, field, ours);
                continue;
            }

            if (UPDATED_AT.equals(field) || UPDATED_BY.equals(field)) {
                addOnce(settled, Bundle.message("git.merge.audit"));
                continue;
            }
            if (ORDER.contains(field)) {
                addOnce(settled, Bundle.message("git.merge.position"));
                set(merged, field, yours);
                continue;
            }
            if (SETTLED.contains(field)) continue;

            merging.questions().add(new Question(field, text(ours), text(yours)));
        }

        stampTheLaterEdit(merged, merging.mine(), merging.theirs());

        return merging.done();
    }

    private static void addOnce(final @NotNull List<String> settled, final @NotNull String said) {
        if (!settled.contains(said)) settled.add(said);
    }

    private static void stampTheLaterEdit(final @NotNull ObjectNode merged, final @NotNull ObjectNode mine, final @NotNull ObjectNode theirs) {
        final @NotNull ZonedDateTime mineAt = DateFormats.stored(text(mine.path(UPDATED_AT))).orElse(DateFormats.NOT_EXECUTED);
        final @NotNull ZonedDateTime theirsAt = DateFormats.stored(text(theirs.path(UPDATED_AT))).orElse(DateFormats.NOT_EXECUTED);

        final @NotNull ObjectNode later = theirsAt.isAfter(mineAt) ? theirs : mine;

        set(merged, UPDATED_AT, later.path(UPDATED_AT));
        set(merged, UPDATED_BY, later.path(UPDATED_BY));
    }

    private static @NotNull Set<String> fields(final @NotNull ObjectNode mine, final @NotNull ObjectNode theirs) {
        final @NotNull Set<String> names = new LinkedHashSet<>();
        mine.fieldNames().forEachRemaining(names::add);
        theirs.fieldNames().forEachRemaining(names::add);

        return names;
    }

    private static void set(final @NotNull ObjectNode target, final @NotNull String field, final @NotNull JsonNode value) {
        if (value.isMissingNode()) target.remove(field);
        else target.set(field, value);
    }

    private static boolean same(final @NotNull JsonNode one, final @NotNull JsonNode other) {
        final boolean oneEmpty = one.isMissingNode() || one.isNull();
        final boolean otherEmpty = other.isMissingNode() || other.isNull();

        if (oneEmpty || otherEmpty) return oneEmpty == otherEmpty;

        return one.equals(other);
    }

    private static @NotNull String text(final @NotNull JsonNode value) {
        if (value.isMissingNode() || value.isNull()) return "";

        return value.isValueNode() ? value.asText() : value.toString();
    }
}
