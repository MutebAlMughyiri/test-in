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


package org.testin.help;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GuidesTest {
    // Rule-INTERNAL-131
    @Test
    public void everyGuideHasItsPageAndEveryPageIsAGuide() {
        final @NotNull Set<String> listed = Arrays.stream(Guide.values()).map(Guide::getPage).collect(Collectors.toSet());
        final @NotNull Set<String> written = Arrays.stream(Objects.requireNonNull(new File("docs/guides").list())).map(name -> "guides/" + name).collect(Collectors.toSet());

        assertEquals(written, listed);
    }

    @Test
    public void everyGuideHasItsOwnTitle() {
        final @NotNull List<String> titles = Arrays.stream(Guide.values()).map(Guide::getTitle).toList();

        assertEquals(Set.copyOf(titles).size(), titles.size());
    }

    // Rule-INTERNAL-130
    @Test
    public void aLinkFromAGuideResolvesToTheBundledPageItNames() {
        final @NotNull BundledPage guide = new BundledPage(Guide.SET_UP_THIS_MACHINE.getPage());

        assertEquals(guide.follow("../setting/setTestinFolder.md").path(), "setting/setTestinFolder.md");
        assertEquals(guide.follow("linkThisRepository.md#next").path(), "guides/linkThisRepository.md");
    }

    // Rule-INTERNAL-130
    @Test
    public void everyRelativeLinkInAGuideIsAPageAndAWebAddressIsNot() {
        assertTrue(BundledPage.isPage("../setting/setTestinFolder.md"));
        assertFalse(BundledPage.isPage("https://cli.github.com"));
    }
}
