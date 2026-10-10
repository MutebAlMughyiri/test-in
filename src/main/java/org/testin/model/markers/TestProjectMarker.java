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

package org.testin.model.markers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.testin.model.status.NodeStatus;
import org.testin.model.status.ProjectStatus;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;

@Setter
@Getter
@ToString(callSuper = true)
public class TestProjectMarker extends AbstractMarker {
    // Rule-INTERNAL-091
    public static final int FORMAT = 3;

    @NonNull
    private ProjectStatus status = ProjectStatus.ACTIVE;

    // Rule-INTERNAL-091
    private int format;

    // Rule-INTERNAL-091
    @JsonIgnore
    public @NotNull Optional<String> whyNotReadable() {
        if (format == FORMAT) return Optional.empty();

        return Optional.of(format > FORMAT
                ? Bundle.message("scan.newer.format")
                : Bundle.message("scan.older.format"));
    }

    @Override
    public @NotNull NodeStatus status() {
        return status;
    }

    @Override
    public @NotNull List<NodeStatus> statuses() {
        return List.of(ProjectStatus.values());
    }

    @Override
    public void applyStatus(final @NotNull NodeStatus status) {
        setStatus((ProjectStatus) status);
    }
}
