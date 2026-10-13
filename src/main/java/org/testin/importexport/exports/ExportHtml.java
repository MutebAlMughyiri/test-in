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
import org.testin.logger.TestinLogger;
import org.testin.model.ReportColor;
import org.testin.model.TestCaseDto;
import org.testin.testcase.Can;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.FailureText;
import org.testin.util.ReportFont;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class ExportHtml {
    // UC-SHARE-002, Rule-SHARE-022
    public void exportToFile(final @NotNull File destFile, final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile), StandardCharsets.UTF_8))) {
            writeHtmlDocument(writer, sheetsData);
        } catch (final IOException ex) {
            TestinLogger.error(FailureText.of(ex));
            throw new RuntimeException(ex);
        }
    }

    private void writeHtmlDocument(final @NotNull BufferedWriter writer, final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        try {
            writer.write("<!DOCTYPE html>");
            writer.newLine();
            writer.write("<html lang=\"en\">");
            writer.newLine();
            writer.write("<head>");
            writer.newLine();
            writer.write("<meta charset=\"UTF-8\">");
            writer.newLine();
            writer.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            writer.newLine();
            writer.write("<title>" + Bundle.message("export.html.title") + "</title>");
            writer.newLine();
            writer.write("<style>");
            writer.newLine();
            writer.write("  body { font-family: " + ReportFont.CSS_FAMILY + "; margin: 20px; }");
            writer.newLine();
            writer.write("  h2 { color: #" + ReportColor.MUTED.hex() + "; margin-top: 30px; }");
            writer.newLine();
            writer.write("  table { border-collapse: collapse; width: 100%; margin-bottom: 30px; }");
            writer.newLine();
            writer.write("  th, td { border: 1px solid #" + ReportColor.LINE.hex() + "; padding: 8px 12px; text-align: left; vertical-align: top; }");
            writer.newLine();
            writer.write("  th { background-color: #" + ReportColor.PANEL.hex() + "; font-weight: bold; }");
            writer.newLine();
            writer.write("  tr:nth-child(even) { background-color: #" + ReportColor.PANEL.hex() + "; }");
            writer.newLine();
            writer.write("  .section-title { margin-top: 20px; }");
            writer.newLine();
            writer.write("</style>");
            writer.newLine();
            writer.write("</head>");
            writer.newLine();
            writer.write("<body>");
            writer.newLine();

            writer.write("<h1>" + Bundle.message("export.html.title") + "</h1>");
            writer.newLine();

            int totalExported = 0;

            for (final Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
                final @NotNull String sheetName = entry.getKey();
                final @NotNull List<TestCaseDto> testCases = entry.getValue();

                if (testCases.isEmpty()) continue;

                writer.write("<h2>" + StringUtil.escapeXmlEntities(sheetName) + "</h2>");
                writer.newLine();

                writer.write("<table>");
                writer.newLine();

                writer.write("<tr>");
                for (final TestSetEditorAttributes attribute : TestSetEditorAttributes.all(Can.EXPORT)) {
                    writer.write("<th>" + StringUtil.escapeXmlEntities(attribute.getName()) + "</th>");
                }
                writer.write("</tr>");
                writer.newLine();

                for (final TestCaseDto testCase : testCases) {
                    writer.write("<tr>");
                    for (final TestSetEditorAttributes attribute : TestSetEditorAttributes.all(Can.EXPORT)) {
                        writer.write("<td>" + StringUtil.escapeXmlEntities(attribute.gridValue(testCase)) + "</td>");
                    }
                    writer.write("</tr>");
                    writer.newLine();
                    totalExported++;
                }

                writer.write("</table>");
                writer.newLine();
            }

            writer.write("<p><em>" + Bundle.message("export.html.total", String.valueOf(totalExported)) + "</em></p>");
            writer.newLine();

            final @NotNull String exportDate = Display.formatDate(ZonedDateTime.now(ZoneId.systemDefault()));
            writer.write("<p><em>" + Bundle.message("export.html.on", StringUtil.escapeXmlEntities(exportDate)) + "</em></p>");
            writer.newLine();

            writer.write("</body>");
            writer.newLine();
            writer.write("</html>");
            writer.newLine();
        } catch (final IOException ex) {
            TestinLogger.error(FailureText.of(ex));
            throw new RuntimeException(ex);
        }
    }
}
