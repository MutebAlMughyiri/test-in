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

package org.testin.logger;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class LogWriterTest {

    private static final long FULL = 5L * 1024 * 1024;

    private static void inAFolder(final @NotNull Consumer<Path> test) {
        try {
            final @NotNull Path folder = Files.createTempDirectory("testin-log");
            try {
                test.accept(folder);
            } finally {
                FileUtil.delete(folder.toFile());
            }
        } catch (final IOException ex) {
            throw new AssertionError("could not make the log's folder: " + ex.getMessage(), ex);
        }
    }

    private static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static void write(final @NotNull Path file, final @NotNull String text) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text);
        } catch (final IOException ex) {
            throw new AssertionError("could not write " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull LogWriter writingTo(final @NotNull Path file) {
        final @NotNull LogWriter writer = new LogWriter(file);
        writer.setLogLevel(LogLevel.INFO);
        return writer;
    }

    private static void writeLongLines(final @NotNull LogWriter writer, final int lines) {
        final @NotNull String line = "x".repeat(1023);
        for (int i = 0; i < lines; i++) writer.log(LogLevel.INFO, "LogWriterTest", line);
    }

    // UC-SETTING-007
    @Test
    public void aLineAtTheLevelIsWrittenToANewLogAndOneBelowItIsNot() {
        inAFolder(folder -> {
            final @NotNull Path log = folder.resolve("testin.log");
            final @NotNull LogWriter writer = writingTo(log);

            writer.log(LogLevel.INFO, "LogWriterTest", "loud enough");
            writer.log(LogLevel.DEBUG, "LogWriterTest", "too quiet");
            writer.dispose();

            assertTrue(read(log).contains("[LogWriterTest] loud enough"), "a line at the level was not written: " + read(log));
            assertFalse(read(log).contains("too quiet"), "a line below the level was written");
            assertFalse(writer.writes(LogLevel.ERROR), "a log that was shut down still takes lines");
        });
    }

    // Rule-SETTING-047
    @Test
    public void aFullLogMovesToTestinLog1ReplacingTheOneThereAndANewOneBegins() {
        inAFolder(folder -> {
            final @NotNull Path log = folder.resolve("testin.log");
            final @NotNull Path older = folder.resolve("testin.log.1");
            write(older, "the log before");
            final @NotNull LogWriter writer = writingTo(log);

            writeLongLines(writer, 5200);
            writer.log(LogLevel.INFO, "LogWriterTest", "after the move");
            writer.dispose();

            assertTrue(older.toFile().length() >= FULL, "the full log did not move to testin.log.1: " + older.toFile().length() + " bytes there");
            assertFalse(read(older).contains("the log before"), "the log that was there before was kept");
            assertTrue(log.toFile().length() < FULL && read(log).contains("after the move"), "no new log began after the move");
        });
    }

    // Rule-SETTING-047
    @Test
    public void aFullLogThatCannotBeMovedStopsWhereItIs() {
        inAFolder(folder -> {
            final @NotNull Path log = folder.resolve("testin.log");
            write(folder.resolve("testin.log.1").resolve("in the way"), "a folder cannot be replaced by a file");
            final @NotNull LogWriter writer = writingTo(log);

            writeLongLines(writer, 5200);
            writer.log(LogLevel.INFO, "LogWriterTest", "after the move");
            writer.dispose();

            assertTrue(log.toFile().length() >= FULL, "the log did not fill before it tried to move");
            assertFalse(read(log).contains("after the move"), "a log that could not move went on writing");
            assertTrue(Files.isDirectory(folder.resolve("testin.log.1")), "what was in the way of the move was replaced");
        });
    }

    @Test
    public void aLogWhoseFolderIsGoneWritesNothingAndStillShutsDownWithItsQueueFull() {
        inAFolder(folder -> {
            final @NotNull Path log = folder.resolve("gone").resolve("testin.log");
            final @NotNull LogWriter writer = writingTo(log);

            writeLongLines(writer, 10_001);
            writer.dispose();

            assertFalse(Files.exists(log), "a log was written into a folder that is not there");
            assertFalse(writer.writes(LogLevel.ERROR), "a log that was shut down still takes lines");
        });
    }
}
