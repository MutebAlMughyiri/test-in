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

import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.services.Services;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class HeldFileWritesIdeTest extends AbstractTempRootIdeTest {

    private static void close(final @NotNull FileInputStream held) {
        try {
            held.close();
        } catch (final IOException ex) {
            throw new AssertionError("Could not let go of the file: " + ex.getMessage(), ex);
        }
    }

    private static void writeEmpty(final @NotNull Path file) {
        try {
            Files.writeString(file, "{}", StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError("Could not set up the held file: " + ex.getMessage(), ex);
        }
    }

    // Rule-INTERNAL-113
    public void testAWriteWaitsForAReaderThatHoldsTheFileForAMoment() {
        final @NotNull Path file = root.resolve("held.json");
        writeEmpty(file);
        try (FileInputStream held = new FileInputStream(file.toFile())) {
            final @NotNull ScheduledFuture<?> letGo = AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> close(held), 100, TimeUnit.MILLISECONDS);

            assertTrue("a write over a file a reader held for a moment failed", Services.getInstance(getProject(), TestDataFiles.class).write(file, Map.of("written", true)));
            assertTrue("the file does not hold what was written", Files.readString(file, StandardCharsets.UTF_8).contains("written"));
            letGo.get();
        } catch (final InterruptedException | ExecutionException ex) {
            throw new AssertionError("The reader never let go of the file: " + ex.getMessage(), ex);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read the written file back: " + ex.getMessage(), ex);
        }
    }
}
