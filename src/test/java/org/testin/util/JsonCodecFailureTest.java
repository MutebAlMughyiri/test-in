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

import org.testin.model.TestCase;
import org.testng.annotations.Test;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class JsonCodecFailureTest {

    @Test
    public void aSerializationFailureRaisesRatherThanReturningNothing() {
        final IllegalStateException failure = expectThrows(IllegalStateException.class,
                () -> RealJsonCodec.build().writeValueAsBytes(new NotSerializable()));

        assertNotNull(failure.getMessage());
        assertTrue(String.valueOf(failure.getMessage()).contains("NotSerializable"),
                "the message names what could not be written: " + failure.getMessage());
    }

    @Test
    public void theSameIsTrueForTheStringForm() {
        final IllegalStateException failure = expectThrows(IllegalStateException.class,
                () -> RealJsonCodec.build().writeValueAsString(new NotSerializable()));

        assertTrue(String.valueOf(failure.getMessage()).contains("NotSerializable"));
    }

    @Test
    public void whatIsWrittenIsNeverEmpty() {
        final byte[] testCase = RealJsonCodec.build().writeValueAsBytes(TestCase.builder().description("a case").build());

        assertTrue(testCase.length > 0);
        assertTrue(new String(testCase, StandardCharsets.UTF_8).contains("a case"));
    }

    @Test
    public void aTestCaseSurvivesTheRoundTrip() {
        final JsonCodec mapper = RealJsonCodec.build();
        final TestCase original = TestCase.builder().description("written and read back").build();

        final TestCase readBack = mapper.readValue(mapper.writeValueAsString(original), TestCase.class);

        assertEquals(readBack.getDescription(), original.getDescription());
        assertEquals(readBack.getId(), original.getId());
    }

    @Test
    public void aFileThatIsNotJsonFailsAsUnreadableAndNamesWhatWasExpected() {
        final UncheckedIOException failure = expectThrows(UncheckedIOException.class,
                () -> RealJsonCodec.build().readValue("not json".getBytes(StandardCharsets.UTF_8), TestCase.class));

        assertTrue(String.valueOf(failure.getMessage()).contains(TestCase.class.getSimpleName()), "the message names what could not be read: " + failure.getMessage());
    }

    private static final class NotSerializable {
    }
}
