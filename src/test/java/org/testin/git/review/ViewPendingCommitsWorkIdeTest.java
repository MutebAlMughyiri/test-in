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

package org.testin.git.review;

import com.intellij.notification.Notification;
import org.jetbrains.annotations.NotNull;
import org.testin.Said;
import org.testin.git.AbstractGitRemoteIdeTest;
import org.testin.util.Bundle;

import java.util.List;

public class ViewPendingCommitsWorkIdeTest extends AbstractGitRemoteIdeTest {

    // UC-SHARE-015, Rule-SHARE-066, Rule-SHARE-067
    public void testCommitsNotOnTheRemoteAreCountedRatherThanReportedAsNoChanges() {
        write(work, "a.tc", "{}");
        write(work, "b.tc", "{}");
        write(work, "c.tc", "{}");
        commitAll(work, "three test cases");
        write(work, "d.tc", "{}");
        commitAll(work, "one more");
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        new ViewPendingCommitsWork(getProject()).openFor(work);

        final @NotNull Notification waiting = titled(said, Bundle.message("git.not.pushed.title"));
        assertEquals("two commits holding four files", Bundle.message("git.not.pushed.many", "2"), waiting.getContent());
        assertEquals(List.of(Bundle.message("git.push.action")), answers(waiting));
    }
}
