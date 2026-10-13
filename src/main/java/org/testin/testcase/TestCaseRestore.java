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
import org.testin.model.TestCaseDto;
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
    static boolean restore(final @NotNull Project p, final @NotNull List<TestCaseSnapshot> target, final @NotNull List<TestCaseSnapshot> expected) {
        if (!expected.stream().allMatch(snapshot -> stillStands(p, snapshot))) {
            Services.getInstance(p, Notifier.class).softRefuse(p,
                    Bundle.message("snapshot.changed.title"),
                    Bundle.message("snapshot.changed.message"));
            return false;
        }

        // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-215
        final @NotNull RestoredTestCases written = new RestoredTestCases();
        final boolean allBack = BackgroundWork.synchronously(p, Bundle.message("remove.undo.progress"), false, () -> {
            boolean all = true;
            for (final TestCaseSnapshot snapshot : target) all &= removeAbsent(p, snapshot, written);
            for (final TestCaseSnapshot snapshot : target) all &= restorePresent(p, snapshot, written);
            return all;
        }, false);

        written.generate(p);
        tellTheSurfaces(p, target);
        return allBack;
    }

    private static void tellTheSurfaces(final @NotNull Project p, final @NotNull List<TestCaseSnapshot> written) {
        final @NotNull TestinEditors editors = Services.getInstance(p, TestinEditors.class);

        written.forEach(snapshot -> editors.reloadOpen(snapshot.testSetPath()));

        written.forEach(snapshot -> ViewToolWindowFactory.refreshIfShowing(p, snapshot.present()));
    }

    // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-255
    private static boolean stillStands(final @NotNull Project p, final @NotNull TestCaseSnapshot snapshot) {
        return Services.getInstance(p, Nodes.class).nodeExists(snapshot.testSetPath())
                && snapshot.sameAs(TestCaseSnapshot.of(p, snapshot.testSetPath(), snapshot.ids()), Services.getInstance(p, JsonCodec.class));
    }

    // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-215
    private static boolean removeAbsent(final @NotNull Project p, final @NotNull TestCaseSnapshot snapshot, final @NotNull RestoredTestCases written) {
        final @NotNull TestCases testCases = Services.getInstance(p, TestCases.class);

        final @NotNull List<TestCaseDto> stillThere = snapshot.absent().stream().flatMap(testCaseId -> testCases.findTestCase(testCaseId).stream()).toList();

        boolean allWent = true;
        for (final TestCaseDto testCase : stillThere) {
            if (testCases.removeTestCase(snapshot.testSetPath(), testCase.getId())) written.removed().add(testCase);
            else allWent = false;
        }

        return allWent;
    }

    // UC-EDITOR-PANEL-017
    private static boolean restorePresent(final @NotNull Project p, final @NotNull TestCaseSnapshot snapshot, final @NotNull RestoredTestCases written) {
        final @NotNull TestCases testCases = Services.getInstance(p, TestCases.class);

        boolean allBack = true;
        for (final TestCaseDto testCase : snapshot.present()) {
            final boolean isComingBack = testCases.findTestCase(testCase.getId()).isEmpty();

            final @NotNull TestCaseDto stored = testCase.copy();
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
