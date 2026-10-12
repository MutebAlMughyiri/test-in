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
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.JsonCodec;

import java.util.List;

public record MergeOutcome(@NotNull ObjectNode merged, @NotNull List<Question> questions, @NotNull List<String> settled) {
    // UC-SHARE-017, Rule-SHARE-109
    static @NotNull String settledSentence(final @NotNull List<String> settled, final @NotNull String separator) {
        final @NotNull String lead = settled.size() == 1
                ? Bundle.message("dialog.conflict.settled.one")
                : Bundle.message("dialog.conflict.settled.many");

        return lead + separator + String.join(separator, settled);
    }

    public boolean isSettled() {
        return questions.isEmpty();
    }

    // UC-SHARE-018
    public void answer(final @NotNull JsonCodec mapper, final @NotNull Question question, final boolean takeTheirs, final @NotNull String theirs) {
        if (!takeTheirs) return;

        final int dot = question.field().indexOf('.');
        if (dot < 0) {
            merged.set(question.field(), mapper.readTree(theirs).path(question.field()).deepCopy());
            return;
        }

        final @NotNull String object = question.field().substring(0, dot);
        final @NotNull String key = question.field().substring(dot + 1);
        final @NotNull JsonNode value = mapper.readTree(theirs).path(object).path(key);

        merged.withObjectProperty(object).set(key, value.deepCopy());
    }
}
