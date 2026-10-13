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

package org.testin.testcase;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.open.TestinEditors;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.JsonCodec;
import org.testin.view.ViewToolWindowFactory;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TestCaseRestore {
    // UC-INTERNAL-005, Rule-INTERNAL-063
    static boolean restore(final @NotNull Project project, final @NotNull List<TestCaseSnapshot> target, final @NotNull List<TestCaseSnapshot> expected) {
        if (!expected.stream().allMatch(snapshot -> stillStands(project, snapshot))) {
            Services.getInstance(project, Notifier.class).softRefuse(project,
                    Bundle.message("snapshot.changed.title"),
                    Bundle.message("snapshot.changed.message"));
            return false;
        }

        // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-215
        final @NotNull RestoredTestCases written = new RestoredTestCases();
        final boolean allBack = BackgroundWork.synchronously(project, Bundle.message("remove.undo.progress"), false, () -> {
            boolean all = true;
            for (final TestCaseSnapshot snapshot : target) all &= removeAbsent(project, snapshot, written);
            for (final TestCaseSnapshot snapshot : target) all &= restorePresent(project, snapshot, written);
            return all;
        }, false);

        written.generate(project);
        tellTheSurfaces(project, target);
        return allBack;
    }

    private static void tellTheSurfaces(final @NotNull Project project, final @NotNull List<TestCaseSnapshot> written) {
        final @NotNull TestinEditors editors = Services.getInstance(project, TestinEditors.class);

        written.forEach(snapshot -> editors.reloadOpen(snapshot.testSetPath()));

        written.forEach(snapshot -> ViewToolWindowFactory.refreshIfShowing(project, snapshot.present()));
    }

    // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-255
    private static boolean stillStands(final @NotNull Project project, final @NotNull TestCaseSnapshot snapshot) {
        return Services.getInstance(project, Nodes.class).nodeExists(snapshot.testSetPath())
                && snapshot.sameAs(TestCaseSnapshot.of(project, snapshot.testSetPath(), snapshot.ids()), Services.getInstance(project, JsonCodec.class));
    }

    // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-215
    private static boolean removeAbsent(final @NotNull Project project, final @NotNull TestCaseSnapshot snapshot, final @NotNull RestoredTestCases written) {
        final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);

        final @NotNull List<TestCase> stillThere = snapshot.absent().stream().flatMap(testCaseId -> testCases.findTestCase(testCaseId).stream()).toList();

        boolean allWent = true;
        for (final TestCase testCase : stillThere) {
            if (testCases.removeTestCase(snapshot.testSetPath(), testCase.getId())) written.removed().add(testCase);
            else allWent = false;
        }

        return allWent;
    }

    // UC-EDITOR-PANEL-017
    private static boolean restorePresent(final @NotNull Project project, final @NotNull TestCaseSnapshot snapshot, final @NotNull RestoredTestCases written) {
        final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);

        boolean allBack = true;
        for (final TestCase testCase : snapshot.present()) {
            final boolean isComingBack = testCases.findTestCase(testCase.getId()).isEmpty();

            final @NotNull TestCase stored = testCase.copy();
            if (!testCases.putTestCaseVerbatim(snapshot.testSetPath(), stored)) {
                allBack = false;
                continue;
            }

            written.landed().add(stored);
            if (isComingBack) written.comingBack().add(stored);
        }

        return allBack;
    }
}
