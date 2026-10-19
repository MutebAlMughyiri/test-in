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

import org.testng.annotations.Test;

import javax.lang.model.SourceVersion;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

public class GeneratedNamesTest {

    private static final String[] UNNAMEABLE = {"!!!", "???", "...", "---", "@#$", "()"};

    private static final String[] KEYWORDS = {"New", "Class", "Import", "Return", "Do", "Switch", "Package"};

    @Test
    public void strippingACharacterDoesNotLeaveTheGapBehind() {
        assertEquals(NameSanitizer.description("absher status = 0"), "absher status 0",
                "the space on each side of the stripped = is left as a double space");

        assertEquals(NameSanitizer.description("a < b > c"), "a b c",
                "every stripped character closes its own gap");

        assertEquals(NameSanitizer.description("  leading and trailing  "), "leading and trailing",
                "the ends are trimmed as they always were");

        assertEquals(NameSanitizer.description("one two"), "one two",
                "a description with nothing to strip is untouched, single spaces and all");
    }

    @Test
    public void aPackageNameIsTheSameEveryTimeItIsAskedFor() {
        for (final String name : UNNAMEABLE) {
            assertEquals(NameSanitizer.packageName(name), NameSanitizer.packageName(name),
                    "'" + name + "' named a different package on two calls, so nothing that generated code"
                            + " under it could ever find that code again");
        }
    }

    // Rule-CODEGEN-011
    @Test
    public void aClassNameIsTheSameEveryTimeItIsAskedFor() {
        for (final String name : UNNAMEABLE) {
            assertEquals(NameSanitizer.className(name), NameSanitizer.className(name));
        }
    }

    // Rule-CODEGEN-011
    @Test
    public void twoTestSetsNeverWriteIntoOneClass() {
        for (int i = 0; i < UNNAMEABLE.length; i++) {
            for (int j = i + 1; j < UNNAMEABLE.length; j++) {
                assertNotEquals(NameSanitizer.className(UNNAMEABLE[i]), NameSanitizer.className(UNNAMEABLE[j]),
                        "'" + UNNAMEABLE[i] + "' and '" + UNNAMEABLE[j] + "' both name "
                                + NameSanitizer.className(UNNAMEABLE[i]) + ", so one set's methods land in the other's class");
            }
        }
    }

    @Test
    public void twoTestSetsNeverWriteIntoOnePackage() {
        for (int i = 0; i < UNNAMEABLE.length; i++) {
            for (int j = i + 1; j < UNNAMEABLE.length; j++) {
                assertNotEquals(NameSanitizer.packageName(UNNAMEABLE[i]), NameSanitizer.packageName(UNNAMEABLE[j]));
            }
        }
    }

    @Test
    public void everyFallbackIsSomethingJavaAccepts() {
        for (final String name : UNNAMEABLE) {
            assertTrue(SourceVersion.isName(NameSanitizer.className(name)),
                    "'" + name + "' names the class " + NameSanitizer.className(name) + ", which Java will not accept");

            assertTrue(SourceVersion.isName(NameSanitizer.packageName(name)),
                    "'" + name + "' names the package " + NameSanitizer.packageName(name) + ", which Java will not accept");
        }
    }

    // Rule-CODEGEN-073
    @Test
    public void aJavaKeywordNeverBecomesAPackageName() {
        for (final String name : KEYWORDS) {
            assertTrue(SourceVersion.isName(NameSanitizer.packageName(name)),
                    "'" + name + "' names the package " + NameSanitizer.packageName(name)
                            + ", which Java will not accept - every test case under it is in a file that does not compile");
        }
    }

    // Rule-CODEGEN-073
    @Test
    public void aKeywordNamesTheSamePackageEveryTime() {
        for (final String name : KEYWORDS) {
            assertEquals(NameSanitizer.packageName(name), NameSanitizer.packageName(name));
        }

        for (int i = 0; i < KEYWORDS.length; i++) {
            for (int j = i + 1; j < KEYWORDS.length; j++) {
                assertNotEquals(NameSanitizer.packageName(KEYWORDS[i]), NameSanitizer.packageName(KEYWORDS[j]));
            }
        }
    }

    @Test
    public void theTreeCanTellWhichNamesItWillHaveToRename() {
        for (final String name : KEYWORDS) {
            assertFalse(NameSanitizer.canMakePackageName(name),
                    "'" + name + "' would be accepted by a create or rename dialog and then generate a package called something else");
        }

        for (final String name : UNNAMEABLE) {
            assertFalse(NameSanitizer.canMakePackageName(name));
        }

        assertTrue(NameSanitizer.canMakePackageName("Checkout"));
        assertTrue(NameSanitizer.canMakePackageName("payment methods"));
        assertTrue(NameSanitizer.canMakePackageName("4 digit pin"));
    }

    // Rule-CODEGEN-008
    @Test
    public void anOrdinaryNameIsUntouchedByAnyOfThis() {
        assertEquals(NameSanitizer.className("Login"), "LoginTest");
        assertEquals(NameSanitizer.className("user login"), "UserLoginTest");
        assertEquals(NameSanitizer.packageName("Checkout"), "checkout");
        assertEquals(NameSanitizer.packageName("Home Screen"), "homescreen");
    }

    // Rule-CODEGEN-011
    @Test
    public void aTestSetWithNoNameAtAllIsDefaultTest() {
        assertEquals(NameSanitizer.className(""), "DefaultTest");
        assertEquals(NameSanitizer.className("   "), "DefaultTest");
    }

    // Rule-CODEGEN-012
    @Test
    public void aMethodNameIsTheDescriptionInLowerCamelCaseWithOnlyLettersAndDigits() {
        assertEquals(NameSanitizer.methodName("Log in, with a VALID user #2!"), "logInWithAValidUser2",
                "everything but letters and digits is removed, the first word lowercase and every later word capitalized");
    }

    // Rule-CODEGEN-016
    @Test
    public void descriptionsThatDifferOnlyInPunctuationOrCapitalsAreOneMethod() {
        assertEquals(NameSanitizer.methodName("Log in!"), NameSanitizer.methodName("LOG-IN"),
                "two descriptions a tester reads as one would write two methods for one test");
    }

    // Rule-CODEGEN-008
    @Test
    public void aTestProjectNamedAfterItsTestCasesRepositoryGivesThePackageItsCloneWould() {
        assertEquals(NameSanitizer.packageName("nafath-test-cases"), "nafath");
        assertEquals(NameSanitizer.packageName("NAFATH"), "nafath");
        assertEquals(NameSanitizer.withoutTestCases("nafath-test-cases-old"), "nafath-test-cases-old", "only a trailing -test-cases is dropped");
    }
}
