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

import com.intellij.openapi.application.ApplicationManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Logger {
    private static final @NotNull StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static void setLogLevel(final @NotNull Level level) {
        getService().ifPresent(service -> service.setLogLevel(level));
    }

    public static void trace(final @NotNull String message) {
        log(Level.TRACE, message);
    }

    public static void debug(final @NotNull String message) {
        log(Level.DEBUG, message);
    }

    public static void info(final @NotNull String message) {
        log(Level.INFO, message);
    }

    public static void warn(final @NotNull String message) {
        log(Level.WARN, message);
    }

    public static void error(final @NotNull String message) {
        log(Level.ERROR, message);
    }

    private static void log(final @NotNull Level level, final @NotNull String message) {
        final @NotNull Optional<LogWriter> service = getService();
        if (!service.map(writer -> writer.writes(level)).orElse(true)) return;

        final @NotNull String callerClass = WALKER.walk(frames -> frames.skip(2).findFirst()).orElseThrow().getDeclaringClass().getSimpleName();
        service.ifPresentOrElse(
                writer -> writer.log(level, callerClass, message),
                () -> System.getLogger(Logger.class.getName()).log(System.Logger.Level.INFO, "[" + level.paddedName + "] [" + callerClass + "] " + message));
    }

    private static @NotNull Optional<LogWriter> getService() {
        return Optional.ofNullable(ApplicationManager.getApplication())
                .map(application -> application.getService(LogWriter.class));
    }
}
