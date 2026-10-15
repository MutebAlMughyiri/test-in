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

package org.testin.services;

import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.AsyncPromise;
import org.testin.AbstractIdeTest;
import org.testin.Await;
import org.testin.TestinLog;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackgroundWorkIdeTest extends AbstractIdeTest {

    // Rule-CODEGEN-089
    public void testTheEndIsReportedWhenTheTesterCancels() {
        final AtomicBoolean reported = new AtomicBoolean();

        BackgroundWork.run(getProject(), "Writing bodies", "Writing failed", true, ProgressIndicator::cancel, () -> reported.set(true));

        Await.until("a canceled work never reported how it ended", reported::get);
    }

    public void testACancelledReadIsNotLoggedAsAFailureAndAFailedOneIs() {
        final @NotNull List<String> logged = TestinLog.during(() -> {
            final @NotNull CompletableFuture<Void> cancelled = new CompletableFuture<>();
            BackgroundWork.logged(cancelled, "Reading the cancelled one");
            cancelled.cancel(false);

            final @NotNull AsyncPromise<Void> expired = new AsyncPromise<>();
            BackgroundWork.logged(expired, "Reading the expired one");
            expired.cancel();

            BackgroundWork.logged(CompletableFuture.failedFuture(new IllegalStateException("the disk went away")), "Reading the failed one");
        });

        assertTrue("a cancelled read was logged as a failure: " + logged, logged.stream().noneMatch(line -> line.contains("cancelled one") || line.contains("expired one")));
        assertTrue("a failed read was not logged: " + logged, logged.stream().anyMatch(line -> line.contains("Reading the failed one failed")));
    }
}
