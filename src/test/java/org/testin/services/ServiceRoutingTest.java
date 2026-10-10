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

package org.testin.services;

import org.testin.editor.open.TestinEditors;
import org.testin.indexer.OwnWrites;
import org.testin.indexer.Rescan;
import org.testin.logger.LogWriter;
import org.testin.setting.AppSettingsState;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ServiceRoutingTest {

    @Test
    public void theSettingsBelongToTheApplication() {
        assertTrue(Services.isApplicationLevel(AppSettingsState.class),
                "the settings are one object for the IDE; a per-project copy gets its own testinSettings.xml "
                        + "and the name typed in Settings never reaches a run");
    }

    @Test
    public void everyApplicationServiceIsRecognized() {
        for (final Class<?> service : new Class<?>[]{AppSettingsState.class, OwnWrites.class, Rescan.class, LogWriter.class}) {
            assertTrue(Services.isApplicationLevel(service),
                    service.getSimpleName() + " declares Service.Level.APP but would be built per project");
        }
    }

    @Test
    public void aProjectServiceStaysWithItsProject() {
        assertFalse(Services.isApplicationLevel(TestinEditors.class),
                "TestinEditors is per project - one shared across projects would close the wrong editors");
    }

    @Test
    public void somethingThatIsNotAServiceStaysWithItsProject() {
        assertFalse(Services.isApplicationLevel(String.class),
                "a class with no @Service annotation must not be diverted to the application container");
    }

    @Test
    public void theCachedAnswerIsTheSameAnswer() {
        final boolean first = Services.isApplicationLevel(AppSettingsState.class);
        final boolean cached = Services.isApplicationLevel(AppSettingsState.class);

        assertEquals(cached, first, "the cache answered differently the second time");
        assertTrue(first, "the settings are the application's on the first ask");

        final boolean firstProject = Services.isApplicationLevel(TestinEditors.class);
        final boolean cachedProject = Services.isApplicationLevel(TestinEditors.class);

        assertEquals(cachedProject, firstProject, "the cache answered differently the second time");
        assertFalse(firstProject, "a project service stays with its project");
    }
}
