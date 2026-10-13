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

package org.testin.testrun.failure;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.Failure;
import org.testin.model.testrun.RunItem;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogComponent;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public final class FailureFields {
    private final @NotNull ActualResultSection actualResult;
    private final @NotNull BugSeveritySection bugSeverity;
    private final @NotNull BugPrioritySection bugPriority;
    private final @NotNull StacktraceSection stacktrace;
    private final @NotNull ScreenshotsSection screenshots;

    private final @NotNull List<FailureSection> sections;

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-148
    public FailureFields(final @NotNull Project project, final @NotNull Path testRunPath, final @NotNull RunItem runItem) {
        actualResult = new ActualResultSection(project, runItem);
        bugSeverity = BugSeveritySection.of(runItem);
        bugPriority = BugPrioritySection.of(runItem);
        stacktrace = StacktraceSection.of(project, runItem);
        screenshots = new ScreenshotsSection(project, testRunPath, runItem);

        sections = List.of(actualResult, bugSeverity, bugPriority, stacktrace, screenshots);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-148
    public @NotNull List<? extends DialogPart<?>> components() {
        return sections.stream().map(FailureSection::component).toList();
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-145
    public @NotNull Failure failure() {
        return new Failure(actualResult.typed(), stacktrace.typed(), screenshots.names());
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-147
    public void applyBugTo(final @NotNull RunItem runItem) {
        runItem.recordBug(bugSeverity.selected(), bugPriority.selected());
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    public void storePasted(final @NotNull Function<List<byte[]>, List<String>> store) {
        screenshots.storePasted(store);
    }

    // Rule-EDITOR-PANEL-202, Rule-EDITOR-PANEL-219, Rule-INTERNAL-097
    public void onResized(final @NotNull Runnable resized) {
        screenshots.onChange(resized);
        actualResult.growsWith(resized);
    }

    public @NotNull DialogComponent actualResult() {
        return actualResult.component().getComponent();
    }
}
