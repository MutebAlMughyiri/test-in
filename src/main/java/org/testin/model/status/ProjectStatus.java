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

package org.testin.model.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

@Getter
@AllArgsConstructor
public enum ProjectStatus implements NodeStatus {
    ACTIVE(
            Bundle.message("status.project.active"),
            Bundle.message("status.project.active.action"),
            Bundle.message("status.project.active.description"),
            true
    ),

    INACTIVE(
            Bundle.message("status.project.inactive"),
            Bundle.message("status.project.inactive.action"),
            Bundle.message("status.project.inactive.description"),
            false
    );

    private final @NotNull String label;
    private final @NotNull String buttonName;
    private final @NotNull String buttonDescription;

    private final boolean active;
}
