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

package org.testin.indexer;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class AwaitIndexingIdeTest extends AbstractReadTheRootIdeTest {

    private static final long PATIENCE_SECONDS = 5;

    private static void await(final @NotNull CountDownLatch latch) {
        try {
            if (!latch.await(60, TimeUnit.SECONDS)) throw new AssertionError("a latch the test relies on never opened");
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting", interrupted);
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-111
    public void testWaitingForTheIndexInsideAReadActionReturnsAtOnce() {
        SyntheticTree.write(root, 1, 1);

        final @NotNull CountDownLatch held = new CountDownLatch(1);
        final @NotNull CountDownLatch release = new CountDownLatch(1);
        final @NotNull CountDownLatch returned = new CountDownLatch(1);
        final @NotNull AtomicBoolean builtWhenItReturned = new AtomicBoolean(true);

        final @NotNull Future<?> holder = ApplicationManager.getApplication().executeOnPooledThread(() ->
                indexer().getScanCoordinator().exclusively(() -> {
                    held.countDown();
                    await(release);
                }));
        await(held);

        final @NotNull Thread indexing = new Thread(() -> indexer().indexWithProgress(), "Testin indexing held back");
        indexing.start();
        Await.until("the read never started", () -> indexing.getState() == Thread.State.WAITING || indexing.getState() == Thread.State.TERMINATED);

        final @NotNull Future<?> waiter = ApplicationManager.getApplication().executeOnPooledThread(() -> ApplicationManager.getApplication().runReadAction(() -> {
            indexer().awaitIndexing();
            builtWhenItReturned.set(indexer().isIndexed());
            returned.countDown();
        }));

        try {
            assertTrue("waiting for the index inside a read action held the read action until the index was built",
                    returned.await(PATIENCE_SECONDS, TimeUnit.SECONDS));
            assertFalse("the wait returned only once the index was built", builtWhenItReturned.get());
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting for the wait to return", interrupted);
        } finally {
            release.countDown();
            Await.until("the held-back read never finished", () -> Stream.of(holder, waiter).allMatch(Future::isDone) && !indexing.isAlive());
        }
    }
}
