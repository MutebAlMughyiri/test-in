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

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.testin.model.status.NodeStatus;
import org.testin.model.status.TestSetStatus;

import java.util.List;

@Setter
@Getter
@ToString(callSuper = true)
public class TestSetMarker extends AbstractMarker {
    @NonNull
    private TestSetStatus status = TestSetStatus.ACTIVE;

    @Override
    public @NotNull NodeStatus nodeStatus() {
        return status;
    }

    @Override
    public @NotNull List<NodeStatus> nodeStatuses() {
        return List.of(TestSetStatus.values());
    }

    @Override
    public void changeNodeStatus(final @NotNull NodeStatus nodeStatus) {
        setStatus((TestSetStatus) nodeStatus);
    }
}
