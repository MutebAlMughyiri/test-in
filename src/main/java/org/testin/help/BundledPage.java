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

import com.intellij.markdown.utils.MarkdownToHtmlConverterKt;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.Html;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public record BundledPage(@NotNull String path) {
    private static final @NotNull String ROOT = "/docs/";

    // Rule-INTERNAL-130
    public static boolean isPage(final @NotNull String href) {
        return !href.contains(":") && URI.create(href).getPath().endsWith(".md");
    }

    // UC-INTERNAL-009, Rule-INTERNAL-130
    public @NotNull String html() {
        return markdown()
                .map(MarkdownToHtmlConverterKt::convertMarkdownToHtml)
                .orElseGet(() -> "<p>" + Html.ofText(Bundle.message("guide.page.missing", path)) + "</p>");
    }

    // UC-INTERNAL-009, Rule-INTERNAL-130
    public @NotNull BundledPage follow(final @NotNull String href) {
        return new BundledPage(URI.create(path).resolve(URI.create(href).getPath()).getPath());
    }

    @NotNull Optional<String> markdown() {
        try (final InputStream in = BundledPage.class.getResourceAsStream(ROOT + path)) {
            if (in == null) {
                TestinLogger.warn("No bundled documentation page at " + path);
                return Optional.empty();
            }
            return Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (final IOException ex) {
            TestinLogger.error("Could not read the bundled documentation page " + path + ": " + FailureText.of(ex));
            return Optional.empty();
        }
    }
}
