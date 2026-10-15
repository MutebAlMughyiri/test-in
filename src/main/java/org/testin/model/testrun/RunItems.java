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

package org.testin.model.testrun;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// Rule-PRODUCT-009
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class RunItems {
    @NotNull
    @Builder.Default
    private List<RunItem> all = new ArrayList<>();

    public @NotNull RunItems coverOnly(final @NotNull Set<UUID> wanted) {
        final @NotNull Map<UUID, RunItem> held = new LinkedHashMap<>();
        all.forEach(runItem -> held.put(runItem.getUuid(), runItem));

        final @NotNull List<RunItem> covered = new ArrayList<>();
        held.forEach((testCaseUuid, runItem) -> {
            if (wanted.contains(testCaseUuid)) covered.add(runItem);
        });

        wanted.stream()
                .filter(testCaseUuid -> !held.containsKey(testCaseUuid))
                .forEach(testCaseUuid -> covered.add(new RunItem().setUuid(testCaseUuid)));

        return new RunItems().setAll(covered);
    }

    // UC-TREE-PANEL-022, Rule-TREE-PANEL-076, Rule-INTERNAL-117
    public void cover(final @NotNull Set<UUID> wanted) {
        all = coverOnly(wanted).getAll();
    }

    @JsonIgnore
    public boolean isFullyJudged() {
        return !all.isEmpty() && all.stream().allMatch(RunItem::isJudged);
    }

    public @NotNull Set<UUID> coveredUuids() {
        return all.stream().map(RunItem::getUuid).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // Rule-EDITOR-PANEL-127, Rule-REPORT-026
    public @NotNull RunItems inOrderOf(final @NotNull List<UUID> testCaseUuids) {
        final @NotNull Map<UUID, Integer> places = new HashMap<>();
        for (int i = 0; i < testCaseUuids.size(); i++) {
            places.putIfAbsent(testCaseUuids.get(i), i);
        }

        return new RunItems().setAll(all.stream()
                .sorted(Comparator.comparingInt(runItem -> places.getOrDefault(runItem.getUuid(), testCaseUuids.size())))
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    public @NotNull Optional<RunItem> runItemOf(final @NotNull UUID testCaseUuid) {
        return all.stream().filter(runItem -> runItem.getUuid().equals(testCaseUuid)).findFirst();
    }
}