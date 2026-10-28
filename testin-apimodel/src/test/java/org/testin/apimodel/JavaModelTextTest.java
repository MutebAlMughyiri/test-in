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

package org.testin.apimodel;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class JavaModelTextTest {
    private static final @NotNull String REQUEST = """
            { "first-name": "Muteb", "mobile": "0501234567", "pin": "4321",
              "address": { "city": "Riyadh", "street": "King Fahd Rd" },
              "roles": ["customer"],
              "cards": [ { "type": "debit" }, { "type": "credit", "limit": 5000 } ] }""";

    private static final @NotNull String RESPONSE = """
            { "id": 7, "first-name": "Muteb", "status": "ACTIVE", "balance": 1250.75, "class": "gold",
              "address": { "city": "Riyadh" },
              "cards": [ { "last4": "4242" }, { "last4": "1881", "frozen": true } ] }""";

    private static @NotNull String expected(final @NotNull String file) {
        try (final InputStream in = Objects.requireNonNull(JavaModelTextTest.class.getResourceAsStream(file), file + " is not beside the test")) {
            final @NotNull List<String> fenced = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            return String.join("\n", fenced.subList(1, fenced.size() - 1)) + "\n";
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + file, ex);
        }
    }

    private static @NotNull ModelType model(final @NotNull String name, final @NotNull String json) {
        return JsonModel.of(name, JsonModel.read(json).orElseThrow());
    }

    // UC-CODEGEN-022, Rule-CODEGEN-100, Rule-CODEGEN-101
    @Test
    public void theRequestIsOneLombokClassWithItsNestedClassesInside() {
        assertEquals(JavaModelText.requestClass("com.example.api", model("UserRequest", REQUEST)), expected("UserRequest.md"));
    }

    // UC-CODEGEN-022, Rule-CODEGEN-100, Rule-CODEGEN-101
    @Test
    public void theResponseIsOneRecordWithOneComponentToALine() {
        assertEquals(JavaModelText.responseRecord("com.example.api", model("UserResponse", RESPONSE)), expected("UserResponse.md"));
    }

    // Rule-CODEGEN-101
    @Test
    public void anEmptyObjectIsAClassWithNoConstructorTwiceAndAnEmptyRecord() {
        final @NotNull ModelType root = model("Holder", "{ \"inner\": {} }");

        assertEquals(JavaModelText.requestClass("", root), expected("Holder.request.md"));
        assertEquals(JavaModelText.responseRecord("", root), expected("Holder.response.md"));
    }

    // Rule-CODEGEN-101
    @Test
    public void aKeyThatIsAlreadyItsNameCarriesNoAnnotationAndNoImport() {
        final @NotNull String text = JavaModelText.responseRecord("api", model("Plain", "{ \"id\": 1, \"userId\": 2 }"));

        assertFalse(text.contains("JsonProperty"), "a camelCase key was annotated: " + text);
    }

    // Rule-CODEGEN-101
    @Test
    public void aKeyWithAQuoteOrBackslashIsWrittenAsAJavaString() {
        final @NotNull String text = JavaModelText.responseRecord("api", model("Odd", "{ \"a\\\"b\\\\c\": 1 }"));

        assertEquals(text.lines().filter(line -> line.contains("@JsonProperty")).findFirst().orElseThrow().strip(), "@JsonProperty(\"a\\\"b\\\\c\")");
    }

    // UC-CODEGEN-022, Rule-CODEGEN-101
    @Test
    public void aWholeNumberTooBigForALongBringsItsImport() {
        final @NotNull String written = JavaModelText.requestClass("com.example.api", model("Ledger", """
                { "total": 123456789012345678901234567890 }"""));

        assertTrue(written.contains("import java.math.BigInteger;"), "a number too big for a long does not import its type: " + written);
    }

    // Rule-CODEGEN-103
    @Test
    public void aKeyWithNoLetterOrDigitIsNamedField() {
        final @NotNull String written = JavaModelText.responseRecord("com.example.api", model("Odd", """
                { "@@": 1 }"""));

        assertTrue(written.contains(" field"), "a key with no letter or digit has no name: " + written);
    }
}
