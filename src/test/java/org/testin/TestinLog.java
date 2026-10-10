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

package org.testin;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Level;
import org.testin.logger.LogWriter;
import org.testin.logger.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class TestinLog {
    private static final @NotNull Path LOG = Path.of(PathManager.getLogPath(), "testin.log");

    private final @NotNull String start = "a test reads the Testin log from here: " + UUID.randomUUID();

    private TestinLog() {
        Logger.info(start);
    }

    public static @NotNull TestinLog fromNow(final @NotNull Disposable owner) {
        final @NotNull Level before = levelNow();
        Logger.setLogLevel(Level.INFO);
        Disposer.register(owner, () -> Logger.setLogLevel(before));
        return new TestinLog();
    }

    public static @NotNull List<String> during(final @NotNull Runnable gesture) {
        final @NotNull Disposable reading = Disposer.newDisposable();
        try {
            final @NotNull TestinLog log = fromNow(reading);
            gesture.run();
            return log.lines();
        } finally {
            Disposer.dispose(reading);
        }
    }

    private static @NotNull String everything() {
        try {
            return Files.exists(LOG) ? Files.readString(LOG, StandardCharsets.UTF_8) : "";
        } catch (final IOException beingWritten) {
            return "";
        }
    }

    private static @NotNull Level levelNow() {
        final @NotNull LogWriter writer = ApplicationManager.getApplication().getService(LogWriter.class);
        return Arrays.stream(Level.values()).filter(level -> level != Level.DISABLED && writer.writes(level)).findFirst().orElse(Level.DISABLED);
    }

    public @NotNull List<String> lines() {
        return written().lines().toList();
    }

    public @NotNull String written() {
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        final @NotNull String upTo = "a test read the Testin log up to here: " + UUID.randomUUID();
        Logger.info(upTo);
        Await.until("the Testin log never caught up with what the test wrote to it", () -> everything().contains(upTo));

        final @NotNull String everything = everything();
        final int from = everything.indexOf(start);
        return everything.substring(from < 0 ? 0 : from + start.length(), everything.indexOf(upTo));
    }
}
