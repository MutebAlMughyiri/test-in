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

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.git.GitFailureNotice;
import org.testin.git.conflict.GitConflictOffer;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.status.RunItemStatus;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testrun.RunItemRecorder;

import java.util.List;

public class GuidesOfferedIdeTest extends AbstractTempRootIdeTest {

    private @NotNull Guides guides() {
        return Services.getInstance(getProject(), Guides.class);
    }

    // Rule-INTERNAL-129
    public void testAnErrorOffersTheLogsGuideAndARefusalDoesNot() {
        guides().forgetAll();

        Services.getInstance(getProject(), Notifier.class).softRefuse(getProject(), "Nothing to Sync");
        assertFalse("a refusal offered the logs guide", guides().offered().contains(Guide.COLLECT_LOGS));

        Services.getInstance(getProject(), Notifier.class).error(getProject(), "Push Failed", "fatal: could not read from remote repository");
        assertTrue("an error did not offer the logs guide", guides().offered().contains(Guide.COLLECT_LOGS));
    }

    // Rule-INTERNAL-129
    public void testAGitFailureOffersGitSharingAndAConflictOffersConflicts() {
        guides().forgetAll();

        GitFailureNotice.show(getProject(), "Sync Failed", "fatal: could not read from remote repository");
        GitConflictOffer.show(getProject(), List.of("Test Cases/Login/a.tc"), () -> {
        }, () -> {
        }, () -> {
        });

        assertEquals(List.of(Guide.SHARE_OVER_GIT, Guide.RESOLVE_GIT_CONFLICTS, Guide.COLLECT_LOGS), guides().offered());
    }

    // Rule-INTERNAL-129
    public void testATestRunOffersItsShortcutsAndAFailedRunItemOffersBugReports() {
        guides().forgetAll();
        final @NotNull TestProjectNode tp = EditorFixtures.testProject(getProject(), root);
        final @NotNull List<TestCaseDto> testCases = EditorFixtures.testCases(getProject(), EditorFixtures.testSet(getProject(), tp, "Login"), 2);
        final @NotNull TestRunNode tr = EditorFixtures.testRun(getProject(), tp, testCases.stream().map(EditorFixtures::pending).toList());

        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), tr, getTestRootDisposable());
        try {
            assertEquals("opening a test run did not offer its shortcuts", List.of(Guide.TEST_RUN_EDITOR_SHORTCUTS), guides().offered());

            Services.getInstance(getProject(), RunItemRecorder.class).applyStatus(editor, List.of(testCases.getFirst()), RunItemStatus.PASSED);
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertFalse("a pass offered the bug reports guide", guides().offered().contains(Guide.RAISE_BUG_REPORTS));

            Services.getInstance(getProject(), RunItemRecorder.class).applyStatus(editor, List.of(testCases.getLast()), RunItemStatus.FAILED);
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertTrue("a failure did not offer the bug reports guide", guides().offered().contains(Guide.RAISE_BUG_REPORTS));
        } finally {
            Disposer.dispose(editor);
        }
    }
}
