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

package org.testin.git.review;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GitIdentityDialogTest {

    // Rule-SHARE-108
    @Test
    public void anEmailAddressIsSomethingAnAtSignAndADottedDomain() {
        assertTrue(GitIdentityDialog.isEmailAddress("sara@example.com"));
        assertTrue(GitIdentityDialog.isEmailAddress("sara.otaibi@qa.example.org"));
        assertTrue(GitIdentityDialog.isEmailAddress("  sara@example.com  "), "the space around the address is not part of it");
    }

    // Rule-SHARE-108
    @Test
    public void whatIsPlainlyNotAnEmailAddressIsRefused() {
        assertFalse(GitIdentityDialog.isEmailAddress("sara"), "no at sign");
        assertFalse(GitIdentityDialog.isEmailAddress("@example.com"), "nothing before the at sign");
        assertFalse(GitIdentityDialog.isEmailAddress("sara@localhost"), "no dot after the at sign");
        assertFalse(GitIdentityDialog.isEmailAddress("sara@example."), "nothing after the last dot");
        assertFalse(GitIdentityDialog.isEmailAddress("sara otaibi@example.com"), "a space inside the address");
        assertFalse(GitIdentityDialog.isEmailAddress("sara@@example.com"), "two at signs");
        assertFalse(GitIdentityDialog.isEmailAddress("sara@e."), "a domain too short to hold a name and a dot");
        assertFalse(GitIdentityDialog.isEmailAddress("sara@.example"), "a domain that starts with its dot");
    }
}
