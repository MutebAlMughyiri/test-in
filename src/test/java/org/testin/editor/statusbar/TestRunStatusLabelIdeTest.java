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

package org.testin.editor.statusbar;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.status.TestRunStatus;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

import java.util.List;

public class TestRunStatusLabelIdeTest extends AbstractIdeTest {
    private static @NotNull TestRunMarker aTestRunMarkerIn(final @NotNull TestRunStatus testRunStatus) {
        final @NotNull TestRunMarker marker = new TestRunMarker();
        marker.changeStatus(testRunStatus);
        return marker;
    }

    // Rule-EDITOR-PANEL-265
    public void testAnAssignedTestRunShowsTheTesterBesideItsIcon() {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);
        final @NotNull String was = settings.testerName;
        settings.testerName = "Sara";
        try {
            assertEquals("Sara", EditorStatusBar.besideTheIcon(aTestRunMarkerIn(TestRunStatus.ASSIGNED)));
        } finally {
            settings.testerName = was;
        }
    }

    // Rule-EDITOR-PANEL-265
    public void testACommittedTestRunShowsItsShortCommitBesideItsIcon() {
        final @NotNull TestRunMarker marker = new TestRunMarker();
        marker.recordCommit("ea9a50107afbbaa1831909436b781f0e3c2d1a55");

        assertEquals("ea9a501", EditorStatusBar.besideTheIcon(marker));
    }

    // Rule-EDITOR-PANEL-265
    public void testEveryOtherTestRunStatusShowsTheIconAlone() {
        for (final TestRunStatus testRunStatus : List.of(TestRunStatus.CREATED, TestRunStatus.IN_PROGRESS, TestRunStatus.COMPLETED, TestRunStatus.CLOSED)) {
            assertEquals("a " + testRunStatus + " test run showed text beside its icon", "", EditorStatusBar.besideTheIcon(aTestRunMarkerIn(testRunStatus)));
        }
    }
}
