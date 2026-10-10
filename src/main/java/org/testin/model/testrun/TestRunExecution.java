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

package org.testin.model.testrun;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.model.markers.DetailRow;
import org.testin.model.markers.TestRunMarker;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum TestRunExecution {
    STARTED(
            Bundle.message("execution.started"),
            TestRunMarker::getExecutionStartedAt
    ),

    ENDED(
            Bundle.message("execution.ended"),
            TestRunMarker::getExecutionEndedAt
    );

    private final @NotNull String displayName;

    @Getter(AccessLevel.NONE)
    private final @NotNull Function<TestRunMarker, ZonedDateTime> at;

    private static @NotNull String tookIn(final @NotNull TestRunMarker testRun) {
        final @NotNull ZonedDateTime from = testRun.getExecutionStartedAt();
        final @NotNull ZonedDateTime to = testRun.getExecutionEndedAt();

        if (Config.isNotExecuted(from) || Config.isNotExecuted(to)) return "";

        return Display.formatTestRunClock(Duration.between(from, to));
    }

    public static @NotNull List<DetailRow> rowsOf(final @NotNull TestRunMarker testRun) {
        return Stream.concat(
                        Arrays.stream(values()).map(field -> new DetailRow(field.displayName, field.valueIn(testRun))),
                        Stream.of(new DetailRow(Bundle.message("execution.time"), tookIn(testRun))))
                .toList();
    }

    public @NotNull String valueIn(final @NotNull TestRunMarker testRun) {
        return Display.formatDate(at.apply(testRun));
    }
}
