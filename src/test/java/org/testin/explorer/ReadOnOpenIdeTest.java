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

package org.testin.explorer;

import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.TimeoutUtil;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.indexer.AbstractReadTheRootIdeTest;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;

import java.nio.file.Path;

public class ReadOnOpenIdeTest extends AbstractReadTheRootIdeTest {

    private static final @NotNull Continuation<Unit> NO_ONE_WAITS = new Continuation<>() {
        @Override
        public @NotNull CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        public void resumeWith(final @NotNull Object result) {
        }
    };

    @Override
    protected void setUp() {
        super.setUp();
        TestinStartup.forgetTheRead(getProject());
    }

    private void theCodeProjectOpens() {
        new TestinStartup().execute(getProject(), NO_ONE_WAITS);

        final long until = System.currentTimeMillis() + 1_000;
        while (System.currentTimeMillis() < until) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            TimeoutUtil.sleep(20);
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-115
    public void testACodeProjectBoundToATestProjectReadsItWhenItOpens() {
        final @NotNull Path checkout = aTestProjectAt(root.resolve("Checkout"));
        Services.getInstance(getProject(), BoundTestProject.class).choose("Checkout");

        theCodeProjectOpens();

        Await.until("a code project bound to a test project did not read it when it opened", () -> nodes().nodeExists(checkout));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-115
    public void testACodeProjectWithNoTestProjectBoundReadsNothingUntilTheIndexIsNeeded() {
        final @NotNull Path checkout = aTestProjectAt(root.resolve("Checkout"));

        theCodeProjectOpens();

        assertFalse("a code project with no test project bound read the Testin folder when it opened", nodes().nodeExists(checkout));
        assertFalse("a code project with no test project bound started reading when it opened", indexer().isIndexed());

        indexer().awaitIndexing();

        Await.until("asking for Testin's index did not read the Testin folder", () -> nodes().nodeExists(checkout));
    }
}
