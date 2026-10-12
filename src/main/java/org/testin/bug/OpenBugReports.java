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

package org.testin.bug;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.RunItem;
import org.testin.util.Bundle;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service(Service.Level.PROJECT)
public final class OpenBugReports {
    private final @NotNull Map<RunItemPath, Stage> onTheWay = new HashMap<>();
    private final @NotNull Map<RunItemPath, BugReportEdits> unsent = new HashMap<>();

    void begin(final @NotNull RunItemPath runItemPath) {
        onTheWay.put(runItemPath, Stage.PREPARING);
    }

    void moveTo(final @NotNull RunItemPath runItemPath, final @NotNull Stage stage) {
        onTheWay.replace(runItemPath, stage);
    }

    boolean end(final @NotNull RunItemPath runItemPath, final @NotNull Stage stage) {
        return onTheWay.remove(runItemPath, stage);
    }

    boolean anotherIsOpen(final @NotNull RunItemPath runItemPath) {
        return onTheWay.entrySet().stream().anyMatch(entry -> entry.getValue() == Stage.OPEN && !entry.getKey().equals(runItemPath));
    }

    void keep(final @NotNull RunItemPath runItemPath, final @NotNull BugReportEdits edits) {
        unsent.put(runItemPath, edits);
    }

    @NotNull Optional<BugReportEdits> unsent(final @NotNull RunItemPath runItemPath) {
        return Optional.ofNullable(unsent.get(runItemPath));
    }

    void discard(final @NotNull RunItemPath runItemPath) {
        unsent.remove(runItemPath);
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-072
    public @NotNull Optional<String> whyReportBugIsOff(final @NotNull RunItemPath runItemPath, final @NotNull RunItem runItem) {
        final @NotNull Optional<Stage> stage = Optional.ofNullable(onTheWay.get(runItemPath));
        if (stage.isPresent()) return stage.map(Stage::getReason);

        if (runItem.bugIssue().isPresent()) return Optional.of(Bundle.message("bug.already.reported"));
        if (anotherIsOpen(runItemPath)) return Optional.of(Bundle.message("bug.finish.open.report"));

        return Optional.empty();
    }
}
