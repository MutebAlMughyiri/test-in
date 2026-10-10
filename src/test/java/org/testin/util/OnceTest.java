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

package org.testin.util;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class OnceTest {

    private static final Key<Boolean> STARTED = Key.create("test.started");

    private static final Key<Boolean> OTHER = Key.create("test.other");

    @Test
    public void theFirstCallerIsAdmittedAndEveryOneAfterIsRefused() {
        final UserDataHolderBase project = new UserDataHolderBase();

        assertTrue(Once.claim(project, STARTED), "nobody had claimed it");
        assertFalse(Once.claim(project, STARTED), "the second door finds it taken");
        assertFalse(Once.claim(project, STARTED), "and so does the third");
    }

    @Test
    public void oneProjectClaimingSaysNothingAboutAnother() {
        final UserDataHolderBase opened = new UserDataHolderBase();
        final UserDataHolderBase alsoOpened = new UserDataHolderBase();

        assertTrue(Once.claim(opened, STARTED));
        assertTrue(Once.claim(alsoOpened, STARTED),
                "a second project opening has its own startup to run");
    }

    @Test
    public void twoQuestionsOnOneProjectAreTwoAnswers() {
        final UserDataHolderBase project = new UserDataHolderBase();

        assertTrue(Once.claim(project, STARTED));
        assertTrue(Once.claim(project, OTHER), "a different key is a different question");
    }

    @Test
    public void exactlyOneOfManyThreadsArrivingTogetherIsAdmitted() {
        try {
            final UserDataHolderBase project = new UserDataHolderBase();
            final AtomicInteger admitted = new AtomicInteger();
            final int doors = 32;
            final CountDownLatch open = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(doors);
            try (ExecutorService threads = Executors.newFixedThreadPool(doors)) {
                for (int i = 0; i < doors; i++) {
                    threads.execute(() -> {
                        try {
                            open.await();
                            if (Once.claim(project, STARTED)) admitted.incrementAndGet();
                        } catch (final InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        } finally {
                            done.countDown();
                        }
                    });
                }

                open.countDown();
                assertTrue(done.await(10, TimeUnit.SECONDS), "the threads should all have finished");
            }

            assertEquals(admitted.get(), 1,
                    "reading the flag and setting it have to be one step, or two threads both start Testin");
        } catch (final InterruptedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void aClaimIsRememberedOnTheProjectItWasMadeOn() {
        final UserDataHolderBase project = new UserDataHolderBase();

        Once.claim(project, STARTED);

        assertEquals(project.getUserData(STARTED), Boolean.TRUE,
                "the flag is the project's own data, so it lives exactly as long as the project does");
    }
}
