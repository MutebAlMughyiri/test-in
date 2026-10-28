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

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class PluginDependenciesTest {

    private static final @NotNull Path PLUGIN_XML = Path.of("src", "main", "resources", "META-INF", "plugin.xml");

    private static @NotNull List<Element> elements(final @NotNull String tag) {
        try {
            final @NotNull NodeList found = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(PLUGIN_XML.toFile()).getElementsByTagName(tag);
            final @NotNull List<Element> elements = new ArrayList<>();
            for (int i = 0; i < found.getLength(); i++) elements.add((Element) found.item(i));
            return elements;
        } catch (final Exception ex) {
            throw new AssertionError("Could not read " + PLUGIN_XML + ": " + ex.getMessage(), ex);
        }
    }

    @Test
    public void testinNeedsOnlyThePlatformAndEveryOtherPluginIsOptional() {
        final @NotNull List<String> required = elements("depends").stream()
                .filter(depends -> !"true".equals(depends.getAttribute("optional")))
                .map(Element::getTextContent)
                .toList();
        assertEquals(required, List.of("com.intellij.modules.platform", "com.intellij.modules.lang"), "an IDE without one of these cannot install Testin at all");

        final @NotNull List<String> mandatoryModules = elements("module").stream()
                .filter(module -> module.hasAttribute("loading") && !"optional".equals(module.getAttribute("loading")))
                .map(module -> module.getAttribute("name"))
                .toList();
        assertEquals(mandatoryModules, List.of(), "a content module that is not optional fails to load in an IDE without its plugin");
    }

    @Test
    public void testinIsNotOfferedToTheRemoteDevelopmentThinClientAndStillLoadsInIntellijIdea() {
        assertEquals(elements("incompatible-with").stream().map(Element::getTextContent).toList(), List.of("com.intellij.jetbrains.client"), "JetBrains Client holds no project and lacks the Git4Idea classes Testin calls; com.jetbrains.gateway is also a plugin IntelliJ IDEA bundles");
    }
}
