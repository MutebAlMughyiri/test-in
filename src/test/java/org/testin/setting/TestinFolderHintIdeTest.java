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


package org.testin.setting;

import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Said;
import org.testin.help.Hint;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.List;

public class TestinFolderHintIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull AppSettingsState wasStored = new AppSettingsState();

    private static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    private @NotNull List<String> waiting() {
        return Services.getInstance(getProject(), Hints.class).waiting().stream().filter(hint -> hint.step() == SetupStep.TESTIN_FOLDER).map(Hint::text).toList();
    }

    @Override
    protected void setUp() {
        super.setUp();
        XmlSerializerUtil.copyBean(settings(), wasStored);
    }

    @Override
    protected void tearDown() {
        try {
            XmlSerializerUtil.copyBean(wasStored, settings());
            Services.getInstance(getProject(), Hints.class).clear(SetupStep.TESTIN_FOLDER);
        } finally {
            super.tearDown();
        }
    }

    // UC-SETTING-002, Rule-SETTING-014, Rule-INTERNAL-127
    public void testNoTestinFolderIsAHintNotAMessageAndSettingOneClearsIt() {
        settings().rootTestinPath = "";

        final @NotNull List<String> said = Said.during(getProject(), () -> StartupActivity.hintTestinFolder(getProject()));

        assertEquals("a message was raised for the Testin folder", List.of(), said);
        assertEquals(List.of(Bundle.message("startup.setup.message")), waiting());

        settings().rootTestinPath = root.toString();
        StartupActivity.hintTestinFolder(getProject());
        assertEquals("setting the folder left its hint waiting", List.of(), waiting());
    }
}
