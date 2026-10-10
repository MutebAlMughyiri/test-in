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


package org.testin.help;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service(Service.Level.PROJECT)
public final class Hints {
    private final @NotNull Project p;

    private final @NotNull Map<SetupStep, Hint> waiting = new EnumMap<>(SetupStep.class);

    public Hints(final @NotNull Project p) {
        this.p = p;
    }

    // UC-INTERNAL-009, Rule-INTERNAL-127, Rule-INTERNAL-129
    public synchronized void fire(final @NotNull Hint hint) {
        waiting.put(hint.step(), hint);
        Services.getInstance(p, Guides.class).add(hint.step().getGuide());
        HelpMark.redraw(p);
    }

    // UC-INTERNAL-009, Rule-INTERNAL-127
    public synchronized void clear(final @NotNull SetupStep step) {
        waiting.remove(step);
        HelpMark.redraw(p);
    }

    // UC-INTERNAL-009, Rule-INTERNAL-127
    public synchronized void clear(final @NotNull SetupStep step, final @NotNull Path subject) {
        Optional.ofNullable(waiting.get(step))
                .filter(hint -> hint.subject().isEmpty() || hint.subject().equals(subject.toString()))
                .ifPresent(_ -> clear(step));
    }

    public synchronized @NotNull List<Hint> waiting() {
        return List.copyOf(waiting.values());
    }
}
