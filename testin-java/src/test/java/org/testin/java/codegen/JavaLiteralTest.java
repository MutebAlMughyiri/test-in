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

package org.testin.java.codegen;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class JavaLiteralTest {

    @Test
    public void theAnswerCarriesItsOwnQuotes() {
        assertEquals(JavaLiteral.of("verify login"), "\"verify login\"");
    }

    @Test
    public void aQuoteInsideIsEscapedRatherThanEndingTheLiteral() {
        assertEquals(JavaLiteral.of("say \"hello\""), "\"say \\\"hello\\\"\"");
    }

    @Test
    public void aWindowsPathDoesNotBecomeAnIllegalEscape() {
        assertEquals(JavaLiteral.of("clear C:\\Users\\temp"), "\"clear C:\\\\Users\\\\temp\"");
    }

    @Test
    public void aNewlineIsEscapedRatherThanWritten() {
        assertEquals(JavaLiteral.of("verify citizen\nlogin successfully"),
                "\"verify citizen\\nlogin successfully\"");
    }

    @Test
    public void punctuationIsCarriedThroughUntouched() {
        assertEquals(JavaLiteral.of("Login, then log out (as admin)"),
                "\"Login, then log out (as admin)\"");
    }

    @Test
    public void everyAnswerIsOneQuotedLiteral() {
        for (final String typed : new String[]{"", "plain", "with \"quotes\"", "with\nnewline", "C:\\path"}) {
            final String literal = JavaLiteral.of(typed);

            assertTrue(literal.startsWith("\"") && literal.endsWith("\""),
                    "a caller writes this straight into source, so it opens and closes itself: " + literal);
            assertFalseUnescapedNewline(literal);
        }
    }

    private void assertFalseUnescapedNewline(final String literal) {
        assertFalse(literal.contains("\n"),
                "a raw newline splits the literal across two lines and the annotation stops parsing: " + literal);
    }
}
