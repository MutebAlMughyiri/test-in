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

package org.testin.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.testin.model.markers.Marker;
import org.testin.model.markers.TestCasesFolderMarker;
import org.testin.model.markers.TestProjectMarker;
import org.testin.model.markers.TestSetMarker;
import org.testin.model.status.TestSetStatus;
import org.testng.annotations.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class MarkerJsonTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final ZonedDateTime when = ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS).minusYears(3);
    private final String onDisk = DateTimeFormatter.ofPattern(DateFormats.DATE_FORMAT_PATTERN, Locale.US).format(when);

    @Test
    public void theFoldersIdIsWrittenOnlyOnceItHasOne() {
        try {
            assertFalse(mapper.writeValueAsString(new TestSetMarker()).contains("\"id\""), "a marker with no id says nothing about one");

            final TestSetMarker marker = new TestSetMarker();
            marker.setId("0bb7f0de-8a59-4a0c-9a2f-0e2ba6a3b8f1");

            final String json = mapper.writeValueAsString(marker);
            assertTrue(json.contains("\"id\":\"0bb7f0de-8a59-4a0c-9a2f-0e2ba6a3b8f1\""), json);
            assertEquals(mapper.readValue(json, TestSetMarker.class).getId(), "0bb7f0de-8a59-4a0c-9a2f-0e2ba6a3b8f1");
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void writesWhatDiffersFromAFreshMarkerAndNothingElse() {
        try {
            final TestSetMarker marker = new TestSetMarker();
            marker.setCreatedBy("mtb");

            final String json = mapper.writeValueAsString(marker);

            assertTrue(json.contains("\"createdBy\":\"mtb\""), json);
            assertTrue(json.contains("\"createdAt\""), "when a node was created is always written: " + json);
            assertFalse(json.contains("\"status\""), "a fresh marker's status is the default, so it is not written: " + json);
            assertFalse(json.contains("statusLabel"), json);

            marker.setStatus(TestSetStatus.DEPRECATED);
            assertTrue(mapper.writeValueAsString(marker).contains("\"status\":\"DEPRECATED\""), "a status a tester chose is written");
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void anUnorderedMarkerCarriesNoOrderAtAll() {
        try {
            assertFalse(mapper.writeValueAsString(new TestSetMarker()).contains("order"),
                    mapper.writeValueAsString(new TestSetMarker()));

            assertEquals(mapper.readValue("{}", TestSetMarker.class).getOrder(), Marker.NOT_ORDERED);
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void anOrderedMarkerCarriesTheNumberTyped() {
        try {
            final TestSetMarker marker = new TestSetMarker();
            marker.setOrder(3);

            final String json = mapper.writeValueAsString(marker);

            assertTrue(json.contains("\"order\":3"), json);
            assertEquals(mapper.readValue(json, TestSetMarker.class).getOrder(), 3);
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void readsThePreRenameKeysOnEveryMarker() {
        try {
            final String old = "{\"createdBy\":\"a\",\"updatedBy\":\"b\",\"updatedAt\":\"" + onDisk + "\",\"status\":\"ACTIVE\"}";

            assertEquals(mapper.readValue(old, TestProjectMarker.class).getUpdatedBy(), "b");
            assertEquals(mapper.readValue(old, TestProjectMarker.class).getUpdatedAt().toInstant(), when.toInstant());
            assertEquals(mapper.readValue(old, TestSetMarker.class).getUpdatedBy(), "b");
            assertEquals(mapper.readValue(old, TestSetMarker.class).getUpdatedAt().toInstant(), when.toInstant());
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void ignoresAKeyTheMarkerDoesNotHave() {
        try {
            final String withStatus = "{\"createdBy\":\"a\",\"status\":\"ACTIVE\",\"whatever\":1}";

            assertEquals(mapper.readValue(withStatus, TestCasesFolderMarker.class).getCreatedBy(), "a");
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void aMarkerNeverModifiedReportsItsCreation() {
        try {
            final String onlyCreated = "{\"createdBy\":\"mtb\",\"createdAt\":\"" + onDisk + "\"}";

            final TestCasesFolderMarker marker = mapper.readValue(onlyCreated, TestCasesFolderMarker.class);

            assertEquals(marker.getUpdatedBy(), "mtb", "nobody has modified it, so it stands as its creator made it");
            assertEquals(marker.getUpdatedAt().toInstant(), when.toInstant(), "and at the time they made it");
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void roundTripsTheDateFormatOnDisk() {
        try {
            final TestSetMarker marker = new TestSetMarker();
            marker.setCreatedAt(when);
            marker.setUpdatedAt(when);

            final String json = mapper.writeValueAsString(marker);
            assertTrue(json.contains(onDisk), json);

            final TestSetMarker read = mapper.readValue(json, TestSetMarker.class);
            assertEquals(read.getCreatedAt().toInstant(), when.toInstant());
            assertEquals(read.getUpdatedAt().toInstant(), when.toInstant());
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
