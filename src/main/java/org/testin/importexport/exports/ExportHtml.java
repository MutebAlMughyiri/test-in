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

package org.testin.importexport.exports;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.model.ReportColor;
import org.testin.model.TestCase;
import org.testin.testcase.Can;
import org.testin.testcase.TestSetEditorFields;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.ReportFont;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class ExportHtml {
    // UC-SHARE-002
    public void exportToFile(final @NotNull File destination, final @NotNull Map<String, List<TestCase>> sheets) {
        try {
            Files.writeString(destination.toPath(), htmlOf(sheets), StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void line(final @NotNull StringBuilder sb, final @NotNull String text) {
        sb.append(text).append(System.lineSeparator());
    }

    // UC-SHARE-002, Rule-SHARE-014
    static @NotNull String htmlOf(final @NotNull Map<String, List<TestCase>> sheets) {
        final @NotNull StringBuilder sb = new StringBuilder();
        line(sb, "<!DOCTYPE html>");
        line(sb, "<html lang=\"en\">");
        line(sb, "<head>");
        line(sb, "<meta charset=\"UTF-8\">");
        line(sb, "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        line(sb, "<title>" + Bundle.message("export.html.title") + "</title>");
        line(sb, "<style>");
        line(sb, "  body { font-family: " + ReportFont.CSS_FAMILY + "; margin: 20px; }");
        line(sb, "  h2 { color: #" + ReportColor.MUTED.hex() + "; margin-top: 30px; }");
        line(sb, "  table { border-collapse: collapse; width: 100%; margin-bottom: 30px; }");
        line(sb, "  th, td { border: 1px solid #" + ReportColor.LINE.hex() + "; padding: 8px 12px; text-align: left; vertical-align: top; }");
        line(sb, "  th { background-color: #" + ReportColor.PANEL.hex() + "; font-weight: bold; }");
        line(sb, "  tr:nth-child(even) { background-color: #" + ReportColor.PANEL.hex() + "; }");
        line(sb, "  .section-title { margin-top: 20px; }");
        line(sb, "</style>");
        line(sb, "</head>");
        line(sb, "<body>");
        line(sb, "<h1>" + Bundle.message("export.html.title") + "</h1>");

        int totalExported = 0;
        for (final Map.Entry<String, List<TestCase>> sheet : sheets.entrySet()) {
            if (sheet.getValue().isEmpty()) continue;

            line(sb, "<h2>" + StringUtil.escapeXmlEntities(sheet.getKey()) + "</h2>");
            line(sb, "<table>");
            line(sb, "<tr>" + cells("th", TestSetEditorFields.all(Can.EXPORT).stream().map(TestSetEditorFields::getName).toList()) + "</tr>");
            for (final TestCase testCase : sheet.getValue()) {
                line(sb, "<tr>" + cells("td", TestSetEditorFields.all(Can.EXPORT).stream().map(field -> field.gridValue(testCase)).toList()) + "</tr>");
                totalExported++;
            }
            line(sb, "</table>");
        }

        line(sb, "<p><em>" + Bundle.message("export.html.total", String.valueOf(totalExported)) + "</em></p>");
        line(sb, "<p><em>" + Bundle.message("export.html.on", StringUtil.escapeXmlEntities(Display.formatDate(ZonedDateTime.now(ZoneId.systemDefault())))) + "</em></p>");
        line(sb, "</body>");
        line(sb, "</html>");
        return sb.toString();
    }

    private static @NotNull String cells(final @NotNull String tag, final @NotNull List<String> values) {
        final @NotNull StringBuilder row = new StringBuilder();
        values.forEach(value -> row.append('<').append(tag).append('>').append(StringUtil.escapeXmlEntities(value)).append("</").append(tag).append('>'));
        return row.toString();
    }
}
