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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service(Service.Level.APP)
public final class LogWriter implements Disposable {
    private static final long MAX_LOG_SIZE = 5L * 1024 * 1024;

    private static final int JOIN_TIMEOUT = 2000;

    private static final @NotNull Logger IDE_LOG = Logger.getInstance(LogWriter.class);

    private static final @NotNull Object SHUTDOWN = new Object();

    private final @NotNull BlockingQueue<Object> logQueue = new ArrayBlockingQueue<>(10000);

    private final @NotNull DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    // Rule-SETTING-026
    private final @NotNull Path logFile = Path.of(PathManager.getLogPath(), "testin.log");

    private final @NotNull AtomicBoolean dropping = new AtomicBoolean();

    private volatile boolean isRunning = true;

    private volatile @NotNull LogLevel currentLogLevel = LogLevel.DISABLED;

    private @NotNull Optional<Thread> writerThread = Optional.empty();

    public LogWriter() {
        startWriterThread();
    }

    private static void report(final long elapsedNanos, final boolean timedOut) {
        IDE_LOG.info("Testin logger shutdown took " + elapsedNanos / 1_000_000 + " ms"
                + (timedOut ? ", and the writer did not finish within " + JOIN_TIMEOUT + " ms - it was interrupted"
                : ", writer finished on its own"));
    }

    // UC-SETTING-007, Rule-SETTING-024
    public void setLogLevel(final @NotNull LogLevel level) {
        this.currentLogLevel = level;
    }

    private void startWriterThread() {
        final @NotNull Thread thread = new Thread(this::writeLoop, "Testin-Async-Logger");
        thread.setDaemon(true);
        thread.start();
        writerThread = Optional.of(thread);
    }

    // Rule-SETTING-026
    private void writeLoop() {
        try {
            long written;
            try {
                written = Files.size(logFile);
            } catch (final NoSuchFileException ex) {
                written = 0;
            }
            BufferedWriter writer = Files.newBufferedWriter(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            try {
                while (isRunning || !logQueue.isEmpty()) {
                    final @NotNull Optional<Object> taken = Optional.ofNullable(logQueue.poll(500, TimeUnit.MILLISECONDS));
                    if (taken.isEmpty()) {
                        writer.flush();
                        continue;
                    }

                    if (!(taken.orElseThrow() instanceof String message)) continue;

                    writer.write(message);
                    writer.newLine();
                    written += message.length() + 1;

                    if (written >= MAX_LOG_SIZE) {
                        final @NotNull Optional<BufferedWriter> rolled = rollOver(writer);

                        if (rolled.isEmpty()) return;

                        writer = rolled.get();
                        written = 0;
                    }
                }
            } finally {
                writer.close();
            }
        } catch (final IOException ex) {
            IDE_LOG.warn("Testin's log writer stopped, so " + logFile + " ends here", ex);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private @NotNull Optional<BufferedWriter> rollOver(final @NotNull BufferedWriter writer) {
        try {
            writer.close();
            Files.move(logFile, logFile.resolveSibling("testin.log.1"), StandardCopyOption.REPLACE_EXISTING);
            return Optional.of(Files.newBufferedWriter(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
        } catch (final IOException ex) {
            IDE_LOG.warn("Testin's log could not roll over to testin.log.1, so " + logFile + " ends here", ex);
            return Optional.empty();
        }
    }

    public boolean writes(final @NotNull LogLevel level) {
        return isRunning && currentLogLevel != LogLevel.DISABLED && level.priority >= currentLogLevel.priority;
    }

    public void log(final @NotNull LogLevel level, final @NotNull String callerClass, final @NotNull String message) {
        if (!writes(level)) return;

        final @NotNull String formattedMessage = "[" + LocalDateTime.now(ZoneId.systemDefault()).format(formatter) + "] " + "[" + level.paddedName + "] " + "[" + callerClass + "] " + message;

        if (logQueue.offer(formattedMessage)) {
            dropping.set(false);
            return;
        }

        sayTheQueueIsFull();
    }

    private void sayTheQueueIsFull() {
        if (dropping.getAndSet(true)) return;

        IDE_LOG.warn("Testin's log queue is full and lines are being dropped - the writer thread"
                + " has stopped draining it, so " + logFile + " ends where it stopped.");
    }

    @Override
    public void dispose() {
        final long started = System.nanoTime();

        isRunning = false;

        if (!logQueue.offer(SHUTDOWN)) writerThread.ifPresent(Thread::interrupt);

        boolean timedOut = false;

        if (writerThread.isPresent()) {
            final @NotNull Thread thread = writerThread.orElseThrow();
            try {
                thread.join(JOIN_TIMEOUT);
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            timedOut = thread.isAlive();
            if (timedOut) thread.interrupt();
        }

        report(System.nanoTime() - started, timedOut);
    }
}
