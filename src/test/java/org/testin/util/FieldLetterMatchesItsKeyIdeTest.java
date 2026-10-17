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

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.clipboard.CopyChoice;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UpdateTestCaseFields;

import javax.swing.Icon;
import java.awt.event.KeyEvent;

public class FieldLetterMatchesItsKeyIdeTest extends AbstractIdeTest {

    public void testEveryFieldDrawsTheLetterOfTheKeyThatOpensIt() {
        for (final CreateTestCaseFields field : CreateTestCaseFields.values()) {
            assertLetter("CreateTestCaseFields." + field.name(), field.getIcon(), field.getShortcut());
        }

        for (final CopyChoice choice : CopyChoice.values()) {
            assertLetter("CopyChoice." + choice.name(), choice.getIcon(), choice.getShortcut());
        }

        for (final UpdateTestCaseFields field : UpdateTestCaseFields.values()) {
            if (field.getIcon() instanceof Icons.LetterIcon drawn) {
                assertLetter("UpdateTestCaseFields." + field.name(), drawn, field.getShortcut());
            }
        }
    }

    private void assertLetter(final @NotNull String named, final @NotNull Icon icon, final @NotNull Shortcuts shortcut) {
        assertTrue(named + " draws no letter", icon instanceof Icons.LetterIcon);
        final @NotNull String letter = ((Icons.LetterIcon) icon).letter();
        final @NotNull String key = KeyEvent.getKeyText(shortcut.getKey().getKeyCode());

        assertEquals(named + " draws " + letter + " and its key is " + key
                + ". A field's letter is the letter of the key that opens it, so re-keying the field"
                + " has to move the letter with it.", key, letter);
    }
}
