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

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.jetbrains.annotations.NotNull;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class WordLayoutTest {

    @Test
    public void anAutofitTableSurvivesBeingWrittenAndReadBack() {
        try (ByteArrayOutputStream saved = new ByteArrayOutputStream()) {
            try (XWPFDocument written = new XWPFDocument()) {
                final @NotNull XWPFTable table = written.createTable(2, 4);

                for (final XWPFTableRow row : table.getRows()) {
                    for (final XWPFTableCell cell : row.getTableCells()) {
                        cell.getCTTc().addNewTcPr().addNewTcW().setW(2500);
                    }
                }

                autoFit(table);
                written.write(saved);
            }

            try (XWPFDocument read = new XWPFDocument(new ByteArrayInputStream(saved.toByteArray()))) {
                final @NotNull XWPFTable table = read.getTables().getFirst();

                assertEquals(table.getCTTbl().getTblPr().getTblLayout().getType(), STTblLayoutType.AUTOFIT,
                        "the table should be marked autofit, or Word keeps the widths it was given");

                for (final XWPFTableRow row : table.getRows()) {
                    for (final XWPFTableCell cell : row.getTableCells()) {
                        assertEquals(cell.getCTTc().getTcPr().getTcW().getType(), STTblWidth.AUTO,
                                "every cell should ask for no particular width");
                        final @NotNull String cellXml = cell.getCTTc().getTcPr().xmlText();
                        assertEquals(cellXml.split("<w:tcW", -1).length - 1, 1,
                                "a cell with two width elements is invalid and Word refuses to open it: " + cellXml);
                    }
                }
            }

        } catch (final Exception cannotRun) {
            fail("Could not round-trip the table: " + cannotRun.getMessage(), cannotRun);
        }
    }

    private void autoFit(final @NotNull XWPFTable table) {
        final var properties = table.getCTTbl().getTblPr();
        final var layout = properties.isSetTblLayout() ? properties.getTblLayout() : properties.addNewTblLayout();
        layout.setType(STTblLayoutType.AUTOFIT);

        for (final XWPFTableRow row : table.getRows()) {
            for (final XWPFTableCell cell : row.getTableCells()) {
                final var cellProperties = cell.getCTTc().getTcPr() != null
                        ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
                (cellProperties.isSetTcW() ? cellProperties.getTcW() : cellProperties.addNewTcW())
                        .setType(STTblWidth.AUTO);
            }
        }
    }
}
