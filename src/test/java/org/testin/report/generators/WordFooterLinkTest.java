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

package org.testin.report.generators;

import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class WordFooterLinkTest {

    @Test
    public void theFooterCarriesAClickableLink() {
        try (ByteArrayOutputStream saved = new ByteArrayOutputStream()) {
            try (XWPFDocument written = new XWPFDocument()) {
                final @NotNull XWPFFooter footer =
                        written.createFooter(HeaderFooterType.DEFAULT);
                final @NotNull XWPFParagraph line = footer.createParagraph();

                line.createRun().setText("Generated automatically by ");
                final @NotNull XWPFHyperlinkRun link = line.createHyperlinkRun(ReportText.PLUGIN_URL);
                link.setText("Testin");
                line.createRun().setText(" IntelliJ plugin.");

                written.write(saved);
            }

            try (XWPFDocument read = new XWPFDocument(new ByteArrayInputStream(saved.toByteArray()))) {
                final @NotNull XWPFFooter reopened = read.getFooterList().getFirst();
                final @NotNull XWPFParagraph line = reopened.getParagraphs().getFirst();

                final @NotNull StringBuilder linked = new StringBuilder();
                for (final XWPFRun run : line.getRuns()) {
                    if (run instanceof XWPFHyperlinkRun hyperlink) {
                        linked.append(run.text());

                        final @NotNull String target = reopened.getPackagePart()
                                .getRelationship(hyperlink.getHyperlinkId()).getTargetURI().toString();

                        assertEquals(target, ReportText.PLUGIN_URL,
                                "the link points somewhere other than the plugin page");
                    }
                }

                assertEquals(linked.toString(), "Testin",
                        "the linked words should be the plugin's name and nothing else");
                assertTrue(line.getText().contains("IntelliJ plugin."),
                        "the rest of the sentence should still be there, unlinked");
            }

        } catch (final Exception cannotRun) {
            fail("Could not round-trip the footer: " + cannotRun.getMessage(), cannotRun);
        }
    }
}
