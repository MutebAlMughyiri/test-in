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

package org.testin.testproject;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.Nodes;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;

import java.nio.file.Path;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestProjectFolder {
    // UC-TREE-PANEL-002, UC-TREE-PANEL-003, Rule-TREE-PANEL-017, Rule-TREE-PANEL-107
    public static @NotNull Optional<Path> free(final @NotNull Project p, final @NotNull String name) {
        final @NotNull Path folder = Services.getInstance(p, TestinRoot.class).absolutePath().resolve(name);

        if (Services.getInstance(p, Nodes.class).isTaken(folder, Optional.empty())) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Refused.ALREADY_EXISTS, name);
            return Optional.empty();
        }

        return Optional.of(folder);
    }
}
