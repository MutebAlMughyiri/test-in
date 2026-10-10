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
package org.testin.importexport.imports;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.model.status.TestCaseStatus;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertNotEquals;

public class ImportJsonIdeTest extends BasePlatformTestCase {

    private static final @NotNull UUID EXPORTED_ID = UUID.fromString("8f7b1a52-0c44-4c1e-9d6b-2a3f5e7c9011");

    private static final @NotNull String EXPORTED = """
            {"Login":[{"id":"8f7b1a52-0c44-4c1e-9d6b-2a3f5e7c9011","description":"Log in with a valid user",\
            "expectedResult":"The dashboard opens","module":"Accounts","status":"REVIEWED","order":"a5"}]}""";

    private @NotNull TestCaseDto imported() {
        try {
            final @NotNull Path file = Files.createTempFile("testin-import", ".json");
            Files.writeString(file, EXPORTED, StandardCharsets.UTF_8);

            final @NotNull Map<String, List<TestCaseDto>> read = new ImportJson().parseFile(getProject(), new File(file.toString()));

            return read.values().iterator().next().getFirst();
        } catch (final Exception e) {
            throw new AssertionError("the exported file could not be written for the test", e);
        }
    }

    // UC-SHARE-005, Rule-SHARE-024
    public void testAnImportedTestCaseIsNewAndNotTheOneTheFileNames() {
        assertNotEquals("the id in the file names a test case in the project it was exported from", EXPORTED_ID, imported().getId());
    }

    // UC-SHARE-005, Rule-SHARE-029
    public void testTheStatusInTheFileIsNotRead() {
        assertEquals(TestCaseStatus.PENDING, imported().getStatus());
    }

    // UC-SHARE-005, Rule-SHARE-025, Rule-SHARE-029
    public void testTheOrderInTheFileIsNotRead() {
        assertEquals("an imported test case is placed after everything already in the test set", "", imported().getOrder());
    }

    // UC-SHARE-005, Rule-SHARE-029
    public void testTheThirteenThatMayBeImportedStillArrive() {
        final @NotNull TestCaseDto imported = imported();

        assertEquals("Log in with a valid user", imported.getDescription());
        assertEquals("The dashboard opens", imported.getExpectedResult());
        assertEquals("Accounts", imported.getModule());
    }
}
