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

package org.testin.git;

import com.intellij.ide.trustedProjects.TrustedProjects;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.Said;
import org.testin.explorer.TreePanel;
import org.testin.testproject.CloneTestProject;
import org.testin.util.Bundle;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SafeModeIdeTest extends AbstractIdeTest {

    private static final @NotNull String TRUST_CHECKED_IN_TESTS = "idea.trust.headless.disabled";

    private void inSafeMode(final @NotNull Runnable gesture) {
        final @NotNull String checkedBefore = System.getProperty(TRUST_CHECKED_IN_TESTS, "true");
        System.setProperty(TRUST_CHECKED_IN_TESTS, "false");
        TrustedProjects.setProjectTrusted(getProject(), false);
        try {
            gesture.run();
        } finally {
            TrustedProjects.setProjectTrusted(getProject(), true);
            System.setProperty(TRUST_CHECKED_IN_TESTS, checkedBefore);
        }
    }

    // Rule-SHARE-132
    public void testAGitStepInSafeModeRunsNoGitAndSaysTrustProjectOnce() {
        inSafeMode(() -> {
            final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());
            final @NotNull AtomicBoolean started = new AtomicBoolean();

            GitBackgroundTask.run(getProject(), "Syncing", false, _ -> started.set(true), _ -> started.set(true));
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertFalse("a Git step started in safe mode", started.get());
            assertEquals("safe mode was not said once, naming Trust Project", List.of(Bundle.message("git.safe.mode")), said.shown());
        });
    }

    // UC-TREE-PANEL-003, Rule-SHARE-132
    public void testACloneInSafeModeStartsNothingAndSaysTrustProjectOnce() {
        final @NotNull TreePanel panel = new TreePanel(getProject());
        Disposer.register(getTestRootDisposable(), panel);

        inSafeMode(() -> {
            final @NotNull List<String> said = Said.during(getProject(), () -> new CloneTestProject(getProject(), "https://github.com/acme/nafath-test-cases.git", "NAFATH", panel).execute());

            assertEquals("safe mode was not said once, naming Trust Project", List.of(Bundle.message("git.safe.mode")), said);
        });
    }

    // Rule-SHARE-132
    public void testATrustedProjectIsNotRefusedGit() {
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        assertFalse("a trusted project was refused Git", new GitRepositoryService(getProject()).refusedInSafeMode());
        assertEquals("a trusted project was told about safe mode", List.of(), said.shown());
    }
}
