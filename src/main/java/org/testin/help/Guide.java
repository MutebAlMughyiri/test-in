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

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

@Getter
@AllArgsConstructor
public enum Guide {
    GETTING_STARTED(
            Bundle.message("guide.getting.started"),
            "guides/firstRun.md"
    ),

    SET_UP_THIS_MACHINE(
            Bundle.message("guide.set.up.this.machine"),
            "guides/setUpThisMachine.md"
    ),

    LINK_THIS_REPOSITORY(
            Bundle.message("guide.link.this.repository"),
            "guides/linkThisRepository.md"
    ),

    SHARE_OVER_GIT(
            Bundle.message("guide.share.over.git"),
            "guides/shareOverGit.md"
    ),

    RESOLVE_GIT_CONFLICTS(
            Bundle.message("guide.resolve.git.conflicts"),
            "guides/resolveGitConflicts.md"
    ),

    RAISE_BUG_REPORTS(
            Bundle.message("guide.raise.bug.reports"),
            "guides/raiseBugReports.md"
    ),

    AUTOMATION_CODE(
            Bundle.message("guide.automation.code"),
            "guides/automationCode.md"
    ),

    TEST_CASE_EDITOR_SHORTCUTS(
            Bundle.message("guide.test.set.editor.shortcuts"),
            "guides/testSetEditorShortcuts.md"
    ),

    TEST_RUN_EDITOR_SHORTCUTS(
            Bundle.message("guide.test.run.editor.shortcuts"),
            "guides/testRunEditorShortcuts.md"
    ),

    COLLECT_LOGS(
            Bundle.message("guide.collect.logs"),
            "guides/collectLogs.md"
    );

    private final @NotNull String title;

    private final @NotNull String page;

    // UC-INTERNAL-009, Rule-INTERNAL-130
    public void open(final @NotNull Project p) {
        new GuideDialog(p, this).show();
    }
}
