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

package org.testin;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.indexer.TestRuns;
import org.testin.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractTempRootIdeTest extends BasePlatformTestCase {

    protected Path root;

    @Override
    protected void setUp() {
        try {
            root = Files.createTempDirectory("testin-" + getClass().getSimpleName()).toRealPath();
            super.setUp();
        } catch (final Exception ex) {
            throw new AssertionError("Could not set up " + getName() + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    protected void tearDown() {
        try {
            Services.getInstance(getProject(), TestRuns.class).awaitWrites();
            TempTree.delete(root);
            super.tearDown();
        } catch (final Exception ex) {
            throw new AssertionError("Could not tear down " + getName() + ": " + ex.getMessage(), ex);
        }
    }
}
