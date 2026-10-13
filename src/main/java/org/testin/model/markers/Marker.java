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
import org.jetbrains.annotations.NotNull;
import org.testin.model.status.NoNodeStatus;
import org.testin.model.status.NodeStatus;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public interface Marker {
    int NOT_ORDERED = Integer.MAX_VALUE;

    @NotNull String getId();

    void setId(@NotNull String markerId);

    int getOrder();

    void setOrder(int order);

    @NotNull String getCreatedBy();

    void setCreatedBy(@NotNull String createdBy);

    @NotNull ZonedDateTime getCreatedAt();

    void setCreatedAt(@NotNull ZonedDateTime createdAt);

    @NotNull String getUpdatedBy();

    void setUpdatedBy(@NotNull String updatedBy);

    @NotNull ZonedDateTime getUpdatedAt();

    void setUpdatedAt(@NotNull ZonedDateTime updatedAt);

    default void stampCreated(final @NotNull String tester) {
        final @NotNull ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);
        setCreatedBy(tester);
        setCreatedAt(now);
        setUpdatedBy(tester);
        setUpdatedAt(now);
    }

    default void touch(final @NotNull String tester) {
        setUpdatedBy(tester);
        setUpdatedAt(ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS));
    }

    default @NotNull NodeStatus nodeStatus() {
        return NoNodeStatus.NONE;
    }

    default @NotNull List<NodeStatus> nodeStatuses() {
        return List.of();
    }

    default void changeNodeStatus(final @NotNull NodeStatus nodeStatus) {
    }

    @JsonIgnore
    default @NotNull String getStatusLabel() {
        return nodeStatus().getLabel();
    }

    @JsonIgnore
    default @NotNull List<DetailRow> getDetailRows() {
        return List.of();
    }
}
