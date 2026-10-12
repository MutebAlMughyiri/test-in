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

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.testin.model.StatusBarItem;
import org.testin.services.Services;
import org.testin.testcase.TestCaseDialogKey;
import org.testin.ui.framework.ShortcutStrip;
import org.testin.ui.framework.StatusBarShortcut;

public class ShortcutHintsSettingIdeTest extends BasePlatformTestCase {

    private static final StatusBarItem @NotNull [] TEST_CASE_DIALOG = {TestCaseDialogKey.SAVE, TestCaseDialogKey.CANCEL};

    private static final StatusBarItem @NotNull [] FAILURE_DIALOG = {StatusBarShortcut.corrections(), StatusBarShortcut.pasteScreenshot()};

    private static void withHints(final boolean shown, final @NotNull Runnable check) {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);
        final boolean was = settings.showShortcutHints;

        settings.showShortcutHints = shown;
        try {
            check.run();
        } finally {
            settings.showShortcutHints = was;
        }
    }

    private static boolean shows(final StatusBarItem @NotNull [] keys) {
        return new ShortcutStrip(keys).getPanel().isVisible();
    }

    // Rule-SETTING-028
    public void testOneAnswerCoversEveryDialog() {
        withHints(false, () -> {
            assertFalse("the test case dialog kept its strip with the hints turned off", shows(TEST_CASE_DIALOG));
            assertFalse("the failure dialog kept its strip with the hints turned off", shows(FAILURE_DIALOG));
        });
    }

    // Rule-SETTING-029
    public void testTheSettingTakesEffectOnTheNextStrip() {
        withHints(true, () -> assertTrue("a strip was hidden with the hints turned on", shows(TEST_CASE_DIALOG)));
        withHints(false, () -> assertFalse("the next strip ignored the hints being turned off", shows(TEST_CASE_DIALOG)));
    }

    // Rule-SETTING-030
    public void testTheSettingCanOnlyTakeAStripAway() {
        withHints(true, () -> {
            final @NotNull ShortcutStrip strip = new ShortcutStrip(TEST_CASE_DIALOG);
            strip.setShown(false);
            assertFalse("the setting put back a strip its dialog left out", strip.getPanel().isVisible());
        });
        withHints(false, () -> {
            final @NotNull ShortcutStrip strip = new ShortcutStrip(TEST_CASE_DIALOG);
            strip.setShown(true);
            assertFalse("a dialog showed its strip with the hints turned off", strip.getPanel().isVisible());
        });
    }
}
