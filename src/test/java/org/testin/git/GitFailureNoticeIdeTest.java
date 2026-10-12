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

import com.intellij.notification.Notification;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.Said;
import org.testin.util.Bundle;

import java.util.List;

public class GitFailureNoticeIdeTest extends BasePlatformTestCase {

    // Rule-SHARE-127
    public void testAFailedGitStepOffersTheGitLog() {
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        GitFailureNotice.show(getProject(), "Sync Failed", "fatal: could not read from remote repository");

        Await.until("the failure was never shown", () -> !said.isEmpty());
        final @NotNull Notification shown = said.getFirst();
        assertEquals("fatal: could not read from remote repository", shown.getContent());
        assertTrue("the failure offers nothing to press", shown.getActions().stream().anyMatch(action -> Bundle.message("git.show.log").equals(action.getTemplatePresentation().getText())));
    }
}
